package com.militopia.controller;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.militopia.config.GameConfig;
import com.militopia.config.StructureType;
import com.militopia.config.UnitType;
import com.militopia.components.*;
import com.militopia.data.GameState;
import com.militopia.net.NetworkMessage;
import com.militopia.factories.EntityFactory;
import com.militopia.factories.UnitFactory;
import com.militopia.map.MapGenerator;
import com.militopia.screen.GameScreen;
import com.militopia.systems.CombatSystem;
import com.militopia.ui.GameHUD;
import com.militopia.utils.GameLogger;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.managers.TutorialManager;
import java.util.ArrayList;
import java.util.List;

public class GameInputController extends InputAdapter {

    private final GameScreen screen;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final PooledEngine engine;
    private final MapGenerator.GameMap gameMap;
    private final UnitFactory unitFactory;
    private final EntityFactory entityFactory;
    private final GameHUD gameHUD;
    private final CombatSystem combatSystem;

    private final UnitSelectionHandler selectionHandler;

    private int lastTouchX, lastTouchY;
    private int hoveredX = -1, hoveredY = -1;
    private boolean inputEnabled = true;

    private boolean isTargetingAbility = false;
    private String targetingAbilityKey = null;
    private Entity targetingUnit = null;

    // Dev Mode
    public enum DevSpawnMode {
        NONE, SPAWNING_UNIT, BUILDING_STRUCTURE, KILLING_UNIT, REMOVING_OBJECT, BUILDING_MAP_OBJ, SETTING_BASE_LEVEL
    }

    private enum ClickTargetType { UNIT, ANIMAL, STRUCTURE, TERRAIN }

    private DevSpawnMode devSpawnMode = DevSpawnMode.NONE;
    private UnitType devSpawnUnitType;
    private StructureType devSpawnStructureType;
    private String devSpawnMapObjType;
    public int devSpawnOwner = 1;

