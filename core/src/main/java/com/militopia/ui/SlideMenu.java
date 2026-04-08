package com.militopia.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.militopia.MilitopiaGame;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.config.BaseLevelConfig;
import com.militopia.config.StructureType;
import com.militopia.controller.GameInputController;
import com.militopia.data.GameState;
import com.militopia.config.UnitType;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.net.NetworkMessage;
import com.militopia.map.MapGenerator;
import com.militopia.screen.GameScreen;
import com.militopia.systems.ScavengeSystem;
import com.militopia.systems.StructurePlacementSystem;
import com.militopia.utils.GameLogger;
import com.militopia.utils.HoverListener;

/**
 * The sliding bottom menu panel — used for summon, hunt, capture, and build
 * menus.
 * Animates up from below the screen; the tile-info panel and bottom bar slide
 * away.
 */
public class SlideMenu {

    private final MilitopiaGame game;
    private final AssetManager assets;
    private final Stage stage;
    private final HudBottomBar bottomBar;
    private final InfoPanel infoPanel;
    private final GameScreen gameScreen;

    /** The actual menu Table added to stage. Also exposed for GameHUD compat. */
    public final Table menuTable;

    // State
    private int currentBaseOwner = 1;
    private int currentBaseLevel = 1;
    private int currentIncome = 0;
    private int buildX, buildY, buildParentX, buildParentY;

    // References kept alive for button callbacks
    private GameInputController inputController;
    private UnitFactory unitFactory;

    private static final float PANEL_HEIGHT = 140f;
    private static final Color BG_COLOR = new Color(0.1f, 0.1f, 0.1f, 0.95f);

    private final ScavengeSystem scavengeSystem;
    private final StructurePlacementSystem placementSystem;

    public SlideMenu(MilitopiaGame game, AssetManager assets, Stage stage, HudBottomBar bottomBar, InfoPanel infoPanel,
            GameScreen gameScreen, GameInputController inputController, UnitFactory unitFactory,
            ScavengeSystem scavengeSystem, StructurePlacementSystem placementSystem) {
        this.game = game;
        this.assets = assets;
        this.stage = stage;
        this.bottomBar = bottomBar;
        this.infoPanel = infoPanel;
        this.gameScreen = gameScreen;
        this.inputController = inputController;
        this.unitFactory = unitFactory;
        this.scavengeSystem = scavengeSystem;
        this.placementSystem = placementSystem;

        menuTable = new Table();
        menuTable.setSize(stage.getWidth(), PANEL_HEIGHT);
        menuTable.setPosition(0, -PANEL_HEIGHT);
        stage.addActor(menuTable);
    }

    // -------------------------------------------------------------------------
    // Public open-menu API
    // -------------------------------------------------------------------------

    public void openSummonMenu(int owner, GameState state, int level, String producerType) {
        this.currentBaseOwner = owner;
        this.currentBaseLevel = level;
        populateSummonMenu(state, producerType);
        GameLogger.log(GameLogger.UI,
                "SlideMenu: Open Summon Menu | Owner: P" + owner + " | Lvl: " + level + " | Type: " + producerType);
        slideIn(true);
    }

    public void openHuntMenu(final Entity animalEntity, final Entity hunterUnit,
            final MapGenerator.ObjectType animalType,
            final UnitFactory factory,
            final GameInputController controller) {
        menuTable.clear();
        menuTable.setBackground(game.skin.newDrawable("white", BG_COLOR));
        Table content = new Table();

        UnitFactory.UiInfo info = factory.getObjectUi(animalType);
        GameLogger.log(GameLogger.UI, "SlideMenu: Open Hunt Menu | Target: " + info.name);
        TextureRegion iconRegion = factory.getHudIcon(animalType);
        SummonButton.addTo(content, iconRegion, "Hunt " + info.name, game, assets,
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (gameScreen.getGameState().isLanGame) {
                            GridPositionComponent hPos = hunterUnit.getComponent(GridPositionComponent.class);
                            GridPositionComponent aPos = animalEntity.getComponent(GridPositionComponent.class);
                            if (hPos != null && aPos != null) {
                                gameScreen.getNetworkManager()
                                        .send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_ATTACK,
                                                hPos.x + "," + hPos.y + "," + aPos.x + "," + aPos.y));
                            }
                        }
                        controller.performHunt(animalEntity, hunterUnit);

