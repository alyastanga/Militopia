package com.militopia.managers;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.militopia.data.GameState;
import com.militopia.data.StructureData;
import com.militopia.data.UnitData;
import com.militopia.data.AnimalData;
import com.militopia.components.AbilitiesComponent;
import com.militopia.components.AnimalComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.map.MapGenerator;
import com.militopia.utils.GameLogger;

public class SaveManager {

    /**
     * Collects current engine state into the GameState data object.
     * Package-private for testability — call saveGame() in production.
     */
    void collectState(GameState state, PooledEngine engine, MapGenerator.GameMap map) {
        state.units.clear();
        state.structures.clear();
        state.animals.clear();

        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class).get());

        for (Entity e : entities) {
            TypeComponent type = e.getComponent(TypeComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            StatsComponent stats = e.getComponent(StatsComponent.class);

            if (type.type == TypeComponent.Type.UNIT) {
                String savedTypeKey = (stats.unitType != null) ? stats.unitType.name() : stats.unitTypeKey;
                AbilitiesComponent ab = e.getComponent(AbilitiesComponent.class);

                UnitData ud = new UnitData(pos.x, pos.y, stats.name, stats.owner, savedTypeKey,
                        stats.currentHP, stats.maxHP, stats.hasMoved, stats.hasActed,
                        ab != null && ab.isCloaked,
                        ab != null && ab.isCloakBroken,
                        ab != null && ab.isDiggingIn,
                        ab != null && ab.hasUsedDigIn,
                        ab != null && ab.isOverwatchActive,
                        ab != null && ab.pendingSkirmishMove,
                        ab != null && ab.isUnreachable,
                        ab != null ? ab.fuel : -1);
                ud.nukeCooldown = ab != null ? ab.nukeCooldown : 0;
                ud.idleTimer    = ab != null ? ab.idleTimer : 0f;
                state.units.add(ud);
            } else if (type.type == TypeComponent.Type.OBJECT) {

                // --- DETECT ANIMALS VIA COMPONENT TAG ---
                AnimalComponent animal = e.getComponent(AnimalComponent.class);
                if (animal != null) {
                    state.animals.add(new AnimalData(pos.x, pos.y, animal.animalType));
                }
                // --- DETECT STRUCTURES ---
                else if (stats != null && stats.owner != 0) {
                    StructureData sd = new StructureData(
                            pos.x, pos.y, stats.owner, stats.level,
                            stats.currentBaseXP, stats.name, stats.baseOrdinal,
                            stats.xpGain, stats.chosenSuperUnit);
                    sd.parentBaseX = stats.parentBaseX;
                    sd.parentBaseY = stats.parentBaseY;
                    sd.unitTypeKey = stats.unitTypeKey;
                    state.structures.add(sd);
                } else if (stats != null) {
                    // Check for neutral Towns
                    MapGenerator.ObjectType objType = map.objects[pos.x][pos.y];
                    if (objType == MapGenerator.ObjectType.TOWN) {
                        StructureData sdTown = new StructureData(
                                pos.x, pos.y, stats.owner, stats.level,
                                stats.currentBaseXP, stats.name, stats.baseOrdinal,
                                stats.xpGain, stats.chosenSuperUnit);
                        sdTown.unitTypeKey = stats.unitTypeKey;
                        state.structures.add(sdTown);
                    }
                }
            }
        }

        state.mapObjects = map.objects;
        state.railGrid = map.rails;
    }

    /**
     * Collects current engine state and writes it to a JSON save file.
     *
     * @param state  game state object that receives collected data and provides the save file name
     * @param engine Ashley engine whose entities are serialised
     * @param map    game map needed for object-type context during collection
     */
    public void saveGame(GameState state, PooledEngine engine, MapGenerator.GameMap map) {
        collectState(state, engine, map);

        Json json = new Json();
        String jsonText = json.toJson(state);

        FileHandle file = Gdx.files.local("saves/" + state.saveName + ".json");
        file.writeString(jsonText, false);

        GameLogger.logScreen("Game Saved: " + file.path());
    }
}
