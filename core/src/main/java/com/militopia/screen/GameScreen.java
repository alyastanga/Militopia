package com.militopia.screen;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.militopia.MilitopiaGame;
import com.militopia.components.AbilitiesComponent;
import com.militopia.components.AnimalComponent;
import com.militopia.components.MovementComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.config.GameConfig;
import com.militopia.config.StructureType;
import com.militopia.config.UnitType;
import com.militopia.controller.GameInputController;
import com.militopia.systems.*;
import com.militopia.data.AnimalData;
import com.militopia.data.GameState;
import com.militopia.data.UnitData;
import com.militopia.factories.EntityFactory;
import com.militopia.factories.UnitFactory;
import com.militopia.data.TurnSnapshot;
import com.militopia.data.UnitSnapshot;
import com.militopia.data.StructureSnapshot;
import com.militopia.managers.SaveManager;
import com.militopia.managers.AudioManager;
import com.militopia.managers.TurnHistoryManager;
import com.militopia.map.MapGenerator;
import com.militopia.systems.CombatSystem;
import com.militopia.systems.FogSystem;
import com.militopia.systems.MapRenderSystem;
import com.militopia.systems.MovementSystem;
import com.militopia.systems.AnimationSystem;
import com.militopia.systems.UnitRenderSystem;
import com.militopia.systems.FloatingTextSystem;
import com.militopia.systems.AbilityStatusSystem;
import com.militopia.systems.StructureEconomySystem;
import com.militopia.ui.DevPanel;
import com.militopia.ui.GameHUD;
import com.militopia.utils.GameLogger;
import com.militopia.net.NetworkManager;
import com.militopia.net.NetworkMessage;
import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class GameScreen implements Screen {

    private final MilitopiaGame game;
    private final GameState gameState;
    private final NetworkManager networkManager;

    private OrthographicCamera camera;
    private ExtendViewport viewport;
    private PooledEngine engine;
    private MapGenerator.GameMap gameMap;

    private UnitFactory unitFactory;
    private EntityFactory entityFactory;
    private SaveManager saveManager;

    private MapRenderSystem mapRenderSystem;
    private UnitRenderSystem unitRenderSystem;
    private GameInputController inputController;
    public GameHUD gameHUD;
    public DevPanel devPanel;
    private AbilityStatusSystem abilityStatusSystem;
    private StructureEconomySystem structureEconomySystem;
    private WinConditionSystem winConditionSystem;
    private FogSystem fogSystem;
    private boolean isFogEnabled = true;
    private BitmapFont font;
    private TurnHistoryManager turnHistory = new TurnHistoryManager();
    private SnapshotRestorer snapshotRestorer;

    private enum TurnState {
        PLAYING, FADING_OUT, FADING_IN
    }

    private TurnState turnState = TurnState.PLAYING;
    private float fadeTime = 0f;
    private final float FADE_DURATION = 0.4f;
    private ShapeRenderer shapeRenderer;
    private boolean disconnectHandled = false;

    public GameScreen(final MilitopiaGame game, GameState loadedState) {
        this(game, loadedState, null);
    }

    public GameScreen(final MilitopiaGame game, GameState loadedState, NetworkManager networkManager) {
        this.game = game;
        this.gameState = loadedState;
        this.networkManager = networkManager;
        this.shapeRenderer = new ShapeRenderer();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

        viewport = new ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT, camera);

        engine = new PooledEngine();
        entityFactory = new EntityFactory(engine, game.assets);
        unitFactory = new UnitFactory(engine, game.assets);
        unitFactory.setEntityFactory(entityFactory);
        saveManager = new SaveManager();

        font = game.skin.getFont("default-font");
        font.getData().setScale(0.5f);

        MapGenerator generator = new MapGenerator();
        gameMap = generator.generateMap(loadedState.mapWidth, loadedState.mapHeight, loadedState.seed);
        if (loadedState.mapObjects != null) {
            gameMap.objects = loadedState.mapObjects;
        }
        if (loadedState.railGrid != null) {
            gameMap.rails = loadedState.railGrid;
        }

        centerCameraOnBase(gameState.currentPlayer);

        gameState.p1BaseCount = 0;
        gameState.p2BaseCount = 0;

        List<GridPoint2> initialBases = new ArrayList<>();

        for (int x = 0; x < gameMap.width; x++) {
            for (int y = 0; y < gameMap.height; y++) {
                MapGenerator.ObjectType type = gameMap.objects[x][y];
                if (type != null && type != MapGenerator.ObjectType.NONE) {
                    unitFactory.createObjectEntity(x, y, type, gameState);

                    if (type == MapGenerator.ObjectType.BASE_P1 || type == MapGenerator.ObjectType.BASE_P2) {
                        initialBases.add(new GridPoint2(x, y));
                    }
                }
            }
        }

        if (loadedState.animals != null && !loadedState.animals.isEmpty()) {
            GameLogger.logScreen("Loading " + loadedState.animals.size() + " saved animals.");
            for (AnimalData a : loadedState.animals) {
                if (a.type == null) {
                    GameLogger.logScreen("Null animal type in save — skipping");
                    continue;
                }
                MapGenerator.ObjectType type;
                try {
                    type = MapGenerator.ObjectType.valueOf(a.type);
                } catch (IllegalArgumentException ex) {
                    GameLogger.logScreen("Unknown animal type in save: " + a.type + " — skipping");
                    continue;
                }
                unitFactory.createObjectEntity(a.x, a.y, type, gameState);
            }
        } else {
            GameLogger.logScreen("Generating new animals for initial bases.");
            for (GridPoint2 pos : initialBases) {
                unitFactory.spawnAnimalsAroundBase(pos.x, pos.y, gameMap, gameState);
            }
        }

        engine.addSystem(new MovementSystem());
        engine.addSystem(new AnimationSystem());

        CombatSystem combatSystem = new CombatSystem(gameMap, entityFactory, gameState);
        engine.addSystem(combatSystem);
        engine.addSystem(new EffectSystem());

        // 3. Initialize fog/render systems
        // IMPORTANT: In LAN, we MUST lock these to the local player ID immediately.
        int localID = getActiveLocalPlayer();
        fogSystem = new FogSystem(gameMap, localID);
        unitRenderSystem = new UnitRenderSystem(game.batch, gameMap, font);
        unitRenderSystem.setPlayer(localID);
        unitRenderSystem.setShadowRegion(new com.badlogic.gdx.graphics.g2d.TextureRegion(
                game.assets.get(com.militopia.managers.AssetManager.SHADOW)));
        unitRenderSystem.setInvincibleRegions(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(
                        game.assets.get(com.militopia.managers.AssetManager.INVINCIBLE_RED)),
                new com.badlogic.gdx.graphics.g2d.TextureRegion(
                        game.assets.get(com.militopia.managers.AssetManager.INVINCIBLE_BLUE)));
        unitRenderSystem.setDigInRegion(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(
                        game.assets.get(com.militopia.managers.AssetManager.DIG_IN)));
        unitRenderSystem.setRecruitRunAnim(entityFactory.getRecruitRunAnim());
        unitRenderSystem.setRangerRunAnim(entityFactory.getRangerRunAnim());
        unitRenderSystem.setSniperRunAnim(entityFactory.getSniperRunAnim());
        unitRenderSystem.setJuggernautJumpAnim(entityFactory.getJuggernautJumpAnim());
        unitRenderSystem.setHelicopterMoveAnim(entityFactory.getHelicopterMoveAnim());
        unitFactory.setTankIdleAnim(entityFactory.getTankIdleAnim());
        unitRenderSystem.setJuggernautBoostersAnim(entityFactory.getJuggernautBoostersAnim());
        unitRenderSystem.setDroneMovingAnim(entityFactory.getDroneMovingAnim());

        mapRenderSystem = new MapRenderSystem(game.batch, unitFactory, gameMap, game.assets);

        engine.addSystem(unitRenderSystem);
        engine.addSystem(mapRenderSystem);
        engine.addSystem(fogSystem);

        abilityStatusSystem = new AbilityStatusSystem(gameMap);
        engine.addSystem(abilityStatusSystem);

        gameHUD = new GameHUD(game);

        structureEconomySystem = new StructureEconomySystem(loadedState, unitFactory, entityFactory, null);
        engine.addSystem(structureEconomySystem);
        winConditionSystem = new WinConditionSystem(loadedState, winnerID -> {
            boolean localWon = (winnerID == getActiveLocalPlayer());
            AudioManager.getInstance().playSFX(localWon
                    ? com.militopia.managers.SFXKeys.VICTORY
                    : com.militopia.managers.SFXKeys.DEFEAT);
            gameHUD.showGameOverPopup(winnerID);
        });
        engine.addSystem(winConditionSystem);

        engine.addSystem(new FloatingTextSystem());

        if (loadedState.units != null) {
            for (UnitData u : loadedState.units) {
                String key = (u.unitTypeKey != null) ? u.unitTypeKey : u.type;
                UnitType ut = UnitType.fromKey(key);
                if (ut == null)
                    ut = UnitType.RECRUIT;

                unitFactory.createUnit(ut, u.x, u.y, u.owner, u.hasActed);

                // Restore HP and moved flag
                Entity freshUnit = findUnitAt(u.x, u.y);
                if (freshUnit != null) {
                    StatsComponent s = freshUnit.getComponent(StatsComponent.class);
                    if (s != null) {
                        if (u.hp > 0)
                            s.currentHP = u.hp;
                        if (u.maxHp > 0)
                            s.maxHP = u.maxHp;
                        s.hasMoved = u.hasMoved;
                        s.hasActed = u.hasActed;

                        AbilitiesComponent ab = freshUnit.getComponent(AbilitiesComponent.class);
                        if (ab != null) {
                            ab.isCloaked = u.isCloaked;
                            ab.isCloakBroken = u.isCloakBroken;
                            ab.isDiggingIn = u.isDiggingIn;
                            ab.hasUsedDigIn = u.hasUsedDigIn;
                            ab.isOverwatchActive = u.isOverwatchActive;
                            ab.pendingSkirmishMove = u.pendingSkirmishMove;
                            ab.isUnreachable = u.isUnreachable;
                            if (u.fuel >= 0)
                                ab.fuel = u.fuel;
                            ab.idleTimer = u.idleTimer;
                        }
                    }
                }
            }
        }

        if (loadedState.structures != null) {
            for (com.militopia.data.StructureData s : loadedState.structures) {
                Entity e = findEntityAt(s.x, s.y);
                if (e == null) {
                    // Built structure (Solar, Hospital, Jammer, etc.) — not in gameMap.objects
                    // so no entity was created during map init. Recreate it now.
                    com.militopia.config.StructureType st = (s.unitTypeKey != null && !s.unitTypeKey.isEmpty())
                            ? com.militopia.config.StructureType.fromKey(s.unitTypeKey)
                            : com.militopia.config.StructureType.fromDisplayName(s.baseName);
                    if (st != null && st != com.militopia.config.StructureType.BASE) {
                        unitFactory.createStructure(st.getKey(), s.x, s.y, s.owner,
                                s.parentBaseX, s.parentBaseY);
                    }
                } else {
                    unitFactory.updateStructureFromSave(e, s, gameMap);
                }
            }
        }

        structureEconomySystem.setGameHUD(gameHUD);

        inputController = new GameInputController(
                this, camera, viewport, engine, gameMap, unitFactory, entityFactory, gameHUD, combatSystem);

        snapshotRestorer = new SnapshotRestorer(
                engine, gameState, gameMap, unitFactory,
                fogSystem, unitRenderSystem, gameHUD, inputController,
                turnHistory, structureEconomySystem
        );

        ScavengeSystem scavengeSystem = new ScavengeSystem(engine, unitFactory, entityFactory, gameState, gameMap);
        StructurePlacementSystem placementSystem = new StructurePlacementSystem(engine, unitFactory, gameState, gameMap);
        gameHUD.build(this, inputController, unitFactory, gameState, turnHistory, scavengeSystem, placementSystem);
        gameHUD.updateTurn(gameState.turnCount, gameState.currentPlayer, getActiveLocalPlayer());
        gameHUD.updateXP(1, gameState.p1XP);

        if (gameState.isDevMode) {
            devPanel = new DevPanel(game, gameHUD.stage, this, inputController, gameState);
            devPanel.build();
            gameHUD.setDevPanel(devPanel);
        }

        if (gameState.isLanGame && networkManager != null) {
            String localName = (gameState.localPlayerID == 1) ? gameState.p1Name : gameState.p2Name;
            gameHUD.buildChatPanel(networkManager, localName);
        }

        int startIncome = calculateIncome(gameState.currentPlayer);
        gameHUD.updateFunding(gameState.currentPlayer, gameState.p1Funding, startIncome);

        logBaseXPStatus();

        // --- Prime fog visibility AFTER all entities are spawned ---
        // Without this, the first render frame sees all-false visibleTiles
        // from a new boolean[][], causing incorrect fog state.
        fogSystem.update(0);

        // --- Snapshot the initial state so undo can rewind to turn 1 ---
        turnHistory.push(unitFactory.captureSnapshot(engine, gameState, gameMap));

        // --- NEW: Handle Finished Games on Load ---
        if (gameState.isGameOver) {
            GameLogger.logScreen("Loading a finished game. Showing Game Over popup.");
            gameHUD.showGameOverPopup(gameState.winnerID);
            winConditionSystem.setPlaying(false);
        }

        // Start the fade-in sequence when screen first loads
        turnState = TurnState.FADING_IN;
        fadeTime = FADE_DURATION;

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(gameHUD.stage);
        multiplexer.addProcessor(inputController);
        Gdx.input.setInputProcessor(multiplexer);
    }

    private Entity findEntityAt(int x, int y) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (pos.x == x && pos.y == y) {
                TypeComponent type = e.getComponent(TypeComponent.class);
                if (type != null && type.type == TypeComponent.Type.OBJECT
                        && pos.zIndex != 2
                        && e.getComponent(com.militopia.components.AnimalComponent.class) == null) {
                    return e;
                }
            }
        }
        return null;
    }

    private Entity findUnitAt(int x, int y) {
        ImmutableArray<Entity> entities = engine
                .getEntitiesFor(Family.all(GridPositionComponent.class, TypeComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TypeComponent type = e.getComponent(TypeComponent.class);
            if (pos.x == x && pos.y == y && type.type == TypeComponent.Type.UNIT) {
                return e;
            }
        }
        return null;
    }

    public PooledEngine getEngine() {
        return engine;
    }

    public EntityFactory getEntityFactory() {
        return entityFactory;
    }

    public TurnHistoryManager getTurnHistory() {
        return turnHistory;
    }

    public GameState getGameState() {
        return gameState;
    }

    public MapGenerator.GameMap getGameMap() {
        return gameMap;
    }

    public GameInputController getInputController() {
        return inputController;
    }

    public int getCurrentPlayer() {
        return gameState.currentPlayer;
    }

    public void endTurnAction() {
        if (turnState == TurnState.PLAYING) {
            // --- LAN LOCKDOWN ---
            // Only the owner of the current turn can trigger an end-turn action!
            if (gameState.currentPlayer != getActiveLocalPlayer()) {
                GameLogger.log(GameLogger.INPUT, "LAN: Ignored end-turn click (not your turn)");
                return;
            }

            GameLogger.log(GameLogger.ECONOMY,
                    "P" + gameState.currentPlayer + " ends turn " + gameState.turnCount);

            // --- LAN: Send FRESH snapshot to opponent before fading ---
            if (gameState.isLanGame && networkManager != null) {
                // Capture the board state AFTER all moves are done
                TurnSnapshot finalSnap = unitFactory.captureSnapshot(engine, gameState, gameMap);
                Json json = new Json();
                String snapJson = json.toJson(finalSnap);
                networkManager.send(NetworkMessage.endTurn(snapJson));
            }

            AudioManager.getInstance().playSFX(com.militopia.managers.SFXKeys.TURN_END_PLAYER);
            turnState = TurnState.FADING_OUT;
            fadeTime = 0f;
            inputController.setInputEnabled(false);
            winConditionSystem.setPlaying(false);
        }
    }

    private void centerCameraOnBase(int playerID) {
        MapGenerator.ObjectType targetBase = (playerID == 1) ? MapGenerator.ObjectType.BASE_P1
                : MapGenerator.ObjectType.BASE_P2;
        for (int x = 0; x < gameMap.width; x++) {
            for (int y = 0; y < gameMap.height; y++) {
                if (gameMap.objects[x][y] == targetBase) {
                    float isoX = (x - y) * (GameConfig.TILE_WIDTH / 2.0f);
                    float isoY = (x + y) * (GameConfig.TILE_HEIGHT / 2.0f);
                    camera.position.set(isoX, isoY, 0);
                    camera.zoom = 0.6f;
                    camera.update();
                    return;
                }
            }
        }
    }

    public void saveAndExit() {
        saveManager.saveGame(gameState, engine, gameMap);  // always save
        if (gameState.isLanGame && networkManager != null
                && networkManager.getState() == NetworkManager.State.CONNECTED) {
            networkManager.send(NetworkMessage.disconnect());
        }
        game.setScreen(new com.militopia.screen.MenuScreen(game));
    }

    private boolean isBlitz() { return gameState.mapWidth == 16; }

    private void handleTimerExpired(int losingPlayer) {
        if (gameState.isGameOver) return;
        if (gameState.isLanGame && networkManager != null
                && networkManager.getState() == NetworkManager.State.CONNECTED) {
            networkManager.send(NetworkMessage.timerOut());
        }
        int winnerID = (losingPlayer == 1) ? 2 : 1;
        gameState.isGameOver = true;
        gameState.winnerID = winnerID;
        AudioManager.getInstance().playSFX(winnerID == getActiveLocalPlayer()
                ? com.militopia.managers.SFXKeys.VICTORY
                : com.militopia.managers.SFXKeys.DEFEAT);
        gameHUD.showGameOverPopup(winnerID);
    }

    public boolean toggleFog() {
        isFogEnabled = !isFogEnabled;
        mapRenderSystem.setFogEnabled(isFogEnabled);
        unitRenderSystem.setFogEnabled(isFogEnabled);
        return isFogEnabled;
    }

    public boolean isFogEnabled() {
        return isFogEnabled;
    }

    private void resetUnitActions() {
        ImmutableArray<Entity> units = engine.getEntitiesFor(Family.all(StatsComponent.class).get());
        for (Entity entity : units) {
            StatsComponent stats = entity.getComponent(StatsComponent.class);
            if (stats.owner == gameState.currentPlayer) {
                stats.hasActed = false;
                stats.hasMoved = false;

                AbilitiesComponent ab = entity.getComponent(AbilitiesComponent.class);
                if (ab != null) {
                    ab.isCloakBroken = false;
                }
            }
        }
    }

    /**
     * Reverts the game to the start of the most recent turn.
     * Dead units are resurrected; HP, funding, XP, and map ownership are all
     * restored.
     * Does nothing if there is no history.
     */
    public void undoTurn() {
        TurnSnapshot snap = turnHistory.undo();
        if (snap == null)
            return;
        GameLogger.log(GameLogger.INPUT,
                "Undo — reverting to P" + snap.currentPlayer + " T" + snap.turn);
        snapshotRestorer.restore(snap);
    }

    /**
     * Jumps to the snapshot at the given index (0 = most recent) in the undo stack.
     */
    public void undoToSnapshot(int index) {
        TurnSnapshot snap = turnHistory.undoToIndex(index);
        if (snap == null)
            return;
        GameLogger.log(GameLogger.INPUT,
                "Jump to P" + snap.currentPlayer + " T" + snap.turn);
        snapshotRestorer.restore(snap);
    }

    /** Steps forward one turn (redo). */
    public void redoTurn() {
        TurnSnapshot snap = turnHistory.redo();
        if (snap == null)
            return;
        GameLogger.log(GameLogger.INPUT,
                "Redo — forward to P" + snap.currentPlayer + " T" + snap.turn);
        snapshotRestorer.restore(snap);
    }

    /**
     * Jumps to the snapshot at the given index (0 = next step forward) in the redo
     * stack.
     */
    public void redoToSnapshot(int index) {
        TurnSnapshot snap = turnHistory.redoToIndex(index);
        if (snap == null)
            return;
        GameLogger.log(GameLogger.INPUT,
                "Redo jump to P" + snap.currentPlayer + " T" + snap.turn);
        snapshotRestorer.restore(snap);
    }

    public int calculateBaseXPGain(Entity base) {
        return structureEconomySystem.calculateBaseXPGain(base);
    }

    public int calculateBaseIncome(Entity entity) {
        return structureEconomySystem.calculateBaseIncome(entity);
    }

    /**
     * Calculates the grouped income for a base, including all linked structures.
     * Used by InfoPanel for display.
     */
    public int calculateGroupedBaseIncome(Entity base) {
        return structureEconomySystem.calculateGroupedBaseIncome(base);
    }

    public int calculateIncome(int playerID) {
        return structureEconomySystem.calculateIncome(playerID);
    }

    private int[] processTurnEconomy(int playerID) {
        int totalIncome = calculateIncome(playerID);
        // XP distribution, Hospital healing, and base leveling are handled
        // by StructureEconomySystem to keep this screen thin.
        int totalXP = structureEconomySystem.processTurn(playerID);
        return new int[] { totalIncome, totalXP };
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.60f, 0.80f, 1.00f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (turnState == TurnState.FADING_OUT) {
            fadeTime += delta;
            if (fadeTime >= FADE_DURATION) {
                fadeTime = FADE_DURATION;
                // 1. Swap Player
                gameState.currentPlayer = (gameState.currentPlayer == 1) ? 2 : 1;
                if (gameState.currentPlayer == 1) {
                    gameState.turnCount++;
                }

                // 2. Process Economy, XP, and HUD for the NEW active player
                startActiveTurn();
            }
        } else if (turnState == TurnState.FADING_IN) {
            fadeTime -= delta;
            if (fadeTime <= 0) {
                fadeTime = 0;
                turnState = TurnState.PLAYING;
                // Only re-enable map input if the level-up popup isn't still on screen.
                // (processTurnEconomy may have shown a popup and already set
                // inputEnabled=false;
                // re-enabling here would silently override that lock.)
                if (!gameHUD.isLevelUpPopupVisible()) {
                    inputController.setInputEnabled(true);
                }
                winConditionSystem.setPlaying(true);
                // Snapshot the start of this new turn (before player acts)
                turnHistory.push(unitFactory.captureSnapshot(engine, gameState, gameMap));

                // Tutorial: auto-skip P2's turn so the player stays in control
                if (com.militopia.managers.TutorialManager.getInstance().isActive()
                        && gameState.currentPlayer == 2) {
                    endTurnAction();
                }
            }
        }

        if (isBlitz() && turnState == TurnState.PLAYING) {
            boolean networkPaused = gameState.isLanGame && networkManager != null
                    && networkManager.getState() == NetworkManager.State.DISCONNECTED;
            if (!networkPaused) {
                if (gameState.currentPlayer == 1) {
                    gameState.p1TimeLeft = Math.max(0, gameState.p1TimeLeft - delta);
                    if (gameState.p1TimeLeft == 0) handleTimerExpired(1);
                } else {
                    gameState.p2TimeLeft = Math.max(0, gameState.p2TimeLeft - delta);
                    if (gameState.p2TimeLeft == 0) handleTimerExpired(2);
                }
            }
            float displayTime = (gameState.currentPlayer == 1) ? gameState.p1TimeLeft : gameState.p2TimeLeft;
            gameHUD.updateTimer(displayTime, networkPaused);
        }

        inputController.update(delta);

        // --- LAN: Poll for incoming network messages ---
        if (gameState.isLanGame && networkManager != null) {
            pollNetwork();
        }

        if (gameState.isLanGame && networkManager != null
                && networkManager.getState() == NetworkManager.State.DISCONNECTED
                && !disconnectHandled) {
            disconnectHandled = true;
            saveManager.saveGame(gameState, engine, gameMap);
            gameHUD.showDisconnectPopup("Connection lost.\nGame has been saved.");
        }

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        mapRenderSystem.updateState(
                inputController.getHoveredX(), inputController.getHoveredY(),
                inputController.getBouncingX(), inputController.getBouncingY(),
                inputController.getBounceTimer(),
                inputController.getLastClickedX(), inputController.getLastClickedY());

        unitRenderSystem.updateState(
                inputController.getHoveredX(), inputController.getHoveredY(),
                inputController.getBouncingX(), inputController.getBouncingY(),
                inputController.getBounceTimer());

        engine.update(delta);

        gameHUD.render(delta);

        if (turnState != TurnState.PLAYING) {
            drawFadeOverlay();
        }
    }

    private void logBaseXPStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("       TURN ").append(gameState.turnCount).append(" STATUS REPORT       \n");
        sb.append("========================================\n");

        int p1Inc = calculateIncome(1);
        int p2Inc = calculateIncome(2);

        sb.append(String.format("%-15s | Funds: %3d | Inc: +%d\n",
                gameState.p1Name.toUpperCase(), gameState.p1Funding, p1Inc));

        sb.append(String.format("%-15s | Funds: %3d | Inc: +%d\n",
                gameState.p2Name.toUpperCase(), gameState.p2Funding, p2Inc));

        sb.append("----------------------------------------\n");
        sb.append("ACTIVE PLAYER : ")
                .append((gameState.currentPlayer == 1 ? gameState.p1Name : gameState.p2Name).toUpperCase())
                .append("\n");
        sb.append("----------------------------------------\n");

        List<String> p1Logs = new ArrayList<>();
        List<String> p2Logs = new ArrayList<>();

        ImmutableArray<Entity> entities = engine
                .getEntitiesFor(Family.all(StatsComponent.class, TypeComponent.class).get());

        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            TypeComponent type = e.getComponent(TypeComponent.class);

            if (type.type == TypeComponent.Type.OBJECT && (stats.owner == 1 || stats.owner == 2)) {

                // --- FIX: Only list Bases in the log ---
                if (stats.unitTypeKey != null && stats.unitTypeKey.startsWith("BASE")) {
                    int bInc = calculateGroupedBaseIncome(e);
                    String entry = String.format("  - %-25s (Lv %d) : %4.0f / %4.0f XP (+%d) | Inc: +%d",
                            stats.name, stats.level, stats.currentBaseXP, stats.maxBaseXP, this.calculateBaseXPGain(e),
                            bInc);
                    if (stats.owner == 1) {
                        p1Logs.add(entry);
                    } else {
                        p2Logs.add(entry);
                    }
                }
            }
        }

        Collections.sort(p1Logs);
        Collections.sort(p2Logs);

        sb.append(gameState.p1Name.toUpperCase()).append(" BASES:\n");
        if (p1Logs.isEmpty()) {
            sb.append("  (No Bases)\n");
        }
        for (String s : p1Logs) {
            sb.append(s).append("\n");
        }

        sb.append("\n").append(gameState.p2Name.toUpperCase()).append(" BASES:\n");
        if (p2Logs.isEmpty()) {
            sb.append("  (No Bases)\n");
        }
        for (String s : p2Logs) {
            sb.append(s).append("\n");
        }

        sb.append("========================================\n");
        GameLogger.logScreen(sb.toString());
    }

    /**
     * Common initialization logic when a new player's turn starts.
     * Handles economy calculations, XP distribution, HUD updates, and turn state
     * transitions.
     */
    private void startActiveTurn() {
        int grossIncome = calculateIncome(gameState.currentPlayer);
        int netIncome = grossIncome;

        // XP distribution, Hospital healing, and base leveling
        int xpGain = structureEconomySystem.processTurn(gameState.currentPlayer);

        int currentTotal = (gameState.currentPlayer == 1) ? gameState.p1Funding : gameState.p2Funding;

        // Apply net income (skip Turn 1 income if logic dictates)
        if (gameState.turnCount > 1) {
            if (gameState.currentPlayer == 1) {
                gameState.p1Funding = Math.max(0, gameState.p1Funding + netIncome);
                currentTotal = gameState.p1Funding;
            } else {
                gameState.p2Funding = Math.max(0, gameState.p2Funding + netIncome);
                currentTotal = gameState.p2Funding;
            }

            // Show economy popup ONLY if this is the hardware owner's turn
            if (gameState.currentPlayer == getActiveLocalPlayer()) {
                LinkedHashMap<String, Integer> incomeBreakdown = structureEconomySystem
                        .getIncomeBreakdown(gameState.currentPlayer);
                LinkedHashMap<String, Integer> xpBreakdown = structureEconomySystem
                        .getXPBreakdown(gameState.currentPlayer);

                AudioManager.getInstance().playSFX(com.militopia.managers.SFXKeys.UI_NOTIFICATION);
                gameHUD.showEconomyPopup(gameState.turnCount, netIncome, xpGain, currentTotal, incomeBreakdown,
                        xpBreakdown);
            }
        }

        // Update technical systems for the new player
        GameLogger.setContext(gameState.turnCount, gameState.currentPlayer);
        int currentXP = (gameState.currentPlayer == 1) ? gameState.p1XP : gameState.p2XP;
        GameLogger.log(GameLogger.ECONOMY,
                "Turn " + gameState.turnCount
                        + " | P" + gameState.currentPlayer + " starts"
                        + " | net_income=+" + netIncome
                        + " | funds=" + currentTotal);

        abilityStatusSystem.onTurnStart(gameState.currentPlayer);
        logBaseXPStatus();
        resetUnitActions();

        // Refresh UI
        gameHUD.updateTurn(gameState.turnCount, gameState.currentPlayer, getActiveLocalPlayer());
        gameHUD.updateXP(gameState.currentPlayer, currentXP);
        gameHUD.updateFunding(gameState.currentPlayer, currentTotal, netIncome);

        // --- VISIBILITY LOCKDOWN (LAN) ---
        // In LAN, the device always shows the LOCAL player's fog/knowledge.
        int localID = getActiveLocalPlayer();
        fogSystem.setPlayer(localID);
        fogSystem.update(0);
        AudioManager.getInstance().playSFX(com.militopia.managers.SFXKeys.FOG_REVEAL);
        unitRenderSystem.setPlayer(localID);

        // Only center camera if it's the LOCAL hardware's turn
        if (gameState.currentPlayer == localID) {
            centerCameraOnBase(gameState.currentPlayer);
        }

        // Turn start SFX
        if (gameState.currentPlayer == getActiveLocalPlayer()) {
            AudioManager.getInstance().playSFX(com.militopia.managers.SFXKeys.TURN_START_PLAYER);
        } else {
            AudioManager.getInstance().playSFX(com.militopia.managers.SFXKeys.TURN_START_ENEMY);
        }

        // Start the fade-in sequence
        turnState = TurnState.FADING_IN;
        fadeTime = FADE_DURATION;
    }

    private void drawFadeOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(gameHUD.stage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float alpha = Math.min(1.0f, Math.max(0.0f, fadeTime / FADE_DURATION));
        shapeRenderer.setColor(0, 0, 0, alpha);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        gameHUD.resize(width, height);
        if (devPanel != null)
            devPanel.resize();
    }

    @Override
    public void dispose() {
        if (font != null) {
            font.getData().setScale(1.0f);
        }
        engine.clearPools();
        gameHUD.dispose();
        shapeRenderer.dispose();
        if (networkManager != null) {
            networkManager.disconnect();
        }
    }

    // -------------------------------------------------------------------------
    // LAN Multiplayer
    // -------------------------------------------------------------------------

    private void pollNetwork() {
        NetworkMessage msg = networkManager.poll();
        if (msg == null)
            return;

        if (NetworkMessage.TYPE_END_TURN.equals(msg.type)) {
            GameLogger.log(GameLogger.INPUT, "LAN: Received END_TURN from opponent");

            // 1. Sync game state from snapshot
            Json json = new Json();
            TurnSnapshot snap = json.fromJson(TurnSnapshot.class, msg.payload);
            snapshotRestorer.restore(snap);

            // 2. The snapshot contains the state AFTER the opponent's moves,
            // but BEFORE the turn was swapped. We now advance the turn locally.
            gameState.currentPlayer = (gameState.currentPlayer == 1) ? 2 : 1;
            if (gameState.currentPlayer == 1) {
                gameState.turnCount++;
            }

            // 3. Process economy for the NEW active player and unlock HUD
            startActiveTurn();
        } else if (msg.type.startsWith("ACTION_") || msg.type.equals(NetworkMessage.TYPE_SYNC_ECONOMY) ||
                msg.type.equals(NetworkMessage.TYPE_ACTION_MOVE) || msg.type.equals(NetworkMessage.TYPE_ACTION_ATTACK)
                ||
                msg.type.equals(NetworkMessage.TYPE_ACTION_SUMMON) || msg.type.equals(NetworkMessage.TYPE_ACTION_BUILD)
                ||
                msg.type.equals(NetworkMessage.TYPE_ACTION_SCAVENGE)
                || msg.type.equals(NetworkMessage.TYPE_ACTION_CAPTURE) ||
                msg.type.equals(NetworkMessage.TYPE_ACTION_ABILITY)
                || msg.type.equals(NetworkMessage.TYPE_ACTION_DEMOLISH) ||
                msg.type.equals(NetworkMessage.TYPE_ACTION_DISBAND)) {
            processRemoteAction(msg);
        } else if (NetworkMessage.TYPE_DISCONNECT.equals(msg.type)) {
            GameLogger.log(GameLogger.INPUT, "LAN: Opponent disconnected");
            if (!disconnectHandled) {
                disconnectHandled = true;
                saveManager.saveGame(gameState, engine, gameMap);
                gameHUD.showDisconnectPopup("The opponent has disconnected.\nGame has been saved.");
            }
        } else if (NetworkMessage.TYPE_CHAT.equals(msg.type)) {
            int idx = msg.payload.indexOf(':');
            if (idx > 0) {
                int remoteID = (gameState.localPlayerID == 1) ? 2 : 1;
                gameHUD.addChatMessage(remoteID, msg.payload.substring(0, idx), msg.payload.substring(idx + 1));
            }
        } else if (NetworkMessage.TYPE_TIMER_OUT.equals(msg.type)) {
            int opponentID = (gameState.localPlayerID == 1) ? 2 : 1;
            handleTimerExpired(opponentID);
        }
    }

    private void processRemoteAction(NetworkMessage msg) {
        String[] p = msg.payload.split(",");
        GameLogger.log(GameLogger.INPUT, "LAN: Processing remote action " + msg.type + " | " + msg.payload);

        try {
            if (NetworkMessage.TYPE_ACTION_MOVE.equals(msg.type)) {
                // x,y,tx,ty
                int x = Integer.parseInt(p[0]);
                int y = Integer.parseInt(p[1]);
                int tx = Integer.parseInt(p[2]);
                int ty = Integer.parseInt(p[3]);
                Entity unit = findUnitAt(x, y);
                if (unit != null) {
                    GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);
                    unit.add(new MovementComponent(pos.x, pos.y, tx, ty));
                    pos.x = tx;
                    pos.y = ty;
                    StatsComponent stats = unit.getComponent(StatsComponent.class);
                    if (stats != null)
                        stats.hasMoved = true;
                }
            } else if (NetworkMessage.TYPE_ACTION_ATTACK.equals(msg.type)) {
                // ax,ay,dx,dy
                int ax = Integer.parseInt(p[0]);
                int ay = Integer.parseInt(p[1]);
                int dx = Integer.parseInt(p[2]);
                int dy = Integer.parseInt(p[3]);
                Entity attacker = findUnitAt(ax, ay);
                Entity defender = findUnitAt(dx, dy);
                if (defender == null)
                    defender = findStructureAt(dx, dy);
                if (attacker != null && defender != null) {
                    engine.getSystem(com.militopia.systems.CombatSystem.class).resolveAttack(attacker, defender);
                }
            } else if (NetworkMessage.TYPE_ACTION_SUMMON.equals(msg.type)) {
                // type,x,y,owner
                String typeKey = p[0];
                int x = Integer.parseInt(p[1]);
                int y = Integer.parseInt(p[2]);
                int owner = Integer.parseInt(p[3]);

                UnitType ut = com.militopia.config.UnitType.fromKey(typeKey);
                unitFactory.createUnit(ut, x, y, owner, true);

                // --- LAN FUNDING SYNC ---
                int cost = com.militopia.factories.UnitFactory.getUnitCost(ut);
                if (owner == 1)
                    gameState.p1Funding -= cost;
                else
                    gameState.p2Funding -= cost;

                int updatedFunds = (owner == 1) ? gameState.p1Funding : gameState.p2Funding;
                gameHUD.updateFunding(owner, updatedFunds, calculateIncome(owner));

                AudioManager.getInstance().playSFX(com.militopia.managers.SFXKeys.UNIT_DEPLOY);
            } else if (NetworkMessage.TYPE_ACTION_BUILD.equals(msg.type)) {
                // type,x,y,owner
                String typeKey = p[0];
                int x = Integer.parseInt(p[1]);
                int y = Integer.parseInt(p[2]);
                int owner = Integer.parseInt(p[3]);

                // --- LAN FUNDING SYNC ---
                int cost = unitFactory.getStructureCost(typeKey);
                gameHUD.getStructurePlacementSystem().performBuild(typeKey, x, y, owner, cost, x, y);

                int updatedFunds = (owner == 1) ? gameState.p1Funding : gameState.p2Funding;
                gameHUD.updateFunding(owner, updatedFunds, calculateIncome(owner));

                AudioManager.getInstance().playSFX(com.militopia.managers.SFXKeys.ACTION_BUILD);
            } else if (NetworkMessage.TYPE_ACTION_SCAVENGE.equals(msg.type)) {
                // x,y,owner
                int x = Integer.parseInt(p[0]);
                int y = Integer.parseInt(p[1]);
                Entity ruins = findObjectAt(x, y, com.militopia.map.MapGenerator.ObjectType.RUINS);
                if (ruins == null)
                    ruins = findObjectAt(x, y, com.militopia.map.MapGenerator.ObjectType.TREE);
                if (ruins == null)
                    ruins = findObjectAt(x, y, com.militopia.map.MapGenerator.ObjectType.CACTUS);

                if (ruins != null) {
                    gameHUD.getScavengeSystem().performScavenge(ruins, null); // unit=null is handled in ScavengeSystem
                                                                              // if we just want to remove and reward
                    AudioManager.getInstance().playSFX(com.militopia.managers.SFXKeys.ACTION_SCAVENGE);
                }
            } else if (NetworkMessage.TYPE_ACTION_CAPTURE.equals(msg.type)) {
                // x,y,newOwner
                int x = Integer.parseInt(p[0]);
                int y = Integer.parseInt(p[1]);
                int newOwner = Integer.parseInt(p[2]);
                Entity struct = findStructureAt(x, y);
                unitFactory.captureStructure(struct, newOwner, gameMap, gameState);
                AudioManager.getInstance().playSFX(com.militopia.managers.SFXKeys.STRUCTURE_CAPTURE);

                // Force refresh local income label to prevent stale state
                int local = getActiveLocalPlayer();
                gameHUD.updateFunding(local, (local == 1 ? gameState.p1Funding : gameState.p2Funding),
                        calculateIncome(local));
            } else if (NetworkMessage.TYPE_ACTION_DEMOLISH.equals(msg.type)) {
                // x,y,owner
                int x = Integer.parseInt(p[0]);
                int y = Integer.parseInt(p[1]);
                Entity struct = findStructureAt(x, y);
                if (struct != null) {
                    engine.removeEntity(struct);
                    AudioManager.getInstance().playSFX(com.militopia.managers.SFXKeys.ACTION_DEMOLISH);

                    // Force refresh local income label
                    int local = getActiveLocalPlayer();
                    gameHUD.updateFunding(local, (local == 1 ? gameState.p1Funding : gameState.p2Funding),
                            calculateIncome(local));
                }
            } else if (NetworkMessage.TYPE_ACTION_DISBAND.equals(msg.type)) {
                // x,y,owner
                int x = Integer.parseInt(p[0]);
                int y = Integer.parseInt(p[1]);
                Entity unit = findUnitAt(x, y);
                if (unit != null) {
                    engine.removeEntity(unit);
                }
            } else if (NetworkMessage.TYPE_SYNC_ECONOMY.equals(msg.type)) {
                // owner,funding,xp,income
                int owner = Integer.parseInt(p[0]);
                int funding = Integer.parseInt(p[1]);
                int xp = Integer.parseInt(p[2]);
                int income = Integer.parseInt(p[3]);
                if (owner == 1) {
                    gameState.p1Funding = funding;
                    gameState.p1XP = xp;
                } else {
                    gameState.p2Funding = funding;
                    gameState.p2XP = xp;
                }
                gameHUD.updateXP(owner, xp);
                gameHUD.updateFunding(owner, funding, income);
            } else if (NetworkMessage.TYPE_SYNC_BASE.equals(msg.type)) {
                // x,y,xp,level,owner
                int x = Integer.parseInt(p[0]);
                int y = Integer.parseInt(p[1]);
                int xp = Integer.parseInt(p[2]);
                int level = Integer.parseInt(p[3]);
                int owner = Integer.parseInt(p[4]);

                Entity base = findStructureAt(x, y);
                if (base != null) {
                    StatsComponent stats = base.getComponent(StatsComponent.class);
                    if (stats != null) {
                        int oldLevel = stats.level;
                        stats.currentBaseXP = xp;
                        stats.level = level;

                        // Retrieve level-up metadata for popup
                        com.militopia.config.BaseLevelConfig.LevelData data = com.militopia.config.BaseLevelConfig
                                .getLevel(level);
                        stats.maxBaseXP = data.maxXP;
                        stats.income = data.income;
                        stats.vision = data.borderRadius;

                        // If level increased, force a fog update because vision might have expanded
                        if (level > oldLevel) {
                            engine.getSystem(com.militopia.systems.FogSystem.class).update(0);
                            // Show level-up popup
                            gameHUD.showLevelUpPopup(owner, stats.name, level, data.fundingBonus, data.unlockedUnits,
                                    data.unlockedStructures, unitFactory);
                        }
                    }
                }
            }
        } catch (Exception e) {
            GameLogger.log(GameLogger.INPUT,
                    "LAN ERROR: Failed to process remote action: " + msg.type + " payload: " + msg.payload);
            e.printStackTrace();
        }
    }

    private Entity findStructureAt(int x, int y) {
        ImmutableArray<Entity> structs = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class, StatsComponent.class, TypeComponent.class).get());
        for (Entity e : structs) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TypeComponent type = e.getComponent(TypeComponent.class);
            if (pos.x == x && pos.y == y && type.type == TypeComponent.Type.OBJECT) {
                return e;
            }
        }
        return null;
    }

    private Entity findObjectAt(int x, int y, com.militopia.map.MapGenerator.ObjectType objType) {
        ImmutableArray<Entity> objs = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class, TypeComponent.class).get());
        for (Entity e : objs) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TypeComponent type = e.getComponent(TypeComponent.class);
            // Objects are identified by texture region usually, or we can check the map
            // array
            if (pos.x == x && pos.y == y && type.type == TypeComponent.Type.OBJECT) {
                // If it's the right type in the map array, it's probably this entity
                if (gameMap.objects[x][y] == objType)
                    return e;
            }
        }
        return null;
    }

    public void syncEconomy(int owner) {
        if (networkManager == null || !gameState.isLanGame)
            return;
        int funding = (owner == 1) ? gameState.p1Funding : gameState.p2Funding;
        int xp = (owner == 1) ? (int) gameState.p1XP : (int) gameState.p2XP;
        int income = calculateIncome(owner);
        networkManager.send(NetworkMessage.action(NetworkMessage.TYPE_SYNC_ECONOMY,
                owner + "," + funding + "," + xp + "," + income));

        // Sync individual base states for this owner
        ImmutableArray<Entity> bases = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class, StatsComponent.class).get());
        for (Entity b : bases) {
            StatsComponent s = b.getComponent(StatsComponent.class);
            if (s.owner == owner && s.unitTypeKey != null && s.unitTypeKey.startsWith("BASE")) {
                syncBaseState(b);
            }
        }
    }

    public void syncBaseState(Entity base) {
        if (networkManager == null || !gameState.isLanGame)
            return;
        GridPositionComponent pos = base.getComponent(GridPositionComponent.class);
        StatsComponent stats = base.getComponent(StatsComponent.class);
        if (pos == null || stats == null)
            return;

        networkManager.send(NetworkMessage.action(NetworkMessage.TYPE_SYNC_BASE,
                pos.x + "," + pos.y + "," + stats.currentBaseXP + "," + stats.level + "," + stats.owner));
    }

    // -------------------------------------------------------------------------
    // Dev Mode Actions
    // -------------------------------------------------------------------------

    public void devHealAll(int owner) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(
                Family.all(StatsComponent.class, TypeComponent.class).get());
        for (Entity e : entities) {
            TypeComponent t = e.getComponent(TypeComponent.class);
            StatsComponent s = e.getComponent(StatsComponent.class);
            if (t.type == TypeComponent.Type.UNIT && s.owner == owner) {
                s.currentHP = s.maxHP;
            }
        }
        GameLogger.log(GameLogger.INPUT, "[DEV] Healed all units for P" + owner);
    }

    public void devResetActions(int owner) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(
                Family.all(StatsComponent.class, TypeComponent.class).get());
        for (Entity e : entities) {
            TypeComponent t = e.getComponent(TypeComponent.class);
            StatsComponent s = e.getComponent(StatsComponent.class);
            if (t.type == TypeComponent.Type.UNIT && s.owner == owner) {
                s.hasActed = false;
                s.hasMoved = false;
            }
        }
        GameLogger.log(GameLogger.INPUT, "[DEV] Reset actions for P" + owner);
    }

    public void devAddFundsToPlayer(int owner, int amount) {
        if (owner == 1)
            gameState.p1Funding += amount;
        else
            gameState.p2Funding += amount;
        int updated = (owner == 1) ? gameState.p1Funding : gameState.p2Funding;
        gameHUD.updateFunding(owner, updated, calculateIncome(owner));
        GameLogger.log(GameLogger.INPUT, "[DEV] Added " + amount + " funds to P" + owner);
    }

    public void devAddXP(int amount, int owner) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(
                Family.all(StatsComponent.class, TypeComponent.class).get());
        for (Entity e : entities) {
            TypeComponent t = e.getComponent(TypeComponent.class);
            StatsComponent s = e.getComponent(StatsComponent.class);
            if (t.type == TypeComponent.Type.OBJECT && s.owner == owner && s.level > 0) {
                s.currentBaseXP += amount;
            }
        }
        GameLogger.log(GameLogger.INPUT, "[DEV] Added " + amount + " XP to P" + owner + " bases");
    }

    public void devSetBaseLevel(Entity base, int targetLevel) {
        StatsComponent s = base.getComponent(StatsComponent.class);
        GridPositionComponent pos = base.getComponent(GridPositionComponent.class);
        if (s == null || pos == null)
            return;
        com.militopia.config.BaseLevelConfig.LevelData data = com.militopia.config.BaseLevelConfig
                .getLevel(targetLevel);
        float prevXP = targetLevel > 1 ? com.militopia.config.BaseLevelConfig.getLevel(targetLevel - 1).maxXP : 0;
        s.level = targetLevel;
        s.income = data.income;
        s.currentBaseXP = prevXP;
        s.maxBaseXP = data.maxXP;
        unitFactory.updateBaseTexture(base, s);
        GameLogger.log(GameLogger.INPUT, "[DEV] Set base at (" + pos.x + "," + pos.y + ") to level " + targetLevel);
    }

    public void devToggleWinCondition(boolean enabled) {
        winConditionSystem.setPlaying(enabled);
        GameLogger.log(GameLogger.INPUT, "[DEV] WinCondition: " + (enabled ? "enabled" : "disabled"));
    }

    public void devOpenBaseLevelPicker(Entity base) {
        if (devPanel != null)
            devPanel.showBaseLevelPicker(base);
    }

    /**
     * Returns the player ID that owns the local hardware.
     * In LAN, this is fixed (1 or 2). In hotseat, it matches currentPlayer.
     */
    public int getActiveLocalPlayer() {
        if (gameState.isLanGame)
            return gameState.localPlayerID;
        return gameState.currentPlayer;
    }

    public MilitopiaGame getGame() {
        return game;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    @Override
    public void show() {
        AudioManager.getInstance().setBGMVolume(0.2f);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        if (font != null) {
            font.getData().setScale(1.0f);
        }
    }
}
