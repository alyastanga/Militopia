package com.militopia.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages TCP socket connections for LAN multiplayer.
 * <p>
 * The host calls {@link #startHost()} to listen for a client on port {@value #PORT}.
 * The client calls {@link #connectToHost(String)} with the host's IP.
 * <p>
 * Messages are exchanged as single-line JSON strings. A background reader thread
 * enqueues incoming messages into a thread-safe queue, which is polled on the
 * render thread via {@link #poll()}.
 * <p>
 * LAN discovery uses UDP broadcast on port {@value #DISCOVERY_PORT}.
 */
public class NetworkManager {

    private static final String TAG = "NetworkManager";
    public static final int PORT = 7777;
    public static final int DISCOVERY_PORT = 7778;
    private static final String DISCOVERY_MAGIC = "MILITOPIA_LAN";

    public enum Role { HOST, CLIENT }
    public enum State { IDLE, WAITING, CONNECTED, DISCONNECTED, ERROR }

    private Role role;
    private volatile State state = State.IDLE;
    private volatile String errorMessage = "";

    // TCP
    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread readerThread;

    // UDP discovery
    private DatagramSocket discoverySocket;
    private Thread discoveryThread;
    private volatile String discoveredHostIP = null;

    // Thread-safe incoming message queue (polled on render thread)
    private final ConcurrentLinkedQueue<NetworkMessage> incomingQueue = new ConcurrentLinkedQueue<>();

    private final Json json = new Json();

    // -------------------------------------------------------------------------
    // Host
    // -------------------------------------------------------------------------

    /**
     * Starts listening for a client connection in a background thread.
     * State transitions: IDLE → WAITING → CONNECTED.
     */
    public void startHost() {
        role = Role.HOST;
        state = State.WAITING;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));
                Gdx.app.log(TAG, "Host listening on port " + PORT);

                // Start UDP broadcast so clients can discover us
                startDiscoveryBroadcast();

                socket = serverSocket.accept();
                Gdx.app.log(TAG, "Client connected: " + socket.getInetAddress());

                setupStreams();
                state = State.CONNECTED;
                stopDiscovery();
                startReaderThread();

            } catch (IOException e) {
                if (state != State.DISCONNECTED) {
                    Gdx.app.error(TAG, "Host error", e);
                    errorMessage = e.getMessage();
                    state = State.ERROR;
                }
            }
        }, "NetworkHost").start();
    }

    /**
     * Broadcasts a UDP discovery packet every second so LAN clients can find us.
     */
    private void startDiscoveryBroadcast() {
        discoveryThread = new Thread(() -> {
            try {
                discoverySocket = new DatagramSocket();
                discoverySocket.setBroadcast(true);
                byte[] data = DISCOVERY_MAGIC.getBytes();
                InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");

                while (state == State.WAITING) {
                    DatagramPacket packet = new DatagramPacket(data, data.length,
                            broadcastAddr, DISCOVERY_PORT);
                    discoverySocket.send(packet);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                // Expected when socket is closed or state changes
            } finally {
                if (discoverySocket != null && !discoverySocket.isClosed()) {
                    discoverySocket.close();
                }
            }
        }, "DiscoveryBroadcast");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    // -------------------------------------------------------------------------
    // Client
    // -------------------------------------------------------------------------

    /**
     * Connects to a host at the given IP in a background thread.
     * State transitions: IDLE → WAITING → CONNECTED.
     */
    public void connectToHost(final String hostIP) {
        role = Role.CLIENT;
        state = State.WAITING;

        new Thread(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(hostIP, PORT), 5000);
                Gdx.app.log(TAG, "Connected to host: " + hostIP);

                setupStreams();
                state = State.CONNECTED;
                startReaderThread();

            } catch (IOException e) {
                Gdx.app.error(TAG, "Client connect error", e);
                errorMessage = e.getMessage();
                state = State.ERROR;
            }
        }, "NetworkClient").start();
    }

    /**
     * Starts listening for UDP discovery broadcasts from a host.
     * When found, sets {@link #discoveredHostIP}.
     */
    public void startDiscoveryListener() {
        discoveredHostIP = null;
        discoveryThread = new Thread(() -> {
            try {
                discoverySocket = new DatagramSocket(DISCOVERY_PORT);
                discoverySocket.setSoTimeout(2000);
                byte[] buf = new byte[256];

                while (discoveredHostIP == null && state != State.CONNECTED && state != State.DISCONNECTED) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        discoverySocket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());
                        if (DISCOVERY_MAGIC.equals(msg)) {
                            discoveredHostIP = packet.getAddress().getHostAddress();
                            Gdx.app.log(TAG, "Discovered host at: " + discoveredHostIP);
                        }
                    } catch (SocketTimeoutException e) {
                        // Keep listening
                    }
                }
            } catch (Exception e) {
                Gdx.app.error(TAG, "Discovery listener error", e);
            } finally {
                if (discoverySocket != null && !discoverySocket.isClosed()) {
                    discoverySocket.close();
                }
            }
        }, "DiscoveryListener");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    public String getDiscoveredHostIP() {
        return discoveredHostIP;
    }

    // -------------------------------------------------------------------------
    // Shared I/O
    // -------------------------------------------------------------------------

    private void setupStreams() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void startReaderThread() {
        readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    NetworkMessage msg = json.fromJson(NetworkMessage.class, line);
                    if (msg != null) {
                        incomingQueue.add(msg);
                        Gdx.app.log(TAG, "Received: " + msg.type);

                        if (NetworkMessage.TYPE_DISCONNECT.equals(msg.type)) {
                            state = State.DISCONNECTED;
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                if (state != State.DISCONNECTED) {
                    Gdx.app.log(TAG, "Connection lost");
                    state = State.DISCONNECTED;
                }
            }
        }, "NetworkReader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sends a message to the connected peer.
     * Safe to call from any thread.
     */
    public void send(NetworkMessage message) {
        if (out == null || state != State.CONNECTED) return;
        String line = json.toJson(message);
        // Replace newlines to keep the message on a single line
        line = line.replace("\n", " ").replace("\r", " ");
        out.println(line);
        Gdx.app.log(TAG, "Sent: " + message.type);
    }

    /**
     * Polls the next incoming message (non-blocking).
     * Call this from the render thread every frame.
     *
     * @return the next message, or null if the queue is empty
     */
    public NetworkMessage poll() {
        return incomingQueue.poll();
    }

    public State getState() {
        return state;
    }

    public Role getRole() {
        return role;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isHost() {
        return role == Role.HOST;
    }

    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    /**
     * Returns the local IP address suitable for display to the user.
     */
    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Gdx.app.error(TAG, "Could not get local IP", e);
        }
        return "Unknown";
    }

    /**
     * Shuts down all sockets and threads gracefully.
     */
    public void disconnect() {
        state = State.DISCONNECTED;
        stopDiscovery();
        try {
            if (out != null) {
                // Send disconnect notice before closing
                try {
                    String line = json.toJson(NetworkMessage.disconnect());
                    out.println(line);
                } catch (Exception ignored) {}
            }
            if (socket != null && !socket.isClosed()) socket.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            Gdx.app.error(TAG, "Disconnect error", e);
        }
    }

    private void stopDiscovery() {
        if (discoverySocket != null && !discoverySocket.isClosed()) {
            discoverySocket.close();
        }
    }
}
