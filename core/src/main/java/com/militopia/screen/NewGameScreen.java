package com.militopia.screen;

import com.militopia.screen.MenuScreen;
import com.militopia.screen.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.data.GameState;
import com.militopia.MilitopiaGame;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.managers.VideoBackgroundManager;
import com.militopia.utils.GameLogger;
import com.militopia.utils.HoverListener;

public class NewGameScreen implements Screen {

    final MilitopiaGame game;
    Stage stage;

    TextField nameField, seedField, p1NameField, p2NameField;
    Label errorLabel, titleLabel;
    Label modeInfoLabel;

    // Mode Selection
    SelectBox<String> modeSelectBox;
    int selectedWidth = 16;
    int selectedHeight = 16;
    boolean devModeEnabled = false;

    public NewGameScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        nameField = new TextField("", game.skin);
        nameField.setMessageText("Enter Game Name");

        seedField = new TextField("", game.skin);
        seedField.setMessageText("Enter Seed");

        p1NameField = new TextField("", game.skin);
        p1NameField.setMessageText("Player 1 Name");

        p2NameField = new TextField("", game.skin);
        p2NameField.setMessageText("Player 2 Name");

        Label.LabelStyle errorStyle = new Label.LabelStyle(game.skin.getFont("default-font"), Color.RED);
        errorLabel = new Label("", errorStyle);

        titleLabel = new Label("NEW GAME", game.skin, "default-font", Color.WHITE);
        titleLabel.setFontScale(1.8f);

        // --- MODE SELECTOR ---
        modeSelectBox = new SelectBox<>(game.skin);
        modeSelectBox.setItems("Select Game Mode...", "Blitz", "Marathon");
        modeSelectBox.setSelected("Select Game Mode...");

        modeInfoLabel = new Label("16x16 map", game.skin);
        modeInfoLabel.setColor(Color.LIGHT_GRAY);
        modeInfoLabel.setFontScale(0.8f);
        modeInfoLabel.setVisible(false);

        modeSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selected = modeSelectBox.getSelected();
                if (selected.contains("Blitz")) {
                    modeInfoLabel.setText("16x16 map");
                    selectedWidth = 16;
                    selectedHeight = 16;
                } else if (selected.contains("Marathon")) {
                    modeInfoLabel.setText("32x32 map");
                    selectedWidth = 32;
                    selectedHeight = 32;
                } else {
                    modeInfoLabel.setText("16x16 map");
                    selectedWidth = 16;
                    selectedHeight = 16;
                }
            }
        });

        modeSelectBox.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1)
                    modeInfoLabel.setVisible(true);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1)
                    modeInfoLabel.setVisible(false);
            }
        });

        TextButton startBtn = new TextButton("Start Game", game.skin, "militopia-btn");
        startBtn.addListener(new HoverListener());
        TextButton backBtn = new TextButton("Back", game.skin, "militopia-btn");
        backBtn.addListener(new HoverListener());
        table.clear(); // Clear to rebuild neatly
        table.setFillParent(true);
        table.add(titleLabel).colspan(2).pad(15).row();
        table.add(nameField).width(300).pad(10).row();
        table.add(seedField).width(300).pad(10).row();
        table.add(p1NameField).width(300).pad(10).row();
        table.add(p2NameField).width(300).pad(10).row();

        // Add Mode Selection Row using balancing spacer trick
        Table modeTable = new Table();
        modeTable.add().width(110);
        modeTable.add(modeSelectBox).width(300).pad(5);
        modeTable.add(modeInfoLabel).width(100).padLeft(10);

        table.row().pad(10);
        table.add(modeTable).left().row();

        final TextButton devModeBtn = new TextButton("DEV MODE: OFF", game.skin, "militopia-btn");
        devModeBtn.addListener(new HoverListener());
        devModeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                devModeEnabled = !devModeEnabled;
                devModeBtn.setText("DEV MODE: " + (devModeEnabled ? "ON" : "OFF"));
                devModeBtn.setColor(devModeEnabled ? Color.ORANGE : Color.WHITE);
                AudioManager.getInstance().playSFX(SFXKeys.UI_TOGGLE);
            }
        });

        Table btnRow = new Table();
        btnRow.add(devModeBtn).width(300).pad(10).colspan(2).row();
        btnRow.add(backBtn).fillX().width(200).pad(10);
        btnRow.add(startBtn).fillX().width(200).pad(10);
        table.add(btnRow).colspan(2).padTop(120).row();

        table.row();
        table.add(errorLabel).colspan(2).pad(10).row();

        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isEmpty(nameField) || isEmpty(seedField)) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    errorLabel.setText("All fields are required!");
                } else {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                    long seed;
                    try {
                        seed = Long.parseLong(seedField.getText());
                    } catch (Exception e) {
                        seed = seedField.getText().hashCode();
                    }

                    String mode = (selectedWidth == 16) ? "Blitz(16x16)" : "Marathon(32x32)";
                    GameLogger.logScreen("Game started | name=" + nameField.getText()
                            + " seed=" + seed + " mode=" + mode);
                    // Create GameState with selected Dimensions
                    GameState newState = new GameState(seed, nameField.getText() + '_' + seed, selectedWidth,
                            selectedHeight);
                    String p1 = p1NameField.getText().trim();
                    String p2 = p2NameField.getText().trim();
                    newState.p1Name = p1.isEmpty() ? "Player 1" : p1;
                    newState.p2Name = p2.isEmpty() ? "Player 2" : p2;
                    newState.isDevMode = devModeEnabled;
                    if (newState.mapWidth == 16) {
                        newState.p1TimeLeft = 600f;
                        newState.p2TimeLeft = 600f;
                    }
                    if (devModeEnabled)
                        GameLogger.logScreen("[DEV MODE] Starting dev session");
                    game.setScreen(new GameScreen(game, newState));
                }
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                GameLogger.logScreen("Navigating → Main Menu");
                game.setScreen(new MenuScreen(game));
            }
        });
    }

    private boolean isEmpty(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
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
        GameLogger.logScreen("New Game Screen opened");
        VideoBackgroundManager.getInstance().play();
    }

    @Override
    public void hide() {
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
