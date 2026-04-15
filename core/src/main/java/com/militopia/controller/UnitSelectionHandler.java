package com.militopia.controller;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.utils.Array;
import com.militopia.components.AbilitiesComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.MovementComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TextureComponent;
import com.militopia.components.TypeComponent;
import com.militopia.config.AnimalType;
import com.militopia.config.CombatConstants;
import com.militopia.config.GameConfig;
import com.militopia.config.UnitType;
import com.militopia.data.GameState;
import com.militopia.factories.EntityFactory;
import com.militopia.factories.UnitFactory;
import com.militopia.map.MapGenerator;
import com.militopia.net.NetworkMessage;
import com.militopia.screen.GameScreen;
import com.militopia.systems.CombatSystem;
import com.militopia.ui.GameHUD;
import com.militopia.utils.GameLogger;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.managers.TutorialManager;

/**
 * UnitSelectionHandler — extracted from GameInputController.
 * Owns all unit selection state and handles movement, attack, and target routing.
 */
public class UnitSelectionHandler {

    private final GameScreen screen;
    private final PooledEngine engine;
    private final MapGenerator.GameMap gameMap;
    private final UnitFactory unitFactory;
    private final EntityFactory entityFactory;
    private final GameHUD gameHUD;
    private final CombatSystem combatSystem;

    // Spatial index: type → (cellKey → entity), where cellKey = x<<16|y
    private final java.util.Map<TypeComponent.Type, java.util.Map<Long, Entity>> spatialIndex
            = new java.util.EnumMap<>(TypeComponent.Type.class);
    // Reverse map to look up position+type when removing (position can't be read after removal in edge cases)
    private final java.util.Map<Entity, long[]> entityIndexInfo = new java.util.HashMap<>();

    // Selection state
    Entity selectedUnitEntity = null;
    int lastClickedX = -1;
    int lastClickedY = -1;
    int selectionIndex = 0;
    int bouncingX = -1;
    int bouncingY = -1;
    float bounceTimer = 0;

    public UnitSelectionHandler(
            GameScreen screen,
            PooledEngine engine,
            MapGenerator.GameMap gameMap,
            UnitFactory unitFactory,
            EntityFactory entityFactory,
            GameHUD gameHUD,
            CombatSystem combatSystem) {
        this.screen = screen;
        this.engine = engine;
        this.gameMap = gameMap;
        this.unitFactory = unitFactory;
        this.entityFactory = entityFactory;
        this.gameHUD = gameHUD;
        this.combatSystem = combatSystem;
        buildSpatialIndex();
    }

    // -------------------------------------------------------------------------
    // State accessors
    // -------------------------------------------------------------------------

    public Entity getSelectedUnit() { return selectedUnitEntity; }
    public void setSelectedUnit(Entity e) { this.selectedUnitEntity = e; }

    public int getBouncingX() { return bouncingX; }
    public int getBouncingY() { return bouncingY; }
    public float getBounceTimer() { return bounceTimer; }

    public int getLastClickedX() { return lastClickedX; }
    public int getLastClickedY() { return lastClickedY; }
    public void setLastClicked(int x, int y) { this.lastClickedX = x; this.lastClickedY = y; }
    public void resetLastClicked() { this.lastClickedX = -1; this.lastClickedY = -1; }

    public int getSelectionIndex() { return selectionIndex; }
    public void incrementSelectionIndex() { selectionIndex++; }
    public void resetSelectionIndex() { selectionIndex = 0; }

    public void syncSpatialIndex() {
        for (java.util.Map.Entry<Entity, long[]> entry : entityIndexInfo.entrySet()) {
            Entity e = entry.getKey();
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (pos != null) {
                long expectedKey = cellKey(pos.x, pos.y);
                if (entry.getValue()[0] != expectedKey) {
                    TypeComponent.Type type = TypeComponent.Type.values()[(int) entry.getValue()[1]];
                    spatialIndex.get(type).remove(entry.getValue()[0]);
                    spatialIndex.get(type).put(expectedKey, e);
                    entry.getValue()[0] = expectedKey;
                }
            }
        }
    }

    public void update(float deltaTime) {
        if (bounceTimer > 0) {
            bounceTimer -= deltaTime;
            if (bounceTimer <= 0) {
                bouncingX = -1;
                bouncingY = -1;
            }
        }
        syncSpatialIndex();
    }

    public void deselect() {
        if (selectedUnitEntity != null) {
            AudioManager.getInstance().playSFX(SFXKeys.UNIT_DESELECT);
        }
        clearMarkers();
        selectedUnitEntity = null;
        gameHUD.hideSummonMenu();
        gameHUD.hideTileInfo();
        lastClickedX = -1;
        lastClickedY = -1;
        selectionIndex = 0;
    }

    // -------------------------------------------------------------------------
    // Bounce / markers
    // -------------------------------------------------------------------------