                        // Tutorial Hook: Hunt Animal
                        if (com.militopia.managers.TutorialManager.getInstance().isActive()
                                && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.HUNT_ANIMAL) {
                            com.militopia.managers.TutorialManager.getInstance().nextStep();
                        }
                    }
                });

        menuTable.add(content).expandX().center();
        slideIn(true);
    }

    public void openCaptureMenu(final Entity structureEntity, final Entity capturingUnit,
            final UnitFactory factory,
            final GameInputController controller,
            final MapGenerator.GameMap map,
            final GameState state) {
        menuTable.clear();
        menuTable.setBackground(game.skin.newDrawable("white", BG_COLOR));
        Table content = new Table();

        final int newOwner = capturingUnit.getComponent(StatsComponent.class).owner;
        TextureRegion baseRegion = (newOwner == 1)
                ? factory.getHudIcon(MapGenerator.ObjectType.BASE_P1)
                : factory.getHudIcon(MapGenerator.ObjectType.BASE_P2);
        // Determine label
        StatsComponent sStats = structureEntity.getComponent(StatsComponent.class);
        String label = "Capture Structure";
        if (sStats != null) {
            if (StructureType.fromKey(sStats.unitTypeKey) == StructureType.TOWN)
                label = "Capture Town";
            else
                label = "Capture Enemy Base";
        }

        GameLogger.log(GameLogger.UI, "SlideMenu: Open Capture Menu | Target: " + label);

        SummonButton.addTo(content, baseRegion, label, game, assets,
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (gameScreen.getGameState().isLanGame) {
                            GridPositionComponent sPos = structureEntity.getComponent(GridPositionComponent.class);
                            if (sPos != null) {
                                gameScreen.getNetworkManager()
                                        .send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_CAPTURE,
                                                sPos.x + "," + sPos.y + "," + newOwner));
                                gameScreen.syncEconomy(newOwner);
                            }
                        }
                        factory.captureStructure(structureEntity, newOwner, map, state);
                        AudioManager.getInstance().playSFX(SFXKeys.STRUCTURE_CAPTURE);
                        // Logging
                        StatsComponent struct = structureEntity.getComponent(StatsComponent.class);
                        GridPositionComponent sPos = structureEntity.getComponent(GridPositionComponent.class);
                        String sName = (struct != null) ? struct.name : "Structure";
                        String spr = (sPos != null) ? GameLogger.pos(sPos.x, sPos.y) : "(?,?)";
                        GameLogger.log(GameLogger.CAPTURE, newOwner, "Captured " + sName + " at " + spr);

                        StatsComponent unitStats = capturingUnit.getComponent(StatsComponent.class);
                        if (unitStats != null) {
                            unitStats.hasActed = true;
                            unitStats.hasMoved = true;
                        }

                        int newXP = (newOwner == 1) ? state.p1XP : state.p2XP;
                        int curFunds = (newOwner == 1) ? state.p1Funding : state.p2Funding;
                        int newIncome = gameScreen.calculateIncome(newOwner);

                        gameScreen.gameHUD.updateXP(newOwner, newXP);
                        gameScreen.gameHUD.updateFunding(newOwner, curFunds, newIncome);

                        // Tutorial Hook: Capture Town
                        if (com.militopia.managers.TutorialManager.getInstance().isActive()
                                && com.militopia.managers.TutorialManager.getInstance()
                                        .getCurrentStep() == com.militopia.managers.TutorialManager.Step.CAPTURE_TOWN) {
                            com.militopia.managers.TutorialManager.getInstance().nextStep();
                        }

                        hide();
                        controller.deselect();
                    }
                });

        menuTable.add(content).expandX().center();
        slideIn(true);
    }

    public void openScavengeMenu(final Entity ruinsEntity, final Entity unit,
            final UnitFactory factory,
            final GameInputController controller) {
        menuTable.clear();
        menuTable.setBackground(game.skin.newDrawable("white", BG_COLOR));
        Table content = new Table();

        TextureRegion ruinsRegion = factory.getHudIcon(MapGenerator.ObjectType.RUINS);
        GameLogger.log(GameLogger.UI, "SlideMenu: Open Scavenge Menu");

        SummonButton.addTo(content, ruinsRegion, "Scavenge Ruins", game, assets,
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        performScavenge(ruinsEntity, unit, factory, controller);
                    }
                });

        menuTable.add(content).expandX().center();
        slideIn(true);
    }

    private void performScavenge(Entity ruinsEntity, Entity unit, UnitFactory factory, GameInputController controller) {
        StatsComponent unitStats = unit.getComponent(StatsComponent.class);
        int owner = unitStats.owner;
        GameState state = gameScreen.getGameState();

        if (state.isLanGame) {
            GridPositionComponent rPos = ruinsEntity.getComponent(GridPositionComponent.class);
            if (rPos != null) {
                gameScreen.getNetworkManager().send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_SCAVENGE,
                        rPos.x + "," + rPos.y + "," + owner));
                gameScreen.syncEconomy(owner);
            }
        }

        ScavengeSystem.ScavengeReward reward = scavengeSystem.performScavenge(ruinsEntity, unit);
        if (reward == null)
            return;
        AudioManager.getInstance().playSFX(SFXKeys.ACTION_SCAVENGE);

        // Update HUD (ScavengeSystem already updated the GameState numbers, but UI
        // needs snap)
        gameScreen.gameHUD.updateXP(owner, (owner == 1) ? state.p1XP : state.p2XP);
        gameScreen.gameHUD.updateFunding(owner, (owner == 1) ? state.p1Funding : state.p2Funding, currentIncome);

        // Tutorial Hook: Scavenge Ruins
        if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager
                .getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.SCAVENGE_RUINS) {
            com.militopia.managers.TutorialManager.getInstance().nextStep();
        }

        hide();
        controller.deselect();
    }

    public void openBuildMenu(int x, int y, int owner, int maxLevel,
            boolean isWater, boolean isCoastalWater, boolean isCoastalLand,
            GameState state, int parentX, int parentY,
            MapGenerator.TerrainType terrain, UnitFactory unitFactory) {
        this.buildX = x;
        this.buildY = y;
        this.currentBaseOwner = owner;
        this.buildParentX = parentX;
        this.buildParentY = parentY;
        boolean hasItems = populateBuildMenu(state, maxLevel, isWater, isCoastalWater, isCoastalLand);
        if (hasItems) {
            GameLogger.log(GameLogger.UI, "SlideMenu: Open Build Menu | At: " + GameLogger.pos(x, y) + " | Owner: P"
                    + owner + " | Max Lvl: " + maxLevel);
            slideIn(true);
        } else {
            // No buildable structures for this tile — show terrain info in the Info Panel
            // instead.
            infoPanel.showTileInfo(unitFactory.getTerrainUi(terrain).name,
                    unitFactory.getTextureForTerrain(terrain.ordinal()));
        }
    }

    /** Slides the menu back down (hides). */
    public void hide() {
        AudioManager.getInstance().playSFX(SFXKeys.UI_PANEL_CLOSE);
        menuTable.clearActions();
        menuTable.addAction(Actions.moveTo(0, -menuTable.getHeight(), 0.3f, Interpolation.pow2In));
        bottomBar.getBottomContainer().clearActions();
        bottomBar.getBottomContainer()
                .addAction(Actions.moveTo(bottomBar.getBottomContainer().getX(), 0,
                        0.3f, Interpolation.pow2In));
        GameLogger.log(GameLogger.UI, "SlideMenu: Hide");
    }

    public void resize(int width, int height) {
        menuTable.setWidth(width);
        menuTable.setX(0);
    }

    // -------------------------------------------------------------------------
    // Private menu builders
    // -------------------------------------------------------------------------

    private void populateSummonMenu(GameState state, String producerType) {
        menuTable.clear();
        menuTable.setBackground(game.skin.newDrawable("white", BG_COLOR));
        Table content = new Table();
        populateSummonContent(content, state, producerType);
        menuTable.add(content).expandX().center();
    }

    private void populateSummonContent(Table content, GameState state, String producerType) {
        java.util.Set<String> unlocked = unlockedForLevel(currentBaseLevel, false);
        UnitType[] allUnits = {
                UnitType.RECRUIT, UnitType.RANGER, UnitType.SNIPER, UnitType.TANK, UnitType.RECON_DRONE,
                UnitType.SUICIDE_DRONE, UnitType.APACHE, UnitType.GUNBOAT, UnitType.DESTROYER, UnitType.CARRIER
        };

        for (final UnitType unit : allUnits) {
            if (!unlocked.contains(unit.name()))
                continue;
            StatsComponent.MoveType moveType = unitFactory.getUnitMoveType(unit);
            boolean show = StructureType.PORT.getKey().equals(producerType)
                    ? moveType == StatsComponent.MoveType.SEA
                    : moveType == StatsComponent.MoveType.LAND || moveType == StatsComponent.MoveType.AIR;
            if (!show)
                continue;

            UnitFactory.UiInfo info = unitFactory.getUnitUi(unit);
            final int cost = UnitFactory.getUnitCost(unit);

            SummonButton.addTo(content, info.region, info.name + " (" + cost + ")", game, assets,
                    new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            int funds = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                            if (funds < cost) {
                                GameLogger.log(GameLogger.SUMMON, currentBaseOwner,
                                        "Attempted " + unit.name() + " — insufficient funds ("
                                                + funds + "<" + cost + ")");
                                return;
                            }
                            int tx = inputController.getLastClickedX();
                            int ty = inputController.getLastClickedY();
                            if (tx == -1 || ty == -1)
                                return;

                            int[] spawn = unitFactory.findValidSpawnPoint(
                                    tx, ty, moveType, gameScreen.getGameMap());
                            if (spawn == null) {
                                GameLogger.log(GameLogger.SUMMON, currentBaseOwner,
                                        "Attempted " + unit.name() + " — no valid spawn point found");
                                return;
                            }
                            if (currentBaseOwner == 1)
                                state.p1Funding -= cost;
                            else
                                state.p2Funding -= cost;

                            if (state.isLanGame) {
                                gameScreen.getNetworkManager().send(NetworkMessage.action(
                                        NetworkMessage.TYPE_ACTION_SUMMON,
                                        unit.name() + "," + spawn[0] + "," + spawn[1] + "," + currentBaseOwner));
                                gameScreen.syncEconomy(currentBaseOwner);
                            }

                            unitFactory.createUnit(unit, spawn[0], spawn[1], currentBaseOwner, true);
                            AudioManager.getInstance().playSFX(SFXKeys.UNIT_DEPLOY);
                            int remaining = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                            GameLogger.log(GameLogger.SUMMON, currentBaseOwner,
                                    "Summoned " + unit.name() + " at " + GameLogger.pos(spawn[0], spawn[1])
                                            + " | cost=" + cost + " | funds remaining=" + remaining);

                            // Use fresh income calculation for HUD update
                            int newIncome = gameScreen.calculateIncome(currentBaseOwner);
                            gameScreen.gameHUD.updateFunding(currentBaseOwner, remaining, newIncome);

                            // Tutorial Hook: Summon Unit
                            if (com.militopia.managers.TutorialManager.getInstance().isActive()
                                    && com.militopia.managers.TutorialManager.getInstance()
                                            .getCurrentStep() == com.militopia.managers.TutorialManager.Step.SUMMON_UNIT) {
                                com.militopia.managers.TutorialManager.getInstance().nextStep();
                            }

                            hide();
                            inputController.resetLastClicked();
                        }
                    });
        }

        // --- NEW: Add Chosen Super Units from all friendly bases ---
        com.badlogic.ashley.core.Engine engine = unitFactory.getEngine();
        com.badlogic.ashley.utils.ImmutableArray<Entity> allBases = engine.getEntitiesFor(
                com.badlogic.ashley.core.Family.all(StatsComponent.class).get());
        java.util.Set<String> superUnitsAdded = new java.util.HashSet<>();

        for (Entity base : allBases) {
            StatsComponent bs = base.getComponent(StatsComponent.class);
            if (bs.owner == currentBaseOwner && bs.chosenSuperUnit != null && !bs.chosenSuperUnit.isEmpty()) {
                if (superUnitsAdded.contains(bs.chosenSuperUnit))
                    continue;

                final UnitType superUnit = UnitType.fromKey(bs.chosenSuperUnit);
                if (superUnit == null)
                    continue;

                StatsComponent.MoveType moveType = unitFactory.getUnitMoveType(superUnit);
                boolean show = StructureType.PORT.getKey().equals(producerType)
                        ? moveType == StatsComponent.MoveType.SEA
                        : moveType == StatsComponent.MoveType.LAND || moveType == StatsComponent.MoveType.AIR;

                if (!show)
                    continue;

                superUnitsAdded.add(bs.chosenSuperUnit);
                UnitFactory.UiInfo info = unitFactory.getUnitUi(superUnit);
                final int cost = UnitFactory.getUnitCost(superUnit);

                SummonButton.addTo(content, info.region, info.name + " (" + cost + ")", game, assets,
                        new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                int funds = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                                if (funds < cost) {
                                    AudioManager.getInstance().playSFX(SFXKeys.RESOURCE_INSUFFICIENT);
                                    return;
                                }
                                int tx = inputController.getLastClickedX();
                                int ty = inputController.getLastClickedY();
                                if (tx == -1 || ty == -1)
                                    return;

                                int[] spawn = unitFactory.findValidSpawnPoint(tx, ty, moveType,
                                        gameScreen.getGameMap());
                                if (spawn == null)
                                    return;

                                if (currentBaseOwner == 1)
                                    state.p1Funding -= cost;
                                else
                                    state.p2Funding -= cost;

                                if (state.isLanGame) {
                                    gameScreen.getNetworkManager()
                                            .send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_SUMMON,
                                                    superUnit.name() + "," + spawn[0] + "," + spawn[1] + ","
                                                            + currentBaseOwner));
                                    gameScreen.syncEconomy(currentBaseOwner);
                                }

                                unitFactory.createUnit(superUnit, spawn[0], spawn[1], currentBaseOwner, true);
                                AudioManager.getInstance().playSFX(SFXKeys.UNIT_DEPLOY);
                                int remaining = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                                int newIncome = gameScreen.calculateIncome(currentBaseOwner);
                             gameScreen.gameHUD.updateFunding(currentBaseOwner, remaining, newIncome);
                                hide();
                                inputController.resetLastClicked();
                            }
                        });
            }
        }

        // Cancel button
        com.badlogic.gdx.scenes.scene2d.ui.TextButton closeBtn = new com.badlogic.gdx.scenes.scene2d.ui.TextButton(
                "Cancel", game.skin);
        closeBtn.addListener(new HoverListener());
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                hide();
                inputController.resetLastClicked();
            }
        });
        content.add(closeBtn).pad(10);
    }

    /**
     * Populates the build-menu content table. Returns {@code true} if at least
     * one build button was added, or {@code false} if nothing is buildable on
     * this tile (so the caller can fall back to showTileInfo).
     */
    private boolean populateBuildMenu(GameState state, int maxLevel,
            boolean isWater, boolean isCoastalWater, boolean isCoastalLand) {
        menuTable.clear();
        menuTable.setBackground(game.skin.newDrawable("white", BG_COLOR));
        Table content = new Table();

        java.util.Set<String> unlocked = unlockedForLevel(maxLevel, true);
        boolean isOilTile = gameScreen.getGameMap().objects[buildX][buildY] == MapGenerator.ObjectType.OIL;
        int addedCount = 0;

        // Check for existing built structures not tracked in gameMap.objects
        Entity existingStruct = unitFactory.getEntityAt(buildX, buildY, 1);
        boolean hasBlockingStructure = false;
        if (existingStruct != null) {
            StatsComponent stats = existingStruct.getComponent(StatsComponent.class);
            // Allow building ONLY if it's an Oil Reservoir (allows Oil Derrick)
            if (stats == null || !MapGenerator.ObjectType.OIL.name().equals(stats.unitTypeKey)) {
                hasBlockingStructure = true;
            }
        }

        for (final String struct : unlocked) {
            boolean show;
            if (hasBlockingStructure) {
                show = false;
            } else if (isOilTile) {
                // On Oil tiles, ONLY show Oil Derrick
                show = StructureType.OIL_DERRICK.getKey().equals(struct);
            } else {
                // On regular tiles, show everything BUT Oil Derrick (and respect water/land)
                if (StructureType.PORT.getKey().equals(struct))
                    show = isWater && isCoastalWater;
                else if (StructureType.NUCLEAR_PLANT.getKey().equals(struct))
                    show = !isWater && isCoastalLand;
                else if (StructureType.OIL_DERRICK.getKey().equals(struct))
                    show = false; // Hide on non-oil tiles
                else
                    show = !isWater;
            }
            if (!show)
                continue;

            addedCount++;
            TextureRegion icon = unitFactory.getTextureForPopup(struct);
            final int cost = unitFactory.getStructureCost(struct);
            String niceName = unitFactory.toNiceName(struct) + " (" + cost + ")";

            SummonButton.addToWrapped(content, icon, niceName, game, assets,
                    new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            if (!placementSystem.canBuild(struct, buildX, buildY, currentBaseOwner, cost, isWater,
                                    isCoastalWater, isCoastalLand)) {
                                return;
                            }

                            if (state.isLanGame) {
                                gameScreen.getNetworkManager()
                                        .send(NetworkMessage.action(NetworkMessage.TYPE_ACTION_BUILD,
                                                struct + "," + buildX + "," + buildY + "," + currentBaseOwner));
                                gameScreen.syncEconomy(currentBaseOwner);
                            }

                            placementSystem.performBuild(struct, buildX, buildY, currentBaseOwner, cost, buildParentX,
                                    buildParentY);
                            AudioManager.getInstance().playSFX(SFXKeys.ACTION_BUILD);

                            int remaining = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                            int newIncome = gameScreen.calculateIncome(currentBaseOwner);

                            gameScreen.gameHUD.updateFunding(currentBaseOwner, remaining, newIncome);

                            // Tutorial Hook: Build Structure
                            if (com.militopia.managers.TutorialManager.getInstance().isActive()
                                    && com.militopia.managers.TutorialManager.getInstance()
                                            .getCurrentStep() == com.militopia.managers.TutorialManager.Step.BUILD_STRUCTURE) {
                                com.militopia.managers.TutorialManager.getInstance().nextStep();
                            }

                            hide();
                            inputController.resetLastClicked();
                        }
                    });
        }
        // --- Build Railway button ---
        MapGenerator.TerrainType tileType = gameScreen.getGameMap().terrain[buildX][buildY];
        boolean canShowRail = tileType != MapGenerator.TerrainType.WATER
                && tileType != MapGenerator.TerrainType.DEEP_WATER
                && tileType != MapGenerator.TerrainType.MOUNTAIN
                && !gameScreen.getGameMap().rails[buildX][buildY];
        if (canShowRail) {
            final int railCost = 3;
            String railLabel = "Build Railway (" + railCost + ")";
            TextureRegion railIcon = unitFactory.getTextureForPopup("RAILWAY");
            SummonButton.addToWrapped(content, railIcon, railLabel, game, assets,
                    new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            int funds = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                            if (funds < railCost)
                                return;
                            if (currentBaseOwner == 1)
                                state.p1Funding -= railCost;
                            else
                                state.p2Funding -= railCost;
                            gameScreen.getGameMap().rails[buildX][buildY] = true;
                            AudioManager.getInstance().playSFX(SFXKeys.ACTION_BUILD);
                            GameLogger.log(GameLogger.BUILD, currentBaseOwner,
                                    "Built Railway at (" + buildX + "," + buildY + ")");
                            int remaining = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                            int newIncome = gameScreen.calculateIncome(currentBaseOwner);
                            gameScreen.gameHUD.updateFunding(currentBaseOwner, remaining, newIncome);
                            hide();
                            inputController.resetLastClicked();
                        }
                    });
            addedCount++;
        }

        if (addedCount > 0) {
            menuTable.add(content).expandX().center();
            menuTable.row();
        }
        return addedCount > 0;
    }

    // -------------------------------------------------------------------------
    // Animation helpers
    // -------------------------------------------------------------------------

    private void slideIn(boolean hideInfoPanel) {
        AudioManager.getInstance().playSFX(SFXKeys.UI_PANEL_OPEN);
        menuTable.setSize(stage.getWidth(), PANEL_HEIGHT);
        menuTable.setX(0);
        stage.addActor(menuTable);

        if (hideInfoPanel) {
            infoPanel.getTileInfoTable().clearActions();
            infoPanel.getTileInfoTable().addAction(
                    Actions.moveTo(0, -infoPanel.getTileInfoTable().getHeight(),
                            0.3f, Interpolation.pow2In));
        }
        menuTable.clearActions();
        menuTable.addAction(Actions.moveTo(0, 0, 0.3f, Interpolation.pow2Out));

        bottomBar.getBottomContainer().clearActions();
        bottomBar.getBottomContainer().addAction(
                Actions.moveTo(bottomBar.getBottomContainer().getX(),
                        -bottomBar.getBottomContainer().getHeight(),
                        0.3f, Interpolation.pow2Out));
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /** Collect unlocked unit OR structure keys for levels 1..maxLevel. */
    private java.util.Set<String> unlockedForLevel(int maxLevel, boolean structs) {
        return BaseLevelConfig.getUnlockedForLevel(maxLevel, structs);
    }

    /**
     * Package-visible accessors used by {@link GameHUD} to forward to InfoPanel.
     */
    GameInputController getInputController() {
        return inputController;
    }

    UnitFactory getUnitFactory() {
        return unitFactory;
    }

    GameScreen getGameScreen() {
        return gameScreen;
    }

    /**
     * Called by GameHUD.updateFunding() so menu buttons know the current income.
     */
    public void setCurrentIncome(int income) {
        this.currentIncome = income;
    }
}