    public GameInputController(GameScreen screen, OrthographicCamera camera, Viewport viewport,
            PooledEngine engine, MapGenerator.GameMap gameMap, UnitFactory unitFactory,
            EntityFactory entityFactory, GameHUD gameHUD, CombatSystem combatSystem) {
        this.screen = screen;
        this.camera = camera;
        this.viewport = viewport;
        this.engine = engine;
        this.gameMap = gameMap;
        this.unitFactory = unitFactory;
        this.entityFactory = entityFactory;
        this.gameHUD = gameHUD;
        this.combatSystem = combatSystem;
        this.selectionHandler = new UnitSelectionHandler(screen, engine, gameMap, unitFactory, entityFactory, gameHUD, combatSystem);
    }

    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
        if (!enabled) {
            deselect();
        }
    }

    public void enterDevSpawn(DevSpawnMode mode, UnitType type, int owner) {
        devSpawnMode = mode;
        devSpawnUnitType = type;
        devSpawnOwner = owner;
    }

    public void enterDevBuild(String type, int owner) {
        devSpawnMode = DevSpawnMode.BUILDING_STRUCTURE;
        devSpawnStructureType = StructureType.fromKey(type);
        devSpawnOwner = owner;
    }

    public void enterDevBuildMapObj(String type, int owner) {
        devSpawnMode = DevSpawnMode.BUILDING_MAP_OBJ;
        devSpawnMapObjType = type;
        devSpawnOwner = owner;
    }

    public void enterDevKill() {
        devSpawnMode = DevSpawnMode.KILLING_UNIT;
    }

    public void enterDevRemove() {
        devSpawnMode = DevSpawnMode.REMOVING_OBJECT;
    }

    public void enterDevSetBaseLevel() {
        devSpawnMode = DevSpawnMode.SETTING_BASE_LEVEL;
    }

    public void exitDevMode() {
        devSpawnMode = DevSpawnMode.NONE;
    }

    public DevSpawnMode getDevSpawnMode() {
        return devSpawnMode;
    }

    public void deselect() {
        selectionHandler.deselect();
    }

    public int getHoveredX() {
        return hoveredX;
    }

    public int getHoveredY() {
        return hoveredY;
    }

    public int getBouncingX() {
        return selectionHandler.getBouncingX();
    }

    public int getBouncingY() {
        return selectionHandler.getBouncingY();
    }

    public float getBounceTimer() {
        return selectionHandler.getBounceTimer();
    }

    public int getLastClickedX() {
        return selectionHandler.getLastClickedX();
    }

    public int getLastClickedY() {
        return selectionHandler.getLastClickedY();
    }

    public void resetLastClicked() {
        selectionHandler.resetLastClicked();
    }

    public void update(float deltaTime) {
        selectionHandler.update(deltaTime);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (!inputEnabled)
            return false;

        // Undo Shortcut (Ctrl+Z)
        if (keycode == Input.Keys.Z && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))) {
            if (screen.getGameState().isLanGame)
                return false;
            screen.undoTurn();
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        GameLogger.log(GameLogger.INPUT, "Mouse scrolled: x=" + amountX + ", y=" + amountY);
        float oldZoom = camera.zoom;
        camera.zoom += amountY * GameConfig.ZOOM_SPEED;
        camera.zoom = MathUtils.clamp(camera.zoom, GameConfig.ZOOM_MIN, GameConfig.ZOOM_MAX);
        camera.update();
        if (oldZoom != camera.zoom) {
            GameLogger.log(GameLogger.CAMERA, String.format("Camera zoom: %.2f (delta: %.2f)", camera.zoom, amountY));

            // Tutorial Hook: Camera / Zoom
            if (com.militopia.managers.TutorialManager.getInstance().isActive()
                    && com.militopia.managers.TutorialManager.getInstance()
                            .getCurrentStep() == com.militopia.managers.TutorialManager.Step.CAMERA) {
                com.militopia.managers.TutorialManager.getInstance().nextStep();
            }
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Convert screen coordinates to world coordinates
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0),
                viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;

        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);

        // --- LAN LOCKDOWN ---
        // Allow inspection (selecting units/terrain) even if it's not our turn,
        // but block all commands (movement, attack, summon, build).
        boolean isMyTurn = (screen.getGameState().currentPlayer == screen.getActiveLocalPlayer());
        if (!isMyTurn) {
            boolean isVisible = !screen.isFogEnabled() || (gridX >= 0 && gridX < gameMap.width && gridY >= 0
                    && gridY < gameMap.height && gameMap.visibleTiles[gridX][gridY]);
            handleInspection(gridX, gridY, isVisible);
            return true;
        }

        lastTouchX = screenX;
        lastTouchY = screenY;

        if (gridX >= 0 && gridX < gameMap.width && gridY >= 0 && gridY < gameMap.height) {
            // Reset HUD stage scroll focus so map zoom (scroll wheel) works even
            // after the user has interacted with the dev panel ScrollPane.
            gameHUD.stage.setScrollFocus(null);

            // Auto-retract dev panel when the user clicks the map.
            if (screen.getGameState().isDevMode && screen.devPanel != null
                    && screen.devPanel.isVisible()) {
                screen.devPanel.retract();
            }

            // --- DEV MODE INTERCEPT ---
            if (screen.getGameState().isDevMode && devSpawnMode != DevSpawnMode.NONE) {
                handleDevTouch(gridX, gridY);
                return true;
            }

            // --- ABILITY TARGETING ---
            if (isTargetingAbility) {
                executeTargetingAbility(gridX, gridY);
                return true;
            }

            boolean isVisible = gameMap.visibleTiles[gridX][gridY];
            // --- NEW: Movement into Fog (Jammers) ---
            // Allow interaction even if fogged IF there's a movement marker there.
            Entity clickedMoveMarker = selectionHandler.getEntityAt(gridX, gridY, TypeComponent.Type.MARKER);

            if (screen.isFogEnabled() && !isVisible && clickedMoveMarker == null) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                deselect();
                return true;
            }

            // --- MOVEMENT: click on a blue move-marker ---
            if (clickedMoveMarker != null && selectionHandler.getSelectedUnit() != null) {
                selectionHandler.moveUnit(selectionHandler.getSelectedUnit(), gridX, gridY);
                return true;
            }

            // --- TRANSFORMATION: click on a transform-marker ---
            Entity clickedTransformMarker = selectionHandler.getEntityAt(gridX, gridY, TypeComponent.Type.TRANSFORM_MARKER);
            if (clickedTransformMarker != null && selectionHandler.getSelectedUnit() != null) {
                selectionHandler.transformUnit(selectionHandler.getSelectedUnit(), gridX, gridY);
                return true;
            }

            // --- ATTACK: click on a red attack-marker tile ---
            Entity clickedAttackMarker = selectionHandler.getEntityAt(gridX, gridY, TypeComponent.Type.ATTACK_MARKER);
            if (clickedAttackMarker != null && selectionHandler.getSelectedUnit() != null) {
                Entity selectedUnit = selectionHandler.getSelectedUnit();
                StatsComponent aStats = selectedUnit.getComponent(StatsComponent.class);
                Entity targetUnit = selectionHandler.getVisibleUnitAt(gridX, gridY);
                Entity enemy = null;
                if (targetUnit != null) {
                    StatsComponent tStats = targetUnit.getComponent(StatsComponent.class);
                    if (tStats != null && tStats.owner != screen.getActiveLocalPlayer()) {
                        enemy = targetUnit;
                    }
                }
                if (aStats != null && aStats.unitType == UnitType.JUGGERNAUT) {
                    selectionHandler.performJump(selectedUnit, enemy, gridX, gridY);
                    return true;
                }
                if (enemy != null) {
                    selectionHandler.performAttack(selectedUnit, enemy);
                    return true;
                }
                return true;
            }

            // --- ATTACK: directly click enemy unit tile within range (no marker needed) ---
            if (selectionHandler.getSelectedUnit() != null) {
                Entity selectedUnit = selectionHandler.getSelectedUnit();
                Entity directTarget = selectionHandler.getVisibleUnitAt(gridX, gridY);
                if (directTarget != null) {
                    StatsComponent tStats = directTarget.getComponent(StatsComponent.class);
                    StatsComponent aStats = selectedUnit.getComponent(StatsComponent.class);
                    GridPositionComponent aPos = selectedUnit.getComponent(GridPositionComponent.class);
                    if (tStats != null && aStats != null && aPos != null
                            && tStats.owner != screen.getActiveLocalPlayer()
                            && !aStats.hasActed && !aStats.hasMoved) {
                        int dist = selectionHandler.chebyshev(aPos.x, aPos.y, gridX, gridY);
                        if (dist <= aStats.attackRange) {
                            if (aStats.unitType == UnitType.JUGGERNAUT) {
                                selectionHandler.performJump(selectedUnit, directTarget, gridX, gridY);
                            } else {
                                selectionHandler.performAttack(selectedUnit, directTarget);
                            }
                            return true;
                        }
                    }
                }
            }

            // --- Normal click cycling ---
            if (gridX == selectionHandler.getLastClickedX() && gridY == selectionHandler.getLastClickedY()) {
                selectionHandler.incrementSelectionIndex();
            } else {
                selectionHandler.resetSelectionIndex();
            }
            selectionHandler.setLastClicked(gridX, gridY);

            // LOG: raw tile click
            MapGenerator.TerrainType clickedTerrain = gameMap.terrain[gridX][gridY];
            GameLogger.log(GameLogger.INPUT, "Click " + GameLogger.pos(gridX, gridY)
                    + " | terrain=" + clickedTerrain.name());

            Entity foundUnit = null;
            Entity foundAnimal = null;
            Entity foundStructure = null;

            ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class).get());
            for (Entity e : entities) {
                GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
                if (pos.x == gridX && pos.y == gridY) {
                    TypeComponent type = e.getComponent(TypeComponent.class);
                    if (type.type == TypeComponent.Type.UNIT) {
                        Entity visibleUnit = selectionHandler.getVisibleUnitAt(gridX, gridY);
                        if (visibleUnit != null) {
                            foundUnit = visibleUnit;
                        }
                    } else if (type.type == TypeComponent.Type.OBJECT) {
                        if (pos.zIndex == 2 || e.getComponent(AnimalComponent.class) != null) {
                            foundAnimal = e;
                        } else {
                            foundStructure = e;
                        }
                    }
                }
            }

            List<ClickTargetType> targets = new ArrayList<>();
            if (foundUnit != null)
                targets.add(ClickTargetType.UNIT);
            if (foundAnimal != null)
                targets.add(ClickTargetType.ANIMAL);
            if (foundStructure != null)
                targets.add(ClickTargetType.STRUCTURE);
            targets.add(ClickTargetType.TERRAIN);

            ClickTargetType currentTarget = targets.get(selectionHandler.getSelectionIndex() % targets.size());

            selectionHandler.clearMarkers();
            selectionHandler.setSelectedUnit(null);
            gameHUD.hideSummonMenu();
            selectionHandler.triggerBounce(gridX, gridY);

            if (currentTarget == ClickTargetType.UNIT)
                selectionHandler.handleUnitTarget(foundUnit, foundAnimal, foundStructure, gridX, gridY);
            else if (currentTarget == ClickTargetType.ANIMAL)
                selectionHandler.handleAnimalTarget(foundAnimal);
            else if (currentTarget == ClickTargetType.STRUCTURE)
                selectionHandler.handleStructureTarget(foundStructure, gridX, gridY);
            else
                handleTerrainSelection(gridX, gridY, clickedTerrain);

        } else {
            AudioManager.getInstance().playSFX(SFXKeys.TILE_DESELECT);
            deselect();
            gameHUD.hideTileInfo(); // NEW: Auto-hide D-03
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Attack helpers — delegated to UnitSelectionHandler
    // -------------------------------------------------------------------------

    private void handleInspection(int gridX, int gridY, boolean isVisible) {
        if (gridX >= 0 && gridX < gameMap.width && gridY >= 0 && gridY < gameMap.height) {
            selectionHandler.triggerBounce(gridX, gridY);
        }

        boolean withinBounds = gridX >= 0 && gridX < gameMap.width && gridY >= 0 && gridY < gameMap.height;
        Entity foundUnit = (withinBounds && isVisible) ? selectionHandler.getEntityAt(gridX, gridY, TypeComponent.Type.UNIT) : null;
        Entity foundStructure = (withinBounds && isVisible) ? selectionHandler.getEntityAt(gridX, gridY, TypeComponent.Type.OBJECT)
                : null;
        MapGenerator.TerrainType clickedTerrain = withinBounds ? gameMap.terrain[gridX][gridY] : null;

        if (foundUnit != null) {
            StatsComponent stats = foundUnit.getComponent(StatsComponent.class);
            UnitFactory.UiInfo info = unitFactory.getUnitUi(stats.unitType);
            String label = stats.owner == screen.getActiveLocalPlayer() ? stats.name : stats.name + " (Enemy)";
            gameHUD.showUnitInfo(foundUnit, label, info.region, stats.currentHP, stats.maxHP);
            AudioManager.getInstance().playSFX(SFXKeys.UNIT_SELECT);
        } else if (foundStructure != null) {
            StatsComponent stats = foundStructure.getComponent(StatsComponent.class);
            gameHUD.showBaseInfoUnified(foundStructure, screen.getGameState(), stats.level, "");
            AudioManager.getInstance().playSFX(SFXKeys.TILE_CLICK);
        } else {
            gameHUD.showTileInfo((withinBounds && isVisible) ? clickedTerrain.name() : "Undiscovered",
                    (withinBounds && isVisible) ? screen.getGame().assets.getTerrainRegion(clickedTerrain)
                            : unitFactory.fogRegion);
            AudioManager.getInstance().playSFX(SFXKeys.TILE_CLICK);
        }
    }

    // -------------------------------------------------------------------------
    // Unit / target handlers and Hunt — delegated to UnitSelectionHandler
    // -------------------------------------------------------------------------

    public void performHunt(Entity animal, Entity hunter) {
        selectionHandler.performHunt(animal, hunter);
    }

    // -------------------------------------------------------------------------
    // Abilities
    // -------------------------------------------------------------------------

    public void performAbility(Entity unit, String abilityKey) {
        StatsComponent stats = unit.getComponent(StatsComponent.class);
        if (screen.getGameState().isLanGame) {
            GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);
            if (pos != null) {
                screen.getNetworkManager().send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_ABILITY,
                        pos.x + "," + pos.y + "," + abilityKey));
                screen.syncEconomy(stats.owner);
            }
        }

        AbilitiesComponent abilities = unit.getComponent(AbilitiesComponent.class);
        if (stats == null || abilities == null)
            return;

        GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);
        String posStr = (pos != null) ? GameLogger.pos(pos.x, pos.y) : "(?,?)";

        if (abilityKey.equals(AbilitiesComponent.KEY_DIG_IN)) {
            GameLogger.log(GameLogger.ABILITY, stats.owner,
                    "DIG IN: " + stats.name + " digs in at " + posStr);
            abilities.isDiggingIn = true;
            abilities.hasUsedDigIn = true;
            stats.hasActed = true;
            stats.hasMoved = true;
            // Visual feedback could be added here (e.g., spawn floating text "DUG IN")
            gameHUD.snapHP(stats.currentHP, stats.maxHP); // Refresh UI
            deselect();
        } else if (abilityKey.equals(AbilitiesComponent.KEY_OVERWATCH)) {
            GameLogger.log(GameLogger.ABILITY, stats.owner,
                    "OVERWATCH: " + stats.name + " goes into overwatch at " + posStr);
            abilities.isOverwatchActive = true;
            stats.hasActed = true;
            stats.hasMoved = true;
            gameHUD.snapHP(stats.currentHP, stats.maxHP); // Refresh UI
            deselect();
        }
    }

    private void executeTargetingAbility(int tx, int ty) {
        isTargetingAbility = false;
        targetingAbilityKey = null;
        targetingUnit = null;
        deselect();
    }

    // -------------------------------------------------------------------------
    // Terrain
    // -------------------------------------------------------------------------

    private void handleTerrainSelection(int x, int y, MapGenerator.TerrainType terrain) {
        MapGenerator.ObjectType obj = gameMap.objects[x][y];

        // Check for existing built structures not tracked in gameMap.objects
        Entity existingStruct = selectionHandler.getEntityAt(x, y, TypeComponent.Type.OBJECT);
        boolean isOccupied = (obj != MapGenerator.ObjectType.NONE && obj != MapGenerator.ObjectType.OIL);

        if (existingStruct != null) {
            StatsComponent stats = existingStruct.getComponent(StatsComponent.class);
            // Allow building ONLY if it's an Oil Reservoir (allows Oil Derrick)
            if (stats == null || !MapGenerator.ObjectType.OIL.name().equals(stats.unitTypeKey)) {
                isOccupied = true;
            }
        }

        // If there's a blocking object or structure, just show terrain info
        if (isOccupied) {
            gameHUD.showTileInfo(unitFactory.getTerrainUi(terrain).name,
                    unitFactory.getTextureForTerrain(terrain.ordinal()));
            return;
        }

        int owner = screen.getActiveLocalPlayer();

        // --- 1. TERRITORY CHECK (Critical for Build Menu) ---
        // [0]=isTerritory(1/0), [1]=maxLevel, [2]=parentX, [3]=parentY
        int[] territory = selectionHandler.findControllingBase(x, y, owner);

        boolean isMountain = (terrain == MapGenerator.TerrainType.MOUNTAIN);
        boolean isWater = (terrain == MapGenerator.TerrainType.WATER || terrain == MapGenerator.TerrainType.DEEP_WATER);
        boolean hasRail = gameMap.rails[x][y];
        boolean canBuildRail = !isMountain && !isWater && !hasRail;

        // If there's a blocking object or structure, we only allow opening the build
        // menu if a rail can be built
        if (isOccupied && !canBuildRail) {
            gameHUD.showTileInfo(unitFactory.getTerrainUi(terrain).name,
                    unitFactory.getTextureForTerrain(terrain.ordinal()));
            return;
        }

        // --- 3. BUILD MENU OR TERRAIN INFO ---
        boolean inTerritory = (territory[0] == 1);
        boolean isCoastalWater = isWater && hasAdjacentLand(x, y);
        boolean isCoastalLand = !isWater && hasAdjacentWater(x, y);

        // Can build structures if in territory AND (land OR coastal water)
        boolean canBuildStructure = inTerritory && (!isWater || isCoastalWater);

        if (canBuildStructure || canBuildRail) {
            gameHUD.openBuildMenu(x, y, owner, territory[1], isWater, isCoastalWater, isCoastalLand,
                    screen.getGameState(), territory[2], territory[3], terrain, unitFactory);
        } else {
            gameHUD.showTileInfo(unitFactory.getTerrainUi(terrain).name,
                    unitFactory.getTextureForTerrain(terrain.ordinal()));
        }
    }

    // -------------------------------------------------------------------------
    // Territory queries — delegated to UnitSelectionHandler
    // -------------------------------------------------------------------------

    private boolean hasAdjacentLand(int x, int y) {
        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < gameMap.width && ny >= 0 && ny < gameMap.height) {
                MapGenerator.TerrainType t = gameMap.terrain[nx][ny];
                if (t != MapGenerator.TerrainType.WATER && t != MapGenerator.TerrainType.DEEP_WATER)
                    return true;
            }
        }
        return false;
    }

    private boolean hasAdjacentWater(int x, int y) {
        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < gameMap.width && ny >= 0 && ny < gameMap.height) {
                MapGenerator.TerrainType t = gameMap.terrain[nx][ny];
                if (t == MapGenerator.TerrainType.WATER || t == MapGenerator.TerrainType.DEEP_WATER)
                    return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Camera / mouse
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (!inputEnabled)
            return false;
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0),
                viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        // Inverse isometric projection — see touchDown for formula derivation
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;
        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);
        if (gridX >= 0 && gridX < gameMap.width && gridY >= 0 && gridY < gameMap.height) {
            this.hoveredX = gridX;
            this.hoveredY = gridY;
        } else {
            this.hoveredX = -1;
            this.hoveredY = -1;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!inputEnabled)
            return false;
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            Vector3 oldWorld = camera.unproject(new Vector3(lastTouchX, lastTouchY, 0),
                    viewport.getScreenX(), viewport.getScreenY(),
                    viewport.getScreenWidth(), viewport.getScreenHeight());
            Vector3 newWorld = camera.unproject(new Vector3(screenX, screenY, 0),
                    viewport.getScreenX(), viewport.getScreenY(),
                    viewport.getScreenWidth(), viewport.getScreenHeight());
            camera.translate(oldWorld.x - newWorld.x, oldWorld.y - newWorld.y);
            lastTouchX = screenX;
            lastTouchY = screenY;
            camera.update();
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Markers
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Movement, markers, entity queries — delegated to UnitSelectionHandler
    // -------------------------------------------------------------------------

    public void clearMarkersPublic() {
        selectionHandler.clearMarkersPublic();
    }

    private void handleDevTouch(int gridX, int gridY) {
        switch (devSpawnMode) {
            case SPAWNING_UNIT: {
                Entity existingUnit = selectionHandler.getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (existingUnit != null) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    return;
                }

                Entity existingObj = selectionHandler.getEntityAt(gridX, gridY, TypeComponent.Type.OBJECT);
                if (existingObj != null) {
                    StatsComponent s = existingObj.getComponent(StatsComponent.class);
                    if (s != null && s.owner > 0 && s.owner != devSpawnOwner) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                }

                MapGenerator.TerrainType terrain = gameMap.terrain[gridX][gridY];
                StatsComponent.MoveType moveType = unitFactory.getUnitMoveType(devSpawnUnitType);
                boolean isWater = (terrain == MapGenerator.TerrainType.WATER
                        || terrain == MapGenerator.TerrainType.DEEP_WATER);

                if (moveType == StatsComponent.MoveType.LAND && isWater) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    return;
                } else if (moveType == StatsComponent.MoveType.SEA && !isWater) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    return;
                }

                unitFactory.createUnit(devSpawnUnitType, gridX, gridY, devSpawnOwner, false);
                GameLogger.log(GameLogger.INPUT, "[DEV] Spawned " + devSpawnUnitType
                        + " at (" + gridX + "," + gridY + ") for P" + devSpawnOwner);
                break;
            }
            case BUILDING_STRUCTURE: {
                Entity existingStructure = selectionHandler.getEntityAtLayer(gridX, gridY, 1);
                MapGenerator.ObjectType existingMapObj = gameMap.objects[gridX][gridY];
                boolean isOilTile = (existingMapObj == MapGenerator.ObjectType.OIL);

                if (existingStructure != null) {
                    StatsComponent stats = existingStructure.getComponent(StatsComponent.class);
                    boolean isOilRes = (stats != null && MapGenerator.ObjectType.OIL.name().equals(stats.unitTypeKey));
                    if (!(devSpawnStructureType == StructureType.OIL_DERRICK && isOilRes)) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                } else if (existingMapObj != null && existingMapObj != MapGenerator.ObjectType.NONE && !isOilTile) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    return;
                }

                MapGenerator.TerrainType buildTerrain = gameMap.terrain[gridX][gridY];
                boolean buildIsWater = (buildTerrain == MapGenerator.TerrainType.WATER
                        || buildTerrain == MapGenerator.TerrainType.DEEP_WATER);
                boolean buildIsCoastalWater = buildIsWater && hasAdjacentLand(gridX, gridY);
                boolean buildIsCoastalLand = !buildIsWater && hasAdjacentWater(gridX, gridY);

                if (devSpawnStructureType == StructureType.PORT) {
                    if (!buildIsWater || !buildIsCoastalWater) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                } else if (devSpawnStructureType == StructureType.NUCLEAR_PLANT) {
                    if (buildIsWater || !buildIsCoastalLand) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                } else if (devSpawnStructureType == StructureType.OIL_DERRICK) {
                    if (!isOilTile) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                } else {
                    if (buildIsWater) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                }

                if (devSpawnStructureType == StructureType.OIL_DERRICK && isOilTile) {
                    gameMap.objects[gridX][gridY] = MapGenerator.ObjectType.NONE;
                    if (existingStructure != null) {
                        engine.removeEntity(existingStructure);
                    }
                }

                if (devSpawnStructureType == StructureType.BASE) {
                    MapGenerator.ObjectType baseType = (devSpawnOwner == 1) ? MapGenerator.ObjectType.BASE_P1
                            : MapGenerator.ObjectType.BASE_P2;
                    gameMap.objects[gridX][gridY] = baseType;
                    unitFactory.createObjectEntity(gridX, gridY, baseType, screen.getGameState());
                } else if (devSpawnStructureType == StructureType.TOWN) {
                    gameMap.objects[gridX][gridY] = MapGenerator.ObjectType.TOWN;
                    unitFactory.createObjectEntity(gridX, gridY, MapGenerator.ObjectType.TOWN, screen.getGameState());
                } else {
                    unitFactory.createStructure(devSpawnStructureType.getKey(), gridX, gridY, devSpawnOwner, -1, -1);
                }

                GameLogger.log(GameLogger.INPUT, "[DEV] Built " + devSpawnStructureType.getKey()
                        + " at (" + gridX + "," + gridY + ") for P" + devSpawnOwner);
                break;
            }
            case KILLING_UNIT:
                Entity unitAtTile = selectionHandler.getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (unitAtTile != null) {
                    engine.removeEntity(unitAtTile);
                    GameLogger.log(GameLogger.INPUT, "[DEV] Killed unit at (" + gridX + "," + gridY + ")");
                }
                break;
            case REMOVING_OBJECT:
                boolean removedSomething = false;
                Entity unitToRemove = selectionHandler.getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (unitToRemove != null) {
                    engine.removeEntity(unitToRemove);
                    removedSomething = true;
                }
                Entity objToRemove = selectionHandler.getEntityAt(gridX, gridY, TypeComponent.Type.OBJECT);
                if (objToRemove != null) {
                    engine.removeEntity(objToRemove);
                    removedSomething = true;
                }
                // Always clear the tile's object layer in the map data, even if it's just a
                // tree
                if (gameMap.objects[gridX][gridY] != MapGenerator.ObjectType.NONE) {
                    gameMap.objects[gridX][gridY] = MapGenerator.ObjectType.NONE;
                    removedSomething = true;
                }
                if (removedSomething) {
                    GameLogger.log(GameLogger.INPUT, "[DEV] Removed object/unit at (" + gridX + "," + gridY + ")");
                    AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                }
                break;
            case BUILDING_MAP_OBJ:
                // Check terrain validation
                boolean objIsWater = devSpawnMapObjType.equals("FISH") || devSpawnMapObjType.equals("OIL")
                        || devSpawnMapObjType.equals("RUINS");
                boolean tileIsWater = (gameMap.terrain[gridX][gridY] == MapGenerator.TerrainType.WATER
                        || gameMap.terrain[gridX][gridY] == MapGenerator.TerrainType.DEEP_WATER);

                if (objIsWater != tileIsWater) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    break;
                }

                boolean isAnimal = (com.militopia.config.AnimalType.fromKey(devSpawnMapObjType) != null);

                // Check stacking rules: non-animals cannot be placed if there's already any
                // object (static or animal)
                if (!isAnimal) {
                    Entity animalAtTile = selectionHandler.getEntityAtLayer(gridX, gridY, 2);
                    if (gameMap.objects[gridX][gridY] != MapGenerator.ObjectType.NONE || animalAtTile != null) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        break;
                    }
                }

                if (isAnimal) {
                    // Prevent animal stacking: cannot place an animal if one already exists
                    Entity existAnimal = selectionHandler.getEntityAtLayer(gridX, gridY, 2);
                    if (existAnimal != null) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        break;
                    }
                }

                if (devSpawnMapObjType.equals("MOUNTAIN")) {
                    gameMap.terrain[gridX][gridY] = MapGenerator.TerrainType.MOUNTAIN;
                    gameMap.objects[gridX][gridY] = MapGenerator.ObjectType.MOUNTAIN_OBJ;
                    unitFactory.createObjectEntity(gridX, gridY, MapGenerator.ObjectType.MOUNTAIN_OBJ,
                            screen.getGameState());
                } else {
                    MapGenerator.ObjectType objType = MapGenerator.ObjectType.valueOf(devSpawnMapObjType);

                    if (!isAnimal) {
                        gameMap.objects[gridX][gridY] = objType;
                    }

                    unitFactory.createObjectEntity(gridX, gridY, objType, screen.getGameState());
                }

                GameLogger.log(GameLogger.INPUT, "[DEV] Built Map Obj " + devSpawnMapObjType
                        + " at (" + gridX + "," + gridY + ")");
                break;
            case SETTING_BASE_LEVEL:
                Entity baseAtTile = selectionHandler.getEntityAt(gridX, gridY, TypeComponent.Type.OBJECT);
                if (baseAtTile != null) {
                    StatsComponent s = baseAtTile.getComponent(StatsComponent.class);
                    if (s != null && s.level > 0) {
                        screen.devOpenBaseLevelPicker(baseAtTile);
                    }
                }
                break;
            default:
                break;
        }
    }

}
