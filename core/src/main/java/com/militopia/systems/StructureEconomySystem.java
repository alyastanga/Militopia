package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.militopia.components.FloatingTextComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.config.StructureType;
import com.militopia.data.GameState;
import com.militopia.factories.EntityFactory;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.ui.GameHUD;
import com.militopia.utils.CountingSort;
import com.militopia.utils.GameLogger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * StructureEconomySystem — Ashley ECS system responsible for all per-turn
 * structure economic processing. This decouples the economy loop from
 * GameScreen so it can be unit-tested and extended independently.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Distribute XP from specialized structures to their linked parent
 * base</li>
 * <li>Apply Hospital healing (+3 HP) to adjacent friendly units</li>
 * <li>Apply natural base XP growth per turn</li>
 * <li>Trigger base level-up checks via UnitFactory</li>
 * </ul>
 *
 * <p>
 * Income calculation remains in
 * {@link com.militopia.screen.GameScreen#calculateIncome}
 * because it is also queried mid-turn (e.g. for HUD display after capturing a
 * structure).
 * XP-side processing is fully owned here.
 *
 * <p>
 * Called explicitly by GameScreen at the start of each player's turn (after
 * the fade-out transition) via {@link #processTurn(int)}.
 */
public class StructureEconomySystem extends EntitySystem {

    private final GameState gameState;
    private final UnitFactory unitFactory;
    private final EntityFactory entityFactory;
    private GameHUD gameHUD;

    public StructureEconomySystem(GameState gameState, UnitFactory unitFactory, EntityFactory entityFactory,
            GameHUD gameHUD) {
        // Priority 0 — runs last in the engine update chain (we call it manually)
        super(0);
        this.gameState = gameState;
        this.unitFactory = unitFactory;
        this.entityFactory = entityFactory;
        this.gameHUD = gameHUD;
    }

    /**
     * Called after GameHUD is constructed, since HUD is created after the engine
     * systems.
     */
    public void setGameHUD(GameHUD gameHUD) {
        this.gameHUD = gameHUD;
    }

    /**
     * Main entry point. Called once per turn start for the player whose turn began.
     *
     * @param playerID the player whose structures should generate economy (1 or 2)
     * @return total XP gained this turn processing (for logging / HUD)
     */
    public int processTurn(int playerID) {
        if (gameState.turnCount <= 1) {
            // No XP or structure bonuses on Turn 1 (first round setup)
            return 0;
        }

        // Play resource collection SFX once at the start of each turn's economy processing
        AudioManager.getInstance().playSFX(SFXKeys.RESOURCE_COLLECT);

        List<Entity> myBases = new ArrayList<>();
        List<Entity> xpStructures = new ArrayList<>();

        ImmutableArray<Entity> entities = getEngine().getEntitiesFor(
                Family.all(StatsComponent.class).get());

        for (Entity entity : entities) {
            StatsComponent stats = entity.getComponent(StatsComponent.class);
            if (stats.owner != playerID)
                continue;

            if (stats.income >= 2 && stats.unitTypeKey != null && stats.unitTypeKey.startsWith("BASE")) {
                myBases.add(entity);
            } else if (stats.xpGain > 0 && stats.parentBaseX != -1
                    && entity.getComponent(GridPositionComponent.class) != null) {
                xpStructures.add(entity);
            }
        }

        int totalXPGain = 0;

        // 1. Structure XP → parent base
        for (Entity struct : xpStructures) {
            StatsComponent structStats = struct.getComponent(StatsComponent.class);
            Entity parentBase = findEntityAt(structStats.parentBaseX, structStats.parentBaseY);
            if (parentBase != null) {
                StatsComponent baseStats = parentBase.getComponent(StatsComponent.class);
                if (baseStats.owner == playerID) {
                    baseStats.currentBaseXP += structStats.xpGain;
                    totalXPGain += structStats.xpGain;

                    // --- NEW: Floating Text for Structure XP ---
                    float worldX = EntityFactory.gridToIsoX(structStats.parentBaseX, structStats.parentBaseY);
                    float worldY = EntityFactory.gridToIsoY(structStats.parentBaseX, structStats.parentBaseY);
                    entityFactory.createFloatingText("+" + structStats.xpGain + " XP", worldX, worldY,
                            FloatingTextComponent.Type.XP);

                    GameLogger.log(GameLogger.ECONOMY, playerID,
                            "Structure XP: " + structStats.name
                                    + " → " + baseStats.name
                                    + " +" + structStats.xpGain + " XP");
                }
            }
        }

        // 2. Hospital: heal adjacent friendly units by +3 HP
        ImmutableArray<Entity> allUnits = getEngine().getEntitiesFor(
                Family.all(StatsComponent.class, GridPositionComponent.class, TypeComponent.class).get());
        for (Entity struct : xpStructures) {
            StatsComponent sStats = struct.getComponent(StatsComponent.class);
            if (!sStats.name.equals("Field Hospital"))
                continue;
            GridPositionComponent sPos = struct.getComponent(GridPositionComponent.class);
            for (Entity unit : allUnits) {
                TypeComponent tc = unit.getComponent(TypeComponent.class);
                if (tc.type != TypeComponent.Type.UNIT)
                    continue;
                StatsComponent uStats = unit.getComponent(StatsComponent.class);
                GridPositionComponent uPos = unit.getComponent(GridPositionComponent.class);
                if (uStats.owner == playerID
                        && uStats.moveType != StatsComponent.MoveType.AIR
                        && Math.max(Math.abs(sPos.x - uPos.x), Math.abs(sPos.y - uPos.y)) <= 1) {
                    int healed = Math.min(uStats.currentHP + 3, uStats.maxHP) - uStats.currentHP;
                    uStats.currentHP += healed;
                    if (healed > 0) {
                        GameLogger.log(GameLogger.ECONOMY, playerID,
                                "Hospital heal: " + uStats.name
                                        + " at " + GameLogger.pos(uPos.x, uPos.y)
                                        + " +" + healed + " HP");
                    }
                }
            }
        }

        // 3. Base natural XP growth + level-up check
        for (Entity base : myBases) {
            StatsComponent stats = base.getComponent(StatsComponent.class);
            GridPositionComponent pos = base.getComponent(GridPositionComponent.class);
            // Dynamic gain: 250 + (level-1)*10 per turn
            int naturalGain = 250 + ((stats.level - 1) * 10);
            stats.currentBaseXP += naturalGain;
            totalXPGain += naturalGain;

            // --- NEW: Floating Text for Natural XP & Income ---
            if (pos != null) {
                float worldX = EntityFactory.gridToIsoX(pos.x, pos.y);
                float worldY = EntityFactory.gridToIsoY(pos.x, pos.y);

                // Income text (Bases reflect the per-turn income distrib)
                if (stats.income > 0) {
                    entityFactory.createFloatingText("+$" + stats.income, worldX, worldY,
                            FloatingTextComponent.Type.FUNDING);
                    worldY += 15f;
                }
                // XP text
                entityFactory.createFloatingText("+" + naturalGain + " XP", worldX, worldY,
                        FloatingTextComponent.Type.XP);
            }

            unitFactory.checkAndApplyLevelUp(base, gameState, gameHUD);
        }

        // 4. Update global player XP
        if (playerID == 1) {
            gameState.p1XP += totalXPGain;
        } else {
            gameState.p2XP += totalXPGain;
        }

        return totalXPGain;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Entity findEntityAt(int x, int y) {
        ImmutableArray<Entity> entities = getEngine().getEntitiesFor(
                Family.all(GridPositionComponent.class, TypeComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TypeComponent type = e.getComponent(TypeComponent.class);
            if (pos.x == x && pos.y == y && type.type == TypeComponent.Type.OBJECT) {
                return e;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Income / XP queries (used by GameScreen, InfoPanel, SlideMenu, HUD)
    // -------------------------------------------------------------------------

    /** Per-entity income, including Solar Array Adjacency Bonus. */
    public int calculateBaseIncome(Entity entity) {
        StatsComponent stats = entity.getComponent(StatsComponent.class);
        if (stats == null) return 0;

        int income = stats.income;
        GridPositionComponent pos = entity.getComponent(GridPositionComponent.class);
        if (pos != null && StructureType.fromKey(stats.unitTypeKey) == StructureType.SOLAR) {
            // SOLAR ARRAY: Adjacency Bonus (+1 income for each adjacent friendly structure)
            // Use index-based loop to avoid nested-iterator crash when called from
            // inside another for-each over the same family (e.g. calculateGroupedBaseIncome).
            ImmutableArray<Entity> entities = getEngine().getEntitiesFor(
                    Family.all(GridPositionComponent.class, StatsComponent.class).get());
            for (int i = 0; i < entities.size(); i++) {
                Entity other = entities.get(i);
                if (other == entity) continue;
                StatsComponent oStats = other.getComponent(StatsComponent.class);
                GridPositionComponent oPos = other.getComponent(GridPositionComponent.class);
                if (oStats.owner == stats.owner
                        && (oStats.income > 0 || (oStats.unitTypeKey != null && oStats.unitTypeKey.startsWith("BASE")))
                        && Math.max(Math.abs(pos.x - oPos.x), Math.abs(pos.y - oPos.y)) <= 1) {
                    income++;
                }
            }
        }
        return income;
    }

    /** Grouped income for a base: base income + all linked child structures. */
    public int calculateGroupedBaseIncome(Entity base) {
        StatsComponent stats = base.getComponent(StatsComponent.class);
        if (stats == null || stats.unitTypeKey == null || !stats.unitTypeKey.startsWith("BASE"))
            return calculateBaseIncome(base);

        int total = calculateBaseIncome(base);
        GridPositionComponent pos = base.getComponent(GridPositionComponent.class);
        if (pos != null) {
            ImmutableArray<Entity> entities = getEngine().getEntitiesFor(
                    Family.all(StatsComponent.class, GridPositionComponent.class).get());
            for (Entity other : entities) {
                if (other == base) continue;
                StatsComponent oStats = other.getComponent(StatsComponent.class);
                if (oStats.owner == stats.owner && oStats.parentBaseX == pos.x && oStats.parentBaseY == pos.y) {
                    total += calculateBaseIncome(other);
                }
            }
        }
        return total;
    }

    /** XP gain preview for a base (natural + linked structures). Used by InfoPanel. */
    public int calculateBaseXPGain(Entity base) {
        StatsComponent stats = base.getComponent(StatsComponent.class);
        if (stats == null) return 0;

        if (StructureType.fromKey(stats.unitTypeKey) != StructureType.BASE)
            return stats.xpGain;

        int total = 250 + ((stats.level - 1) * 10);
        GridPositionComponent bPos = base.getComponent(GridPositionComponent.class);
        if (bPos != null) {
            ImmutableArray<Entity> entities = getEngine().getEntitiesFor(
                    Family.all(StatsComponent.class, GridPositionComponent.class).get());
            for (Entity other : entities) {
                StatsComponent oStats = other.getComponent(StatsComponent.class);
                if (oStats.owner == stats.owner && oStats.parentBaseX == bPos.x && oStats.parentBaseY == bPos.y) {
                    total += oStats.xpGain;
                }
            }
        }
        return total;
    }

    /** Total gross income for a player across all owned structures. */
    public int calculateIncome(int playerID) {
        int total = 0;
        ImmutableArray<Entity> entities = getEngine().getEntitiesFor(Family.all(StatsComponent.class).get());
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            StatsComponent stats = entity.getComponent(StatsComponent.class);
            if (stats.owner == playerID && entity.getComponent(TypeComponent.class).type == TypeComponent.Type.OBJECT) {
                total += calculateBaseIncome(entity);
            }
        }
        return total;
    }

    public int calculateNetIncome(int playerID) {
        return calculateIncome(playerID);
    }

    /**
     * Returns a per-structure-name income breakdown for a player.
     * Identical names are merged (e.g. two Towns → "Town: +4").
     * Structures contributing 0 income are omitted.
     */
    public LinkedHashMap<String, Integer> getIncomeBreakdown(int playerID) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        ImmutableArray<Entity> entities = getEngine().getEntitiesFor(Family.all(StatsComponent.class).get());
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            StatsComponent stats = entity.getComponent(StatsComponent.class);
            if (stats.owner != playerID) continue;
            int inc = calculateBaseIncome(entity);
            if (inc <= 0) continue;
            Integer existing = map.get(stats.name);
            map.put(stats.name, (existing != null ? existing : 0) + inc);
        }
        return CountingSort.sortEconomyMap(map);
    }

    /**
     * Returns a per-structure XP breakdown for a player.
     * Bases use their name + ordinal as key to distinguish multiple bases.
     * Returns an empty map on Turn 1.
     */
    public LinkedHashMap<String, Integer> getXPBreakdown(int playerID) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        if (gameState.turnCount <= 1) return map;

        ImmutableArray<Entity> entities = getEngine().getEntitiesFor(Family.all(StatsComponent.class).get());
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            StatsComponent stats = entity.getComponent(StatsComponent.class);
            if (stats.owner != playerID) continue;

            if (stats.income >= 2 && stats.unitTypeKey != null && stats.unitTypeKey.startsWith("BASE")) {
                // Natural base XP
                int naturalGain = 250 + ((stats.level - 1) * 10);
                String key = (stats.baseOrdinal != null && !stats.baseOrdinal.isEmpty())
                        ? stats.name + " " + stats.baseOrdinal
                        : stats.name;
                Integer existing = map.get(key);
                map.put(key, (existing != null ? existing : 0) + naturalGain);
            } else if (stats.xpGain > 0 && stats.parentBaseX != -1) {
                // Structure XP contribution
                Integer existing = map.get(stats.name);
                map.put(stats.name, (existing != null ? existing : 0) + stats.xpGain);
            }
        }
        return CountingSort.sortEconomyMap(map);
    }
}
