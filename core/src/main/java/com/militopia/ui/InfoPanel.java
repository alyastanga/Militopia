package com.militopia.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.militopia.MilitopiaGame;
import com.militopia.components.AbilitiesComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.config.BaseLevelConfig;
import com.militopia.config.StructureType;
import com.militopia.config.UnitType;
import com.militopia.controller.GameInputController;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.map.MapGenerator;
import com.militopia.screen.GameScreen;
import com.militopia.utils.GameLogger;
import com.militopia.utils.HoverListener;
import com.militopia.net.NetworkMessage;

import java.util.Set;

/**
 * Sliding tile/unit info panel anchored to the bottom of the screen.
 * Owns the icon, name label, HP label, Atk/Def/Rng/Mov/Vis stats grid,
 * the ability button row, and the close button.
 */
public class InfoPanel {

    private final MilitopiaGame game;
    private final AssetManager assets;
    private final Stage stage;
    private final HudBottomBar bottomBar;

    // Widget references
    private Table tileInfoTable;
    private com.badlogic.gdx.scenes.scene2d.ui.Image tileInfoImage;
    private Label tileInfoLabel;
    private Label hpLabel;
    private Label fuelLabel;
    private Label abilityDescLabel;
    private Table abilityTable;
    private ScrollPane abilityScroll;
    private Table statsTable;
    private Table infoStack;
    private Label atkLabel, defLabel, rngLabel, movLabel, visLabel;

    private final java.util.Map<UnitType, String> abilityDesc = new java.util.EnumMap<>(UnitType.class);
    private final java.util.Map<StructureType, String> structureDesc = new java.util.EnumMap<>(StructureType.class);

    // Base specific labels
    private Label levelLabel;

    private static final float PANEL_HEIGHT = 120f;

