package com.militopia.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
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

public class MenuScreen implements Screen {

    final MilitopiaGame game;
    Stage stage;

    public MenuScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ExtendViewport(1280, 800));
        Gdx.input.setInputProcessor(stage);
        buildUI();
    }

    private void buildUI() {
        stage.clear();

        float panelWidth = stage.getWidth() * 0.27f;
        float panelHeight = stage.getHeight() * 0.82f;
        float contentWidth = panelWidth - 40f;

        Table root = new Table();
        root.setFillParent(true);
        root.left();
        root.padLeft(50);
        stage.addActor(root);

        Table panel = new Table();
        panel.setBackground(game.skin.getDrawable("menu-panel"));
        panel.top().left();
        panel.pad(40, 20, 40, 20);

        // Logo
        Texture logoTex = game.assets.get(AssetManager.TEXT_LOGO);
        Image logoImage = new Image(new TextureRegionDrawable(new TextureRegion(logoTex)));
        logoImage.setScaling(Scaling.fit);
        panel.add(logoImage).width(contentWidth).height(contentWidth * 0.333f).padBottom(4).left();
        panel.row();

        // Subtitle
        Label subtitle = new Label("DIVIDE AND CONQUER", game.skin, "menu-title");
        subtitle.setAlignment(Align.center);
        panel.add(subtitle).fillX().padBottom(50).center();
        panel.row();

        // Menu items
        addMenuItem(panel, panelWidth, "NEW GAME", new Runnable() {
            @Override
            public void run() {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                GameLogger.logScreen("Navigating → New Game Screen");
                game.setScreen(new NewGameScreen(game));
            }
        });
        addMenuItem(panel, panelWidth, "MULTIPLAYER", new Runnable() {
            @Override
            public void run() {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                GameLogger.logScreen("Navigating → LAN Lobby");
                game.setScreen(new LobbyScreen(game));
            }
        });
        addMenuItem(panel, panelWidth, "TUTORIAL", new Runnable() {
            @Override
            public void run() {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                GameLogger.logScreen("Navigating → Tutorial Screen");
                game.setScreen(new TutorialScreen(game));
            }
        });
        addMenuItem(panel, panelWidth, "SAVED GAMES", new Runnable() {
            @Override
            public void run() {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                GameLogger.logScreen("Navigating → Load Game Screen");
                game.setScreen(new LoadGameScreen(game));
            }
        });
        addMenuItem(panel, panelWidth, "SOUND SETTINGS", new Runnable() {
            @Override
            public void run() {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                new SoundSettingsPopup(game, stage, null, null).show();
            }
        });
        addMenuItem(panel, panelWidth, "EXIT", new Runnable() {
            @Override
            public void run() {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                GameLogger.logScreen("Exit requested");
                Gdx.app.exit();
            }
        });

        panel.add().expandY();

        root.add(panel).width(panelWidth).height(panelHeight).top();
    }

    private void addMenuItem(Table panel, float panelWidth, String text, final Runnable onClick) {
        final Table row = new Table();
        row.setBackground(game.skin.newDrawable("white", 0f, 0f, 0f, 0.40f));
        row.setTouchable(Touchable.enabled);
        row.left();

        final Image accentBar = new Image(game.skin.newDrawable("white", 0.95f, 0.79f, 0.30f, 1f));
        accentBar.setVisible(false);

        final Label label = new Label(text, game.skin, "menu-item");
        label.setAlignment(Align.left);

        row.add(accentBar).width(4).fillY();
        row.add(label).padLeft(16).padTop(10).padBottom(10).left().expandX();

        row.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                accentBar.setVisible(true);
                label.setStyle(game.skin.get("menu-item-hover", Label.LabelStyle.class));
                AudioManager.getInstance().playSFX(SFXKeys.UI_HOVER);
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                accentBar.setVisible(false);
                label.setStyle(game.skin.get("menu-item", Label.LabelStyle.class));
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                onClick.run();
            }
        });

        panel.add(row).width(panelWidth * 0.92f).padBottom(8).padRight(8).right();
        panel.row();
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
        buildUI();
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
