package com.militopia.screen;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.militopia.components.AbilitiesComponent;
import com.militopia.components.AnimalComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.config.UnitType;
import com.militopia.data.StructureData;
import com.militopia.data.StructureSnapshot;
import com.militopia.data.TurnSnapshot;
import com.militopia.data.UnitSnapshot;
import com.militopia.controller.GameInputController;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.TurnHistoryManager;
import com.militopia.map.MapGenerator;
import com.militopia.systems.FogSystem;
import com.militopia.systems.StructureEconomySystem;
import com.militopia.systems.UnitRenderSystem;
import com.militopia.ui.GameHUD;

import java.util.ArrayList;
import java.util.List;

public class SnapshotRestorer {

    private final PooledEngine engine;
    private final GameState gameState;
    private final MapGenerator.GameMap gameMap;
    private final UnitFactory unitFactory;
    private final FogSystem fogSystem;
    private final UnitRenderSystem unitRenderSystem;
    private final GameHUD gameHUD;
    private final GameInputController inputController;
    private final TurnHistoryManager turnHistory;
    private final StructureEconomySystem structureEconomySystem;

    public SnapshotRestorer(
        PooledEngine engine,
        GameState gameState,
        MapGenerator.GameMap gameMap,
        UnitFactory unitFactory,
        FogSystem fogSystem,
        UnitRenderSystem unitRenderSystem,
        GameHUD gameHUD,
        GameInputController inputController,
        TurnHistoryManager turnHistory,
        StructureEconomySystem structureEconomySystem
    ) {
        this.engine = engine;
        this.gameState = gameState;
        this.gameMap = gameMap;
        this.unitFactory = unitFactory;
        this.fogSystem = fogSystem;
        this.unitRenderSystem = unitRenderSystem;
        this.gameHUD = gameHUD;
        this.inputController = inputController;
        this.turnHistory = turnHistory;
        this.structureEconomySystem = structureEconomySystem;
    }