    public InfoPanel(MilitopiaGame game, AssetManager assets, Stage stage, HudBottomBar bottomBar) {
        this.game = game;
        this.assets = assets;
        this.stage = stage;
        this.bottomBar = bottomBar;
        com.badlogic.gdx.utils.JsonValue root = new com.badlogic.gdx.utils.JsonReader()
                .parse(com.badlogic.gdx.Gdx.files.internal("game-system/ability_descriptions.json"));
        for (com.badlogic.gdx.utils.JsonValue entry : root.get("units")) {
            UnitType ut = UnitType.fromKey(entry.name);
            if (ut != null) abilityDesc.put(ut, entry.asString());
        }
        for (com.badlogic.gdx.utils.JsonValue entry : root.get("structures")) {
            StructureType st = StructureType.fromKey(entry.name);
            if (st != null) structureDesc.put(st, entry.asString());
        }
        build();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void showTileInfo(String name, TextureRegion region) {
        showTileInfo(name, region, true);
    }

    public void showTileInfo(String name, TextureRegion region, boolean animate) {
        if (abilityTable != null)
            abilityTable.clear();
        atkLabel.setColor(com.badlogic.gdx.graphics.Color.WHITE); // Reset color
        tileInfoImage.setDrawable(new TextureRegionDrawable(region));
        tileInfoLabel.setText(name);
        if (hpLabel != null) {
            hpLabel.setVisible(false);
            infoStack.getCell(hpLabel).height(0);
        }
        if (fuelLabel != null) {
            fuelLabel.setVisible(false);
            infoStack.getCell(fuelLabel).height(0);
        }
        if (abilityDescLabel != null) {
            abilityDescLabel.setVisible(false);
            infoStack.getCell(abilityDescLabel).height(0);
        }
        if (statsTable != null)
            statsTable.setVisible(false);

        GameLogger.log(GameLogger.UI, "InfoPanel: Show Tile Info | " + name);
        if (animate)
            slideIn();
        else
            snapToIn();
    }

    /**
     * Shows base-specific info including level, XP, income, and next rewards.
     */
    public void showBaseInfo(final Entity base, String name, TextureRegion region,
            final GameInputController controller,
            final UnitFactory factory,
            final GameScreen screen, boolean animate) {
        if (abilityTable != null)
            abilityTable.clear();
        tileInfoLabel.setText(name);
        tileInfoImage.setDrawable(new TextureRegionDrawable(region));

        if (hpLabel != null) {
            hpLabel.setVisible(false);
            infoStack.getCell(hpLabel).height(0);
        }
        if (fuelLabel != null) {
            fuelLabel.setVisible(false);
            infoStack.getCell(fuelLabel).height(0);
        }
        if (abilityDescLabel != null) {
            abilityDescLabel.setVisible(false);
            infoStack.getCell(abilityDescLabel).height(0);
        }

        StatsComponent stats = base.getComponent(StatsComponent.class);
        if (stats != null && statsTable != null) {
            boolean isBase = stats.unitTypeKey != null && stats.unitTypeKey.startsWith("BASE");

            // Level line: only for bases
            if (isBase) {
                atkLabel.setColor(com.badlogic.gdx.graphics.Color.GREEN);
                atkLabel.setText("Level: " + stats.level);
            } else {
                atkLabel.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                atkLabel.setText("Status: Strategic");
            }

            // XP line: show bar for bases, simple text for structures
            if (isBase) {
                defLabel.setText("XP: " + (int) stats.currentBaseXP + " / " + (int) stats.maxBaseXP + " (+"
                        + screen.calculateBaseXPGain(base) + ")");
            } else {
                defLabel.setText("XP Gain: +" + screen.calculateBaseXPGain(base));
            }

            rngLabel.setText("Income: +" + screen.calculateGroupedBaseIncome(base));
            movLabel.setText(""); // Label 4 (unused for objs)

            // Next rewards lookup: only for bases
            if (isBase) {
                BaseLevelConfig.LevelData next = BaseLevelConfig.getLevel(stats.level + 1);
                java.util.List<String> items = new java.util.ArrayList<>();
                if (next.unlockedUnits != null) {
                    for (String u : next.unlockedUnits)
                        items.add(u);
                }
                if (next.unlockedStructures != null) {
                    for (String s : next.unlockedStructures)
                        items.add(s);
                }
                if (next.borderRadius > stats.vision) {
                    items.add("+AREA");
                }

                if (items.isEmpty()) {
                    visLabel.setText("Next: None");
                } else {
                    visLabel.setText("Next: " + String.join(", ", items));
                }
            } else {
                visLabel.setText(""); // Hide rewards for non-bases
            }
            statsTable.setVisible(true);

            // Structure description
            if (abilityDescLabel != null) {
                StructureType sType = StructureType.fromKey(stats.unitTypeKey);
                String desc = structureDesc.get(sType);
                if (desc != null) {
                    abilityDescLabel.setText(desc);
                    abilityDescLabel.setVisible(true);
                    infoStack.getCell(abilityDescLabel).height(14);
                }
            }
        }

        // Demolish button: only for the current player's built (non-base, non-town)
        // structures
        if (stats != null && stats.xpGain > 0 && stats.owner == screen.getCurrentPlayer()) {
            final com.militopia.components.GridPositionComponent pos = base
                    .getComponent(com.militopia.components.GridPositionComponent.class);
            final int demolishRefund = factory.getStructureCost(stats.unitTypeKey) / 2;
            addAbilityButton("Demolish (+" + demolishRefund + ")", factory.getTextureForPopup(stats.unitTypeKey),
                    new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            GameState state = screen.getGameState();
                            int player = stats.owner;
                            if (player == 1)
                                state.p1Funding += demolishRefund;
                            else
                                state.p2Funding += demolishRefund;
                            if (screen.getGameState().isLanGame && pos != null) {
                                screen.getNetworkManager()
                                        .send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_DEMOLISH,
                                                pos.x + "," + pos.y + "," + player));
                            }
                            AudioManager.getInstance().playSFX(SFXKeys.ACTION_DEMOLISH);
                            screen.getEngine().removeEntity(base);
                            int newFunds = (player == 1) ? state.p1Funding : state.p2Funding;
                            screen.gameHUD.updateFunding(player, newFunds, screen.calculateIncome(player));
                            GameLogger.log(GameLogger.UI, "InfoPanel: Demolish | " + stats.name
                                    + (pos != null ? " at (" + pos.x + "," + pos.y + ")" : "")
                                    + " | refund=" + demolishRefund);

                            // Tutorial Hook: Demolish Structure
                            if (com.militopia.managers.TutorialManager.getInstance().isActive()
                                    && com.militopia.managers.TutorialManager.getInstance()
                                            .getCurrentStep() == com.militopia.managers.TutorialManager.Step.DEMOLISH_STRUCT) {
                                com.militopia.managers.TutorialManager.getInstance().nextStep();
                            }

                            hideTileInfo();
                        }
                    });
        }

        GameLogger.log(GameLogger.UI, "InfoPanel: Show Structure Info | " + name
                + (stats != null ? " | Lvl: " + stats.level : ""));
        if (animate)
            slideIn();
        else
            snapToIn();
    }

    public void showBaseInfoUnified(final Entity base,
            final GameInputController controller,
            final UnitFactory factory,
            final GameScreen screen) {
        showBaseInfo(base, base.getComponent(StatsComponent.class).name,
                base.getComponent(com.militopia.components.TextureComponent.class).region,
                controller, factory, screen, true);

        // Populate summons in the abilityTable (Center)
        final StatsComponent bs = base.getComponent(StatsComponent.class);
        final GameState state = screen.getGameState();
        Set<String> unlocked = BaseLevelConfig.getUnlockedForLevel(bs.level, false);

        UnitType[] allUnits = {
                UnitType.RECRUIT, UnitType.RANGER, UnitType.SNIPER, UnitType.TANK, UnitType.RECON_DRONE,
                UnitType.SUICIDE_DRONE, UnitType.APACHE, UnitType.GUNBOAT, UnitType.DESTROYER, UnitType.CARRIER
        };

        for (final UnitType unit : allUnits) {
            if (!unlocked.contains(unit.name()))
                continue;
            StatsComponent.MoveType moveType = factory.getUnitMoveType(unit);
            // In a BASE, we only show non-SEA units. PORTS (later) will show SEA units.
            if (moveType == StatsComponent.MoveType.SEA)
                continue;

            UnitFactory.UiInfo info = factory.getUnitUi(unit);
            final int cost = UnitFactory.getUnitCost(unit);

            SummonButton.addTo(abilityTable, info.region, info.name + " (" + cost + ")", game, assets,
                    new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            int funds = (bs.owner == 1) ? state.p1Funding : state.p2Funding;
                            if (funds < cost) {
                                AudioManager.getInstance().playSFX(SFXKeys.RESOURCE_INSUFFICIENT);
                                GameLogger.log(GameLogger.SUMMON, bs.owner,
                                        "Attempted " + unit.name() + " — insufficient funds ("
                                                + funds + "<" + cost + ")");
                                return;
                            }
                            int tx = controller.getLastClickedX();
                            int ty = controller.getLastClickedY();
                            if (tx == -1 || ty == -1)
                                return;

                            int[] spawn = factory.findValidSpawnPoint(
                                    tx, ty, moveType, screen.getGameMap());
                            if (spawn == null) {
                                GameLogger.log(GameLogger.SUMMON, bs.owner,
                                        "Attempted " + unit.name() + " — no valid spawn point found");
                                return;
                            }
                            if (bs.owner == 1)
                                state.p1Funding -= cost;
                            else
                                state.p2Funding -= cost;

                            factory.createUnit(unit, spawn[0], spawn[1], bs.owner, true);
                            AudioManager.getInstance().playSFX(SFXKeys.UNIT_DEPLOY);
                            int remaining = (bs.owner == 1) ? state.p1Funding : state.p2Funding;
                            GameLogger.log(GameLogger.SUMMON, bs.owner,
                                    "Summoned " + unit.name() + " at " + GameLogger.pos(spawn[0], spawn[1])
                                            + " | cost=" + cost + " | funds remaining=" + remaining);
                            screen.gameHUD.updateFunding(bs.owner, remaining, bs.income);
                            hideTileInfo();
                            controller.resetLastClicked();

                            // Tutorial Hook: Summon Unit
                            if (com.militopia.managers.TutorialManager.getInstance().isActive()
                                    && com.militopia.managers.TutorialManager.getInstance()
                                            .getCurrentStep() == com.militopia.managers.TutorialManager.Step.SUMMON_UNIT) {
                                com.militopia.managers.TutorialManager.getInstance().nextStep();
                            }
                        }
                    });
        }

        // --- NEW: Add Chosen Super Unit to Summon Menu ---
        if (bs.chosenSuperUnit != null && !bs.chosenSuperUnit.isEmpty()) {
            final UnitType superUnit = UnitType.fromKey(bs.chosenSuperUnit);
            if (superUnit != null) {
                StatsComponent.MoveType moveType = factory.getUnitMoveType(superUnit);
                // Standard Bases only show non-SEA units (SEA units in PORT)
                if (moveType != StatsComponent.MoveType.SEA) {
                    UnitFactory.UiInfo info = factory.getUnitUi(superUnit);
                    final int cost = UnitFactory.getUnitCost(superUnit);

                    SummonButton.addTo(abilityTable, info.region, info.name + " (" + cost + ")", game, assets,
                            new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {
                                    int funds = (bs.owner == 1) ? state.p1Funding : state.p2Funding;
                                    if (funds < cost) {
                                        AudioManager.getInstance().playSFX(SFXKeys.RESOURCE_INSUFFICIENT);
                                        return;
                                    }
                                    int tx = controller.getLastClickedX();
                                    int ty = controller.getLastClickedY();
                                    if (tx == -1 || ty == -1)
                                        return;

                                    int[] spawn = factory.findValidSpawnPoint(tx, ty, moveType, screen.getGameMap());
                                    if (spawn == null)
                                        return;

                                    if (bs.owner == 1)
                                        state.p1Funding -= cost;
                                    else
                                        state.p2Funding -= cost;

                                    factory.createUnit(superUnit, spawn[0], spawn[1], bs.owner, true);
                                    AudioManager.getInstance().playSFX(SFXKeys.UNIT_DEPLOY);
                                    screen.gameHUD.updateFunding(bs.owner,
                                            (bs.owner == 1) ? state.p1Funding : state.p2Funding, bs.income);
                                    hideTileInfo();
                                    controller.resetLastClicked();
                                }
                            });
                }
            }
        }
    }

    public void showUnitInfo(final Entity unit, String name, TextureRegion region,
            int currentHP, int maxHP,
            final GameInputController controller,
            final UnitFactory factory,
            final GameScreen screen) {
        if (abilityTable != null)
            abilityTable.clear();
        tileInfoLabel.setText(name);
        tileInfoImage.setDrawable(new TextureRegionDrawable(region));

        if (hpLabel != null) {
            hpLabel.setText("HP: " + currentHP + " / " + maxHP);
            hpLabel.setColor(currentHP > maxHP / 2 ? Color.GREEN : Color.YELLOW);
            hpLabel.setVisible(true);
            infoStack.getCell(hpLabel).height(20);
        }

        // Fuel indicator: only for Apache
        AbilitiesComponent abilitiesForFuel = unit.getComponent(AbilitiesComponent.class);
        StatsComponent statsForFuel = unit.getComponent(StatsComponent.class);
        if (fuelLabel != null) {
            if (statsForFuel != null && statsForFuel.unitType == UnitType.APACHE
                    && abilitiesForFuel != null && abilitiesForFuel.fuel >= 0) {
                int f = abilitiesForFuel.fuel;
                int fm = abilitiesForFuel.fuelMax;
                fuelLabel.setText("Fuel: " + f + " / " + fm);
                fuelLabel.setColor(f > 2 ? new Color(1f, 0.65f, 0f, 1f) : Color.RED);
                fuelLabel.setVisible(true);
                infoStack.getCell(fuelLabel).height(20);
            } else {
                fuelLabel.setVisible(false);
                infoStack.getCell(fuelLabel).height(0);
            }
        }

        StatsComponent stats = unit.getComponent(StatsComponent.class);
        if (stats != null && statsTable != null) {
            // Reset label colors for units
            atkLabel.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            atkLabel.setText("Atk: " + stats.attack);
            defLabel.setText("Def: " + stats.defense);
            rngLabel.setText("Rng: " + stats.attackRange);
            movLabel.setText("Mov: " + stats.move);
            visLabel.setText("Vis: " + stats.vision);
            statsTable.setVisible(true);
        } else if (statsTable != null) {
            statsTable.setVisible(false);
        }

        // Ability description
        if (abilityDescLabel != null && stats != null) {
            String desc = abilityDesc.get(stats.unitType);
            if (desc != null) {
                abilityDescLabel.setText(desc);
                abilityDescLabel.setVisible(true);
                infoStack.getCell(abilityDescLabel).height(14);
            } else {
                abilityDescLabel.setVisible(false);
                infoStack.getCell(abilityDescLabel).height(0);
            }
        }

        GameLogger.log(GameLogger.UI, "InfoPanel: Show Unit Info | " + name + " | HP: " + currentHP + "/" + maxHP);
        // Ability buttons for the active player's own units
        AbilitiesComponent abilities = unit.getComponent(AbilitiesComponent.class);
        if (stats != null && abilities != null
                && stats.owner == screen.getCurrentPlayer()
                && !stats.hasActed) {

            if (stats.unitType == UnitType.RECRUIT
                    && !abilities.hasUsedDigIn && !abilities.isDiggingIn) {
                addAbilityButton("Dig In",
                        factory.getTextureForPopup("RECRUIT"),
                        new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                controller.performAbility(unit, AbilitiesComponent.KEY_DIG_IN);
                            }
                        });

            } else if (stats.unitType == UnitType.RANGER && !abilities.isOverwatchActive) {
                addAbilityButton("Overwatch",
                        factory.getTextureForPopup("RANGER"),
                        new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                controller.performAbility(unit, AbilitiesComponent.KEY_OVERWATCH);
                            }
                        });
            }
        }

        // Cut Tree: unit on a tree tile that hasn't acted yet
        final GridPositionComponent unitPos = unit.getComponent(GridPositionComponent.class);
        if (stats != null && stats.owner == screen.getCurrentPlayer() && !stats.hasActed
                && unitPos != null
                && screen.getGameMap().objects[unitPos.x][unitPos.y] == MapGenerator.ObjectType.TREE) {
            addAbilityButton("Cut Tree (+" + com.militopia.config.CombatConstants.TREE_CUT_FUNDING + ")",
                    factory.getHudIcon(MapGenerator.ObjectType.TREE),
                    new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            final int playerID = stats.owner;
                            if (screen.getGameState().isLanGame) {
                                screen.getNetworkManager()
                                        .send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_SCAVENGE,
                                                unitPos.x + "," + unitPos.y + "," + playerID));
                                screen.syncEconomy(playerID);
                            }
                            AudioManager.getInstance().playSFX(SFXKeys.ACTION_CUT_TREE);
                            screen.getGameMap().objects[unitPos.x][unitPos.y] = MapGenerator.ObjectType.NONE;
                            // Remove the tree entity from the engine so it disappears visually
                            ImmutableArray<Entity> objs = screen.getEngine().getEntitiesFor(
                                    Family.all(GridPositionComponent.class, TypeComponent.class).get());
                            for (Entity obj : objs) {
                                GridPositionComponent op = obj.getComponent(GridPositionComponent.class);
                                TypeComponent ot = obj.getComponent(TypeComponent.class);
                                if (op.x == unitPos.x && op.y == unitPos.y
                                        && ot.type == TypeComponent.Type.OBJECT
                                        && obj.getComponent(com.militopia.components.AnimalComponent.class) == null) {
                                    screen.getEngine().removeEntity(obj);
                                    break;
                                }
                            }
                            GameState state = screen.getGameState();
                            if (playerID == 1)
                                state.p1Funding += com.militopia.config.CombatConstants.TREE_CUT_FUNDING;
                            else
                                state.p2Funding += com.militopia.config.CombatConstants.TREE_CUT_FUNDING;
                            stats.hasActed = true;
                            stats.hasMoved = true;
                            int newFunds = (playerID == 1) ? state.p1Funding : state.p2Funding;
                            screen.gameHUD.updateFunding(playerID, newFunds, screen.calculateIncome(playerID));
                            GameLogger.log(GameLogger.UI, "InfoPanel: Cut Tree at ("
                                    + unitPos.x + "," + unitPos.y + ") | +"
                                    + com.militopia.config.CombatConstants.TREE_CUT_FUNDING + " funding");

                            controller.deselect();

                            // Tutorial Hook: Cut Tree
                            if (com.militopia.managers.TutorialManager.getInstance().isActive()
                                    && com.militopia.managers.TutorialManager.getInstance()
                                            .getCurrentStep() == com.militopia.managers.TutorialManager.Step.CUT_TREE) {
                                com.militopia.managers.TutorialManager.getInstance().nextStep();
                            }
                            hideTileInfo();
                        }
                    });
        }

        // Cut Cactus: unit on a cactus tile that hasn't acted yet
        if (stats != null && stats.owner == screen.getCurrentPlayer() && !stats.hasActed
                && unitPos != null
                && screen.getGameMap().objects[unitPos.x][unitPos.y] == MapGenerator.ObjectType.CACTUS) {
            addAbilityButton("Cut Cactus (+" + com.militopia.config.CombatConstants.CACTUS_CUT_FUNDING + ")",
                    factory.getHudIcon(MapGenerator.ObjectType.CACTUS),
                    new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            final int playerID = stats.owner;
                            if (screen.getGameState().isLanGame) {
                                screen.getNetworkManager()
                                        .send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_SCAVENGE,
                                                unitPos.x + "," + unitPos.y + "," + playerID));
                                screen.syncEconomy(playerID);
                            }
                            AudioManager.getInstance().playSFX(SFXKeys.ACTION_CUT_TREE);
                            screen.getGameMap().objects[unitPos.x][unitPos.y] = MapGenerator.ObjectType.NONE;
                            // Remove the cactus entity from the engine so it disappears visually
                            ImmutableArray<Entity> objs = screen.getEngine().getEntitiesFor(
                                    Family.all(GridPositionComponent.class, TypeComponent.class).get());
                            for (Entity obj : objs) {
                                GridPositionComponent op = obj.getComponent(GridPositionComponent.class);
                                TypeComponent ot = obj.getComponent(TypeComponent.class);
                                if (op.x == unitPos.x && op.y == unitPos.y
                                        && ot.type == TypeComponent.Type.OBJECT
                                        && obj.getComponent(com.militopia.components.AnimalComponent.class) == null) {
                                    screen.getEngine().removeEntity(obj);
                                    break;
                                }
                            }
                            GameState state = screen.getGameState();
                            if (playerID == 1)
                                state.p1Funding += com.militopia.config.CombatConstants.CACTUS_CUT_FUNDING;
                            else
                                state.p2Funding += com.militopia.config.CombatConstants.CACTUS_CUT_FUNDING;
                            stats.hasActed = true;
                            stats.hasMoved = true;
                            int newFunds = (playerID == 1) ? state.p1Funding : state.p2Funding;
                            screen.gameHUD.updateFunding(playerID, newFunds, screen.calculateIncome(playerID));
                            GameLogger.log(GameLogger.UI, "InfoPanel: Cut Cactus at ("
                                    + unitPos.x + "," + unitPos.y + ") | +"
                                    + com.militopia.config.CombatConstants.CACTUS_CUT_FUNDING + " funding");
                            controller.deselect();
                            hideTileInfo();
                        }
                    });
        }

        // Disband: refund half the unit's cost to the player
        if (stats != null && stats.owner == screen.getCurrentPlayer()) {
            final int disbandRefund = UnitFactory.getUnitCost(stats.unitType) / 2;
            if (disbandRefund > 0) {
                addAbilityButton("Disband (+" + disbandRefund + ")",
                        region,
                        new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                GameState state = screen.getGameState();
                                int player = stats.owner;
                                if (player == 1)
                                    state.p1Funding += disbandRefund;
                                else
                                    state.p2Funding += disbandRefund;
                                if (screen.getGameState().isLanGame) {
                                    GridPositionComponent p = unit.getComponent(GridPositionComponent.class);
                                    if (p != null) {
                                        screen.getNetworkManager()
                                                .send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_DISBAND,
                                                        p.x + "," + p.y + "," + player));
                                        screen.syncEconomy(player);
                                    }
                                }
                                screen.getEngine().removeEntity(unit);
                                int newFunds = (player == 1) ? state.p1Funding : state.p2Funding;
                                screen.gameHUD.updateFunding(player, newFunds, screen.calculateIncome(player));
                                GameLogger.log(GameLogger.UI, "InfoPanel: Disband " + stats.name
                                        + " | refund=" + disbandRefund);

                                // Tutorial Hook: Disband Unit
                                if (com.militopia.managers.TutorialManager.getInstance().isActive()
                                        && com.militopia.managers.TutorialManager.getInstance()
                                                .getCurrentStep() == com.militopia.managers.TutorialManager.Step.DISBAND_UNIT) {
                                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                                }

                                hideTileInfo();
                            }
                        });
            }
        }

        slideIn();
    }

    public void hideTileInfo() {
        tileInfoTable.clearActions();
        bottomBar.getBottomContainer().clearActions();
        tileInfoTable.addAction(
                Actions.moveTo(0, -tileInfoTable.getHeight(), 0.3f, Interpolation.pow2In));
        bottomBar.getBottomContainer().addAction(
                Actions.moveTo(bottomBar.getBottomContainer().getX(), 0, 0.3f, Interpolation.pow2In));
        GameLogger.log(GameLogger.UI, "InfoPanel: Hide");
    }

    /** Snaps HP label immediately after combat resolves. */
    public void snapHP(int currentHP, int maxHP) {
        if (hpLabel != null && hpLabel.isVisible()) {
            hpLabel.setText("HP: " + currentHP + " / " + maxHP);
            hpLabel.setColor(currentHP > maxHP / 2 ? Color.GREEN : Color.YELLOW);
        }
    }

    public void resize(int width, int height) {
        if (tileInfoTable != null) {
            tileInfoTable.setWidth(width);
            tileInfoTable.setX(0);
        }
    }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    private void build() {
        tileInfoTable = new Table();
        tileInfoTable.setBackground(
                game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.9f)));

        tileInfoImage = new com.badlogic.gdx.scenes.scene2d.ui.Image();
        tileInfoImage.setScaling(Scaling.fit);
        tileInfoTable.add(tileInfoImage).size(70, 70).padLeft(20);

        // Name + HP stacked vertically
        infoStack = new Table();
        tileInfoLabel = new Label("Terrain Name", game.skin, "default-font", Color.WHITE);
        tileInfoLabel.setFontScale(0.8f);
        infoStack.add(tileInfoLabel).left().row();

        hpLabel = new Label("", game.skin, "default-font", Color.WHITE);
        hpLabel.setFontScale(0.65f);
        hpLabel.setVisible(false);
        infoStack.add(hpLabel).left().height(0).row();

        fuelLabel = new Label("", game.skin, "default-font", new Color(1f, 0.65f, 0f, 1f));
        fuelLabel.setFontScale(0.65f);
        fuelLabel.setVisible(false);
        infoStack.add(fuelLabel).left().height(0).row();

        abilityDescLabel = new Label("", game.skin, "default-font", new Color(0.6f, 0.85f, 1f, 1f));
        abilityDescLabel.setFontScale(0.55f);
        abilityDescLabel.setVisible(false);
        infoStack.add(abilityDescLabel).left().height(0).row();

        // Stats grid
        statsTable = new Table();
        atkLabel = makeStatLabel("Atk: 0");
        defLabel = makeStatLabel("Def: 0");
        rngLabel = makeStatLabel("Rng: 0");
        movLabel = makeStatLabel("Mov: 0");
        visLabel = makeStatLabel("Vis: 0");

        statsTable.add(atkLabel).width(120).left().padRight(16);
        statsTable.add(defLabel).width(120).left().row();
        statsTable.add(rngLabel).width(120).left().padRight(16);
        statsTable.add(movLabel).width(120).left().row();
        statsTable.add(visLabel).width(120).left();
        statsTable.setVisible(false);

        infoStack.add(statsTable).left().padTop(0);
        tileInfoTable.add(infoStack).left().padLeft(20).padRight(60);

        // Ability button row (wrapped in ScrollPane for D-02)
        abilityTable = new Table();
        // Custom ScrollPane that does NOT swallow scroll events so map zoom still works
        abilityScroll = new ScrollPane(abilityTable, game.skin) {
            @Override
            public boolean notify(com.badlogic.gdx.scenes.scene2d.Event event, boolean capture) {
                if (!capture && event instanceof com.badlogic.gdx.scenes.scene2d.InputEvent) {
                    com.badlogic.gdx.scenes.scene2d.InputEvent ie = (com.badlogic.gdx.scenes.scene2d.InputEvent) event;
                    if (ie.getType() == com.badlogic.gdx.scenes.scene2d.InputEvent.Type.scrolled) {
                        return false;
                    }
                }
                return super.notify(event, capture);
            }
        };
        abilityScroll.getStyle().background = null;
        abilityScroll.setScrollingDisabled(false, true); // Horizontal scroll only
        tileInfoTable.add(abilityScroll).expandX().fillX().padLeft(20).padRight(10);

        // Close button
        ImageButton.ImageButtonStyle closeStyle = new ImageButton.ImageButtonStyle();
        try {
            Texture closeTex = assets.get(AssetManager.BTN_SLIDEDOWN);
            TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(closeTex));
            closeStyle.imageUp = d;
            closeStyle.imageDown = d.tint(Color.GRAY);
        } catch (Exception e) {
            closeStyle.imageUp = game.skin.newDrawable("white", Color.RED);
        }
        ImageButton closeBtn = new ImageButton(closeStyle);
        closeBtn.addListener(new HoverListener());
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hideTileInfo();
            }
        });
        tileInfoTable.add(closeBtn).size(40, 40).padRight(20);

        tileInfoTable.setPosition(0, -PANEL_HEIGHT);
        tileInfoTable.setSize(stage.getWidth(), PANEL_HEIGHT);
        stage.addActor(tileInfoTable);
    }

    private void slideIn() {
        tileInfoTable.setWidth(stage.getWidth());
        tileInfoTable.setX(0);
        tileInfoTable.clearActions();
        bottomBar.getBottomContainer().clearActions();
        tileInfoTable.addAction(
                Actions.moveTo(0, 0, 0.3f, Interpolation.pow2Out));
        bottomBar.getBottomContainer().addAction(
                Actions.moveBy(0, -bottomBar.getBottomContainer().getHeight(), 0.3f, Interpolation.pow2Out));
    }

    private void snapToIn() {
        tileInfoTable.setWidth(stage.getWidth());
        tileInfoTable.setX(0);
        tileInfoTable.setY(0);
        tileInfoTable.clearActions();
        bottomBar.getBottomContainer().clearActions();
        bottomBar.getBottomContainer().setY(-bottomBar.getBottomContainer().getHeight());
    }

    private void addAbilityButton(String text, TextureRegion icon, ClickListener listener) {
        SummonButton.addTo(abilityTable, icon, text, game, assets, listener);
    }

    private Label makeStatLabel(String text) {
        Label l = new Label(text, game.skin, "default-font", Color.WHITE);
        l.setFontScale(0.6f);
        return l;
    }

    /**
     * Package-visible: SlideMenu needs the tileInfoTable to animate it away on
     * open.
     */
    Table getTileInfoTable() {
        return tileInfoTable;
    }
}
