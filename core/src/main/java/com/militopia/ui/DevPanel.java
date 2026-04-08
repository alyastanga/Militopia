package com.militopia.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.militopia.MilitopiaGame;
import com.militopia.config.UnitType;
import com.militopia.controller.GameInputController;
import com.militopia.data.GameState;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.screen.GameScreen;
import com.militopia.utils.HoverListener;

/**
 * Dev Mode panel — full-height right sidebar that slides in/out from the
 * screen edge. Only constructed when gameState.isDevMode is true.
 */
public class DevPanel {

    private final MilitopiaGame game;
    private final Stage stage;
    private final GameScreen screen;
    private final GameInputController inputController;
    private final GameState gameState;

    /** The sidebar card — placed directly on stage, not via a Table wrapper. */
    private Table card;
    private boolean visible = false;

    private int selectedOwner = 1;
    private boolean winCondEnabled = true;
    private TextButton winCondBtn;
    private Label statusLabel;
    private TextButton p1Btn, p2Btn;

    private static final float PANEL_WIDTH  = 360;
    private static final float SLIDE_DURATION = 0.18f;

    private static final String[] STRUCTURE_TYPES = {
        "BASE", "TOWN", "MUNITION_FACTORY", "PORT", "HOSPITAL", "SOLAR",
        "RADAR", "OIL_DERRICK", "JAMMER", "NUCLEAR"
    };
    private static final String[] MAP_OBJ_TYPES = {
        "MOUNTAIN", "OIL", "RUINS", "CACTUS", "TREE", "HORSE", "FISH", "DEER", "ZEBRA"
    };

    public DevPanel(MilitopiaGame game, Stage stage, GameScreen screen,
                    GameInputController inputController, GameState gameState) {
        this.game = game;
        this.stage = stage;
        this.screen = screen;
        this.inputController = inputController;
        this.gameState = gameState;
    }

