package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.config.CombatConstants;
import com.militopia.config.StructureType;
import com.militopia.map.MapGenerator;

public class FogSystem extends EntitySystem {

    private final MapGenerator.GameMap gameMap;
    private ImmutableArray<Entity> entities;

    // Changed from final to allow switching
    private int playerID;

    public FogSystem(MapGenerator.GameMap map, int initialPlayerID) {
        this.gameMap = map;
        this.playerID = initialPlayerID;
        this.priority = -1; // Must run BEFORE UnitRenderSystem (priority 1) and MapRenderSystem (priority 0)
    }

    // --- NEW: Switch Active Player ---
    public void setPlayer(int id) {
        this.playerID = id;
    }

    @Override
    public void addedToEngine(com.badlogic.ashley.core.Engine engine) {
        entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, StatsComponent.class).get());
    }

    private boolean[][] jammerMask;

    @Override
    public void update(float deltaTime) {
        // 1. Reset Visibility and Jammer Mask
        if (jammerMask == null || jammerMask.length != gameMap.width || jammerMask[0].length != gameMap.height) {
            jammerMask = new boolean[gameMap.width][gameMap.height];
        }

        for (int x = 0; x < gameMap.width; x++) {
            for (int y = 0; y < gameMap.height; y++) {
                gameMap.visibleTiles[x][y] = false;
                gameMap.detectedTiles[x][y] = false; // Reset Detection
                jammerMask[x][y] = false;
            }
        }

        // 2. Identify Enemy Jammers (Blocking our vision)
        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            if (stats.owner != playerID && StructureType.fromKey(stats.unitTypeKey) == StructureType.SIGNAL_JAMMER) {
                GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
                markJammingZone(pos.x, pos.y, CombatConstants.JAMMER_RADIUS);
            }
        }

        // 3. Clear Fog and Detect Stealth for Current Active Player
        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);

            if (stats.owner == playerID) {
                int radius = stats.vision;
                // Jammer Override: units inside a jammed zone see only 1 tile
                if (jammerMask[pos.x][pos.y]) {
                    radius = CombatConstants.JAMMER_SUPPRESSED_VISION;
                }

                clearFog(pos.x, pos.y, radius);

                // Stealth Detection: all units can spot cloaked enemies in adjacent tiles
                markDetectionZone(pos.x, pos.y, CombatConstants.STEALTH_DETECTION_RADIUS);
            }
        }
    }

    private void markDetectionZone(int centerX, int centerY, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (isValid(x, y)) {
                    gameMap.detectedTiles[x][y] = true;
                }
            }
        }
    }

    private void markJammingZone(int centerX, int centerY, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (isValid(x, y)) {
                    jammerMask[x][y] = true;
                }
            }
        }
    }

    private void clearFog(int centerX, int centerY, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (isValid(x, y)) {
                    // Normal tiles revealed up to radius.
                    // Jammed tiles ONLY revealed if within radius 1 of the unit.
                    int dist = Math.max(Math.abs(x - centerX), Math.abs(y - centerY));
                    if (!jammerMask[x][y] || dist <= CombatConstants.JAMMER_SUPPRESSED_VISION) {
                        gameMap.visibleTiles[x][y] = true;
                    }
                }
            }
        }
    }

    private boolean isValid(int x, int y) {
        return x >= 0 && x < gameMap.width && y >= 0 && y < gameMap.height;
    }
}