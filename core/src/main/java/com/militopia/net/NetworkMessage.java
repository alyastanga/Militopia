package com.militopia.net;

/**
 * Simple network message POJO for LAN multiplayer.
 * Serialized to/from JSON via libGDX Json.
 */
public class NetworkMessage {

    public static final String TYPE_GAME_INIT = "GAME_INIT";
    public static final String TYPE_END_TURN = "END_TURN";
    public static final String TYPE_DISCONNECT = "DISCONNECT";
    public static final String TYPE_PLAYER_NAME = "PLAYER_NAME";
    public static final String TYPE_PING          = "PING";
    public static final String TYPE_PONG          = "PONG";
    public static final String TYPE_PASSWORD      = "PASSWORD";
    public static final String TYPE_JOIN_REJECTED = "JOIN_REJECTED";
    public static final String TYPE_CHAT      = "CHAT";
    public static final String TYPE_TIMER_OUT = "TIMER_OUT";

    // --- REAL-TIME ACTIONS ---
    public static final String TYPE_ACTION_MOVE = "MOVE";
    public static final String TYPE_ACTION_ATTACK = "ATTACK";
    public static final String TYPE_ACTION_SUMMON = "SUMMON";
    public static final String TYPE_ACTION_BUILD = "BUILD";
    public static final String TYPE_ACTION_SCAVENGE = "SCAVENGE";
    public static final String TYPE_ACTION_CAPTURE = "CAPTURE";
    public static final String TYPE_ACTION_ABILITY = "ABILITY";
    public static final String TYPE_ACTION_DEMOLISH = "DEMOLISH";
    public static final String TYPE_ACTION_DISBAND = "DISBAND";
    public static final String TYPE_SYNC_ECONOMY = "SYNC_ECONOMY";
    public static final String TYPE_SYNC_BASE = "SYNC_BASE";

    public String type;
    public String payload;

    /** Default constructor required for JSON deserialization. */
    public NetworkMessage() {
    }

    public NetworkMessage(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public static NetworkMessage gameInit(String gameStateJson) {
        return new NetworkMessage(TYPE_GAME_INIT, gameStateJson);
    }

    public static NetworkMessage endTurn(String snapshotJson) {
        return new NetworkMessage(TYPE_END_TURN, snapshotJson);
    }

    public static NetworkMessage disconnect() {
        return new NetworkMessage(TYPE_DISCONNECT, "");
    }

    public static NetworkMessage playerName(String name) {
        return new NetworkMessage(TYPE_PLAYER_NAME, name);
    }

    public static NetworkMessage ping() { return new NetworkMessage(TYPE_PING, ""); }
    public static NetworkMessage pong() { return new NetworkMessage(TYPE_PONG, ""); }
    public static NetworkMessage password(String pw) { return new NetworkMessage(TYPE_PASSWORD, pw); }
    public static NetworkMessage joinRejected(String reason) { return new NetworkMessage(TYPE_JOIN_REJECTED, reason); }

    public static NetworkMessage chat(String senderName, String text) {
        return new NetworkMessage(TYPE_CHAT, senderName + ":" + text);
    }
    public static NetworkMessage timerOut() {
        return new NetworkMessage(TYPE_TIMER_OUT, "");
    }

    public static NetworkMessage action(String type, String payload) {
        return new NetworkMessage(type, payload);
    }
}