    public void triggerBounce(int x, int y) {
        if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height)
            return;
        this.bouncingX = x;
        this.bouncingY = y;
        this.bounceTimer = GameConfig.BOUNCE_DURATION;
        AudioManager.getInstance().playSFX(SFXKeys.TILE_CLICK);
    }

    public void clearMarkers() {
        ImmutableArray<Entity> all = engine.getEntitiesFor(Family.all(TypeComponent.class).get());
        Array<Entity> toRemove = new Array<>();
        for (Entity e : all) {
            TypeComponent.Type t = e.getComponent(TypeComponent.class).type;
            if (t == TypeComponent.Type.MARKER || t == TypeComponent.Type.ATTACK_MARKER
                    || t == TypeComponent.Type.TRANSFORM_MARKER) {
                toRemove.add(e);
            }
        }
        for (Entity e : toRemove)
            engine.removeEntity(e);
    }

    /** Public alias for undo — clears markers and resets selection. */
    public void clearMarkersPublic() {
        clearMarkers();
        selectedUnitEntity = null;
    }

    // -------------------------------------------------------------------------
    // Spatial index helpers
    // -------------------------------------------------------------------------

    private static long cellKey(int x, int y) {
        return ((long) x << 16) | (y & 0xFFFF);
    }

    private void buildSpatialIndex() {
        for (TypeComponent.Type t : TypeComponent.Type.values()) {
            spatialIndex.put(t, new java.util.HashMap<>());
        }
        engine.addEntityListener(
            com.badlogic.ashley.core.Family
                .all(GridPositionComponent.class, TypeComponent.class).get(),
            new com.badlogic.ashley.core.EntityListener() {
                @Override
                public void entityAdded(Entity entity) {
                    GridPositionComponent pos = entity.getComponent(GridPositionComponent.class);
                    TypeComponent tc = entity.getComponent(TypeComponent.class);
                    if (pos == null || tc == null) return;
                    long key = cellKey(pos.x, pos.y);
                    spatialIndex.get(tc.type).put(key, entity);
                    entityIndexInfo.put(entity, new long[]{ key, tc.type.ordinal() });
                }
                @Override
                public void entityRemoved(Entity entity) {
                    long[] info = entityIndexInfo.remove(entity);
                    if (info == null) return;
                    TypeComponent.Type type = TypeComponent.Type.values()[(int) info[1]];
                    spatialIndex.get(type).remove(info[0]);
                }
            }
        );
        // Backfill existing entities
        com.badlogic.ashley.utils.ImmutableArray<Entity> existing = engine.getEntitiesFor(
            com.badlogic.ashley.core.Family.all(GridPositionComponent.class, TypeComponent.class).get());
        for (int i = 0; i < existing.size(); i++) {
            Entity e = existing.get(i);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TypeComponent tc = e.getComponent(TypeComponent.class);
            if (pos == null || tc == null) continue;
            long key = cellKey(pos.x, pos.y);
            spatialIndex.get(tc.type).put(key, e);
            entityIndexInfo.put(e, new long[]{ key, (long) tc.type.ordinal() });
        }
    }

    // -------------------------------------------------------------------------
    // Entity queries
    // -------------------------------------------------------------------------

    public Entity getEntityAt(int x, int y, TypeComponent.Type type) {
        java.util.Map<Long, Entity> map = spatialIndex.get(type);
        return (map != null) ? map.get(cellKey(x, y)) : null;
    }

    public Entity getEntityAtLayer(int x, int y, int zIndex) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (pos.x == x && pos.y == y && pos.zIndex == zIndex)
                return e;
        }
        return null;
    }

    public Entity getVisibleUnitAt(int x, int y) {
        Entity unit = getEntityAt(x, y, TypeComponent.Type.UNIT);
        if (unit != null) {
            StatsComponent stats = unit.getComponent(StatsComponent.class);
            AbilitiesComponent abilities = unit.getComponent(AbilitiesComponent.class);
            if (stats != null && stats.owner != screen.getActiveLocalPlayer()) {
                // 1. Permanent Cloak
                boolean isStealth = (abilities != null && abilities.isCloaked);

                // 2. Sniper Dynamic Stealth
                if (stats.unitType == UnitType.SNIPER && !stats.hasActed) {
                    MapGenerator.TerrainType terrain = gameMap.terrain[x][y];
                    MapGenerator.ObjectType obj = gameMap.objects[x][y];
                    if (terrain == MapGenerator.TerrainType.MOUNTAIN || obj == MapGenerator.ObjectType.TREE
                            || obj == MapGenerator.ObjectType.RUINS || obj == MapGenerator.ObjectType.MOUNTAIN_OBJ) {
                        isStealth = true;
                    }
                }

                // 3. Reveal overrides (attacked or detected)
                if (stats.hasActed || (abilities != null && abilities.isCloakBroken)) {
                    isStealth = false;
                }

                if (isStealth && !gameMap.detectedTiles[x][y]) {
                    return null; // Hidden enemy!
                }
            }
        }
        return unit;
    }

    // -------------------------------------------------------------------------
    // Territory queries
    // -------------------------------------------------------------------------

    /**
     * Finds the highest-level friendly base whose territory covers (tx, ty).
     * Returns [isTerritory(1/0), maxLevel, parentX, parentY].
     */
    public int[] findControllingBase(int tx, int ty, int owner) {
        int maxLevel = 0, parentX = -1, parentY = -1;
        ImmutableArray<Entity> entities = engine.getEntitiesFor(
                Family.all(StatsComponent.class, GridPositionComponent.class).get());
        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (stats.owner == owner && stats.income >= 2 && stats.name.contains("Base")) {
                if (Math.abs(pos.x - tx) <= stats.vision && Math.abs(pos.y - ty) <= stats.vision) {
                    if (stats.level > maxLevel) {
                        maxLevel = stats.level;
                        parentX = pos.x;
                        parentY = pos.y;
                    }
                }
            }
        }
        return new int[] { maxLevel > 0 ? 1 : 0, maxLevel, parentX, parentY };
    }

    /**
     * Returns the highest-level friendly base whose vision radius covers (tx, ty),
     * defaulting to 1 if none found.
     */
    public int findMaxBaseLevelNear(int tx, int ty, int owner) {
        int maxLevel = 1;
        ImmutableArray<Entity> allEnts = engine.getEntitiesFor(
                Family.all(StatsComponent.class, GridPositionComponent.class).get());
        for (Entity e : allEnts) {
            StatsComponent bs = e.getComponent(StatsComponent.class);
            GridPositionComponent bp = e.getComponent(GridPositionComponent.class);
            if (bs.owner == owner && bs.income >= 2 && bs.name.contains("Base")) {
                if (Math.abs(bp.x - tx) <= bs.vision && Math.abs(bp.y - ty) <= bs.vision) {
                    if (bs.level > maxLevel)
                        maxLevel = bs.level;
                }
            }
        }
        return maxLevel;
    }

    // -------------------------------------------------------------------------
    // Attack helpers
    // -------------------------------------------------------------------------

    /** Delegates to CombatSystem, then cleans up selection state. */
    public void performAttack(Entity attacker, Entity defender) {
        StatsComponent aStats = attacker.getComponent(StatsComponent.class);
        GridPositionComponent aPos = attacker.getComponent(GridPositionComponent.class);
        GridPositionComponent dPos = defender.getComponent(GridPositionComponent.class);

        if (screen.getGameState().isLanGame && aPos != null && dPos != null) {
            screen.getNetworkManager().send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_ATTACK,
                    aPos.x + "," + aPos.y + "," + dPos.x + "," + dPos.y));
            screen.syncEconomy(aStats.owner);
        }

        combatSystem.resolveAttack(attacker, defender);
        if (aStats != null && aStats.currentHP > 0) {
            gameHUD.snapHP(aStats.currentHP, aStats.maxHP);
        }

        // Tutorial Hook: Attack Enemy
        if (TutorialManager.getInstance().isActive()
                && TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.ATTACK_ENEMY) {
            TutorialManager.getInstance().nextStep();
        }

        clearMarkers();
        selectedUnitEntity = null;
        gameHUD.hideTileInfo();
    }

    /**
     * Triggers a Juggernaut jump to the target tile. Target may be null for
     * empty-tile jumps.
     */
    public void performJump(Entity attacker, Entity target, int tx, int ty) {
        StatsComponent aStats = attacker.getComponent(StatsComponent.class);
        GridPositionComponent aPos = attacker.getComponent(GridPositionComponent.class);
        GridPositionComponent dPos = target != null ? target.getComponent(GridPositionComponent.class) : null;

        if (screen.getGameState().isLanGame && aPos != null) {
            String targetPart = (dPos != null) ? (dPos.x + "," + dPos.y) : "-1,-1";
            screen.getNetworkManager().send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_ATTACK,
                    aPos.x + "," + aPos.y + "," + targetPart + "," + tx + "," + ty));
            screen.syncEconomy(aStats.owner);
        }

        // Block jump onto water terrain
        MapGenerator.TerrainType terrain = gameMap.terrain[tx][ty];
        if (terrain == MapGenerator.TerrainType.WATER || terrain == MapGenerator.TerrainType.DEEP_WATER)
            return;
        Entity tileUnit = getEntityAt(tx, ty, TypeComponent.Type.UNIT);
        if (tileUnit != null) {
            StatsComponent ts = tileUnit.getComponent(StatsComponent.class);
            if (ts != null) {
                if (ts.owner == screen.getActiveLocalPlayer())
                    return;

                // Stealth Rule: Block jump onto cloaked units silently (unresponsive)
                AbilitiesComponent ab = tileUnit.getComponent(AbilitiesComponent.class);
                if (ab != null && ab.isCloaked) {
                    return; // Swallow click
                }
            }
        }
        combatSystem.resolveJumperAttack(attacker, target, tx, ty);
        if (aStats != null && aStats.currentHP > 0) {
            gameHUD.snapHP(aStats.currentHP, aStats.maxHP);
        }
        clearMarkers();
        selectedUnitEntity = null;
        gameHUD.hideTileInfo();
    }

    /** Chebyshev distance for range checks. */
    public int chebyshev(int ax, int ay, int bx, int by) {
        return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }

    // -------------------------------------------------------------------------
    // Unit / target handlers
    // -------------------------------------------------------------------------

    public void handleUnitTarget(Entity foundUnit, Entity foundAnimal, Entity foundStructure, int gridX, int gridY) {
        StatsComponent unitStats = foundUnit.getComponent(StatsComponent.class);

        if (unitStats.owner != screen.getActiveLocalPlayer()) {
            UnitFactory.UiInfo info = unitFactory.getUnitUi(unitStats.unitType);
            GameLogger.log(GameLogger.INPUT, unitStats.owner,
                    "Enemy unit inspected: " + unitStats.name + " at " + GameLogger.pos(gridX, gridY)
                            + " | HP: " + unitStats.currentHP + "/" + unitStats.maxHP);
            gameHUD.showUnitInfo(foundUnit, unitStats.name + " (Enemy)", info.region, unitStats.currentHP,
                    unitStats.maxHP);

            // Tutorial Hook: Check Stats (Enemy Unit)
            if (TutorialManager.getInstance().isActive()
                    && TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.CHECK_STATS) {
                TutorialManager.getInstance().nextStep();
            }

            return;
        }

        if (!GameConfig.TESTING_MODE && unitStats.hasActed && unitStats.hasMoved) {
            GameLogger.log(GameLogger.INPUT,
                    "Unit fully exhausted: " + unitStats.name + " at " + GameLogger.pos(gridX, gridY));
            AudioManager.getInstance().playSFX(SFXKeys.UNIT_CANT_MOVE);
            UnitFactory.UiInfo info = unitFactory.getUnitUi(unitStats.unitType);
            gameHUD.showTileInfo("Unit Exhausted (" + unitStats.name + ")", info.region);
            return;
        }

        selectedUnitEntity = foundUnit;
        AudioManager.getInstance().playSFX(SFXKeys.UNIT_SELECT);
        GameLogger.log(GameLogger.INPUT, "Unit selected: " + unitStats.name
                + " at " + GameLogger.pos(gridX, gridY)
                + " | HP: " + unitStats.currentHP + "/" + unitStats.maxHP);
        showRangeMarkers(gridX, gridY);
        UnitFactory.UiInfo info = unitFactory.getUnitUi(unitStats.unitType);
        gameHUD.showUnitInfo(foundUnit, info.name, info.region, unitStats.currentHP, unitStats.maxHP);

        // Tutorial Hook: Select Unit
        if (TutorialManager.getInstance().isActive()
                && TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.SELECT_UNIT) {
            TutorialManager.getInstance().nextStep();
        }

        if (foundAnimal != null) {
            String animName = foundAnimal.getComponent(StatsComponent.class).name;
            AnimalType detectedAnimal = AnimalType.fromKey(animName);
            MapGenerator.ObjectType animType = MapGenerator.ObjectType.HORSE;
            if (detectedAnimal == AnimalType.DEER)
                animType = MapGenerator.ObjectType.DEER;
            else if (detectedAnimal == AnimalType.FISH)
                animType = MapGenerator.ObjectType.FISH;
            else if (detectedAnimal == AnimalType.ZEBRA)
                animType = MapGenerator.ObjectType.ZEBRA;
            gameHUD.openHuntMenu(foundAnimal, foundUnit, animType, unitFactory, screen.getInputController());
        }

        if (foundStructure != null) {
            StatsComponent structStats = foundStructure.getComponent(StatsComponent.class);
            MapGenerator.ObjectType type = gameMap.objects[gridX][gridY];
            boolean isCapturable = (type == MapGenerator.ObjectType.BASE_P1
                    || type == MapGenerator.ObjectType.BASE_P2
                    || type == MapGenerator.ObjectType.TOWN);
            if (isCapturable && structStats.owner != unitStats.owner) {
                gameHUD.openCaptureMenu(foundStructure, foundUnit, unitFactory, screen.getInputController(), gameMap, screen.getGameState());
            }

            if (type == MapGenerator.ObjectType.RUINS) {
                gameHUD.openScavengeMenu(foundStructure, foundUnit, unitFactory, screen.getInputController());
            }
        }
    }

    public void handleAnimalTarget(Entity foundAnimal) {
        StatsComponent stats = foundAnimal.getComponent(StatsComponent.class);
        String rawName = (stats != null) ? stats.name : "";
        GameLogger.log(GameLogger.INPUT, "Animal inspected: " + rawName);
        AnimalType detectedAnimal = AnimalType.fromKey(rawName);
        MapGenerator.ObjectType type = MapGenerator.ObjectType.HORSE;
        if (detectedAnimal == AnimalType.DEER)
            type = MapGenerator.ObjectType.DEER;
        else if (detectedAnimal == AnimalType.FISH)
            type = MapGenerator.ObjectType.FISH;
        else if (detectedAnimal == AnimalType.ZEBRA)
            type = MapGenerator.ObjectType.ZEBRA;
        else if (detectedAnimal == AnimalType.HORSE)
            type = MapGenerator.ObjectType.HORSE;
        UnitFactory.UiInfo info = unitFactory.getObjectUi(type);
        gameHUD.showTileInfo(info.name, unitFactory.getHudIcon(type));
    }

    public void handleStructureTarget(Entity foundStructure, int gridX, int gridY) {
        MapGenerator.ObjectType objType = gameMap.objects[gridX][gridY];
        StatsComponent structStats = foundStructure.getComponent(StatsComponent.class);
        int sOwner = (structStats != null) ? structStats.owner : 0;
        String sOwnerStr = (sOwner == 0) ? "neutral" : "P" + sOwner;
        String structName = (structStats != null) ? structStats.name : objType.name();

        GameLogger.log(GameLogger.INPUT,
                "Structure selected: " + structName
                        + " at " + GameLogger.pos(gridX, gridY) + " | owner=" + sOwnerStr);

        // --- Handle Bases ---
        if (objType == MapGenerator.ObjectType.BASE_P1 || objType == MapGenerator.ObjectType.BASE_P2) {
            int owner = (objType == MapGenerator.ObjectType.BASE_P2) ? 2 : 1;

            if (owner == screen.getActiveLocalPlayer()) {
                Entity unitOnTop = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (unitOnTop == null) {
                    int level = structStats.level;
                    gameHUD.showBaseInfoUnified(foundStructure, screen.getGameState(), level, "BASE");

                    // Tutorial Hook: Select Base
                    if (TutorialManager.getInstance().isActive()
                            && TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.SELECT_BASE) {
                        TutorialManager.getInstance().nextStep();
                    }
                    return;
                }
            }
            // Enemy Base or unit on top: Show standard InfoPanel scouting
            gameHUD.showBaseInfo(foundStructure, structName,
                    foundStructure.getComponent(TextureComponent.class).region, true);

            // Tutorial Hook: Check Stats (Enemy Base)
            if (TutorialManager.getInstance().isActive()
                    && TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.CHECK_STATS) {
                TutorialManager.getInstance().nextStep();
            }

            return;
        }

        // --- Handle Specialized Structures (like PORTS) ---
        if (structStats != null && structStats.owner == screen.getActiveLocalPlayer()) {
            if (structStats.name.equalsIgnoreCase("Port")) {
                Entity unitOnTop = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (unitOnTop == null) {
                    int portLevel = findMaxBaseLevelNear(gridX, gridY, structStats.owner);
                    gameHUD.openSummonMenu(structStats.owner, screen.getGameState(), portLevel, "PORT");
                    return;
                }
            }
        }

        // If it's a structure with stats (income/xp), use showBaseInfo to show stats
        if (structStats != null && (structStats.income > 0 || structStats.xpGain > 0 || structStats.owner > 0)) {
            gameHUD.showBaseInfo(foundStructure, structName, unitFactory.getTextureForPopup(structStats.unitTypeKey),
                    true);
        } else {
            UnitFactory.UiInfo info = unitFactory.getObjectUi(objType);
            gameHUD.showTileInfo(info.name, info.region);
        }
    }

    // -------------------------------------------------------------------------
    // Hunt
    // -------------------------------------------------------------------------

    public void performHunt(Entity animal, Entity hunter) {
        StatsComponent hunterStats = hunter.getComponent(StatsComponent.class);
        GameState state = screen.getGameState();
        GameLogger.log(GameLogger.CAPTURE, hunterStats.owner,
                "Hunt: " + hunterStats.name + " hunted animal at " + GameLogger.pos(
                        animal.getComponent(GridPositionComponent.class) != null
                                ? animal.getComponent(GridPositionComponent.class).x
                                : -1,
                        animal.getComponent(GridPositionComponent.class) != null
                                ? animal.getComponent(GridPositionComponent.class).y
                                : -1)
                        + " | +" + CombatConstants.ANIMAL_HUNT_FUNDING + " funding");
        if (hunterStats.owner == 1)
            state.p1Funding += CombatConstants.ANIMAL_HUNT_FUNDING;
        else
            state.p2Funding += CombatConstants.ANIMAL_HUNT_FUNDING;

        if (screen.getGameState().isLanGame) {
            screen.syncEconomy(hunterStats.owner);
        }

        AudioManager.getInstance().playSFX(SFXKeys.ACTION_HUNT);
        engine.removeEntity(animal);
        hunterStats.hasActed = true;
        hunterStats.hasMoved = true;
        int income = screen.calculateIncome(hunterStats.owner);
        gameHUD.updateFunding(hunterStats.owner, (hunterStats.owner == 1) ? state.p1Funding : state.p2Funding, income);
        gameHUD.hideSummonMenu();
        deselect();
    }

    // -------------------------------------------------------------------------
    // Movement
    // -------------------------------------------------------------------------

    public void moveUnit(Entity unit, int targetX, int targetY) {
        // --- Stealth Rule: Block movement into cloaked units silently ---
        Entity tileUnit = getEntityAt(targetX, targetY, TypeComponent.Type.UNIT);
        if (tileUnit != null) {
            // Unresponsive: just return without clearing selection or markers
            return;
        }

        GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);
        if (pos == null)
            return;
        int oldX = pos.x, oldY = pos.y;

        StatsComponent stats = unit.getComponent(StatsComponent.class);
        if (screen.getGameState().isLanGame) {
            screen.getNetworkManager().send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_MOVE,
                    oldX + "," + oldY + "," + targetX + "," + targetY));
            screen.syncEconomy(stats.owner);
        }

        String unitName = (stats != null) ? stats.name : "?";
        int owner = (stats != null) ? stats.owner : 0;
        GameLogger.log(GameLogger.MOVE, owner,
                unitName + " moves " + GameLogger.move(oldX, oldY, targetX, targetY));
        unit.add(new MovementComponent(pos.x, pos.y, targetX, targetY));
        pos.x = targetX;
        pos.y = targetY;

        // Update spatial index for the moved entity
        long[] info = entityIndexInfo.get(unit);
        if (info != null) {
            spatialIndex.get(TypeComponent.Type.values()[(int) info[1]]).remove(info[0]);
            long newKey = cellKey(targetX, targetY);
            spatialIndex.get(TypeComponent.Type.values()[(int) info[1]]).put(newKey, unit);
            info[0] = newKey;
        }

        // RANGER: Overwatch (Check if move triggers an enemy attack)
        combatSystem.checkOverwatch(unit, targetX, targetY);
        if (stats != null) {
            stats.hasMoved = true;
            stats.hasActed = true;

            // Play Movement SFX
            if (stats.moveType == StatsComponent.MoveType.AIR) {
                if (stats.unitType == UnitType.APACHE) {
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_AIR_HELICOPTER);
                } else if (stats.unitType == UnitType.RECON_DRONE
                        || stats.unitType == UnitType.SUICIDE_DRONE) {
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_AIR_DRONE);
                } else {
                    // B2 and any future air units
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_AIR_B2);
                }
            } else if (stats.moveType == StatsComponent.MoveType.SEA) {
                AudioManager.getInstance().playSFX(SFXKeys.MOVE_WATER);
            } else {
                // LAND — each weight class has its own sound
                if (stats.unitType == UnitType.JUGGERNAUT) {
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_LAND_HEAVY_JUGGERNAUT);
                } else if (stats.unitType == UnitType.TANK) {
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_LAND_HEAVY_TANK);
                } else {
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_LAND_LIGHT);
                }
            }
        }

        AbilitiesComponent abilities = unit.getComponent(AbilitiesComponent.class);
        if (abilities != null) {
            abilities.isDiggingIn = false;
            abilities.pendingSkirmishMove = false;
        }
        gameHUD.hideSummonMenu();
        triggerBounce(targetX, targetY);
        clearMarkers();
        selectedUnitEntity = null;

        // Tutorial Hook: Move Unit
        if (TutorialManager.getInstance().isActive()
                && TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.MOVE_UNIT) {
            TutorialManager.getInstance().nextStep();
        }

        // Tutorial Hook: Move to Town
        if (TutorialManager.getInstance().isActive()
                && TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.MOVE_TO_TOWN
                && gameMap.objects[targetX][targetY] == com.militopia.map.MapGenerator.ObjectType.TOWN) {
            TutorialManager.getInstance().nextStep();
        }

        // Tutorial Hook: Move to Tree
        if (TutorialManager.getInstance().isActive()
                && TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.MOVE_TO_TREE
                && gameMap.objects[targetX][targetY] == com.militopia.map.MapGenerator.ObjectType.TREE) {
            TutorialManager.getInstance().nextStep();
        }

        // Tutorial Hook: Move to Deer (onto the same tile as the deer entity)
        if (TutorialManager.getInstance().isActive()
                && TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.MOVE_TO_DEER) {
            ImmutableArray<Entity> allEntities = engine.getEntitiesFor(
                    Family.all(GridPositionComponent.class).get());
            for (Entity e : allEntities) {
                GridPositionComponent ePos = e.getComponent(GridPositionComponent.class);
                if (ePos != null && ePos.x == targetX && ePos.y == targetY
                        && e.getComponent(com.militopia.components.AnimalComponent.class) != null) {
                    TutorialManager.getInstance().nextStep();
                    break;
                }
            }
        }

        // Tutorial Hook: Move adjacent to enemy (for attack step)
        if (TutorialManager.getInstance().isActive()
                && TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.MOVE_TO_ATTACK) {
            StatsComponent movedStats = unit.getComponent(StatsComponent.class);
            ImmutableArray<Entity> allEntities = engine.getEntitiesFor(
                    Family.all(GridPositionComponent.class, StatsComponent.class).get());
            for (Entity e : allEntities) {
                if (e == unit) continue;
                GridPositionComponent ePos = e.getComponent(GridPositionComponent.class);
                StatsComponent eStats = e.getComponent(StatsComponent.class);
                if (ePos != null && eStats != null && movedStats != null
                        && eStats.owner != movedStats.owner
                        && chebyshev(ePos.x, ePos.y, targetX, targetY) == 1) {
                    TutorialManager.getInstance().nextStep();
                    break;
                }
            }
        }

        // Tutorial Hook: Move to Ruins
        if (TutorialManager.getInstance().isActive()
                && TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.MOVE_TO_RUINS
                && gameMap.objects[targetX][targetY] == com.militopia.map.MapGenerator.ObjectType.RUINS) {
            TutorialManager.getInstance().nextStep();
        }
    }

    public void transformUnit(Entity unit, int targetX, int targetY) {
        StatsComponent stats = unit.getComponent(StatsComponent.class);
        GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);
        if (stats == null || pos == null)
            return;

        UnitType targetType = null;
        if (stats.unitType == UnitType.GUNBOAT)
            targetType = UnitType.RANGER;
        else if (stats.unitType == UnitType.DESTROYER)
            targetType = UnitType.TANK;

        if (targetType == null)
            return;

        int owner = stats.owner;
        int currentHP = stats.currentHP;
        int oldX = pos.x;
        int oldY = pos.y;

        GameLogger.log(GameLogger.MOVE, owner,
                stats.name + " transforms into " + targetType.name() + " at " + GameLogger.pos(targetX, targetY));

        // Remove old unit
        engine.removeEntity(unit);

        // Create new unit at target position
        unitFactory.createUnit(targetType, targetX, targetY, owner, true);

        // Update HP to match old unit and add movement animation
        Entity newUnit = getEntityAt(targetX, targetY, TypeComponent.Type.UNIT);
        if (newUnit != null) {
            StatsComponent newStats = newUnit.getComponent(StatsComponent.class);
            if (newStats != null) {
                newStats.currentHP = currentHP;
                newStats.hasActed = true;
                newStats.hasMoved = true;
            }
            // Add movement animation from old water tile to new land tile
            newUnit.add(new MovementComponent(oldX, oldY, targetX, targetY));

            // SFX for the new unit type move
            if (targetType == UnitType.TANK) {
                AudioManager.getInstance().playSFX(SFXKeys.MOVE_LAND_HEAVY_TANK);
            } else {
                AudioManager.getInstance().playSFX(SFXKeys.MOVE_LAND_LIGHT);
            }
        }

        AudioManager.getInstance().playSFX(SFXKeys.UNIT_DEPLOY);
        gameHUD.hideSummonMenu();
        gameHUD.hideTileInfo();
        clearMarkers();
        selectedUnitEntity = null;
    }

    // -------------------------------------------------------------------------
    // Range markers
    // -------------------------------------------------------------------------

    /**
     * Shows both blue movement markers AND red attack-range markers for the
     * selected unit simultaneously.
     */
    public void showRangeMarkers(int startX, int startY) {
        StatsComponent stats = selectedUnitEntity.getComponent(StatsComponent.class);
        int moveRange = (stats != null) ? stats.move : 3;
        int atkRange = (stats != null) ? stats.attackRange : 1;
        StatsComponent.MoveType moveType = (stats != null) ? stats.moveType : StatsComponent.MoveType.LAND;

        // GUNBOAT: Skirmish — cap move to 1 tile after attacking
        AbilitiesComponent selectedAbilities = selectedUnitEntity.getComponent(AbilitiesComponent.class);
        if (selectedAbilities != null && selectedAbilities.pendingSkirmishMove) {
            moveRange = 1;
        }

        // Move range SFX — play once per marker reveal
        AudioManager.getInstance().playSFX(SFXKeys.TILE_MOVE_RANGE);

        // --- Blue move markers ---
        // Juggernaut always jumps — skip flood fill so red attack markers aren't
        // shadowed
        boolean isJuggernautSelected = stats.unitType == UnitType.JUGGERNAUT;
        if (!stats.hasMoved && !isJuggernautSelected) {
            int[][] visitedMoves = new int[gameMap.width][gameMap.height];
            for (int[] row : visitedMoves)
                java.util.Arrays.fill(row, -1);
            floodFill(startX, startY, moveRange * 2, visitedMoves, startX, startY, moveType, stats.unitType,
                    stats.owner);
        }

        // --- Red attack markers ---
        int attackMarkersAdded = 0;
        for (int dx = -atkRange; dx <= atkRange; dx++) {
            for (int dy = -atkRange; dy <= atkRange; dy++) {
                if (dx == 0 && dy == 0)
                    continue; // skip self
                int tx = startX + dx;
                int ty = startY + dy;
                if (tx < 0 || tx >= gameMap.width || ty < 0 || ty >= gameMap.height)
                    continue;
                if (Math.max(Math.abs(dx), Math.abs(dy)) > atkRange)
                    continue;

                // Don't double-up on a tile that is already a blue move marker
                if (getEntityAt(tx, ty, TypeComponent.Type.MARKER) != null)
                    continue;
                // Only show attack markers on tiles occupied by enemies OR empty enemy-reachable tiles
                Entity tileUnit = getVisibleUnitAt(tx, ty);
                if (tileUnit != null) {
                    StatsComponent ts = tileUnit.getComponent(StatsComponent.class);
                    if (ts != null && ts.owner == screen.getActiveLocalPlayer())
                        continue; // skip own units
                }

                // Only show attack markers if the unit has NOT already acted
                boolean isJuggernaut = stats != null && stats.unitType == UnitType.JUGGERNAUT;
                if (!stats.hasActed && !stats.hasMoved && (tileUnit != null || isJuggernaut)) {
                    // Juggernaut cannot jump onto water
                    if (isJuggernaut) {
                        MapGenerator.TerrainType t = gameMap.terrain[tx][ty];
                        if (t == MapGenerator.TerrainType.WATER || t == MapGenerator.TerrainType.DEEP_WATER)
                            continue;
                    }
                    entityFactory.createAttackMarker(tx, ty);
                    attackMarkersAdded++;
                }
            }
        }

        // Attack range SFX — only play if at least one attack marker was actually spawned
        if (attackMarkersAdded > 0) {
            AudioManager.getInstance().playSFX(SFXKeys.TILE_ATTACK_RANGE);
        }

        // --- Transformation markers: Gunboat/Destroyer near land ---
        if (!stats.hasMoved && (stats.unitType == UnitType.GUNBOAT || stats.unitType == UnitType.DESTROYER)) {
            int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
            for (int[] d : dirs) {
                int nx = startX + d[0];
                int ny = startY + d[1];
                if (nx >= 0 && nx < gameMap.width && ny >= 0 && ny < gameMap.height) {
                    MapGenerator.TerrainType t = gameMap.terrain[nx][ny];
                    // If it's land and no unit is there
                    if (t != MapGenerator.TerrainType.WATER && t != MapGenerator.TerrainType.DEEP_WATER) {
                        if (getEntityAt(nx, ny, TypeComponent.Type.UNIT) == null) {
                            entityFactory.createTransformMarker(nx, ny);
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Movement helpers
    // -------------------------------------------------------------------------

    /**
     * Movement cost in x2-scaled units. Rail-to-rail LAND steps = 1 (half cost).
     * All other steps = 2 (normal cost). No bonus in enemy territory.
     */
    private int getRailStepCost(int fromX, int fromY, int toX, int toY,
            StatsComponent.MoveType moveType, int unitOwner) {
        if (moveType != StatsComponent.MoveType.LAND)
            return 2;
        if (fromX < 0 || fromX >= gameMap.width || fromY < 0 || fromY >= gameMap.height)
            return 2;
        if (toX < 0 || toX >= gameMap.width || toY < 0 || toY >= gameMap.height)
            return 2;
        if (!gameMap.rails[fromX][fromY] || !gameMap.rails[toX][toY])
            return 2;
        // No bonus in enemy territory
        int enemyOwner = (unitOwner == 1) ? 2 : 1;
        int[] fromCheck = findControllingBase(fromX, fromY, enemyOwner);
        int[] toCheck = findControllingBase(toX, toY, enemyOwner);
        if (fromCheck[0] == 1 || toCheck[0] == 1)
            return 2;
        return 1; // half cost on connected rail in friendly/neutral territory
    }

    /** Flood-fill BFS for movement range with x2 integer scaling for rail bonus. */
    private void floodFill(int x, int y, int remainingMoves, int[][] visitedMoves,
            int startX, int startY, StatsComponent.MoveType moveType, UnitType unitType,
            int unitOwner) {
        if (remainingMoves < 0)
            return;
        if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height)
            return;
        if (visitedMoves[x][y] >= remainingMoves)
            return;

        boolean isStart = (x == startX && y == startY);
        if (!isStart && !isWalkable(x, y, moveType, unitType))
            return;

        visitedMoves[x][y] = remainingMoves;
        if (!isStart && getEntityAt(x, y, TypeComponent.Type.MARKER) == null) {
            entityFactory.createMovementMarker(x, y);
        }

        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { -1, 1 }, { 1, -1 }, { -1, -1 } };
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            int cost = getRailStepCost(x, y, nx, ny, moveType, unitOwner);
            floodFill(nx, ny, remainingMoves - cost, visitedMoves, startX, startY, moveType, unitType, unitOwner);
        }
    }

    private boolean isWalkable(int x, int y, StatsComponent.MoveType moveType, UnitType unitType) {
        if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height)
            return false;
        MapGenerator.TerrainType terrain = gameMap.terrain[x][y];
        if (moveType == StatsComponent.MoveType.LAND) {
            if (terrain == MapGenerator.TerrainType.WATER || terrain == MapGenerator.TerrainType.DEEP_WATER)
                return false;

            // TANK Restriction: Cannot move to mountains
            if (unitType == UnitType.TANK && gameMap.objects[x][y] == MapGenerator.ObjectType.MOUNTAIN_OBJ) {
                return false;
            }
        } else if (moveType == StatsComponent.MoveType.SEA) {
            if (terrain != MapGenerator.TerrainType.WATER && terrain != MapGenerator.TerrainType.DEEP_WATER)
                return false;
        }
        // Stealth Rule: Invisible enemies do not block movement range markers
        Entity existingUnit = getVisibleUnitAt(x, y);
        if (existingUnit != null) {
            return false; // Blocks move
        }
        return true;
    }
}
