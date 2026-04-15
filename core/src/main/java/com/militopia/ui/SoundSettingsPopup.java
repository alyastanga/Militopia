package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.militopia.MilitopiaGame;
import com.militopia.controller.GameInputController;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.utils.HoverListener;

public class SoundSettingsPopup {

    private final MilitopiaGame game;
    private final Stage stage;
    private final GameInputController inputController; // nullable
    private final HudBottomBar bottomBar;              // nullable

    private Table popupTable;

    public SoundSettingsPopup(MilitopiaGame game, Stage stage,
                               GameInputController inputController,
                               HudBottomBar bottomBar) {
        this.game = game;
        this.stage = stage;
        this.inputController = inputController;
        this.bottomBar = bottomBar;
    }

    public void show() {
        buildPopup();
        popupTable.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        stage.addActor(popupTable);
        popupTable.toFront();

        if (inputController != null) inputController.setInputEnabled(false);
        if (bottomBar != null) bottomBar.setBlocked(true);
    }

    private void buildPopup() {
        final AudioManager audio = AudioManager.getInstance();

        popupTable = new Table();
        popupTable.setFillParent(true);
        popupTable.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        popupTable.setBackground(game.skin.newDrawable("white", new Color(0, 0, 0, 0.85f)));
        popupTable.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int p, int b) { return true; }
            @Override public boolean mouseMoved(InputEvent e, float x, float y) { return true; }
            @Override public boolean keyDown(InputEvent e, int keycode) { return true; }
        });

        Table modal = new Table();
        modal.setBackground(game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 1f)));
        modal.padLeft(40).padRight(40).padTop(25).padBottom(25);

        // Title
        Label title = new Label("SOUND SETTINGS", game.skin, "default-font", Color.WHITE);
        title.setFontScale(1.3f);
        modal.add(title).colspan(3).padBottom(20).row();

        // Master Volume
        addSliderRow(modal, audio,
                "MASTER VOLUME", "Drag to adjust overall volume.",
                audio.getMasterVolume(), audio.isMasterMuted(),
                new SliderCallback() {
                    @Override public void onVolumeChanged(float v) { audio.setMasterVolume(v); }
                    @Override public boolean isMuted() { return audio.isMasterMuted(); }
                    @Override public void setMuted(boolean m) { audio.setMasterMuted(m); }
                });

        modal.add().height(10).colspan(3).row(); // spacer

        // BGM Volume
        addSliderRow(modal, audio,
                "MUSIC", "Drag to adjust background music.",
                audio.getBGMVolume(), audio.isBGMMuted(),
                new SliderCallback() {
                    @Override public void onVolumeChanged(float v) { audio.setBGMVolume(v); }
                    @Override public boolean isMuted() { return audio.isBGMMuted(); }
                    @Override public void setMuted(boolean m) { audio.setBGMMuted(m); }
                });

        modal.add().height(10).colspan(3).row(); // spacer

        // SFX Volume
        addSliderRow(modal, audio,
                "SOUND EFFECTS", "Drag to adjust in-game sound effects.",
                audio.getMasterVolume(), audio.isSFXMuted(),
                new SliderCallback() {
                    @Override public void onVolumeChanged(float v) { audio.setMasterVolume(v); }
                    @Override public boolean isMuted() { return audio.isSFXMuted(); }
                    @Override public void setMuted(boolean m) { audio.setSFXMuted(m); }
                });

        modal.add().height(15).colspan(3).row(); // spacer before back

        // Back button
        TextButton backBtn = new TextButton("BACK", game.skin, "militopia-btn");
        backBtn.addListener(new HoverListener());
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                dismiss();
            }
        });
        modal.add(backBtn).colspan(3).fillX().width(300).padTop(10);

        popupTable.add(modal);
    }

    private void addSliderRow(Table modal, final AudioManager audio,
                               String labelText, String hint,
                               float initialValue, boolean initialMuted,
                               final SliderCallback callback) {
        // Row 1: section label
        Label sectionLabel = new Label(labelText, game.skin, "default-font", Color.WHITE);
        sectionLabel.setFontScale(0.85f);
        modal.add(sectionLabel).left().colspan(3).padBottom(2).row();

        // Row 2: hint label
        Label hintLabel = new Label(hint, game.skin, "standard", Color.LIGHT_GRAY);
        hintLabel.setFontScale(0.65f);
        modal.add(hintLabel).left().colspan(3).padBottom(6).row();

        // Row 3: slider + mute button
        final Slider slider = new Slider(0f, 1f, 0.05f, false, game.skin);
        slider.setValue(initialValue);

        final TextButton muteBtn = new TextButton(initialMuted ? "UNMUTE" : "MUTE", game.skin, "militopia-btn");
        muteBtn.getLabel().setFontScale(0.8f);

        if (initialMuted) {
            slider.setDisabled(true);
        }

        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!slider.isDragging()) return;
                callback.onVolumeChanged(slider.getValue());
            }
        });

        muteBtn.addListener(new HoverListener());
        muteBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_TOGGLE);
                boolean nowMuted = !callback.isMuted();
                callback.setMuted(nowMuted);
                muteBtn.setText(nowMuted ? "UNMUTE" : "MUTE");
                slider.setDisabled(nowMuted);
            }
        });

        modal.add(slider).width(260).padRight(16);
        modal.add(muteBtn).width(120).row();
    }

    private void dismiss() {
        if (popupTable != null) popupTable.remove();
        if (inputController != null) inputController.setInputEnabled(true);
        if (bottomBar != null) bottomBar.setBlocked(false);
    }

    private interface SliderCallback {
        void onVolumeChanged(float v);
        boolean isMuted();
        void setMuted(boolean m);
    }
}
