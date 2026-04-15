package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.militopia.MilitopiaGame;
import com.militopia.config.GameConfig;
import com.militopia.controller.GameInputController;
import com.militopia.managers.AssetManager;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.screen.GameScreen;
import com.militopia.utils.HoverListener;

/**
 * Bottom gradient bar: Settings, Game Stats, (Undo in testing), End Turn
 * buttons.
 * Also owns the Settings overlay (pause menu).
 */
public class HudBottomBar {

    private final MilitopiaGame game;
    private final AssetManager assets;
    private final GameInputController inputController;
    private final GameHUD gameHud;

    private Table bottomContainer;
    private Table settingsOverlay;
    private TutorialCompletePopup tutorialCompletePopup;

    private Label waitingLabel;
    private Table actionTable; // Holds settings, stats, endTurn for easy toggle
    private com.badlogic.gdx.scenes.scene2d.ui.ImageButton settingsBtn;
    private com.badlogic.gdx.scenes.scene2d.ui.ImageButton statsBtn;
    private com.badlogic.gdx.scenes.scene2d.ui.ImageButton endTurnBtn;

    public HudBottomBar(MilitopiaGame game, AssetManager assets,
            final GameScreen screen, Stage stage, GameInputController inputController,
            GameHUD gameHud) {
        this.game = game;
        this.assets = assets;
        this.inputController = inputController;
        this.gameHud = gameHud;

        buildBottomBar(screen, stage);
        buildSettingsOverlay(screen, stage);
        tutorialCompletePopup = new TutorialCompletePopup(game, screen, stage, inputController, this);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns the bottom container Table for adding to rootTable. */
    public Table getActor() {
        return bottomContainer;
    }

    /**
     * Returns the container directly (needed by SlideMenu / InfoPanel for
     * animations).
     */
    public Table getBottomContainer() {
        return bottomContainer;
    }

    /**
     * Enables or disables all touch events on the bottom bar.
     * Call with {@code true} while the Level-Up popup is visible so that
     * End Turn / Settings / Undo cannot be triggered.
     */
    public void setBlocked(boolean blocked) {
        bottomContainer.setTouchable(blocked ? Touchable.disabled : Touchable.childrenOnly);
    }

    public void resize(int width, int height) {
        if (bottomContainer != null) {
            bottomContainer.setWidth(width);
            bottomContainer.setX(0);
        }
    }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    private void buildBottomBar(final GameScreen screen, Stage stage) {
        TextureRegionDrawable bottomBg = createGradientDrawable(80, false);

        actionTable = new Table();
        settingsBtn = createCircleButton(AssetManager.ICON_SETTINGS);
        statsBtn = createCircleButton(AssetManager.ICON_STATS);
        endTurnBtn = createCircleButton(AssetManager.ICON_END);

        waitingLabel = new Label("Wait for the opponent turn end", game.skin, "default-font",
                game.skin.get("color-gold", Color.class));
        waitingLabel.setFontScale(0.85f);
        waitingLabel.setVisible(false);

        actionTable.add(createIconGroup(settingsBtn, "Settings")).expandX().padLeft(20).padRight(30);
        actionTable.add(createIconGroup(statsBtn, "Game Stats")).expandX().padLeft(30).padRight(30);

        if (screen.getGameState().isDevMode) {
            com.badlogic.gdx.scenes.scene2d.ui.ImageButton devBtn = createCircleButton(AssetManager.ICON_SETTINGS);
            devBtn.setColor(new Color(1f, 0.45f, 0f, 1f));
            devBtn.addListener(new HoverListener());
            devBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_TOGGLE);
                    gameHud.toggleDevPanel();
                }
            });
            actionTable.add(createIconGroup(devBtn, "DEV")).expandX().padLeft(30).padRight(30);
        }

        actionTable.add(createIconGroup(endTurnBtn, "End Turn")).expandX().padLeft(30).padRight(20);

        float barWidth = screen.getGameState().isDevMode ? GameConfig.UI_WIDTH + 80 : GameConfig.UI_WIDTH;
        bottomContainer = new Table();
        bottomContainer.setBackground(bottomBg);
        bottomContainer.add(actionTable).width(barWidth).padBottom(15).padTop(25);
        bottomContainer.row();
        bottomContainer.add(waitingLabel).padBottom(15);
        
        bottomContainer.pack();
        bottomContainer.setWidth(stage.getWidth());
        bottomContainer.setPosition(0, 0);

        // Wire button listeners
        settingsBtn.addListener(new HoverListener());
        statsBtn.addListener(new HoverListener());
        endTurnBtn.addListener(new HoverListener());

        statsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                gameHud.showGameStats(screen.getGameState());

                // Tutorial Hook: Check Stats (Game Stats Panel)
                if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.CHECK_STATS) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                }
            }
        });

        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (settingsOverlay != null) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_PANEL_OPEN);
                    settingsOverlay.setVisible(true);
                    // Freeze map while settings overlay is open.
                    inputController.setInputEnabled(false);
                }
            }
        });
        endTurnBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                screen.endTurnAction();
                // Note: endTurnAction plays TURN_END_PLAYER internally

                // Tutorial Hook: End Turn (refresh after summon)
                if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.END_TURN_REFRESH) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                }

                // Tutorial Hook: End Turn (hold town before capture)
                if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.END_TURN_CAPTURE) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                }

                // Tutorial Hook: End Turn (before cut tree)
                if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.END_TURN_CUT) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                }

                // Tutorial Hook: End Turn (after cut tree, before moving to deer)
                if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.END_TURN_BEFORE_DEER) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                }

                // Tutorial Hook: End Turn (before hunt)
                if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.END_TURN_HUNT) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                }

                // Tutorial Hook: End Turn (after hunt, before moving to attack)
                if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.END_TURN_BEFORE_ATTACK) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                }

                // Tutorial Hook: End Turn (before attack)
                if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.END_TURN_ATTACK) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                }

                // Tutorial Hook: End Turn (after attack, before moving to ruins)
                if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.END_TURN_BEFORE_RUINS) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                }

                // Tutorial Hook: End Turn (before scavenge)
                if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.END_TURN_SCAVENGE) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                }

                // Tutorial Hook: End Turn
                if (com.militopia.managers.TutorialManager.getInstance().isActive() && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.END_TURN) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                }
            }
        });
    }

    private void buildSettingsOverlay(final GameScreen screen, Stage stage) {
        settingsOverlay = new Table();
        settingsOverlay.setFillParent(true);
        settingsOverlay.setVisible(false);

        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0.85f);
        p.fill();
        settingsOverlay.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture(p))));
        p.dispose();

        Table menuBox = new Table();
        menuBox.setBackground(game.skin.newDrawable("white", Color.DARK_GRAY));

        Label title = new Label("PAUSED", game.skin);
        title.setFontScale(1.5f);

        final TextButton fogBtn = new TextButton("Toggle Fog: ON", game.skin, "militopia-btn");
        fogBtn.addListener(new HoverListener());
        fogBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_TOGGLE);
                boolean newState = screen.toggleFog();
                fogBtn.setText("Toggle Fog: " + (newState ? "ON" : "OFF"));
            }
        });

        String saveExitText = screen.getGameState().isLanGame ? "Exit Game" : "Save & Exit";
        TextButton saveExitBtn = new TextButton(saveExitText, game.skin, "militopia-btn");
        saveExitBtn.addListener(new HoverListener());
        saveExitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);

                // Tutorial Hook: Save & Exit — show congratulations instead of exiting immediately
                if (com.militopia.managers.TutorialManager.getInstance().isActive()
                        && com.militopia.managers.TutorialManager.getInstance().getCurrentStep() == com.militopia.managers.TutorialManager.Step.SAVE_EXIT) {
                    com.militopia.managers.TutorialManager.getInstance().nextStep();
                    settingsOverlay.setVisible(false);
                    tutorialCompletePopup.show();
                    return;
                }

                screen.saveAndExit();
            }
        });

        final TextButton undoRedoBtn = new TextButton("Undo/Redo: OFF", game.skin, "militopia-btn");
        undoRedoBtn.addListener(new HoverListener());
        undoRedoBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_TOGGLE);
                boolean enabled = gameHud.toggleSnapshotPanel();
                undoRedoBtn.setText("Undo/Redo: " + (enabled ? "ON" : "OFF"));
            }
        });

        TextButton resumeBtn = new TextButton("Resume", game.skin, "militopia-btn");
        resumeBtn.addListener(new HoverListener());
        resumeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_PANEL_CLOSE);
                settingsOverlay.setVisible(false);
                // Restore map input when returning to game.
                inputController.setInputEnabled(true);
            }
        });

        menuBox.add(title).padTop(15).padBottom(15).row();
        if (!screen.getGameState().isLanGame) {
            menuBox.add(fogBtn).fillX().width(240).pad(6).row();
        }
        TextButton soundSettingsBtn = new TextButton("Sound Settings", game.skin, "militopia-btn");
        soundSettingsBtn.addListener(new HoverListener());
        soundSettingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                settingsOverlay.setVisible(false);
                new SoundSettingsPopup(game, stage, inputController, HudBottomBar.this).show();
            }
        });

        if (!screen.getGameState().isLanGame) {
            menuBox.add(undoRedoBtn).fillX().width(240).pad(6).row();
        }
        menuBox.add(soundSettingsBtn).fillX().width(240).pad(6).row();
        menuBox.add(saveExitBtn).fillX().width(240).pad(6).row();
        if (screen.getGameState().isLanGame) {
            TextButton chatBtn = new TextButton("Chat", game.skin, "militopia-btn");
            chatBtn.addListener(new HoverListener());
            chatBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_TOGGLE);
                    settingsOverlay.setVisible(false);
                    inputController.setInputEnabled(true);
                    gameHud.toggleChat();
                }
            });
            menuBox.add(chatBtn).fillX().width(240).pad(6).row();
        }
        menuBox.add(resumeBtn).fillX().width(240).pad(6);
        settingsOverlay.add(menuBox).width(280);
        stage.addActor(settingsOverlay);
    }

    // -------------------------------------------------------------------------
    // Widget helpers
    // -------------------------------------------------------------------------

    private ImageButton createCircleButton(String iconPath) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        try {
            Texture iconTex = assets.get(iconPath);
            iconTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            TextureRegionDrawable icon = new TextureRegionDrawable(new TextureRegion(iconTex));
            style.imageUp = icon;
            style.imageDown = icon.tint(Color.LIGHT_GRAY);
        } catch (Exception e) {
            style.up = game.skin.newDrawable("white", Color.DARK_GRAY);
        }

        ImageButton btn = new ImageButton(style);
        btn.setTransform(true);
        btn.setSize(80, 80);
        btn.setOrigin(Align.center);
        btn.getImageCell().size(80, 80);
        return btn;
    }

    /**
     * Updates the UI based on whose turn it is.
     * Hides the End Turn button and shows "Hold" message if it's the opponent's
     * turn.
     */
    public void updateTurnState(int currentPlayer, int localPlayerID) {
        boolean isMyTurn = (currentPlayer == localPlayerID);
        endTurnBtn.setVisible(isMyTurn);
        waitingLabel.setVisible(!isMyTurn);

        if (!isMyTurn) {
            // Fade effect for the waiting label to make it feel "active"
            waitingLabel.clearActions();
            waitingLabel.setColor(game.skin.get("color-gold", Color.class));
            waitingLabel.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.forever(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                            com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(1.0f),
                            com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn(1.0f))));
        }
    }

    private Table createIconGroup(ImageButton btn, String labelText) {
        Table t = new Table();
        t.add(btn).size(56, 56).row();
        Label lbl = new Label(labelText, game.skin, "default-font", Color.WHITE);
        lbl.setFontScale(0.7f);
        t.add(lbl).padTop(5);
        return t;
    }

    private TextureRegionDrawable createGradientDrawable(int height, boolean isTopDown) {
        Pixmap pixmap = new Pixmap(1, height, Pixmap.Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            float alpha = isTopDown ? 1.0f - ((float) y / height) : ((float) y / height);
            pixmap.setColor(0f, 0f, 0f, alpha);
            pixmap.drawPixel(0, y);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }
}