    public void build() {
        float sh = stage.getHeight();
        float sw = stage.getWidth();

        card = new Table();
        card.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        card.setBackground(makeSolidDrawable(0.07f, 0.07f, 0.07f, 0.96f));
        card.top();

        // ── Title strip ──────────────────────────────────────────────────────
        Table titleStrip = new Table();
        titleStrip.setBackground(makeSolidDrawable(0.20f, 0.08f, 0f, 1f));
        Label title = new Label("DEV MODE", game.skin);
        title.setFontScale(0.75f);
        title.setColor(Color.ORANGE);
        title.setAlignment(Align.center);
        titleStrip.add(title).growX().pad(8);
        card.add(titleStrip).growX().row();

        // ── Status bar ───────────────────────────────────────────────────────
        Table statusStrip = new Table();
        statusStrip.setBackground(makeSolidDrawable(0.04f, 0.04f, 0.04f, 1f));
        statusLabel = new Label("Idle", game.skin);
        statusLabel.setFontScale(0.52f);
        statusLabel.setColor(new Color(0.95f, 0.95f, 0.4f, 1f));
        statusLabel.setAlignment(Align.center);
        statusStrip.add(statusLabel).growX().padTop(4).padBottom(4);
        card.add(statusStrip).growX().row();

        // ── Scrollable content fills remaining height ─────────────────────
        Table content = new Table();
        content.top().padLeft(14).padRight(14).padBottom(12);

        buildPlayerSelector(content);
        addSectionHeader(content, "SPAWN",   new Color(0.2f, 0.8f, 0.9f, 1f));
        buildSpawnSection(content);
        buildBuildSection(content);
        addSectionHeader(content, "ACTIONS", new Color(0.9f, 0.75f, 0.2f, 1f));
        buildActionsSection(content);
        addSectionHeader(content, "ECONOMY", new Color(0.4f, 0.9f, 0.4f, 1f));
        buildEconomySection(content);
        addSectionHeader(content, "TOGGLES", new Color(0.8f, 0.45f, 0.9f, 1f));
        buildTogglesSection(content);

        final ScrollPane scroll = new ScrollPane(content, game.skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        card.add(scroll).growX().growY().row();

        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                stage.setScrollFocus(scroll);
            }
        });

        // Consume touch events on the card background so clicks on empty panel
        // space don't fall through to the map.
        card.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                stage.setScrollFocus(scroll);
                return true;
            }
        });
        card.addListener(new com.badlogic.gdx.scenes.scene2d.EventListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event e) {
                if (e instanceof InputEvent && ((InputEvent) e).getType() == InputEvent.Type.scrolled) {
                    return true;
                }
                return false;
            }
        });

        // Place the card: full height, off-screen to the right (hidden state)
        card.setBounds(sw, 0, PANEL_WIDTH, sh);
        card.setVisible(false);
        stage.addActor(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resize support
    // ─────────────────────────────────────────────────────────────────────────

    public void resize() {
        if (card == null) return;
        float sh = stage.getHeight();
        float sw = stage.getWidth();
        float targetX = visible ? sw - PANEL_WIDTH : sw;
        card.setBounds(targetX, 0, PANEL_WIDTH, sh);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section builders
    // ─────────────────────────────────────────────────────────────────────────

    private void buildPlayerSelector(Table content) {
        Label lbl = new Label("OWNER", game.skin);
        lbl.setFontScale(0.52f);
        lbl.setColor(Color.LIGHT_GRAY);
        content.add(lbl).left().padTop(10).padBottom(4).row();

        Table row = new Table();
        p1Btn = new TextButton("  P1  ", game.skin, "militopia-btn");
        p2Btn = new TextButton("  P2  ", game.skin, "militopia-btn");
        p1Btn.getLabel().setFontScale(0.62f);
        p2Btn.getLabel().setFontScale(0.62f);
        p1Btn.setColor(new Color(0.3f, 0.8f, 1f, 1f));

        p1Btn.addListener(new HoverListener());
        p2Btn.addListener(new HoverListener());
        p1Btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_TOGGLE);
                selectedOwner = 1;
                inputController.devSpawnOwner = 1;
                p1Btn.setColor(new Color(0.3f, 0.8f, 1f, 1f));
                p2Btn.setColor(Color.WHITE);
            }
        });
        p2Btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_TOGGLE);
                selectedOwner = 2;
                inputController.devSpawnOwner = 2;
                p1Btn.setColor(Color.WHITE);
                p2Btn.setColor(new Color(1f, 0.4f, 0.4f, 1f));
            }
        });

        row.add(p1Btn).growX().padRight(4);
        row.add(p2Btn).growX();
        content.add(row).growX().padBottom(6).row();
    }

    private void buildSpawnSection(Table content) {
        addActionBtn(content, "SPAWN UNIT", new Color(0.7f, 1f, 1f, 1f), new Runnable() {
            @Override public void run() { showUnitPicker(); }
        });
    }

    private void buildBuildSection(Table content) {
        addActionBtn(content, "BUILD STRUCTURE", new Color(0.7f, 1f, 0.7f, 1f), new Runnable() {
            @Override public void run() { showStructurePicker(); }
        });
        addActionBtn(content, "BUILD MAP OBJ", new Color(0.8f, 0.9f, 0.6f, 1f), new Runnable() {
            @Override public void run() { showMapObjPicker(); }
        });
    }

    private void buildActionsSection(Table content) {
        addActionBtn(content, "HEAL ALL",                Color.WHITE,                    new Runnable() { @Override public void run() { screen.devHealAll(selectedOwner);      setStatus("Healed all — P" + selectedOwner); }});
        addActionBtn(content, "RESET ACTIONS",          Color.WHITE,                    new Runnable() { @Override public void run() { screen.devResetActions(selectedOwner); setStatus("Actions reset — P" + selectedOwner); }});
        addActionBtn(content, "KILL UNIT  [CLICK MAP]", new Color(1f,0.5f,0.5f,1f),    new Runnable() { @Override public void run() { inputController.enterDevKill();         setStatus("Click unit to kill"); }});
        addActionBtn(content, "REMOVE OBJ [CLICK MAP]", new Color(1f,0.4f,0.4f,1f),    new Runnable() { @Override public void run() { inputController.enterDevRemove();       setStatus("Click object to remove"); }});
        addActionBtn(content, "SET BASE LEVEL  [CLICK]",new Color(1f,0.85f,0.4f,1f),   new Runnable() { @Override public void run() { inputController.enterDevSetBaseLevel(); setStatus("Click base to set level"); }});
        addActionBtn(content, "✕  CANCEL MODE",         new Color(0.65f,0.65f,0.65f,1f),new Runnable() { @Override public void run() { inputController.exitDevMode();         setStatus("Idle"); }});
    }

    private void buildEconomySection(Table content) {
        Table fundRow = new Table();
        Label fl = new Label("Funds:", game.skin); fl.setFontScale(0.52f); fl.setColor(Color.LIGHT_GRAY);
        final TextButton f100  = makeSmallBtn("+100");
        final TextButton f1000 = makeSmallBtn("+1000");
        f100.setColor(new Color(0.9f,1f,0.6f,1f));
        f1000.setColor(new Color(0.9f,1f,0.6f,1f));
        fundRow.add(fl).padRight(6);
        fundRow.add(f100).growX().padRight(3);
        fundRow.add(f1000).growX();
        content.add(fundRow).growX().padBottom(4).row();

        Table xpRow = new Table();
        Label xl = new Label("XP:", game.skin); xl.setFontScale(0.52f); xl.setColor(Color.LIGHT_GRAY);
        final TextButton x500  = makeSmallBtn("+500");
        final TextButton x2000 = makeSmallBtn("+2000");
        x500.setColor(new Color(0.7f,0.9f,1f,1f));
        x2000.setColor(new Color(0.7f,0.9f,1f,1f));
        xpRow.add(xl).padRight(6);
        xpRow.add(x500).growX().padRight(3);
        xpRow.add(x2000).growX();
        content.add(xpRow).growX().padBottom(6).row();

        f100.addListener(new ClickListener()  { @Override public void clicked(InputEvent e,float x,float y){ AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM); screen.devAddFundsToPlayer(selectedOwner,100);  setStatus("+100 funds — P"+selectedOwner); }});
        f1000.addListener(new ClickListener() { @Override public void clicked(InputEvent e,float x,float y){ AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM); screen.devAddFundsToPlayer(selectedOwner,1000); setStatus("+1000 funds — P"+selectedOwner); }});
        x500.addListener(new ClickListener()  { @Override public void clicked(InputEvent e,float x,float y){ AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM); screen.devAddXP(500,selectedOwner);             setStatus("+500 XP — P"+selectedOwner); }});
        x2000.addListener(new ClickListener() { @Override public void clicked(InputEvent e,float x,float y){ AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM); screen.devAddXP(2000,selectedOwner);            setStatus("+2000 XP — P"+selectedOwner); }});
    }

    private void buildTogglesSection(Table content) {
        winCondBtn = new TextButton("Win Condition: ON", game.skin, "militopia-btn");
        winCondBtn.getLabel().setFontScale(0.57f);
        winCondBtn.setColor(new Color(0.3f, 0.9f, 0.3f, 1f));
        winCondBtn.addListener(new HoverListener());
        winCondBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_TOGGLE);
                winCondEnabled = !winCondEnabled;
                screen.devToggleWinCondition(winCondEnabled);
                winCondBtn.setText("Win Condition: " + (winCondEnabled ? "ON" : "OFF"));
                winCondBtn.setColor(winCondEnabled ? new Color(0.3f,0.9f,0.3f,1f) : new Color(0.9f,0.3f,0.3f,1f));
                setStatus("WinCond " + (winCondEnabled ? "enabled" : "disabled"));
            }
        });
        content.add(winCondBtn).growX().padBottom(16).row();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Spawn / Build picker popups
    // ─────────────────────────────────────────────────────────────────────────

    private void showUnitPicker() {
        final Table modal = new Table();
        modal.setFillParent(true);
        modal.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        modal.setBackground(makeSolidDrawable(0f, 0f, 0f, 0.72f));
        modal.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int p, int b) { return true; }
        });
        modal.addListener(new com.badlogic.gdx.scenes.scene2d.EventListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event e) {
                if (e instanceof InputEvent && ((InputEvent) e).getType() == InputEvent.Type.scrolled) {
                    return true;
                }
                return false;
            }
        });

        Table box = new Table();
        box.setBackground(makeSolidDrawable(0.06f, 0.10f, 0.12f, 1f));

        Table strip = new Table();
        strip.setBackground(makeSolidDrawable(0.05f, 0.20f, 0.25f, 1f));
        Label title = new Label("SPAWN UNIT", game.skin);
        title.setFontScale(0.72f);
        title.setColor(new Color(0.2f, 0.85f, 0.95f, 1f));
        title.setAlignment(Align.center);
        strip.add(title).growX().pad(8);
        box.add(strip).growX().row();

        Table list = new Table();
        list.top().padLeft(10).padRight(10).padTop(6).padBottom(6);
        for (final UnitType type : UnitType.values()) {
            final TextButton btn = new TextButton(toNice(type.name()), game.skin, "militopia-btn");
            btn.getLabel().setFontScale(0.6f);
            btn.setColor(new Color(0.7f, 1f, 1f, 1f));
            btn.addListener(new HoverListener());
            btn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                    inputController.enterDevSpawn(GameInputController.DevSpawnMode.SPAWNING_UNIT, type, selectedOwner);
                    setStatus("Placing: " + toNice(type.name()));
                    modal.remove();
                }
            });
            list.add(btn).growX().padBottom(3).row();
        }

        final ScrollPane scroll = new ScrollPane(list, game.skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        box.add(scroll).growX().maxHeight(380).row();

        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                stage.setScrollFocus(scroll);
            }
        });

        TextButton cancelBtn = new TextButton("CANCEL", game.skin, "militopia-btn");
        cancelBtn.getLabel().setFontScale(0.6f);
        cancelBtn.setColor(new Color(0.65f, 0.65f, 0.65f, 1f));
        cancelBtn.addListener(new HoverListener());
        cancelBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                modal.remove();
            }
        });
        box.add(cancelBtn).width(220).pad(6).padBottom(10).row();

        modal.add(box).width(280);
        stage.addActor(modal);
        modal.toFront();
        stage.setScrollFocus(scroll);
    }

    private void showStructurePicker() {
        final Table modal = new Table();
        modal.setFillParent(true);
        modal.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        modal.setBackground(makeSolidDrawable(0f, 0f, 0f, 0.72f));
        modal.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int p, int b) { return true; }
        });
        modal.addListener(new com.badlogic.gdx.scenes.scene2d.EventListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event e) {
                if (e instanceof InputEvent && ((InputEvent) e).getType() == InputEvent.Type.scrolled) {
                    return true;
                }
                return false;
            }
        });

        Table box = new Table();
        box.setBackground(makeSolidDrawable(0.06f, 0.10f, 0.06f, 1f));

        Table strip = new Table();
        strip.setBackground(makeSolidDrawable(0.05f, 0.18f, 0.05f, 1f));
        Label title = new Label("BUILD STRUCTURE", game.skin);
        title.setFontScale(0.72f);
        title.setColor(new Color(0.7f, 1f, 0.7f, 1f));
        title.setAlignment(Align.center);
        strip.add(title).growX().pad(8);
        box.add(strip).growX().row();

        Table list = new Table();
        list.top().padLeft(10).padRight(10).padTop(6).padBottom(6);
        for (final String type : STRUCTURE_TYPES) {
            final TextButton btn = new TextButton(toNice(type), game.skin, "militopia-btn");
            btn.getLabel().setFontScale(0.6f);
            btn.setColor(new Color(0.7f, 1f, 0.7f, 1f));
            btn.addListener(new HoverListener());
            btn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                    inputController.enterDevBuild(type, selectedOwner);
                    setStatus("Placing: " + toNice(type));
                    modal.remove();
                }
            });
            list.add(btn).growX().padBottom(3).row();
        }

        final ScrollPane scroll = new ScrollPane(list, game.skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        box.add(scroll).growX().maxHeight(300).row();

        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                stage.setScrollFocus(scroll);
            }
        });

        TextButton cancelBtn = new TextButton("CANCEL", game.skin, "militopia-btn");
        cancelBtn.getLabel().setFontScale(0.6f);
        cancelBtn.setColor(new Color(0.65f, 0.65f, 0.65f, 1f));
        cancelBtn.addListener(new HoverListener());
        cancelBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                modal.remove();
            }
        });
        box.add(cancelBtn).width(220).pad(6).padBottom(10).row();

        modal.add(box).width(280);
        stage.addActor(modal);
        modal.toFront();
        stage.setScrollFocus(scroll);
    }

    private void showMapObjPicker() {
        final Table modal = new Table();
        modal.setFillParent(true);
        modal.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        modal.setBackground(makeSolidDrawable(0f, 0f, 0f, 0.72f));
        modal.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int p, int b) { return true; }
        });
        modal.addListener(new com.badlogic.gdx.scenes.scene2d.EventListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event e) {
                if (e instanceof InputEvent && ((InputEvent) e).getType() == InputEvent.Type.scrolled) {
                    return true;
                }
                return false;
            }
        });

        Table box = new Table();
        box.setBackground(makeSolidDrawable(0.10f, 0.10f, 0.05f, 1f));

        Table strip = new Table();
        strip.setBackground(makeSolidDrawable(0.18f, 0.18f, 0.03f, 1f));
        Label title = new Label("BUILD MAP OBJ", game.skin);
        title.setFontScale(0.72f);
        title.setColor(new Color(0.9f, 0.9f, 0.5f, 1f));
        title.setAlignment(Align.center);
        strip.add(title).growX().pad(8);
        box.add(strip).growX().row();

        Table list = new Table();
        list.top().padLeft(10).padRight(10).padTop(6).padBottom(6);
        for (final String type : MAP_OBJ_TYPES) {
            final TextButton btn = new TextButton(toNice(type), game.skin, "militopia-btn");
            btn.getLabel().setFontScale(0.6f);
            btn.setColor(new Color(0.9f, 0.9f, 0.6f, 1f));
            btn.addListener(new HoverListener());
            btn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                    inputController.enterDevBuildMapObj(type, selectedOwner);
                    setStatus("Placing: " + toNice(type));
                    modal.remove();
                }
            });
            list.add(btn).growX().padBottom(3).row();
        }

        final ScrollPane scroll = new ScrollPane(list, game.skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        box.add(scroll).growX().maxHeight(300).row();

        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                stage.setScrollFocus(scroll);
            }
        });

        TextButton cancelBtn = new TextButton("CANCEL", game.skin, "militopia-btn");
        cancelBtn.getLabel().setFontScale(0.6f);
        cancelBtn.setColor(new Color(0.65f, 0.65f, 0.65f, 1f));
        cancelBtn.addListener(new HoverListener());
        cancelBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                modal.remove();
            }
        });
        box.add(cancelBtn).width(220).pad(6).padBottom(10).row();

        modal.add(box).width(280);
        stage.addActor(modal);
        modal.toFront();
        stage.setScrollFocus(scroll);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Base level picker modal
    // ─────────────────────────────────────────────────────────────────────────

    public void showBaseLevelPicker(final Entity base) {
        final Table modal = new Table();
        modal.setFillParent(true);
        modal.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        modal.setBackground(makeSolidDrawable(0f, 0f, 0f, 0.65f));
        modal.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int p, int b) { return true; }
        });
        modal.addListener(new com.badlogic.gdx.scenes.scene2d.EventListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event e) {
                if (e instanceof InputEvent && ((InputEvent) e).getType() == InputEvent.Type.scrolled) {
                    return true;
                }
                return false;
            }
        });

        Table box = new Table();
        box.setBackground(makeSolidDrawable(0.12f, 0.08f, 0.02f, 1f));

        Table strip = new Table();
        strip.setBackground(makeSolidDrawable(0.25f, 0.1f, 0f, 1f));
        Label title = new Label("Set Base Level", game.skin);
        title.setFontScale(0.75f);
        title.setColor(Color.ORANGE);
        title.setAlignment(Align.center);
        strip.add(title).growX().pad(8);
        box.add(strip).growX().row();

        for (int lvl = 1; lvl <= 6; lvl++) {
            final int targetLevel = lvl;
            TextButton lvlBtn = new TextButton("Level " + lvl, game.skin, "militopia-btn");
            lvlBtn.getLabel().setFontScale(0.65f);
            lvlBtn.addListener(new HoverListener());
            lvlBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                    screen.devSetBaseLevel(base, targetLevel);
                    inputController.exitDevMode();
                    setStatus("Base → Level " + targetLevel);
                    modal.remove();
                }
            });
            box.add(lvlBtn).width(200).pad(4).row();
        }

        TextButton cancelBtn = new TextButton("Cancel", game.skin, "militopia-btn");
        cancelBtn.getLabel().setFontScale(0.65f);
        cancelBtn.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
        cancelBtn.addListener(new HoverListener());
        cancelBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CANCEL);
                inputController.exitDevMode();
                setStatus("Idle");
                modal.remove();
            }
        });
        box.add(cancelBtn).width(200).pad(4).padBottom(12).row();

        modal.add(box);
        stage.addActor(modal);
        modal.toFront();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Toggle with slide animation
    // ─────────────────────────────────────────────────────────────────────────

    public void toggle() {
        visible = !visible;
        float sw = stage.getWidth();
        float sh = stage.getHeight();

        card.clearActions();
        if (visible) {
            card.setSize(PANEL_WIDTH, sh);
            card.setVisible(true);
            card.addAction(Actions.moveTo(sw - PANEL_WIDTH, 0, SLIDE_DURATION, Interpolation.fastSlow));
        } else {
            card.addAction(Actions.sequence(
                Actions.moveTo(sw, 0, SLIDE_DURATION, Interpolation.fastSlow),
                Actions.run(new Runnable() {
                    @Override public void run() { card.setVisible(false); }
                })
            ));
        }
    }

    /** Close the panel if it's currently open (used by map touch handler). */
    public void retract() {
        if (!visible) return;
        visible = false;
        float sw = stage.getWidth();
        card.clearActions();
        card.addAction(Actions.sequence(
            Actions.moveTo(sw, 0, SLIDE_DURATION, Interpolation.fastSlow),
            Actions.run(new Runnable() {
                @Override public void run() { card.setVisible(false); }
            })
        ));
    }

    public boolean isVisible() {
        return visible;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Widget helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void addSectionHeader(Table content, String text, Color color) {
        Table strip = new Table();
        strip.setBackground(makeSolidDrawable(color.r * 0.15f, color.g * 0.15f, color.b * 0.15f, 1f));
        Label lbl = new Label(text, game.skin);
        lbl.setFontScale(0.52f);
        lbl.setColor(color);
        lbl.setAlignment(Align.center);
        strip.add(lbl).growX().pad(3);
        content.add(strip).growX().padTop(8).padBottom(4).row();
    }

    private void addActionBtn(Table content, String label, Color tint, Runnable action) {
        TextButton btn = new TextButton(label, game.skin, "militopia-btn");
        btn.getLabel().setFontScale(0.57f);
        btn.setColor(tint);
        btn.addListener(new HoverListener());
        final Runnable r = action;
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                r.run();
            }
        });
        content.add(btn).growX().padBottom(4).row();
    }

    private TextButton makeSmallBtn(String label) {
        TextButton btn = new TextButton(label, game.skin, "militopia-btn");
        btn.getLabel().setFontScale(0.55f);
        btn.addListener(new HoverListener());
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Texture / drawable helpers
    // ─────────────────────────────────────────────────────────────────────────

    private TextureRegionDrawable makeSolidDrawable(float r, float g, float b, float a) {
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(r, g, b, a);
        px.fill();
        Texture t = new Texture(px);
        px.dispose();
        return new TextureRegionDrawable(new TextureRegion(t));
    }

    private void setStatus(String text) {
        if (statusLabel != null) statusLabel.setText(text);
    }

    private String toNice(String key) {
        if (key == null || key.isEmpty()) return key;
        return key.replace('_', ' ').toUpperCase();
    }
}
