package com.militopia.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.MilitopiaGame;
import com.militopia.data.GameState;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.managers.VideoBackgroundManager;
import com.militopia.net.NetworkManager;
import com.militopia.net.NetworkMessage;
import com.militopia.utils.GameLogger;
import com.militopia.utils.HoverListener;

/**
 * LAN Lobby screen — allows hosting or joining a LAN game.
 *
 * HOST FLOW:
 *   1. Player configures game (name, seed, mode) and clicks "Host Game"
 *   2. Screen shows the local IP and "Waiting for opponent..."
 *   3. On client connect → host sends GAME_INIT → both enter GameScreen
 *
 * JOIN FLOW:
 *   1. Player sees auto-discovered hosts or enters IP manually
 *   2. Clicks "Join" → connects to host → receives GAME_INIT → enters GameScreen
 */
public class LobbyScreen implements Screen {

    private enum LobbyState { CHOOSE, HOST_CONFIG, HOST_WAITING, JOIN_SCAN, HOST_RESUME }

    final MilitopiaGame game;
    Stage stage;
    LobbyState lobbyState = LobbyState.CHOOSE;

    // Host config
    TextField nameField, seedField, hostNameField;
    SelectBox<String> modeSelectBox;
    Label modeInfoLabel;
    int selectedWidth = 16;
    int selectedHeight = 16;

    // UI
    Table rootTable;
    Label statusLabel;
    Label errorLabel;

    // Join
    TextField ipField, clientNameField;

    // Name exchange state
    private String hostPlayerName = "Player 1";
    private String clientPlayerName = "Player 2";
    private boolean clientNameSent = false;
    private boolean clientNameReceived = false;
    private String receivedClientName = "Player 2";

    // Password / resume state
    private TextField hostPasswordField;
    private TextField joinPasswordField;
    private GameState resumeState = null;
    private boolean isResumeMode = false;
    private boolean passwordReceived = false;

    // Networking
    NetworkManager networkManager;
    private boolean transitioning = false;

    private final Json json = new Json();
    private Texture deleteIconTex;
    private TextureRegionDrawable deleteIconDrawable;
    private boolean isJoinResumeMode = false;