    public void restore(TurnSnapshot snap) {
        // 1. Remove all UNIT entities and non-animal OBJECT entities from the engine
        List<Entity> toRemove = new ArrayList<>();
        ImmutableArray<Entity> all = engine.getEntitiesFor(
                Family.all(TypeComponent.class).get());
        for (Entity e : all) {
            TypeComponent t = e.getComponent(TypeComponent.class);
            if (t.type == TypeComponent.Type.UNIT) {
                toRemove.add(e);
            } else if (t.type == TypeComponent.Type.OBJECT && e.getComponent(AnimalComponent.class) == null) {
                toRemove.add(e);
            }
        }
        for (Entity e : toRemove)
            engine.removeEntity(e);

        // 2. Restore GameState scalars
        gameState.p1Funding = snap.p1Funding;
        gameState.p2Funding = snap.p2Funding;
        gameState.p1XP = snap.p1XP;
        gameState.p2XP = snap.p2XP;
        gameState.turnCount = snap.turn;
        gameState.currentPlayer = snap.currentPlayer;
        gameState.p1BaseCount = snap.p1BaseCount;
        gameState.p2BaseCount = snap.p2BaseCount;

        // 3. Restore map objects array (captures/uncaptures)
        for (int x = 0; x < gameMap.width; x++) {
            System.arraycopy(snap.mapObjects[x], 0, gameMap.objects[x], 0, gameMap.height);
        }

        // 4. Recreate structures from snapshot
        for (StructureSnapshot ss : snap.structures) {
            boolean isMapGenObject = false;
            MapGenerator.ObjectType objType = null;
            try {
                if (ss.unitTypeKey != null && !ss.unitTypeKey.isEmpty()) {
                    objType = MapGenerator.ObjectType.valueOf(ss.unitTypeKey);
                    isMapGenObject = true;
                }
            } catch (IllegalArgumentException ignored) {
                // Not a MapGenerator ObjectType, likely a built structure
            }

            if (isMapGenObject && objType != null) {
                unitFactory.createObjectEntity(ss.x, ss.y, objType, gameState);
                if (objType == MapGenerator.ObjectType.BASE_P1)
                    gameState.p1BaseCount--;
                if (objType == MapGenerator.ObjectType.BASE_P2)
                    gameState.p2BaseCount--;
            } else {
                unitFactory.createStructure(ss.unitTypeKey, ss.x, ss.y, ss.owner, ss.parentBaseX, ss.parentBaseY);
            }

            // After creation, find the entity and apply the snapshot stats
            ImmutableArray<Entity> objects = engine.getEntitiesFor(
                    Family.all(GridPositionComponent.class, TypeComponent.class, StatsComponent.class).get());
            for (Entity e : objects) {
                TypeComponent t = e.getComponent(TypeComponent.class);
                if (t.type != TypeComponent.Type.OBJECT)
                    continue;
                GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
                if (pos.x == ss.x && pos.y == ss.y) {
                    StructureData sd = new StructureData();
                    sd.x = ss.x;
                    sd.y = ss.y;
                    sd.owner = ss.owner;
                    sd.level = ss.level;
                    sd.currentBaseXP = ss.currentBaseXP;
                    sd.baseName = ss.name;
                    sd.baseOrdinal = ss.baseOrdinal;
                    sd.chosenSuperUnit = ss.chosenSuperUnit;
                    sd.xpGain = ss.xpGain;
                    unitFactory.updateStructureFromSave(e, sd, gameMap);
                    e.getComponent(StatsComponent.class).income = ss.income;
                    break;
                }
            }
        }

        // 5. Recreate unit entities from snapshot
        for (UnitSnapshot us : snap.units) {
            UnitType ut = UnitType.fromKey(us.unitTypeKey);
            if (ut == null)
                ut = UnitType.RECRUIT;
            unitFactory.createUnit(ut, us.x, us.y, us.owner, us.hasActed);
            ImmutableArray<Entity> freshUnits = engine.getEntitiesFor(
                    Family.all(GridPositionComponent.class, StatsComponent.class, TypeComponent.class).get());
            for (Entity e : freshUnits) {
                TypeComponent t = e.getComponent(TypeComponent.class);
                if (t.type != TypeComponent.Type.UNIT)
                    continue;
                GridPositionComponent p = e.getComponent(GridPositionComponent.class);
                StatsComponent s = e.getComponent(StatsComponent.class);
                if (p.x == us.x && p.y == us.y && s.owner == us.owner) {
                    s.currentHP = us.currentHP;
                    s.hasActed = us.hasActed;
                    s.hasMoved = us.hasMoved;

                    AbilitiesComponent a = e.getComponent(AbilitiesComponent.class);
                    if (a != null) {
                        a.isDiggingIn = us.isDiggingIn;
                        a.hasUsedDigIn = us.hasUsedDigIn;
                        a.isOverwatchActive = us.isOverwatchActive;
                        a.isCloaked = us.isCloaked;
                        a.isCloakBroken = us.isCloakBroken;
                        a.pendingSkirmishMove = us.pendingSkirmishMove;
                        a.isUnreachable = us.isUnreachable;
                        a.fuel = us.fuel;
                        a.nukeCooldown = us.nukeCooldown;
                        a.idleTimer = us.idleTimer;
                    }
                    break;
                }
            }
        }

        // 6. Refresh fog, HUD
        int fogPlayer = gameState.isLanGame ? gameState.localPlayerID : gameState.currentPlayer;
        fogSystem.setPlayer(fogPlayer);
        fogSystem.update(0);
        unitRenderSystem.setPlayer(fogPlayer);
        int currentFunds = (gameState.currentPlayer == 1) ? gameState.p1Funding : gameState.p2Funding;
        int income = structureEconomySystem.calculateIncome(gameState.currentPlayer);
        gameHUD.updateTurn(gameState.turnCount, gameState.currentPlayer, fogPlayer);
        gameHUD.updateFunding(gameState.currentPlayer, currentFunds, income);
        gameHUD.updateXP(gameState.currentPlayer, (gameState.currentPlayer == 1) ? gameState.p1XP : gameState.p2XP);
        gameHUD.hideTileInfo();
        inputController.clearMarkersPublic();

        // 7. Re-push restored state (preserves redo stack) and refresh panel
        turnHistory.pushRestore(unitFactory.captureSnapshot(engine, gameState, gameMap));
        gameHUD.refreshSnapshotPanel();
    }
}
