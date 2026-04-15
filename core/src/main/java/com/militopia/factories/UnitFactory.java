package com.militopia.factories;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.militopia.components.*;
import com.militopia.config.BaseLevelConfig;
import com.militopia.config.GameConfig;
import com.militopia.config.StructureType;
import com.militopia.config.UnitStatConfig;
import com.militopia.config.UnitType;
import com.militopia.data.GameState;
import com.militopia.data.StructureSnapshot;
import com.militopia.data.TurnSnapshot;
import com.militopia.data.UnitSnapshot;
import com.militopia.managers.AssetManager;
import com.militopia.map.MapGenerator;
import com.militopia.ui.GameHUD;
import com.militopia.utils.GameLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UnitFactory {

    private final PooledEngine engine;
    private final AssetManager assets;
    private EntityFactory entityFactory;

    // Storage for the manually loaded regions
    private final Map<String, TextureRegion[]> unitRegions = new java.util.HashMap<>();
    private final Map<String, TextureRegion> baseRegions = new java.util.HashMap<>();
    private final Map<String, TextureRegion> structRegions = new java.util.HashMap<>();
    private final Map<String, TextureRegion> terrainRegions = new java.util.HashMap<>();
    private final Map<String, TextureRegion> animalRegions = new java.util.HashMap<>();

    public final TextureRegion fogRegion;

    // Idle animations (animals + structures)
    private final java.util.Map<String, Animation<TextureRegion>> idleAnims = new java.util.HashMap<>();

    // Tank idle — injected after EntityFactory builds it
    private Animation<TextureRegion> tankIdleAnim;

    public void setTankIdleAnim(Animation<TextureRegion> anim) {
        this.tankIdleAnim = anim;
    }

    public PooledEngine getEngine() {
        return engine;
    }

    public UnitFactory(PooledEngine engine, AssetManager assets) {
        this.engine = engine;
        this.assets = assets;

        // --- MANUAL LOADING: Base Levels ---
        // BASE_A = red (player 2), BASE_B = blue (player 1)
        baseRegions.put("lvl1_blue",  new TextureRegion(assets.get(AssetManager.BASE_B_1)));
        baseRegions.put("lvl1_red",   new TextureRegion(assets.get(AssetManager.BASE_A_1)));

        baseRegions.put("lvl2_blue",  new TextureRegion(assets.get(AssetManager.BASE_B_2)));
        baseRegions.put("lvl2_red",   new TextureRegion(assets.get(AssetManager.BASE_A_2)));

        baseRegions.put("lvl3_blue",  new TextureRegion(assets.get(AssetManager.BASE_B_3)));
        baseRegions.put("lvl3_red",   new TextureRegion(assets.get(AssetManager.BASE_A_3)));

        baseRegions.put("lvl4_blue",  new TextureRegion(assets.get(AssetManager.BASE_B_4)));
        baseRegions.put("lvl4_red",   new TextureRegion(assets.get(AssetManager.BASE_A_4)));

        baseRegions.put("lvl5_blue",  new TextureRegion(assets.get(AssetManager.BASE_B_5)));
        baseRegions.put("lvl5_red",   new TextureRegion(assets.get(AssetManager.BASE_A_5)));

        baseRegions.put("lvl6_blue",  new TextureRegion(assets.get(AssetManager.BASE_B_6)));
        baseRegions.put("lvl6_red",   new TextureRegion(assets.get(AssetManager.BASE_A_6)));

        baseRegions.put("lvl7_blue",  new TextureRegion(assets.get(AssetManager.BASE_B_7)));
        baseRegions.put("lvl7_red",   new TextureRegion(assets.get(AssetManager.BASE_A_7)));

        baseRegions.put("lvl8_blue",  new TextureRegion(assets.get(AssetManager.BASE_B_8)));
        baseRegions.put("lvl8_red",   new TextureRegion(assets.get(AssetManager.BASE_A_8)));

        baseRegions.put("lvl9_blue",  new TextureRegion(assets.get(AssetManager.BASE_B_9)));
        baseRegions.put("lvl9_red",   new TextureRegion(assets.get(AssetManager.BASE_A_9)));

        baseRegions.put("lvl10_blue", new TextureRegion(assets.get(AssetManager.BASE_B_10)));
        baseRegions.put("lvl10_red",  new TextureRegion(assets.get(AssetManager.BASE_A_10)));

        // UNITS — [0]=right, [1]=left (flipped copy)
        putUnit("RECRUIT",       assets.get(AssetManager.RECRUIT));
        putUnit("RANGER",        assets.get(AssetManager.RANGER));
        putUnit("TANK",          assets.get(AssetManager.TANK));
        putUnit("SNIPER",        assets.get(AssetManager.SNIPER));
        putUnit("RECON_DRONE",   assets.get(AssetManager.RECON_DRONE));
        putUnit("SUICIDE_DRONE", assets.get(AssetManager.SUICIDE_DRONE));
        putUnit("APACHE",        assets.get(AssetManager.APACHE));
        putUnit("GUNBOAT",       assets.get(AssetManager.GUNBOAT));
        putUnit("DESTROYER",     assets.get(AssetManager.DESTROYER));
        putUnit("CARRIER",       assets.get(AssetManager.CARRIER));
        putUnit("JUGGERNAUT",    assets.get(AssetManager.JUGGERNAUT));
        putUnit("B2",            assets.get(AssetManager.B2));
        putUnit("SUBMARINE",     assets.get(AssetManager.SUBMARINE));

        // Structures
        structRegions.put("MUNITION_FACTORY", new TextureRegion(assets.get(AssetManager.MUNITION_FACTORY)));
        structRegions.put("PORT",             new TextureRegion(assets.get(AssetManager.PORT)));
        structRegions.put("SOLAR",            new TextureRegion(assets.get(AssetManager.SOLAR_ARRAY)));
        structRegions.put("OIL_DERRICK",      new TextureRegion(assets.get(AssetManager.OIL_DERRICK)));
        structRegions.put("NUCLEAR",          new TextureRegion(assets.get(AssetManager.NUCLEAR_PLANT)));
        structRegions.put("HOSPITAL",         new TextureRegion(assets.get(AssetManager.FIELD_HOSPITAL)));
        structRegions.put("RADAR",            new TextureRegion(assets.get(AssetManager.RADAR_STATION)));
        structRegions.put("JAMMER",           new TextureRegion(assets.get(AssetManager.SIGNAL_JAMMER)));

        // Terrain tiles
        terrainRegions.put("GRASS",        new TextureRegion(assets.get(AssetManager.TILE_GRASS)));
        terrainRegions.put("WATER",        new TextureRegion(assets.get(AssetManager.TILE_WATER)));
        terrainRegions.put("DEEP_WATER",   new TextureRegion(assets.get(AssetManager.TILE_DEEPWATER)));
        terrainRegions.put("SAND",         new TextureRegion(assets.get(AssetManager.TILE_SAND)));
        terrainRegions.put("MOUNTAIN",     new TextureRegion(assets.get(AssetManager.TILE_MOUNTAIN)));

        // Objects
        terrainRegions.put("TREE",         new TextureRegion(assets.get(AssetManager.OBJ_TREE)));
        terrainRegions.put("RUINS",        new TextureRegion(assets.get(AssetManager.OBJ_RUINS)));
        terrainRegions.put("TOWN",         new TextureRegion(assets.get(AssetManager.STRUCT_TOWN)));
        terrainRegions.put("OIL",          new TextureRegion(assets.get(AssetManager.OBJ_OIL)));
        terrainRegions.put("CACTUS",       new TextureRegion(assets.get(AssetManager.OBJ_CACTUS)));
        terrainRegions.put("MOUNTAIN_OBJ", new TextureRegion(assets.get(AssetManager.OBJ_MOUNTAIN)));

        this.fogRegion = new TextureRegion(assets.get(AssetManager.FOG_OF_WAR));

        // Add to structRegions for consistent lookup via getTextureForPopup
        structRegions.put("TOWN",  terrainRegions.get("TOWN"));
        structRegions.put("RUINS", terrainRegions.get("RUINS"));
        structRegions.put("OIL",   terrainRegions.get("OIL"));
        structRegions.put("RAILWAY", new TextureRegion(assets.get(AssetManager.RAILWAY_ICON)));

        // Animals
        animalRegions.put("HORSE", new TextureRegion(assets.get(AssetManager.HORSE)));
        animalRegions.put("FISH",  new TextureRegion(assets.get(AssetManager.FISH)));
        animalRegions.put("DEER",  new TextureRegion(assets.get(AssetManager.DEER)));
        animalRegions.put("ZEBRA", new TextureRegion(assets.get(AssetManager.ZEBRA)));

        // Idle animations
        idleAnims.put("DEER",            buildIdleAnim(assets.getAtlas(AssetManager.DEER_IDLE_ATLAS)));
        idleAnims.put("FISH",            buildIdleAnim(assets.getAtlas(AssetManager.FISH_IDLE_ATLAS)));
        idleAnims.put("HORSE",           buildIdleAnim(assets.getAtlas(AssetManager.HORSE_IDLE_ATLAS)));
        idleAnims.put("ZEBRA",           buildIdleAnim(assets.getAtlas(AssetManager.ZEBRA_IDLE_ATLAS)));
        idleAnims.put("JAMMER",          buildIdleAnim(assets.getAtlas(AssetManager.JAMMER_IDLE_ATLAS)));
        idleAnims.put("MUNITION_FACTORY",buildIdleAnim(assets.getAtlas(AssetManager.MUNITION_FACTORY_IDLE_ATLAS)));
        idleAnims.put("NUCLEAR",         buildIdleAnim(assets.getAtlas(AssetManager.NUCLEAR_PLANT_IDLE_ATLAS)));
        idleAnims.put("OIL_DERRICK",     buildIdleAnim(assets.getAtlas(AssetManager.OIL_DERRICK_IDLE_ATLAS)));
        idleAnims.put("PORT",            buildIdleAnim(assets.getAtlas(AssetManager.PORT_IDLE_ATLAS)));
        idleAnims.put("RADAR",           buildIdleAnim(assets.getAtlas(AssetManager.RADAR_IDLE_ATLAS)));
        idleAnims.put("SOLAR",           buildIdleAnim(assets.getAtlas(AssetManager.SOLAR_ARRAY_IDLE_ATLAS)));
    }

    private Animation<TextureRegion> buildIdleAnim(TextureAtlas atlas) {
        com.badlogic.gdx.utils.Array<TextureAtlas.AtlasRegion> regions = atlas.getRegions();
        regions.sort((a, b) -> a.name.compareTo(b.name));
        return new Animation<>(2f / 24f, regions, Animation.PlayMode.LOOP);
    }

    /** Builds a [right, left-flipped] pair from a single texture and stores it. */
    private void putUnit(String key, com.badlogic.gdx.graphics.Texture tex) {
        TextureRegion right = new TextureRegion(tex);
        TextureRegion left  = new TextureRegion(tex);
        left.flip(true, false);
        unitRegions.put(key, new TextureRegion[]{ right, left });
    }

    /**
     * Creates and registers a unit entity with stats matching the given type.
     *
     * @param unitType   the type of unit to create
     * @param x          grid column to place the unit
     * @param y          grid row to place the unit
     * @param owner      player ID (1 or 2)
     * @param isSummoned true if summoned mid-turn (marks hasActed so unit cannot act immediately)
     */
    public void createUnit(UnitType unitType, int x, int y, int owner, boolean isSummoned) {
        TextureRegion[] regions = unitRegions.get(unitType.name());
        if (regions == null) {
            regions = unitRegions.get(UnitType.RECRUIT.name());
        }

        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(regions[0]));
        entity.add(new FacingComponent(regions[1], regions[0]));
        entity.add(new TypeComponent(TypeComponent.Type.UNIT));
        entity.add(new AbilitiesComponent());
        entity.add(new AnimationComponent());

        UnitStatConfig.UnitStatData statData = UnitStatConfig.get(unitType);

        StatsComponent stats = new StatsComponent(toNiceName(unitType.name()),
                statData.hp, statData.atk, statData.def, statData.move,
                statData.rng, statData.vis, statData.cost, statData.moveType,
                owner);
        stats.unitTypeKey = unitType.name(); // store raw key for snapshot restoration
        stats.unitType = unitType;
        stats.hasActed = isSummoned;
        stats.hasMoved = isSummoned;

        // --- Post-processing for specific abilities ---
        AbilitiesComponent abilities = entity.getComponent(AbilitiesComponent.class);
        if (stats.unitType == UnitType.APACHE) {
            abilities.fuel = 5;
            abilities.fuelMax = 5;
            abilities.isUnreachable = true; // High Altitude: immune to melee land attacks
        }
        if (stats.unitType == UnitType.SUBMARINE || stats.unitType == UnitType.B2) {
            abilities.isCloaked = true;
        }
        if (stats.unitType == UnitType.RECON_DRONE) {
            abilities.isUnreachable = true; // High Altitude: immune to melee land attacks
        }

        entity.add(stats);

        if (unitType == UnitType.TANK && tankIdleAnim != null) {
            IdleAnimationComponent idleAnim = new IdleAnimationComponent();
            idleAnim.animation = tankIdleAnim;
            idleAnim.overlayOnly = true;
            idleAnim.respectsFacing = true;
            idleAnim.drawOffsetX = GameConfig.TANK_IDLE_OFFSET_X;
            idleAnim.drawOffsetY = GameConfig.TANK_IDLE_OFFSET_Y;
            idleAnim.paused = true;
            idleAnim.pauseTimer = MathUtils.random(2f, 3f);
            entity.add(idleAnim);
        }

        engine.addEntity(entity);
    }

    /**
     * Returns the gold cost for purchasing the given unit type.
     *
     * @param unitType the unit type to query
     * @return cost in gold; 0 for super units or unrecognised types
     */
    public static int getUnitCost(UnitType unitType) {
        return UnitStatConfig.get(unitType).cost;
    }

    /**
     * Returns the movement domain (LAND, AIR, or SEA) for the given unit type.
     *
     * @param unitType the unit type to query
     * @return the corresponding {@link StatsComponent.MoveType}
     */
    public StatsComponent.MoveType getUnitMoveType(UnitType unitType) {
        return UnitStatConfig.get(unitType).moveType;
    }

    // -------------------------------------------------------------------------
    // Snapshot / Undo support
    // -------------------------------------------------------------------------

    /**
     * Captures a full snapshot of current game state for undo.
     * Call this at the START of every turn before the player acts.
     */
    public TurnSnapshot captureSnapshot(Engine engine, GameState state, MapGenerator.GameMap map) {
        List<UnitSnapshot> unitSnaps = new ArrayList<>();
        List<StructureSnapshot> structSnaps = new ArrayList<>();

        ImmutableArray<Entity> all = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class, StatsComponent.class, TypeComponent.class).get());

        for (Entity e : all) {
            TypeComponent type = e.getComponent(TypeComponent.class);
            StatsComponent s = e.getComponent(StatsComponent.class);
            GridPositionComponent p = e.getComponent(GridPositionComponent.class);

            if (type.type == TypeComponent.Type.UNIT) {
                AbilitiesComponent a = e.getComponent(AbilitiesComponent.class);
                boolean isDiggingIn = false, hasUsedDigIn = false, isOverwatchActive = false, isCloaked = false, pendingSkirmishMove = false, isUnreachable = false;
                int fuel = -1, nukeCooldown = 0;
                float idleTimer = 0f;
                if (a != null) {
                    isDiggingIn = a.isDiggingIn;
                    hasUsedDigIn = a.hasUsedDigIn;
                    isOverwatchActive = a.isOverwatchActive;
                    isCloaked = a.isCloaked;
                    pendingSkirmishMove = a.pendingSkirmishMove;
                    isUnreachable = a.isUnreachable;
                    fuel = a.fuel;
                    nukeCooldown = a.nukeCooldown;
                    idleTimer = a.idleTimer;
                }

                UnitSnapshot unitSnap = new UnitSnapshot(
                        s.unitTypeKey, p.x, p.y, s.owner,
                        s.currentHP, s.hasActed, s.hasMoved, s.moveType,
                        isDiggingIn, hasUsedDigIn, isOverwatchActive, isCloaked, a != null ? a.isCloakBroken : false,
                        pendingSkirmishMove, isUnreachable, fuel, nukeCooldown);
                unitSnap.idleTimer = idleTimer;
                unitSnaps.add(unitSnap);

            } else if (type.type == TypeComponent.Type.OBJECT && e.getComponent(AnimalComponent.class) == null) {
                // Capture ALL non-animal static objects/structures so they can be restored
                structSnaps.add(new StructureSnapshot(
                        p.x, p.y, s != null ? s.owner : 0, s != null ? s.level : 1,
                        s != null ? s.currentBaseXP : 0f, s != null ? s.income : 0,
                        s != null ? s.name : "", s != null ? s.baseOrdinal : "",
                        s != null ? s.chosenSuperUnit : "", s != null ? s.unitTypeKey : "",
                        s != null ? s.xpGain : 0, s != null ? s.parentBaseX : -1,
                        s != null ? s.parentBaseY : -1));
            }
        }

        // Clone the map objects array
        MapGenerator.ObjectType[][] objClone = new MapGenerator.ObjectType[map.width][map.height];
        for (int x = 0; x < map.width; x++) {
            System.arraycopy(map.objects[x], 0, objClone[x], 0, map.height);
        }

        return new TurnSnapshot(
                state.p1Funding, state.p2Funding,
                state.p1XP, state.p2XP,
                state.turnCount, state.currentPlayer,
                state.p1BaseCount, state.p2BaseCount,
                unitSnaps, structSnaps, objClone);
    }

    /**
     * Returns the gold cost to build the given structure type.
     *
     * @param type structure key (e.g. "MUNITION_FACTORY", "PORT")
     * @return cost in gold; 0 for unrecognised types
     */
    public int getStructureCost(String type) {
        switch (type) {
            case "MUNITION_FACTORY":
                return 5;
            case "PORT":
                return 7;
            case "HOSPITAL":
                return 15;
            case "SOLAR":
                return 8;
            case "RADAR":
                return 20;
            case "OIL_DERRICK":
                return 10;
            case "JAMMER":
                return 25;
            case "NUCLEAR":
                return 40;
            default:
                return 0;
        }
    }

    /**
     * Creates and registers a non-base structure entity at the given tile.
     *
     * @param type    structure key (e.g. "MUNITION_FACTORY", "PORT")
     * @param x       grid column
     * @param y       grid row
     * @param owner   player ID (1 or 2)
     * @param parentX grid column of the parent base that built this structure
     * @param parentY grid row of the parent base that built this structure
     */
    public void createStructure(String type, int x, int y, int owner, int parentX, int parentY) {
        String regionKey = type;

        TextureRegion region = structRegions.get(regionKey);
        if (region == null) {
            region = animalRegions.get("HORSE"); // Fallback
        }
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 1)); // Layer 1
        entity.add(new TextureComponent(region));
        entity.add(new TypeComponent(TypeComponent.Type.OBJECT));
        entity.add(new AbilitiesComponent()); // NEW: Add abilities state

        StructureType structureType = StructureType.fromKey(type);
        String niceName = (structureType != null) ? structureType.getDisplayName() : type.replace("_", " ");

        // Create Stats (Default Income = 0, we add it to Base XP instead)
        StatsComponent stats = new StatsComponent(niceName, 10, 0, 0, 0, 0, 1, getStructureCost(type),
                StatsComponent.MoveType.LAND, owner, 0);
        stats.unitTypeKey = type; // Store the key for HUD icon lookup

        // --- NEW: Link to Parent Base ---
        stats.parentBaseX = parentX;
        stats.parentBaseY = parentY;

        // --- Set XP Gain & Income per structure type ---
        if (structureType == StructureType.MUNITION_FACTORY) {
            stats.xpGain = 50;
            stats.income = 2;
        }

        if (structureType == StructureType.PORT) {
            stats.xpGain = 50;
            stats.income = 0;
        }

        if (structureType == StructureType.HOSPITAL) {
            stats.xpGain = 50;
            stats.income = 0;
        }

        if (structureType == StructureType.SOLAR) {
            stats.xpGain = 75;
            stats.income = 3;
        }

        if (structureType == StructureType.OIL_DERRICK) {
            stats.xpGain = 100;
            stats.income = 6;
        }

        if (structureType == StructureType.NUCLEAR_PLANT) {
            stats.xpGain = 150;
            stats.income = 15;
        }

        if (structureType == StructureType.RADAR) {
            stats.xpGain = 75;
            stats.income = 0;
            stats.vision = 4;
        }

        if (structureType == StructureType.SIGNAL_JAMMER) {
            stats.xpGain = 75;
            stats.income = 0;
            stats.vision = 2;
        }

        entity.add(stats);

        Animation<TextureRegion> structAnim = idleAnims.get(type);
        if (structAnim != null) {
            IdleAnimationComponent idleAnim = new IdleAnimationComponent();
            idleAnim.animation = structAnim;
            idleAnim.paused = true;
            idleAnim.pauseTimer = MathUtils.random(2f, 3f);
            entity.add(idleAnim);
        }

        engine.addEntity(entity);
    }

    /**
     * Creates and registers a map-object entity (animal, base, town, or terrain decoration).
     *
     * @param x     grid column
     * @param y     grid row
     * @param type  the object type that determines which entity variant is spawned
     * @param state game state updated when a base is placed (increments base counts)
     */
    public void createObjectEntity(int x, int y, MapGenerator.ObjectType type, GameState state) {
        UiInfo info = getObjectUi(type);
        if (info.region == null) {
            return;
        }

        boolean isAnimal = (com.militopia.config.AnimalType.fromKey(type.name()) != null);
        int zIndex = isAnimal ? 2 : 1;

        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, zIndex));
        entity.add(new TextureComponent(info.region));
        entity.add(new TypeComponent(TypeComponent.Type.OBJECT));
        entity.add(new AnimationComponent());

        if (isAnimal) {
            // Animal: Use Unit Constructor (0 Income)
            StatsComponent animalStats = new StatsComponent(info.name, 1, 0, 0, 0, 0, 0, 0,
                    StatsComponent.MoveType.LAND, 0);
            animalStats.name = "ANIMAL_" + type.name();
            animalStats.unitTypeKey = type.name();
            entity.add(animalStats);
            entity.add(new AnimalComponent(type.name()));
            Animation<TextureRegion> animalAnim = idleAnims.get(type.name());
            if (animalAnim != null) {
                IdleAnimationComponent idleAnim = new IdleAnimationComponent();
                idleAnim.animation = animalAnim;
                boolean isFish = type.name().equals("FISH");
                if (isFish) {
                    idleAnim.looping = true;
                    idleAnim.stateTime = MathUtils.random(0f, 2f);
                } else {
                    idleAnim.paused = true;
                    idleAnim.pauseTimer = MathUtils.random(2f, 3f);
                }
                entity.add(idleAnim);
            }
        } else if (type == MapGenerator.ObjectType.BASE_P1 || type == MapGenerator.ObjectType.BASE_P2) {
            int owner = (type == MapGenerator.ObjectType.BASE_P1) ? 1 : 2;
            state.p1BaseCount += (owner == 1 ? 1 : 0);
            state.p2BaseCount += (owner == 2 ? 1 : 0);
            String ordinal = getOrdinal((owner == 1) ? state.p1BaseCount : state.p2BaseCount);

            // Base: Use Structure Constructor (With Income = 2)
            StatsComponent stats = new StatsComponent(
                    (owner == 1 ? state.p1Name : state.p2Name) + "'s " + ordinal + " Base",
                    100, 0, 0, 0, 0, GameConfig.BORDER_RADIUS, 0, StatsComponent.MoveType.LAND, owner, 2);

            stats.unitTypeKey = type.name(); // Store key (e.g. "BASE_P1")
            stats.baseOrdinal = ordinal;
            updateBaseTexture(entity, stats);
            entity.add(stats);
        } else {
            // Town: Use Structure Constructor (With Income = 1)
            int income = (type == MapGenerator.ObjectType.TOWN) ? 1 : 0;
            StatsComponent stats = new StatsComponent(info.name, 10, 0, 0, 0, 0, 1, 0, StatsComponent.MoveType.LAND, 0,
                    income);
            stats.unitTypeKey = type.name(); // Store key (e.g. "TOWN", "RUINS")
            entity.add(stats);
        }
        engine.addEntity(entity);
    }

    /**
     * Checks if the base has enough XP to level up and applies all level-up effects.
     * Loops until XP falls below the threshold (handles multiple level-ups in one turn).
     *
     * @param baseEntity the base entity whose XP should be checked
     * @param state      game state to apply funding bonuses and unlocks against
     * @param hud        HUD to show the level-up popup (may be null in tests)
     */
    public void checkAndApplyLevelUp(Entity baseEntity, GameState state, GameHUD hud) {
        StatsComponent stats = baseEntity.getComponent(StatsComponent.class);
        if (stats == null) {
            return;
        }
        while (stats.currentBaseXP >= stats.maxBaseXP) {
            stats.currentBaseXP -= stats.maxBaseXP;
            stats.level++;
            BaseLevelConfig.LevelData data = BaseLevelConfig.getLevel(stats.level);
            stats.maxBaseXP = data.maxXP;
            stats.income = data.income;
            stats.vision = data.borderRadius;
            if (data.fundingBonus > 0) {
                if (stats.owner == 1) {
                    state.p1Funding += data.fundingBonus;
                } else {
                    state.p2Funding += data.fundingBonus;
                }
            }
            updateBaseTexture(baseEntity, stats);
            if (hud != null) {
                hud.showLevelUpPopup(stats.owner, stats.name, stats.level, data.fundingBonus, data.unlockedUnits,
                        data.unlockedStructures, this);
                // At level 5+, trigger super unit choice if not yet chosen for this base
                if (stats.level >= 5 && stats.chosenSuperUnit == null) {
                    hud.showSuperUnitChoicePopup(baseEntity, state, this);
                }
            }
            GameLogger.log(GameLogger.ECONOMY, stats.owner,
                    stats.name + " leveled up to Level " + stats.level + "!");
        }
    }

    /**
     * Records the player's super unit choice on the base and spawns one immediately.
     * Called from SuperUnitChoicePopup when the player picks a super unit.
     */
    public void chooseSuperUnit(Entity baseEntity, String superUnitType,
            GameState state, MapGenerator.GameMap map) {
        StatsComponent stats = baseEntity.getComponent(StatsComponent.class);
        GridPositionComponent pos = baseEntity.getComponent(GridPositionComponent.class);
        if (stats == null || pos == null) return;

        stats.chosenSuperUnit = superUnitType;
        UnitType superType = UnitType.fromKey(superUnitType);
        if (superType == null) return;

        int spawnX = pos.x;
        int spawnY = pos.y;

        // --- SUBMARINE SPECIAL LOGIC: Priority Spawning at Ports ---
        if (superType == UnitType.SUBMARINE) {
            Entity bestPort = findBestPortForSuperUnit(stats.owner, pos.x, pos.y);
            if (bestPort != null) {
                GridPositionComponent pPos = bestPort.getComponent(GridPositionComponent.class);
                spawnX = pPos.x;
                spawnY = pPos.y;
            }
        }

        // Spawn unit
        int[] spawn = findValidSpawnPoint(spawnX, spawnY, getUnitMoveType(superType), map);
        if (spawn != null) {
            createUnit(superType, spawn[0], spawn[1], stats.owner, false);
            GameLogger.log(GameLogger.SUMMON, stats.owner,
                    stats.name + " chose super unit: " + superUnitType
                            + " — spawned at " + GameLogger.pos(spawn[0], spawn[1]));
        } else {
            GameLogger.log(GameLogger.SUMMON, stats.owner,
                    stats.name + " chose super unit: " + superUnitType
                            + " — no valid spawn point found nearby, available via summon menu");
        }
    }

    /**
     * Finds the best Port to spawn a Submarine based on priority:
     * 1. Port in current base territory
     * 2. Port in nearest adjacent base territory
     * 3. Any available Port
     */
    private Entity findBestPortForSuperUnit(int owner, int baseX, int baseY) {
        ImmutableArray<Entity> all = engine.getEntitiesFor(Family.all(StatsComponent.class, GridPositionComponent.class).get());
        List<Entity> myPorts = new ArrayList<>();
        List<Entity> myBases = new ArrayList<>();

        for (Entity e : all) {
            StatsComponent s = e.getComponent(StatsComponent.class);
            if (s.owner != owner) continue;

            if ("PORT".equals(s.unitTypeKey)) {
                myPorts.add(e);
            } else if (s.unitTypeKey != null && s.unitTypeKey.startsWith("BASE")) {
                myBases.add(e);
            }
        }

        if (myPorts.isEmpty()) return null;

        // Priority 1: Local territory
        for (Entity port : myPorts) {
            StatsComponent ps = port.getComponent(StatsComponent.class);
            if (ps.parentBaseX == baseX && ps.parentBaseY == baseY) {
                return port;
            }
        }

        // Priority 2: Nearest adjacent base territory
        myBases.sort((b1, b2) -> {
            GridPositionComponent p1 = b1.getComponent(GridPositionComponent.class);
            GridPositionComponent p2 = b2.getComponent(GridPositionComponent.class);
            float d1 = (float) Math.hypot(p1.x - baseX, p1.y - baseY);
            float d2 = (float) Math.hypot(p2.x - baseX, p2.y - baseY);
            return Float.compare(d1, d2);
        });

        for (Entity otherBase : myBases) {
            GridPositionComponent obPos = otherBase.getComponent(GridPositionComponent.class);
            if (obPos.x == baseX && obPos.y == baseY) continue; // Skip self

            for (Entity port : myPorts) {
                StatsComponent ps = port.getComponent(StatsComponent.class);
                if (ps.parentBaseX == obPos.x && ps.parentBaseY == obPos.y) {
                    return port;
                }
            }
        }

        // Priority 3: Global (Any port, nearest to this base)
        myPorts.sort((p1, p2) -> {
            GridPositionComponent pos1 = p1.getComponent(GridPositionComponent.class);
            GridPositionComponent pos2 = p2.getComponent(GridPositionComponent.class);
            float d1 = (float) Math.hypot(pos1.x - baseX, pos1.y - baseY);
            float d2 = (float) Math.hypot(pos2.x - baseX, pos2.y - baseY);
            return Float.compare(d1, d2);
        });

        return myPorts.get(0);
    }

    /**
     * Swaps the base entity's texture to match its current owner and level.
     *
     * @param entity the base entity to update
     * @param stats  the base's stats component (provides owner and level)
     */
    public void updateBaseTexture(Entity entity, StatsComponent stats) {
        TextureComponent tex = entity.getComponent(TextureComponent.class);
        String color = (stats.owner == 1) ? "blue" : "red";
        int lvl = Math.min(stats.level, 10);
        TextureRegion reg = baseRegions.get("lvl" + lvl + "_" + color);
        if (reg != null) {
            tex.region = reg;
        } else {
            tex.region = baseRegions.get("lvl1_" + color);
        }
    }

    /**
     * Restores a structure entity's stats from a saved {@link com.militopia.data.StructureData} record.
     *
     * @param entity the existing structure entity to update
     * @param data   the persisted data to apply
     * @param map    game map used to determine if the entity is a Town (fixed stats)
     */
    public void updateStructureFromSave(Entity entity, com.militopia.data.StructureData data,
            MapGenerator.GameMap map) {
        StatsComponent stats = entity.getComponent(StatsComponent.class);
        if (stats == null)
            return;

        GridPositionComponent pos = entity.getComponent(GridPositionComponent.class);

        stats.owner = data.owner;
        stats.level = data.level;
        stats.currentBaseXP = data.currentBaseXP;
        stats.name = data.baseName;
        stats.baseOrdinal = data.baseOrdinal;
        stats.xpGain = data.xpGain;
        stats.chosenSuperUnit = data.chosenSuperUnit;

        // --- FIX 3: Only update base-specific stats/texture for actual BASE entities ---
        // isTown check is not sufficient — Oil Derricks and other non-base owned structures
        // would pass the isTown=false branch and have their stats and texture overwritten.
        boolean isTown = (pos != null && map.objects[pos.x][pos.y] == MapGenerator.ObjectType.TOWN);
        boolean isBase = stats.unitTypeKey != null && stats.unitTypeKey.startsWith("BASE");
        if (isBase) {
            com.militopia.config.BaseLevelConfig.LevelData levelData = com.militopia.config.BaseLevelConfig
                    .getLevel(stats.level);
            stats.maxBaseXP = levelData.maxXP;
            stats.income = levelData.income;
            stats.vision = levelData.borderRadius;
            updateBaseTexture(entity, stats);
        } else if (isTown) {
            // Towns have fixed stats
            stats.maxBaseXP = 2000;
            stats.income = 1;
            stats.vision = 1;
        }
        // Other structures (Oil Derrick, etc.) keep their stats as-is from the save data.
    }

    /**
     * Transfers ownership of a structure (town or enemy base) to newOwner,
     * converting it to a base for the capturing player.
     *
     * @param objectEntity the structure entity being captured
     * @param newOwner     player ID of the capturing player (1 or 2)
     * @param map          game map used to update the ObjectType array
     * @param state        game state to update base counts and XP
     */
    public void captureStructure(Entity objectEntity, int newOwner, MapGenerator.GameMap map, GameState state) {
        StatsComponent stats = objectEntity.getComponent(StatsComponent.class);
        TextureComponent tex = objectEntity.getComponent(TextureComponent.class);
        GridPositionComponent pos = objectEntity.getComponent(GridPositionComponent.class);

        MapGenerator.ObjectType oldType = map.objects[pos.x][pos.y];
        boolean wasBase = (oldType == MapGenerator.ObjectType.BASE_P1 || oldType == MapGenerator.ObjectType.BASE_P2);
        boolean isTown = (oldType == MapGenerator.ObjectType.TOWN);
        int oldOwner = stats.owner;
        int preservedLevel = stats.level;

        // Transform into a Base for the new owner, whether it was a Town or an Enemy
        // Base.
        if (newOwner == 1) {
            map.objects[pos.x][pos.y] = MapGenerator.ObjectType.BASE_P1;
            state.p1BaseCount++;
            stats.owner = 1;
            stats.unitTypeKey = "BASE_P1"; // Fix 2: ensure key reflects new base type

            // Update Old Owner
            if (wasBase && oldOwner == 2) {
                state.p2BaseCount = Math.max(0, state.p2BaseCount - 1);
            }

            stats.baseOrdinal = getOrdinal(state.p1BaseCount);
            stats.name = state.p1Name + "'s " + stats.baseOrdinal + " Base";
            int xpGain = isTown ? 50 : 250;
            state.p1XP += xpGain;

            // --- NEW: Floating Text ---
            if (entityFactory != null) {
                float worldX = EntityFactory.gridToIsoX(pos.x, pos.y);
                float worldY = EntityFactory.gridToIsoY(pos.x, pos.y);
                entityFactory.createFloatingText("+" + xpGain + " XP", worldX, worldY,
                        FloatingTextComponent.Type.XP, pos.x, pos.y);
            }
        } else if (newOwner == 2) {
            map.objects[pos.x][pos.y] = MapGenerator.ObjectType.BASE_P2;
            state.p2BaseCount++;
            stats.owner = 2;
            stats.unitTypeKey = "BASE_P2"; // Fix 2: ensure key reflects new base type

            // Update Old Owner
            if (wasBase && oldOwner == 1) {
                state.p1BaseCount = Math.max(0, state.p1BaseCount - 1);
            }

            stats.baseOrdinal = getOrdinal(state.p2BaseCount);
            stats.name = state.p2Name + "'s " + stats.baseOrdinal + " Base";
            int xpGain = isTown ? 50 : 250;
            state.p2XP += xpGain;

            // --- NEW: Floating Text ---
            if (entityFactory != null) {
                float worldX = EntityFactory.gridToIsoX(pos.x, pos.y);
                float worldY = EntityFactory.gridToIsoY(pos.x, pos.y);
                entityFactory.createFloatingText("+" + xpGain + " XP", worldX, worldY,
                        FloatingTextComponent.Type.XP, pos.x, pos.y);
            }
        }

        // Apply preserved level or reset to Level 1 if it was a Town
        if (wasBase) {
            BaseLevelConfig.LevelData levelData = BaseLevelConfig.getLevel(preservedLevel);
            stats.level = preservedLevel;
            stats.income = levelData.income;
            stats.maxBaseXP = levelData.maxXP;
            stats.vision = levelData.borderRadius;
        } else {
            stats.level = 1;
            stats.income = 2;
            stats.maxBaseXP = 2000;
            stats.vision = GameConfig.BORDER_RADIUS;
        }
        stats.currentBaseXP = 0; // Reset progress but keep the level (if wasBase)

        // Ensure texture matches the new owner & level
        updateBaseTexture(objectEntity, stats);

        // Spawn animals upon capture only if it was an enemy base
        if (!isTown) {
            spawnAnimalsAroundBase(pos.x, pos.y, map, state);
        }
    }

    /**
     * Spawns a random set of wild animals on tiles surrounding the given base.
     *
     * @param baseX grid column of the base
     * @param baseY grid row of the base
     * @param map   game map used for terrain and existing-object lookups
     * @param state game state passed through to {@link #createObjectEntity}
     */
    public void spawnAnimalsAroundBase(int baseX, int baseY, MapGenerator.GameMap map, GameState state) {
        int radius = GameConfig.BORDER_RADIUS;
        List<GridPoint> validSpots = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int nx = baseX + dx;
                int ny = baseY + dy;
                if (nx < 0 || nx >= map.width || ny < 0 || ny >= map.height) {
                    continue;
                }
                if (nx == baseX && ny == baseY) {
                    continue;
                }
                if (map.terrain[nx][ny] == MapGenerator.TerrainType.MOUNTAIN) {
                    continue;
                }
                validSpots.add(new GridPoint(nx, ny));
            }
        }
        Collections.shuffle(validSpots);

        int spawnCount = MathUtils.random(GameConfig.WILD_ANIMAL_COUNT_MIN, GameConfig.WILD_ANIMAL_COUNT_MAX);
        int spawned = 0;

        for (GridPoint spot : validSpots) {
            if (spawned >= spawnCount) {
                break;
            }

            MapGenerator.ObjectType animalType = null;
            MapGenerator.TerrainType terrain = map.terrain[spot.x][spot.y];
            MapGenerator.ObjectType existingObj = map.objects[spot.x][spot.y];

            if (existingObj == MapGenerator.ObjectType.TREE) {
                animalType = MapGenerator.ObjectType.DEER;
            } else if (terrain == MapGenerator.TerrainType.WATER || terrain == MapGenerator.TerrainType.DEEP_WATER) {
                animalType = MapGenerator.ObjectType.FISH;
            } else if (terrain == MapGenerator.TerrainType.SAND) {
                animalType = MapGenerator.ObjectType.ZEBRA;
            } else if (terrain == MapGenerator.TerrainType.GRASS) {
                animalType = MapGenerator.ObjectType.HORSE;
            }

            if (animalType != null) {
                // --- FIX: CHECK IF ANIMAL ALREADY EXISTS (Prevents stacking) ---
                if (hasEntityAt(spot.x, spot.y, 2)) {
                    continue; // Skip this spot, an animal (Z=2) is already here
                }

                // Create the visual entity (Layer 2)
                createObjectEntity(spot.x, spot.y, animalType, state);
                spawned++;
            }
        }
    }

    // --- HELPER: Checks for entity at coordinates and layer ---

    /**
     * Returns true if any entity occupies the given tile at the specified Z-layer.
     *
     * @param x      grid column
     * @param y      grid row
     * @param zLayer Z-index layer to check (e.g. 2 for animals, 3 for units)
     * @return true if an entity exists at (x, y, zLayer)
     */
    public boolean hasEntityAt(int x, int y, int zLayer) {
        return getEntityAt(x, y, zLayer) != null;
    }

    /**
     * Returns the entity at the given tile and Z-layer, or null if none exists.
     *
     * @param x      grid column
     * @param y      grid row
     * @param zLayer Z-index layer to search (e.g. 2 for animals, 3 for units)
     * @return the matching entity, or null
     */
    public Entity getEntityAt(int x, int y, int zLayer) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (pos.x == x && pos.y == y && pos.zIndex == zLayer) {
                return e;
            }
        }
        return null;
    }

    /**
     * Finds a valid spawn point adjacent to the producer (x, y) that matches the
     * unit's moveType.
     * Scans the 8 adjacent tiles for an unoccupied tile (no unit at zIndex 3).
     */
    public int[] findValidSpawnPoint(int startX, int startY, StatsComponent.MoveType moveType,
            MapGenerator.GameMap map) {

        // Priority 1: Check the producer tile itself
        if (isValidSpawn(startX, startY, moveType, map)) {
            return new int[] { startX, startY };
        }

        int[][] dirs = {
                { -1, -1 }, { 0, -1 }, { 1, -1 },
                { -1, 0 }, { 1, 0 },
                { -1, 1 }, { 0, 1 }, { 1, 1 }
        };

        for (int[] d : dirs) {
            int tx = startX + d[0];
            int ty = startY + d[1];

            if (tx >= 0 && tx < map.width && ty >= 0 && ty < map.height) {
                if (isValidSpawn(tx, ty, moveType, map)) {
                    return new int[] { tx, ty };
                }
            }
        }
        return null; // No valid spot found
    }

    private boolean isValidSpawn(int x, int y, StatsComponent.MoveType moveType, MapGenerator.GameMap map) {
        if (hasEntityAt(x, y, 3))
            return false;

        MapGenerator.TerrainType terrain = map.terrain[x][y];
        boolean isWater = (terrain == MapGenerator.TerrainType.WATER || terrain == MapGenerator.TerrainType.DEEP_WATER);

        if (moveType == StatsComponent.MoveType.SEA)
            return isWater;
        if (moveType == StatsComponent.MoveType.LAND)
            return !isWater;
        return true; // Air or other
    }

    private static class GridPoint {

        int x, y;

        GridPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

    }

    private String getOrdinal(int i) {
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];
        }
    }

    /**
     * Converts a SCREAMING_SNAKE_CASE type key to a display-friendly name.
     *
     * @param type raw enum name (e.g. "RECON_DRONE")
     * @return display name with capitalised first letter and spaces (e.g. "Recon drone")
     */
    public String toNiceName(String type) {
        return type.charAt(0) + type.substring(1).toLowerCase().replace("_", " ");
    }

    /**
     * Resolves a display texture by lookup key for popup/HUD use.
     * Checks unit regions, structure regions, and base regions in order.
     *
     * @param key unit or structure key (e.g. "TANK", "PORT", "BASE_P1")
     * @return the matching display {@link TextureRegion}, or a fallback if not found
     */
    public TextureRegion getTextureForPopup(String key) {
        if (unitRegions.containsKey(key)) {
            return unitRegions.get(key)[0];
        }
        if (structRegions.containsKey(key)) {
            return structRegions.get(key);
        }
        if (key.startsWith("BASE_P")) {
            return getHudIcon(MapGenerator.ObjectType.valueOf(key));
        }
        return animalRegions.get("HORSE");
    }

    /**
     * Returns the display name and HUD texture for the given unit type.
     *
     * @param unitType the unit type to look up
     * @return a {@link UiInfo} with name and display region; falls back to Recruit if unknown
     */
    public UiInfo getUnitUi(UnitType unitType) {
        TextureRegion[] regs = unitRegions.get(unitType.name());
        return (regs != null) ? new UiInfo(toNiceName(unitType.name()), regs[0])
                : new UiInfo("Unknown", unitRegions.get(UnitType.RECRUIT.name())[0]);
    }

    /**
     * Returns the HUD icon texture for the given map object type (animals, bases, towns).
     *
     * @param type the object type to look up
     * @return the corresponding display {@link TextureRegion}
     */
    public TextureRegion getHudIcon(MapGenerator.ObjectType type) {
        switch (type) {
            case HORSE:
                return animalRegions.get("HORSE");
            case FISH:
                return animalRegions.get("FISH");
            case DEER:
                return animalRegions.get("DEER");
            case ZEBRA:
                return animalRegions.get("ZEBRA");

            case BASE_P1:
                return baseRegions.get("lvl1_blue");
            case BASE_P2:
                return baseRegions.get("lvl1_red");

            case BASE_P1_LVL2:
                return baseRegions.get("lvl2_blue");
            case BASE_P2_LVL2:
                return baseRegions.get("lvl2_red");

            case BASE_P1_LVL3:
                return baseRegions.get("lvl3_blue");
            case BASE_P2_LVL3:
                return baseRegions.get("lvl3_red");

            case BASE_P1_LVL4:
                return baseRegions.get("lvl4_blue");
            case BASE_P2_LVL4:
                return baseRegions.get("lvl4_red");

            case BASE_P1_LVL5:
                return baseRegions.get("lvl5_blue");
            case BASE_P2_LVL5:
                return baseRegions.get("lvl5_red");

            case BASE_P1_LVL6:
                return baseRegions.get("lvl6_blue");
            case BASE_P2_LVL6:
                return baseRegions.get("lvl6_red");

            case BASE_P1_LVL7:
                return baseRegions.get("lvl7_blue");
            case BASE_P2_LVL7:
                return baseRegions.get("lvl7_red");

            case BASE_P1_LVL8:
                return baseRegions.get("lvl8_blue");
            case BASE_P2_LVL8:
                return baseRegions.get("lvl8_red");

            case BASE_P1_LVL9:
                return baseRegions.get("lvl9_blue");
            case BASE_P2_LVL9:
                return baseRegions.get("lvl9_red");

            case BASE_P1_LVL10:
                return baseRegions.get("lvl10_blue");
            case BASE_P2_LVL10:
                return baseRegions.get("lvl10_red");

            case TOWN:
                return terrainRegions.get("TOWN");
            default:
                return getObjectUi(type).region;
        }
    }

    /**
     * Returns the display name and tile texture for the given map object type.
     *
     * @param type the object type to look up
     * @return a {@link UiInfo} with name and texture region; falls back to "Unknown Object" if not matched
     */
    public UiInfo getObjectUi(MapGenerator.ObjectType type) {
        // Base Object
        if (type == MapGenerator.ObjectType.BASE_P1) {
            return new UiInfo("Blue Base", baseRegions.get("lvl1_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2) {
            return new UiInfo("Red Base", baseRegions.get("lvl1_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL2) {
            return new UiInfo("Blue Base", baseRegions.get("lvl2_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL2) {
            return new UiInfo("Red Base", baseRegions.get("lvl2_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL3) {
            return new UiInfo("Blue Base", baseRegions.get("lvl3_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL3) {
            return new UiInfo("Red Base", baseRegions.get("lvl3_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL4) {
            return new UiInfo("Blue Base", baseRegions.get("lvl4_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL4) {
            return new UiInfo("Red Base", baseRegions.get("lvl4_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL5) {
            return new UiInfo("Blue Base", baseRegions.get("lvl5_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL5) {
            return new UiInfo("Red Base", baseRegions.get("lvl5_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL6) {
            return new UiInfo("Blue Base", baseRegions.get("lvl6_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL6) {
            return new UiInfo("Red Base", baseRegions.get("lvl6_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL7) {
            return new UiInfo("Blue Base", baseRegions.get("lvl7_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL7) {
            return new UiInfo("Red Base", baseRegions.get("lvl7_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL8) {
            return new UiInfo("Blue Base", baseRegions.get("lvl8_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL8) {
            return new UiInfo("Red Base", baseRegions.get("lvl8_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL9) {
            return new UiInfo("Blue Base", baseRegions.get("lvl9_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL9) {
            return new UiInfo("Red Base", baseRegions.get("lvl9_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL10) {
            return new UiInfo("Blue Base", baseRegions.get("lvl10_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL10) {
            return new UiInfo("Red Base", baseRegions.get("lvl10_red"));
        }

        // Other Object
        if (type == MapGenerator.ObjectType.TOWN) {
            return new UiInfo("Town", terrainRegions.get("TOWN"));
        }
        if (type == MapGenerator.ObjectType.TREE) {
            return new UiInfo("Oak Tree", terrainRegions.get("TREE"));
        }
        if (type == MapGenerator.ObjectType.RUINS) {
            return new UiInfo("Ancient Ruins", terrainRegions.get("RUINS"));
        }
        if (type == MapGenerator.ObjectType.OIL) {
            return new UiInfo("Oil Reservoir", terrainRegions.get("OIL"));
        }
        if (type == MapGenerator.ObjectType.CACTUS) {
            return new UiInfo("Cactus", terrainRegions.get("CACTUS"));
        }
        if (type == MapGenerator.ObjectType.MOUNTAIN_OBJ) {
            return new UiInfo("Mountain", terrainRegions.get("MOUNTAIN_OBJ"));
        }
        if (type == MapGenerator.ObjectType.HORSE) {
            return new UiInfo("Wild Horse", animalRegions.get("HORSE"));
        }
        if (type == MapGenerator.ObjectType.FISH) {
            return new UiInfo("Fish School", animalRegions.get("FISH"));
        }
        if (type == MapGenerator.ObjectType.DEER) {
            return new UiInfo("Forest Deer", animalRegions.get("DEER"));
        }
        if (type == MapGenerator.ObjectType.ZEBRA) {
            return new UiInfo("Zebra", animalRegions.get("ZEBRA"));
        }

        return new UiInfo("Unknown Object", terrainRegions.get("GRASS"));
    }

    /**
     * Returns the tile texture for the given terrain type ordinal.
     *
     * @param terrainId ordinal index into {@link MapGenerator.TerrainType#values()}
     * @return the matching terrain {@link TextureRegion}; defaults to grass if out of range
     */
    public TextureRegion getTextureForTerrain(int terrainId) {
        MapGenerator.TerrainType[] allTypes = MapGenerator.TerrainType.values();
        if (terrainId < 0 || terrainId >= allTypes.length) {
            return terrainRegions.get("GRASS");
        }
        MapGenerator.TerrainType type = allTypes[terrainId];
        if (type == MapGenerator.TerrainType.WATER) {
            return terrainRegions.get("WATER");
        } else if (type == MapGenerator.TerrainType.DEEP_WATER) {
            return terrainRegions.get("DEEP_WATER");
        } else if (type == MapGenerator.TerrainType.SAND) {
            return terrainRegions.get("SAND");
        } else if (type == MapGenerator.TerrainType.MOUNTAIN) {
            return terrainRegions.get("MOUNTAIN");
        } else {
            return terrainRegions.get("GRASS");
        }
    }

    /**
     * Returns the display name and tile texture for the given terrain type.
     *
     * @param type the terrain type to look up
     * @return a {@link UiInfo} with a human-readable name and texture region
     */
    public UiInfo getTerrainUi(MapGenerator.TerrainType type) {
        switch (type) {
            case WATER:
                return new UiInfo("Shallow Water", terrainRegions.get("WATER"));
            case DEEP_WATER:
                return new UiInfo("Deep Ocean", terrainRegions.get("DEEP_WATER"));
            case SAND:
                return new UiInfo("Desert", terrainRegions.get("SAND"));
            case MOUNTAIN:
                return new UiInfo("Mountain Range", terrainRegions.get("MOUNTAIN"));
            default:
                return new UiInfo("Grassland", terrainRegions.get("GRASS"));
        }
    }

    /**
     * Injects the {@link EntityFactory} used for spawning floating-text and VFX entities.
     *
     * @param entityFactory the factory instance to use
     */
    public void setEntityFactory(EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    public static class UiInfo {

        public String name;
        public TextureRegion region;

        public UiInfo(String name, TextureRegion region) {
            this.name = name;
            this.region = region;
        }
    }
}
