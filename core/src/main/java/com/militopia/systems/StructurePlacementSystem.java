package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.militopia.components.StatsComponent;
import com.militopia.config.StructureType;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.map.MapGenerator;
import com.militopia.utils.GameLogger;

/**
 * Handles the logic for validating and placing structures.
 * Extracted from SlideMenu to follow ECS patterns.
 */
public class StructurePlacementSystem {

    private final PooledEngine engine;
    private final UnitFactory factory;
    private final GameState gameState;
    private final MapGenerator.GameMap map;

    public StructurePlacementSystem(PooledEngine engine, UnitFactory factory, GameState gameState,
            MapGenerator.GameMap map) {
        this.engine = engine;
        this.factory = factory;
        this.gameState = gameState;
        this.map = map;
    }

    /**
     * Checks if a structure can be built on a specific tile.
     */
    public boolean canBuild(String struct, int x, int y, int owner, int cost, boolean isWater, boolean isCoastalWater,
            boolean isCoastalLand) {
        StructureType structureType = StructureType.fromKey(struct);

        int funds = (owner == 1) ? gameState.p1Funding : gameState.p2Funding;
        if (funds < cost) {
            GameLogger.log(GameLogger.BUILD, owner,
                    "Attempted " + struct + " — insufficient funds (" + funds + "<" + cost + ")");
            return false;
        }

        MapGenerator.ObjectType existingObj = map.objects[x][y];
        boolean isOilTile = (existingObj == MapGenerator.ObjectType.OIL);

        if (isOilTile) {
            if (structureType != StructureType.OIL_DERRICK) {
                GameLogger.log(GameLogger.BUILD, owner,
                        "Build " + struct + " at " + GameLogger.pos(x, y) + " — BLOCKED (must be OIL_DERRICK on Oil)");
                return false;
            }
        } else {
            if (structureType == StructureType.OIL_DERRICK) {
                GameLogger.log(GameLogger.BUILD, owner,
                        "Build " + struct + " at " + GameLogger.pos(x, y) + " — BLOCKED (must be Oil Reservoir)");
                return false;
            }
            if (existingObj != null && existingObj != MapGenerator.ObjectType.NONE) {
                GameLogger.log(GameLogger.BUILD, owner,
                        "Build " + struct + " at " + GameLogger.pos(x, y) + " — BLOCKED (occupied)");
                return false;
            }
        }

        // Terrain constraints
        if (structureType == StructureType.PORT) {
            if (!isWater || !isCoastalWater)
                return false;
        } else if (structureType == StructureType.NUCLEAR_PLANT) {
            if (isWater || !isCoastalLand)
                return false;
        } else if (structureType == StructureType.OIL_DERRICK) {
            // Oil Derricks allowed on both land and water (offshore drilling)
            // as long as there is an Oil Reservoir (checked earlier in this method)
        } else {
            if (isWater)
                return false;
        }

        // Check for existing entities at layer 1 (Structures)
        Entity existingAtLayer1 = factory.getEntityAt(x, y, 1);
        if (existingAtLayer1 != null) {
            StatsComponent stats = existingAtLayer1.getComponent(StatsComponent.class);
            boolean isOilRes = (stats != null && MapGenerator.ObjectType.OIL.name().equals(stats.unitTypeKey));
            if (!(structureType == StructureType.OIL_DERRICK && isOilRes)) {
                GameLogger.log(GameLogger.BUILD, owner,
                        "Build " + struct + " at " + GameLogger.pos(x, y) + " — BLOCKED (tile occupied)");
                return false;
            }
        }

        return true;
    }

    /**
     * Performs the actual building of a structure.
     */
    public void performBuild(String struct, int x, int y, int owner, int cost, int parentX, int parentY) {
        StructureType structureType = StructureType.fromKey(struct);

        // Remove existing Oil Reservoir if building a derrick
        MapGenerator.ObjectType existingObj = map.objects[x][y];
        if (structureType == StructureType.OIL_DERRICK && existingObj == MapGenerator.ObjectType.OIL) {
            map.objects[x][y] = MapGenerator.ObjectType.NONE;
            Entity existingAtLayer1 = factory.getEntityAt(x, y, 1);
            if (existingAtLayer1 != null) {
                engine.removeEntity(existingAtLayer1);
            }
        }

        if (owner == 1)
            gameState.p1Funding -= cost;
        else
            gameState.p2Funding -= cost;

        factory.createStructure(struct, x, y, owner, parentX, parentY);

        int remaining = (owner == 1) ? gameState.p1Funding : gameState.p2Funding;
        GameLogger.log(GameLogger.BUILD, owner, "Built " + struct + " at " + GameLogger.pos(x, y) + " | cost=" + cost
                + " | funds remaining=" + remaining);
    }
}
