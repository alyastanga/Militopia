package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.militopia.MilitopiaGame;
import com.militopia.config.GameConfig;
import com.militopia.managers.AssetManager;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Align;

/**
 * Top gradient strip showing XP, Funding, and Turn counter.
 * Owns all three stat labels and the gradient background.
 */
public class HudTopBar {

    private final MilitopiaGame game;
    private final AssetManager assets;

    private Label xpLabel;
    private Label fundsLabel;
    private Label fundingTitleLabel;
    private Label turnLabel;

    private Table timerGroup;
    private Label timerLabel;
    private Label timerTitleLabel;

    private final Table topContainer;

    public HudTopBar(MilitopiaGame game, AssetManager assets) {
        this.game = game;
        this.assets = assets;

        TextureRegionDrawable topBg = createGradientDrawable(80, true);

        Table topContent = new Table();
        topContent.add(createStatGroup("XP", "0")).expandX().padRight(40);
        topContent.add(createStatGroup("Funding", "1000")).expandX().padLeft(40).padRight(40);
        topContent.add(createStatGroup("Turn", "1")).expandX().padLeft(40);

        timerGroup = createTimerGroup();
        topContent.add(timerGroup).expandX().padLeft(40);
        timerGroup.setVisible(false);

        topContainer = new Table();
        topContainer.setBackground(topBg);
        topContainer.add(topContent).width(GameConfig.UI_WIDTH).padTop(10).padBottom(20);
    }

    /** Returns the top container Table for adding to rootTable. */
    public Table getActor() {
        return topContainer;
    }

    // -------------------------------------------------------------------------
    // Update API
    // -------------------------------------------------------------------------

    public void updateXP(int xp) {
        if (xpLabel != null) {
            String currentText = (xpLabel.getText() != null) ? xpLabel.getText().toString() : "";
            if (!String.valueOf(xp).equals(currentText)) {
                xpLabel.setText(String.valueOf(xp));
                xpLabel.setOrigin(Align.center);
                xpLabel.clearActions();
                xpLabel.addAction(Actions.sequence(
                        Actions.scaleTo(1.5f, 1.5f, 0.1f),
                        Actions.color(Color.YELLOW, 0.1f),
                        Actions.scaleTo(1.0f, 1.0f, 0.3f),
                        Actions.color(Color.WHITE, 0.2f)));
            } else {
                xpLabel.setText(String.valueOf(xp));
            }
        }
    }

    public void updateTurn(int turn) {
        if (turnLabel != null)
            turnLabel.setText(String.valueOf(turn));
    }

    public void updateFunding(int funding, int income) {
        if (fundsLabel != null) {
            String currentText = (fundsLabel.getText() != null) ? fundsLabel.getText().toString() : "";
            if (!String.valueOf(funding).equals(currentText)) {
                fundsLabel.setText(String.valueOf(funding));
                fundsLabel.setOrigin(Align.center);
                fundsLabel.clearActions();
                fundsLabel.addAction(Actions.sequence(
                        Actions.scaleTo(1.3f, 1.3f, 0.1f),
                        Actions.color(Color.GREEN, 0.1f),
                        Actions.scaleTo(1.0f, 1.0f, 0.3f),
                        Actions.color(Color.WHITE, 0.2f)));
            } else {
                fundsLabel.setText(String.valueOf(funding));
            }
        }
        if (fundingTitleLabel != null) {
            String prefix = (income >= 0) ? "+" : "-";
            String newIncomeText = "Funding (" + prefix + Math.abs(income) + ")";
            if (!fundingTitleLabel.getText().toString().equals(newIncomeText)) {
                fundingTitleLabel.setText(newIncomeText);
                fundingTitleLabel.clearActions();
                fundingTitleLabel.addAction(Actions.sequence(
                        Actions.color(Color.GOLD, 0.1f),
                        Actions.color(Color.WHITE, 0.4f)));
            }
        }
    }

    public void updateTimer(float timeLeft, boolean paused) {
        if (timerGroup == null) return;
        timerGroup.setVisible(true);
        if (paused) {
            timerLabel.setText("PAUSED");
            timerLabel.setColor(Color.LIGHT_GRAY);
            return;
        }
        int total = Math.max(0, (int) timeLeft);
        timerLabel.setText(total / 60 + ":" + String.format("%02d", total % 60));
        if (timeLeft <= 120f)      timerLabel.setColor(Color.RED);
        else if (timeLeft <= 300f) timerLabel.setColor(Color.YELLOW);
        else                       timerLabel.setColor(Color.WHITE);
    }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    private Table createTimerGroup() {
        Table t = new Table();
        timerTitleLabel = new Label("Timer", game.skin, "default-font",
                game.skin.get("color-gold", Color.class));
        timerTitleLabel.setFontScale(0.75f);
        timerLabel = new Label("10:00", game.skin, "default-font", Color.WHITE);
        timerLabel.setFontScale(1.15f);
        t.add(timerTitleLabel).row();
        t.add(timerLabel);
        return t;
    }

    private Table createStatGroup(String title, String placeholderValue) {
        Table t = new Table();

        if (title.equals("Funding")) {
            fundingTitleLabel = new Label(title + " (+0)", game.skin, "default-font", 
                    game.skin.get("color-gold", Color.class));
            fundingTitleLabel.setFontScale(0.75f);
            t.add(fundingTitleLabel).left().row();

            Table valueRow = new Table();
            try {
                Texture iconTex = assets.get(AssetManager.FUNDING_ICON);
                iconTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                com.badlogic.gdx.scenes.scene2d.ui.Image iconImg = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                        new TextureRegion(iconTex));
                iconImg.setScaling(Scaling.fit);
                valueRow.add(iconImg).size(40, 40).padRight(0);
            } catch (Exception ignored) {
            }

            fundsLabel = new Label(placeholderValue, game.skin, "default-font", Color.WHITE);
            fundsLabel.setFontScale(1.2f);
            valueRow.add(fundsLabel);
            t.add(valueRow).left();

        } else {
            Label titleLbl = new Label(title, game.skin, "default-font", 
                    game.skin.get("color-gold", Color.class));
            titleLbl.setFontScale(0.75f);
            Label valLbl = new Label(placeholderValue, game.skin, "default-font", Color.WHITE);
            valLbl.setFontScale(1.15f);

            if (title.equals("XP"))
                xpLabel = valLbl;
            if (title.equals("Turn"))
                turnLabel = valLbl;

            t.add(titleLbl).row();
            t.add(valLbl);
        }
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
