package com.militopia.screen;

import com.militopia.screen.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.data.GameState;
import com.militopia.MilitopiaGame;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.managers.VideoBackgroundManager;
import com.militopia.utils.GameLogger;
import com.militopia.utils.HoverListener;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class LoadGameScreen implements Screen {

    final MilitopiaGame game;
    Stage stage;
    private ShapeRenderer shapeRenderer;
    private boolean isFadingOut = false;
    private float fadeTime = 0f;
    private final float FADE_DURATION = 0.3f;
    private GameState selectedState = null;
    private Texture deleteIconTex;
    private TextureRegionDrawable deleteIconDrawable;

    public LoadGameScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        shapeRenderer = new ShapeRenderer();
        deleteIconTex = new Texture(Gdx.files.internal("ui/circle_ui_delete.png"));
        deleteIconDrawable = new TextureRegionDrawable(new TextureRegion(deleteIconTex));
        Gdx.input.setInputProcessor(stage);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        mainTable.add(new Label("Saved Games", game.skin)).pad(20).row();

        Table listTable = new Table();

        // 1. GET ALL FILES IN "saves/" DIRECTORY
        FileHandle dir = Gdx.files.local("saves/");
        if (!dir.exists()) {
            dir.mkdirs(); // Create folder if it doesn't exist
        }

        FileHandle[] files = dir.list(".json"); // Only get JSON files

        if (files.length == 0) {
            listTable.add(new Label("No saves found.", game.skin));
        } else {
            Json json = new Json();

            // 2. LOOP THROUGH FILES
            for (FileHandle file : files) {
                try {
                    final GameState state = json.fromJson(GameState.class, file.readString());

                    TextButton.TextButtonStyle milStyle = game.skin.get("militopia-btn", TextButton.TextButtonStyle.class);

                    Table rowTable = new Table();

                    Table entry = new Table();
                    entry.setBackground(milStyle.up);

                    String title = state.saveName != null ? state.saveName : "Unnamed";
                    Label titleLbl = new Label(title, game.skin);
                    entry.add(titleLbl).left().row();

                    String sub = "Seed: " + state.seed + "   |   " + (state.timestamp != null ? state.timestamp : "");
                    if (state.isGameOver) sub += "   |   FINISHED";
                    Label subLbl = new Label(sub, game.skin);
                    subLbl.setFontScale(0.7f);
                    subLbl.setColor(Color.LIGHT_GRAY);
                    entry.add(subLbl).left();

                    entry.addListener(new HoverListener());
                    entry.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            if (!isFadingOut) {
                                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                                isFadingOut = true;
                                fadeTime = 0f;
                                selectedState = state;
                                Gdx.input.setInputProcessor(null); // Disable input during fade
                            }
                        }
                    });

                    ImageButton delBtn = new ImageButton(deleteIconDrawable);
                    delBtn.addListener(new HoverListener());
                    delBtn.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                            file.delete();
                            game.setScreen(new LoadGameScreen(game));
                        }
                    });

                    rowTable.add(entry).fillX().expandX().padRight(10);
                    rowTable.add(delBtn).size(80);

                    listTable.add(rowTable).fillX().width(680).pad(8).row();

                } catch (Exception e) {
                    GameLogger.logScreen("Corrupt save file: " + file.name());
                }
            }
        }

        ScrollPane scroll = new ScrollPane(listTable, game.skin);
        mainTable.add(scroll).size(720, 380).row();

        TextButton backBtn = new TextButton("Back", game.skin, "militopia-btn");
        backBtn.addListener(new HoverListener());
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!isFadingOut) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                    game.setScreen(new MenuScreen(game));
                }
            }
        });
        mainTable.add(backBtn).fillX().width(300).pad(20);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        VideoBackgroundManager.getInstance().render(game.batch);
        stage.act();
        stage.draw();
        
        if (isFadingOut) {
            fadeTime += delta;
            
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            float alpha = Math.min(1.0f, Math.max(0.0f, fadeTime / FADE_DURATION));
            shapeRenderer.setColor(0, 0, 0, alpha);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            
            if (fadeTime >= FADE_DURATION && selectedState != null) {
                game.setScreen(new GameScreen(game, selectedState));
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
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
        shapeRenderer.dispose();
        if (deleteIconTex != null) deleteIconTex.dispose();
    }
}
