package com.militopia;

import com.militopia.managers.AssetManager;
import com.militopia.managers.AudioManager;
import com.militopia.managers.VideoBackgroundManager;
import com.militopia.screen.SplashScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class MilitopiaGame extends Game {

    public SpriteBatch batch;
    public AssetManager assets;
    public Skin skin; // <--- RESTORED PUBLIC VARIABLE
    public com.militopia.net.NetworkManager networkManager;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // 1. Initialize AssetManager
        assets = new AssetManager();
        assets.finishLoading();

        // 2. Initialize Skin from Manager
        this.skin = assets.getSkin();

        // 3. Inject Fonts & White Pixel
        skin.add("default-font", assets.getRussoFont()); // Use Russo as default
        skin.add("russo", assets.getRussoFont());
        skin.add("russo-btn", assets.getRussoBtnFont());
        skin.add("standard", assets.getFont());

        // Menu panel fonts
        FreeTypeFontGenerator menuFontGen = new FreeTypeFontGenerator(Gdx.files.internal("game-system/russo_one.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter menuItemParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        menuItemParam.size = 20;
        menuItemParam.minFilter = Texture.TextureFilter.Linear;
        menuItemParam.magFilter = Texture.TextureFilter.Linear;
        BitmapFont menuItemFont = menuFontGen.generateFont(menuItemParam);
        FreeTypeFontGenerator.FreeTypeFontParameter menuTitleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        menuTitleParam.size = 20;
        menuTitleParam.minFilter = Texture.TextureFilter.Linear;
        menuTitleParam.magFilter = Texture.TextureFilter.Linear;
        BitmapFont menuTitleFont = menuFontGen.generateFont(menuTitleParam);
        menuFontGen.dispose();
        skin.add("russo-menu-item", menuItemFont);
        skin.add("russo-menu-title", menuTitleFont);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture whiteTexture = new Texture(pixmap);
        skin.add("white", whiteTexture);

        // --- Enterprise UI Palette ---
        Color colorSlate = new Color(0.1f, 0.15f, 0.2f, 1.0f);
        Color colorGold = new Color(0.95f, 0.79f, 0.30f, 1.0f);
        skin.add("color-slate", colorSlate);
        skin.add("color-gold", colorGold);

        // --- NEW: Rounded Texture Generation ---
        int bw = 200, bh = 50, r = 12;
        Color colorSlateTrans = new Color(0.1f, 0.15f, 0.2f, 0.7f); // 70% opaque
        skin.add("btn-up", new Texture(createRoundedRect(bw, bh, r, colorSlateTrans, colorGold, 2)));
        skin.add("btn-down",
                new Texture(createRoundedRect(bw, bh, r, new Color(0.2f, 0.2f, 0.2f, 0.8f), colorGold, 2)));
        skin.add("btn-over",
                new Texture(createRoundedRect(bw, bh, r, new Color(0.2f, 0.25f, 0.3f, 0.85f), colorGold, 2)));

        // 4. Setup Default Styles
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("russo");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        Label.LabelStyle menuItemStyle = new Label.LabelStyle();
        menuItemStyle.font = skin.getFont("russo-menu-item");
        menuItemStyle.fontColor = Color.WHITE;
        skin.add("menu-item", menuItemStyle);

        Label.LabelStyle menuItemHoverStyle = new Label.LabelStyle();
        menuItemHoverStyle.font = skin.getFont("russo-menu-item");
        menuItemHoverStyle.fontColor = colorGold;
        skin.add("menu-item-hover", menuItemHoverStyle);

        Label.LabelStyle menuTitleStyle = new Label.LabelStyle();
        menuTitleStyle.font = skin.getFont("russo-menu-title");
        menuTitleStyle.fontColor = colorGold;
        skin.add("menu-title", menuTitleStyle);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = assets.getFont();
        textButtonStyle.fontColor = Color.WHITE;
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.GRAY);
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        skin.add("default", textButtonStyle);

        TextButton.TextButtonStyle goldStyle = new TextButton.TextButtonStyle();
        goldStyle.font = skin.getFont("russo");
        goldStyle.fontColor = colorGold;
        goldStyle.up = skin.getDrawable("btn-up");
        goldStyle.down = skin.getDrawable("btn-down");
        goldStyle.over = skin.getDrawable("btn-over");
        skin.add("gold-round", goldStyle);

        // militopia-button NinePatch style
        Texture milBtnTex = assets.get(AssetManager.MILITOPIA_BTN);
        NinePatch milBtnPatch = new NinePatch(milBtnTex, 50, 50, 30, 30);
        NinePatchDrawable milBtnUp = new NinePatchDrawable(milBtnPatch);
        milBtnUp.setMinWidth(0);
        milBtnUp.setMinHeight(80f);
        milBtnUp.setLeftWidth(60f);
        milBtnUp.setRightWidth(60f);
        milBtnUp.setTopHeight(40f);
        milBtnUp.setBottomHeight(40f);
        NinePatchDrawable milBtnDown = milBtnUp.tint(new Color(0.65f, 0.65f, 0.65f, 1f));
        NinePatchDrawable milBtnOver = milBtnUp.tint(new Color(1.15f, 1.10f, 0.85f, 1f));

        TextButton.TextButtonStyle milBtnStyle = new TextButton.TextButtonStyle();
        milBtnStyle.font = skin.getFont("russo-btn");
        milBtnStyle.fontColor = Color.WHITE;
        milBtnStyle.up = milBtnUp;
        milBtnStyle.down = milBtnDown;
        milBtnStyle.over = milBtnOver;
        skin.add("militopia-btn", milBtnStyle);

        // Side menu panel background — loaded from PNG asset, tinted to 70% opacity
        NinePatch panelPatch = new NinePatch(assets.get(AssetManager.MENU_PANEL), 32, 32, 32, 2);
        panelPatch.setColor(new Color(1f, 1f, 1f, 0.60f));
        skin.add("menu-panel", panelPatch);

        // Standard Button (Dark Gray Rounded)
        Color semiTransGray = new Color(0.15f, 0.15f, 0.15f, 0.7f);
        skin.add("btn-std-up", new Texture(createRoundedRect(bw, bh, r, semiTransGray, Color.GRAY, 1)));
        TextButton.TextButtonStyle stdStyle = new TextButton.TextButtonStyle();
        stdStyle.font = skin.getFont("russo");
        stdStyle.fontColor = Color.WHITE;
        stdStyle.up = skin.getDrawable("btn-std-up");
        skin.add("default", stdStyle);

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        skin.add("default", scrollPaneStyle);

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = assets.getFont();
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", Color.LIGHT_GRAY);
        textFieldStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        skin.add("default", textFieldStyle);

        com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle listStyle = new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle();
        listStyle.font = skin.getFont("russo");
        listStyle.fontColorSelected = Color.BLACK;
        listStyle.fontColorUnselected = Color.WHITE;
        listStyle.selection = skin.newDrawable("white", colorGold);
        skin.add("default", listStyle);

        com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle selectBoxStyle = new com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle();
        selectBoxStyle.font = skin.getFont("russo");
        selectBoxStyle.fontColor = Color.WHITE;
        selectBoxStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        selectBoxStyle.scrollStyle = scrollPaneStyle;
        selectBoxStyle.listStyle = listStyle;
        skin.add("default", selectBoxStyle);

        TextButton.TextButtonStyle toggleStyle = new TextButton.TextButtonStyle();
        toggleStyle.font = assets.getFont();
        toggleStyle.fontColor = Color.WHITE;
        toggleStyle.checkedFontColor = Color.YELLOW;
        toggleStyle.up = skin.newDrawable("white", Color.GRAY);
        toggleStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        toggleStyle.checked = skin.newDrawable("white", Color.NAVY);
        toggleStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        skin.add("toggle", toggleStyle);

        // Slider style for sound settings
        Pixmap trackPixmap = new Pixmap(1, 8, Pixmap.Format.RGBA8888);
        trackPixmap.setColor(new Color(0.3f, 0.3f, 0.3f, 1f));
        trackPixmap.fill();
        TextureRegionDrawable trackDrawable = new TextureRegionDrawable(new TextureRegion(new Texture(trackPixmap)));
        trackPixmap.dispose();

        Pixmap knobPixmap = new Pixmap(16, 24, Pixmap.Format.RGBA8888);
        knobPixmap.setColor(Color.WHITE);
        knobPixmap.fill();
        TextureRegionDrawable knobDrawable = new TextureRegionDrawable(new TextureRegion(new Texture(knobPixmap)));
        knobPixmap.dispose();

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = trackDrawable;
        sliderStyle.knob = knobDrawable;
        skin.add("default-horizontal", sliderStyle);

        this.setScreen(new SplashScreen(this));
    }

    /** Helper to create a rounded rectangle Pixmap with a border */
    private Pixmap createRoundedRect(int width, int height, int radius, Color bgColor, Color borderColor,
            int borderThickness) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        // Fill background
        pixmap.setColor(bgColor);
        pixmap.fillRectangle(radius, 0, width - 2 * radius, height);
        pixmap.fillRectangle(0, radius, width, height - 2 * radius);
        pixmap.fillCircle(radius, radius, radius);
        pixmap.fillCircle(width - radius, radius, radius);
        pixmap.fillCircle(radius, height - radius, radius);
        pixmap.fillCircle(width - radius, height - radius, radius);

        // Draw border
        if (borderThickness > 0) {
            pixmap.setColor(borderColor);
            for (int i = 0; i < borderThickness; i++) {
                int r = radius;
                int w = width - 1 - i * 2;
                int h = height - 1 - i * 2;
                int offset = i;

                // Horizontal/Vertical lines
                pixmap.drawLine(r + offset, offset, width - r - offset, offset); // top
                pixmap.drawLine(r + offset, height - 1 - offset, width - r - offset, height - 1 - offset); // bottom
                pixmap.drawLine(offset, r + offset, offset, height - r - offset); // left
                pixmap.drawLine(width - 1 - offset, r + offset, width - 1 - offset, height - r - offset); // right

                // Simple corner approximations (45 deg)
                // In a real NinePatch we'd use a better circle draw, but lines work for blocky
                // look
            }
        }
        return pixmap;
    }

    @Override
    public void render() {
        AudioManager.getInstance().update();
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        assets.dispose();
        AudioManager.getInstance().dispose();
        VideoBackgroundManager.getInstance().dispose();
    }
}