    public LobbyScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        deleteIconTex = new Texture(Gdx.files.internal("ui/circle_ui_delete.png"));
        deleteIconDrawable = new TextureRegionDrawable(new TextureRegion(deleteIconTex));
        Gdx.input.setInputProcessor(stage);
        showChooseScreen();
    }

    // =========================================================================
    // CHOOSE SCREEN (Host / Join)
    // =========================================================================

    private void showChooseScreen() {
        lobbyState = LobbyState.CHOOSE;
        stage.clear();

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Label title = new Label("LAN Game", game.skin);
        title.setFontScale(1.2f);

        TextButton hostBtn = new TextButton("Host Game", game.skin, "militopia-btn");
        hostBtn.addListener(new HoverListener());
        TextButton joinBtn = new TextButton("Join Game", game.skin, "militopia-btn");
        joinBtn.addListener(new HoverListener());
        TextButton resumeBtn = new TextButton("Resume LAN Game", game.skin, "militopia-btn");
        resumeBtn.addListener(new HoverListener());
        TextButton backBtn = new TextButton("Back", game.skin, "militopia-btn");
        backBtn.addListener(new HoverListener());

        rootTable.add(title).pad(20).colspan(2).row();
        rootTable.add(hostBtn).fillX().width(300).pad(10).row();
        rootTable.add(joinBtn).fillX().width(300).pad(10).row();
        rootTable.add(resumeBtn).fillX().width(300).pad(10).row();
        rootTable.add(backBtn).fillX().width(300).pad(10).row();

        hostBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                showHostConfig();
            }
        });

        joinBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                showJoinScan();
            }
        });

        resumeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                showResumeLanList();
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                game.setScreen(new MenuScreen(game));
            }
        });
    }

    // =========================================================================
    // HOST CONFIG SCREEN
    // =========================================================================

    private void showHostConfig() {
        lobbyState = LobbyState.HOST_CONFIG;
        stage.clear();

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Label title = new Label("Host a LAN Game", game.skin);
        title.setFontScale(1.0f);

        hostNameField = new TextField("", game.skin);
        hostNameField.setMessageText("Your Name");

        nameField = new TextField("", game.skin);
        nameField.setMessageText("Game Name");

        seedField = new TextField("", game.skin);
        seedField.setMessageText("Seed");

        // --- MODE SELECTOR ---
        modeSelectBox = new SelectBox<>(game.skin);
        modeSelectBox.setItems("Game mode", "Blitz", "Marathon");
        modeSelectBox.setSelected("Game mode");

        modeInfoLabel = new Label("16x16 map", game.skin);
        modeInfoLabel.setColor(Color.LIGHT_GRAY);
        modeInfoLabel.setFontScale(0.8f);
        modeInfoLabel.setVisible(false);

        modeSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selected = modeSelectBox.getSelected();
                if (selected.contains("Blitz")) {
                    modeInfoLabel.setText("16x16 map");
                    selectedWidth = 16; selectedHeight = 16;
                } else if (selected.contains("Marathon")) {
                    modeInfoLabel.setText("32x32 map");
                    selectedWidth = 32; selectedHeight = 32;
                } else {
                    modeInfoLabel.setText("16x16 map");
                    selectedWidth = 16; selectedHeight = 16;
                }
            }
        });

        modeSelectBox.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) modeInfoLabel.setVisible(true);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1) modeInfoLabel.setVisible(false);
            }
        });

        Label.LabelStyle errorStyle = new Label.LabelStyle(game.skin.getFont("default-font"), Color.RED);
        errorLabel = new Label("", errorStyle);

        TextButton startBtn = new TextButton("Start Hosting", game.skin, "militopia-btn");
        startBtn.addListener(new HoverListener());
        TextButton backBtn = new TextButton("Back", game.skin, "militopia-btn");
        backBtn.addListener(new HoverListener());

        rootTable.add(title).colspan(2).pad(15).row();
        hostPasswordField = new TextField("", game.skin);
        hostPasswordField.setMessageText("Password (optional)");

        rootTable.add(hostNameField).width(300).pad(10);
        rootTable.row();
        rootTable.add(nameField).width(300).pad(10);
        rootTable.row();
        rootTable.add(seedField).width(300).pad(10);
        rootTable.row();
        rootTable.add(hostPasswordField).width(300).pad(10);
        rootTable.row();

        // Add Mode Selection Row using balancing spacer trick
        Table modeTable = new Table();
        modeTable.add().width(110); 
        modeTable.add(modeSelectBox).width(300).pad(5);
        modeTable.add(modeInfoLabel).width(100).padLeft(10);
        
        rootTable.row().pad(10);
        rootTable.add(modeTable).left().row();

        rootTable.add(errorLabel).colspan(2).pad(10).row();

        rootTable.add(errorLabel).colspan(2).pad(10).row();

        Table btnRow = new Table();
        btnRow.add(backBtn).fillX().width(200).pad(10);
        btnRow.add(startBtn).fillX().width(200).pad(10);
        rootTable.add(btnRow).colspan(2).padTop(120).row();

        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (nameField.getText().trim().isEmpty() || seedField.getText().trim().isEmpty()) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    errorLabel.setText("All fields are required!");
                    return;
                }
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                String hn = hostNameField.getText().trim();
                hostPlayerName = hn.isEmpty() ? "Player 1" : hn;
                clientNameReceived = false;
                receivedClientName = "Player 2";
                isResumeMode = false;
                resumeState = null;
                passwordReceived = false;
                beginHosting();
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                showChooseScreen();
            }
        });
    }

    private void beginHosting() {
        lobbyState = LobbyState.HOST_WAITING;
        stage.clear();

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        String localIP = NetworkManager.getLocalIP();

        Label title = new Label("Waiting for Opponent...", game.skin);
        title.setFontScale(0.9f);
        Label ipLabel = new Label("Your IP: " + localIP, game.skin);
        ipLabel.setColor(Color.YELLOW);
        statusLabel = new Label("Broadcasting on LAN...", game.skin);
        statusLabel.setFontScale(0.7f);
        statusLabel.setColor(Color.LIGHT_GRAY);

        TextButton cancelBtn = new TextButton("Cancel", game.skin, "militopia-btn");
        cancelBtn.addListener(new HoverListener());

        rootTable.add(title).pad(20).row();
        rootTable.add(ipLabel).pad(10).row();
        rootTable.add(statusLabel).pad(10).row();
        rootTable.add(cancelBtn).fillX().width(200).pad(20).row();

        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                if (networkManager != null) networkManager.disconnect();
                showChooseScreen();
            }
        });

        // Start networking
        networkManager = new NetworkManager();
        networkManager.startHost(isResumeMode);
    }

    // =========================================================================
    // RESUME LAN GAME SCREENS
    // =========================================================================

    private void showResumeLanList() {
        lobbyState = LobbyState.HOST_RESUME;
        stage.clear();

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Label title = new Label("Resume LAN Game", game.skin);
        title.setFontScale(1.0f);
        rootTable.add(title).colspan(2).pad(15).row();

        FileHandle dir = Gdx.files.local("saves/");
        if (!dir.exists()) dir.mkdirs();
        FileHandle[] files = dir.list(".json");

        boolean anyFound = false;
        for (FileHandle file : files) {
            try {
                final GameState s = json.fromJson(GameState.class, file.readString());
                if (!s.isLanGame || s.isGameOver) continue;
                anyFound = true;

                Table rowTable = new Table();

                Table entry = new Table();
                entry.setBackground(game.skin.get("militopia-btn", com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle.class).up);

                String info = (s.saveName != null ? s.saveName : "Unnamed")
                        + "   |   " + s.p1Name + " vs " + s.p2Name
                        + "   |   Turn " + s.turnCount;
                Label lbl = new Label(info, game.skin);
                lbl.setFontScale(0.8f);
                entry.add(lbl).left().pad(8);

                entry.addListener(new HoverListener());
                entry.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                        showResumeHostConfig(s);
                    }
                });

                ImageButton delBtn = new ImageButton(deleteIconDrawable);
                delBtn.addListener(new HoverListener());
                delBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                        file.delete();
                        showResumeLanList();
                    }
                });

                rowTable.add(entry).fillX().expandX().padRight(10);
                rowTable.add(delBtn).size(80);

                rootTable.add(rowTable).fillX().width(500).pad(5).row();
            } catch (Exception ignored) {}
        }

        if (!anyFound) {
            rootTable.add(new Label("No resumable LAN saves found.", game.skin)).colspan(2).pad(10).row();
        }

        TextButton backBtn = new TextButton("Back", game.skin, "militopia-btn");
        backBtn.addListener(new HoverListener());
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                showChooseScreen();
            }
        });
        rootTable.add(backBtn).fillX().width(300).pad(20).row();
    }

    private void showResumeHostConfig(GameState selectedSave) {
        stage.clear();

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Label title = new Label("Resume: " + selectedSave.saveName, game.skin);
        title.setFontScale(1.0f);
        rootTable.add(title).colspan(2).pad(15).row();

        Label info = new Label(selectedSave.p1Name + " vs " + selectedSave.p2Name
                + "   |   Turn " + selectedSave.turnCount, game.skin);
        info.setFontScale(0.8f);
        info.setColor(Color.LIGHT_GRAY);
        rootTable.add(info).colspan(2).pad(5).row();

        hostPasswordField = new TextField(selectedSave.lanPassword != null ? selectedSave.lanPassword : "", game.skin);
        hostPasswordField.setMessageText("Password (optional)");
        rootTable.add(new Label("Password:", game.skin)).right().pad(5);
        rootTable.add(hostPasswordField).width(200).pad(5).row();

        Label.LabelStyle errorStyle = new Label.LabelStyle(game.skin.getFont("default-font"), Color.RED);
        errorLabel = new Label("", errorStyle);
        rootTable.add(errorLabel).colspan(2).pad(5).row();

        TextButton startBtn = new TextButton("Start Hosting", game.skin, "militopia-btn");
        startBtn.addListener(new HoverListener());
        TextButton backBtn = new TextButton("Back", game.skin, "militopia-btn");
        backBtn.addListener(new HoverListener());

        Table btnRow = new Table();
        btnRow.add(backBtn).fillX().width(200).pad(10);
        btnRow.add(startBtn).fillX().width(200).pad(10);
        rootTable.add(btnRow).colspan(2).padTop(40).row();

        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                resumeState = selectedSave;
                resumeState.lanPassword = hostPasswordField.getText().trim();
                isResumeMode = true;
                passwordReceived = false;
                hostPlayerName = selectedSave.p1Name;
                clientNameReceived = false;
                receivedClientName = selectedSave.p2Name;
                beginHosting();
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                showResumeLanList();
            }
        });
    }

    // =========================================================================
    // JOIN SCREEN
    // =========================================================================

    private void showJoinScan() {
        lobbyState = LobbyState.JOIN_SCAN;
        stage.clear();

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Label title = new Label("Join a LAN Game", game.skin);
        title.setFontScale(1.0f);

        statusLabel = new Label("Scanning for hosts...", game.skin);
        statusLabel.setFontScale(0.7f);
        statusLabel.setColor(Color.LIGHT_GRAY);

        clientNameField = new TextField("", game.skin);
        clientNameField.setMessageText("Your Name");

        ipField = new TextField("", game.skin);
        ipField.setMessageText("Enter Host IP");

        TextButton connectBtn = new TextButton("Connect", game.skin, "militopia-btn");
        connectBtn.addListener(new HoverListener());
        TextButton backBtn = new TextButton("Back", game.skin, "militopia-btn");
        backBtn.addListener(new HoverListener());

        Label.LabelStyle errorStyle = new Label.LabelStyle(game.skin.getFont("default-font"), Color.RED);
        errorLabel = new Label("", errorStyle);

        joinPasswordField = new TextField("", game.skin);
        joinPasswordField.setMessageText("Password if resuming");

        rootTable.add(title).colspan(2).pad(15).row();
        rootTable.add(clientNameField).colspan(2).width(300).pad(10).row();
        rootTable.add(statusLabel).colspan(2).pad(10).row();
        rootTable.add(new Label("Host IP:", game.skin)).right().pad(5);
        rootTable.add(ipField).width(200).pad(5).row();
        rootTable.add(new Label("Password:", game.skin)).right().pad(5);
        rootTable.add(joinPasswordField).width(200).pad(5).row();
        rootTable.add(errorLabel).colspan(2).pad(10).row();
        
        isJoinResumeMode = false; 
        clientNameField.setVisible(true);

        Table btnRow = new Table();
        btnRow.add(backBtn).fillX().width(150).pad(10);
        btnRow.add(connectBtn).fillX().width(150).pad(10);
        rootTable.add(btnRow).colspan(2).row();

        connectBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String ip = ipField.getText().trim();
                if (ip.isEmpty()) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    errorLabel.setText("Enter the host's IP address!");
                    return;
                }
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                attemptConnect(ip);
            }
        });

        InputListener enterListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Keys.ENTER || keycode == Keys.NUMPAD_ENTER) {
                    String ip = ipField.getText().trim();
                    if (ip.isEmpty()) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        errorLabel.setText("Enter the host's IP address!");
                    } else {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                        attemptConnect(ip);
                    }
                    return true;
                }
                return false;
            }
        };
        clientNameField.addListener(enterListener);
        ipField.addListener(enterListener);
        joinPasswordField.addListener(enterListener);

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                if (networkManager != null) networkManager.disconnect();
                showChooseScreen();
            }
        });

        // Start LAN discovery
        networkManager = new NetworkManager();
        networkManager.startDiscoveryListener();
    }

    private void attemptConnect(String ip) {
        statusLabel.setText("Connecting to " + ip + "...");
        statusLabel.setColor(Color.YELLOW);
        errorLabel.setText("");

        String cn = clientNameField.getText().trim();
        if (isJoinResumeMode) {
            clientPlayerName = ""; // Host will use name from save
        } else {
            clientPlayerName = cn.isEmpty() ? "Player 2" : cn;
        }
        clientNameSent = false;

        if (networkManager != null) networkManager.disconnect();
        networkManager = new NetworkManager();
        networkManager.connectToHost(ip);
    }

    // =========================================================================
    // Render / Update
    // =========================================================================

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        VideoBackgroundManager.getInstance().render(game.batch);

        // --- Network state polling ---
        if (networkManager != null && !transitioning) {

            // HOST: waiting for connection → wait for client name (+ password if resume) → send GAME_INIT
            if (lobbyState == LobbyState.HOST_WAITING) {
                if (networkManager.getState() == NetworkManager.State.CONNECTED) {
                    NetworkMessage nameMsg = networkManager.poll();
                    if (nameMsg != null) {
                        if (!clientNameReceived && NetworkMessage.TYPE_PLAYER_NAME.equals(nameMsg.type)) {
                            String payload = nameMsg.payload.trim();
                            if (!payload.isEmpty()) {
                                receivedClientName = payload;
                            } else if (!isResumeMode) {
                                receivedClientName = "Player 2";
                            }
                            // if empty AND isResumeMode, we keep the one initialized in showResumeHostConfig
                            clientNameReceived = true;
                        } else if (isResumeMode && clientNameReceived && !passwordReceived
                                && NetworkMessage.TYPE_PASSWORD.equals(nameMsg.type)) {
                            String expected = resumeState.lanPassword != null ? resumeState.lanPassword : "";
                            String received = nameMsg.payload != null ? nameMsg.payload.trim() : "";
                            if (expected.isEmpty() || expected.equals(received)) {
                                passwordReceived = true;
                            } else {
                                networkManager.send(NetworkMessage.joinRejected("Wrong password."));
                                networkManager.disconnect();
                                clientNameReceived = false;
                                passwordReceived = false;
                                statusLabel.setText("Wrong password. Waiting again...");
                                networkManager = new NetworkManager();
                                networkManager.startHost(isResumeMode);
                            }
                        }
                    }
                }

                boolean readyToTransition = clientNameReceived && (!isResumeMode || passwordReceived);

                if (readyToTransition && !transitioning) {
                    transitioning = true;
                    GameLogger.logScreen("LAN: Client connected, sending game init");

                    GameState stateToSend;
                    if (isResumeMode) {
                        stateToSend = resumeState;
                        stateToSend.p2Name = receivedClientName;
                        stateToSend.localPlayerID = 1;
                        stateToSend.isLanGame = true;
                    } else {
                        long seed;
                        try {
                            seed = Long.parseLong(seedField.getText());
                        } catch (Exception e) {
                            seed = seedField.getText().hashCode();
                        }
                        stateToSend = new GameState(seed, nameField.getText() + "_LAN", selectedWidth, selectedHeight);
                        stateToSend.isLanGame = true;
                        stateToSend.localPlayerID = 1;
                        stateToSend.p1Name = hostPlayerName;
                        stateToSend.p2Name = receivedClientName;
                        stateToSend.lanPassword = hostPasswordField != null ? hostPasswordField.getText().trim() : "";
                        if (stateToSend.mapWidth == 16) {
                            stateToSend.p1TimeLeft = 600f;
                            stateToSend.p2TimeLeft = 600f;
                        }
                    }

                    String stateJson = json.toJson(stateToSend);
                    networkManager.send(NetworkMessage.gameInit(stateJson));

                    game.setScreen(new GameScreen(game, stateToSend, networkManager));
                    return;
                }
                if (networkManager.getState() == NetworkManager.State.ERROR) {
                    statusLabel.setText("Error: " + networkManager.getErrorMessage());
                    statusLabel.setColor(Color.RED);
                }
            }

            // JOIN: check for auto-discovered host IP
            if (lobbyState == LobbyState.JOIN_SCAN) {
                String discovered = networkManager.getDiscoveredHostIP();
                if (discovered != null && (ipField.getText().isEmpty())) {
                    ipField.setText(discovered);
                    statusLabel.setText("Found host (" + (networkManager.isDiscoveredHostResume() ? "RESUME" : "NEW") + "): " + discovered);
                    statusLabel.setColor(Color.GREEN);
                    
                    // Auto-update resume mode based on discovery
                    isJoinResumeMode = networkManager.isDiscoveredHostResume();
                    clientNameField.setVisible(!isJoinResumeMode);
                }

                if (networkManager.getState() == NetworkManager.State.CONNECTED) {
                    if (!clientNameSent) {
                        networkManager.send(NetworkMessage.playerName(clientPlayerName));
                        networkManager.send(NetworkMessage.password(
                                joinPasswordField != null ? joinPasswordField.getText().trim() : ""));
                        clientNameSent = true;
                    }
                    statusLabel.setText("Connected! Waiting for game data...");
                    statusLabel.setColor(Color.GREEN);
                }

                if (networkManager.getState() == NetworkManager.State.ERROR) {
                    statusLabel.setText("Connection failed");
                    statusLabel.setColor(Color.RED);
                    errorLabel.setText(networkManager.getErrorMessage());
                }

                // Check for incoming messages
                NetworkMessage msg = networkManager.poll();
                if (msg != null && NetworkMessage.TYPE_GAME_INIT.equals(msg.type)) {
                    transitioning = true;
                    GameLogger.logScreen("LAN: Received game init from host");

                    GameState receivedState = json.fromJson(GameState.class, msg.payload);
                    receivedState.isLanGame = true;
                    receivedState.localPlayerID = 2; // Client is always Player 2

                    game.setScreen(new GameScreen(game, receivedState, networkManager));
                    return;
                } else if (msg != null && NetworkMessage.TYPE_JOIN_REJECTED.equals(msg.type)) {
                    errorLabel.setText("Rejected: " + msg.payload);
                    statusLabel.setText("Wrong password.");
                    statusLabel.setColor(Color.RED);
                    networkManager.disconnect();
                    clientNameSent = false;
                }
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
        GameLogger.logScreen("LAN Lobby opened");
        VideoBackgroundManager.getInstance().play();
    }

    @Override
    public void hide() {
        VideoBackgroundManager.getInstance().pause();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (networkManager != null && !transitioning) {
            networkManager.disconnect();
        }
        if (deleteIconTex != null) deleteIconTex.dispose();
    }

    private void addInputRow(Table t, String labelText, TextField field) {
        t.add(new Label(labelText, game.skin)).right().pad(5);
        t.add(field).width(200).pad(5);
        t.row();
    }
}
