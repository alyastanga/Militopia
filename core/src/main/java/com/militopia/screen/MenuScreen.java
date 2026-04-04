package com.militopia.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.militopia.MilitopiaGame;
import com.militopia.managers.AssetManager;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.managers.VideoBackgroundManager;
import com.militopia.utils.GameLogger;
import com.militopia.ui.SoundSettingsPopup;
import com.militopia.utils.HoverListener;

public class MenuScreen implements Screen {

    final MilitopiaGame game;
    Stage stage;

    public MenuScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ExtendViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        TextButton newGameBtn = new TextButton("NEW GAME", game.skin, "militopia-btn");
        newGameBtn.getLabel().setFontScale(1.4f);
        newGameBtn.addListener(new HoverListener());
        TextButton lanBtn = new TextButton("MULTIPLAYER", game.skin, "militopia-btn");
        lanBtn.getLabel().setFontScale(1.4f);
        lanBtn.addListener(new HoverListener());
        TextButton tutorialBtn = new TextButton("TUTORIAL", game.skin, "militopia-btn");
        tutorialBtn.getLabel().setFontScale(1.4f);
        tutorialBtn.addListener(new HoverListener());
        TextButton resumeBtn = new TextButton("SAVED GAMES", game.skin, "militopia-btn");
        resumeBtn.getLabel().setFontScale(1.4f);
        resumeBtn.addListener(new HoverListener());
        TextButton soundBtn = new TextButton("SOUND SETTINGS", game.skin, "militopia-btn");
        soundBtn.getLabel().setFontScale(1.4f);
        soundBtn.addListener(new HoverListener());
        TextButton exitBtn = new TextButton("EXIT", game.skin, "militopia-btn");
        exitBtn.getLabel().setFontScale(1.4f);
        exitBtn.addListener(new HoverListener());

        newGameBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                GameLogger.logScreen("Navigating → New Game Screen");
                game.setScreen(new NewGameScreen(game));
            }
        });

        lanBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                GameLogger.logScreen("Navigating → LAN Lobby");
                game.setScreen(new LobbyScreen(game));
            }
        });

        tutorialBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                GameLogger.logScreen("Navigating → Tutorial Screen");
                game.setScreen(new TutorialScreen(game));
            }
        });

        resumeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                GameLogger.logScreen("Navigating → Load Game Screen");
                game.setScreen(new LoadGameScreen(game));
            }
        });

        soundBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                new SoundSettingsPopup(game, stage, null, null).show();
            }
        });

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                GameLogger.logScreen("Exit requested");
                Gdx.app.exit();
            }
        });

        // --- TEXT LOGO ---
        Texture logoTex = game.assets.get(AssetManager.TEXT_LOGO);
        Image logoImage = new Image(new TextureRegionDrawable(new TextureRegion(logoTex)));
        logoImage.setScaling(Scaling.fit);

        table.add(logoImage).width(Value.percentWidth(0.5f, table)).height(Value.percentHeight(0.3f, table)).padBottom(15);
        table.row();
        table.add(newGameBtn).fillX().width(Value.percentWidth(0.35f, table)).pad(4);
        table.row();
        table.add(lanBtn).fillX().width(Value.percentWidth(0.35f, table)).pad(4);
        table.row();
        table.add(tutorialBtn).fillX().width(Value.percentWidth(0.35f, table)).pad(4);
        table.row();
        table.add(resumeBtn).fillX().width(Value.percentWidth(0.35f, table)).pad(4);
        table.row();
        table.add(soundBtn).fillX().width(Value.percentWidth(0.35f, table)).pad(4);
        table.row();
        table.add(exitBtn).fillX().width(Value.percentWidth(0.35f, table)).pad(4);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        VideoBackgroundManager.getInstance().render(game.batch);
        stage.act();
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
        GameLogger.logScreen("Main Menu opened");
        Gdx.input.setInputProcessor(stage);
        AudioManager.getInstance().setBGMVolume(0.5f);
        AudioManager.getInstance().playBGM("final_battle_theme.ogg", true);
        VideoBackgroundManager.getInstance().play();
    }

    @Override
    public void hide() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        VideoBackgroundManager.getInstance().pause();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
