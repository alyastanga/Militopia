package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.militopia.MilitopiaGame;
import com.militopia.controller.GameInputController;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.screen.GameScreen;
import com.militopia.utils.GameLogger;
import com.militopia.utils.HoverListener;

/**
 * Congratulations popup shown when the player completes the tutorial.
 */
public class TutorialCompletePopup {

    private final Stage stage;
    private final MilitopiaGame game;
    private final GameScreen gameScreen;
    private final GameInputController inputController;
    private final HudBottomBar bottomBar;

    private Table popupTable;

    public TutorialCompletePopup(MilitopiaGame game, GameScreen gameScreen, Stage stage,
            GameInputController inputController, HudBottomBar bottomBar) {
        this.game = game;
        this.gameScreen = gameScreen;
        this.stage = stage;
        this.inputController = inputController;
        this.bottomBar = bottomBar;
    }

    public void show() {
        buildPopup();
        popupTable.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        stage.addActor(popupTable);
        popupTable.toFront();
        inputController.setInputEnabled(false);
        bottomBar.setBlocked(true);
    }

    private void buildPopup() {
        popupTable = new Table();
        popupTable.setFillParent(true);
        popupTable.setBackground(game.skin.newDrawable("white", new Color(0, 0, 0, 0.85f)));

        // Block all input behind the popup
        popupTable.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent e, float x, float y, int p, int b) {
                return true;
            }
            @Override
            public boolean mouseMoved(InputEvent e, float x, float y) {
                return true;
            }
            @Override
            public boolean keyDown(InputEvent e, int keycode) {
                return true;
            }
        });

        Table modal = new Table();

        Label title = new Label("TUTORIAL COMPLETE!", game.skin, "default-font", Color.GOLD);
        title.setFontScale(2.5f);
        modal.add(title).padBottom(20).row();

        Label subtitle = new Label("You're ready to play Militopia!", game.skin, "default-font", Color.WHITE);
        subtitle.setFontScale(1.1f);
        modal.add(subtitle).padBottom(40).row();

        TextButton menuBtn = new TextButton("Back to Main Menu", game.skin, "militopia-btn");
        menuBtn.addListener(new HoverListener());
        menuBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                GameLogger.logScreen("Tutorial complete — navigating to Main Menu");
                gameScreen.saveAndExit();
            }
        });
        modal.add(menuBtn).fillX().width(300);

        popupTable.add(modal);
    }
}
