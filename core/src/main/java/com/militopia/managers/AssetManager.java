package com.militopia.managers;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.militopia.map.MapGenerator;

public class AssetManager {

    public final com.badlogic.gdx.assets.AssetManager manager;

    // Terrain
    public static final String TILE_GRASS    = "tiles/tile_grass.png";
    public static final String TILE_WATER    = "tiles/tile_water.png";
    public static final String TILE_DEEPWATER = "tiles/tile_deepwater.png";
    public static final String TILE_SAND     = "tiles/tile_sand.png";
    public static final String TILE_MOUNTAIN = "tiles/tile_mountain.png";
    public static final String FOG_OF_WAR    = "tiles/fog_of_war.png";

    // Units (single right-facing asset; left is generated via flip)
    public static final String RECRUIT       = "displays/RECRUIT.png";
    public static final String RANGER        = "displays/RANGER.png";
    public static final String SNIPER        = "displays/SNIPER.png";
    public static final String TANK          = "displays/TANK.png";
    public static final String JUGGERNAUT    = "displays/JUGGERNAUT.png";
    public static final String RECON_DRONE   = "displays/DRONE.png";
    public static final String SUICIDE_DRONE = "displays/SUICIDE_DRONE.png";
    public static final String APACHE        = "displays/APACHE.png";
    public static final String B2            = "displays/B2.png";
    public static final String GUNBOAT       = "displays/GUNBOAT.png";
    public static final String DESTROYER     = "displays/DESTROYER.png";
    public static final String CARRIER       = "displays/CARRIER.png";
    public static final String SUBMARINE     = "displays/SUBMARINE.png";

    // Structures
    public static final String MUNITION_FACTORY = "displays/MUNITION_FACTORY.png";
    public static final String PORT             = "displays/PORT.png";
    public static final String SOLAR_ARRAY      = "displays/SOLAR_ARRAY.png";
    public static final String OIL_DERRICK      = "displays/OIL_DERRICK.png";
    public static final String NUCLEAR_PLANT    = "displays/NUCLEAR_PLANT.png";
    public static final String FIELD_HOSPITAL   = "displays/FIELD_HOSPITAL.png";
    public static final String RADAR_STATION    = "displays/RADAR_STATION.png";
    public static final String SIGNAL_JAMMER    = "displays/SIGNAL_JAMMER.png";
    public static final String STRUCT_TOWN      = "displays/TOWN.png";

    // Objects
    public static final String OBJ_TREE     = "displays/TREE.png";
    public static final String OBJ_RUINS    = "displays/RUINS.png";
    public static final String OBJ_OIL      = "displays/OIL.png";
    public static final String OBJ_CACTUS   = "displays/CACTUS.png";
    public static final String OBJ_MOUNTAIN = "displays/MOUNTAIN.png";

    // Animals
    public static final String HORSE = "displays/HORSE.png";
    public static final String FISH  = "displays/FISH.png";
    public static final String DEER  = "displays/DEER.png";
    public static final String ZEBRA = "displays/ZEBRA.png";

    // Bases (A = player 1, B = player 2, levels 1-10)
    public static final String BASE_A_1  = "displays/BASE_A_1.png";
    public static final String BASE_A_2  = "displays/BASE_A_2.png";
    public static final String BASE_A_3  = "displays/BASE_A_3.png";
    public static final String BASE_A_4  = "displays/BASE_A_4.png";
    public static final String BASE_A_5  = "displays/BASE_A_5.png";
    public static final String BASE_A_6  = "displays/BASE_A_6.png";
    public static final String BASE_A_7  = "displays/BASE_A_7.png";
    public static final String BASE_A_8  = "displays/BASE_A_8.png";
    public static final String BASE_A_9  = "displays/BASE_A_9.png";
    public static final String BASE_A_10 = "displays/BASE_A_10.png";

    public static final String BASE_B_1  = "displays/BASE_B_1.png";
    public static final String BASE_B_2  = "displays/BASE_B_2.png";
    public static final String BASE_B_3  = "displays/BASE_B_3.png";
    public static final String BASE_B_4  = "displays/BASE_B_4.png";
    public static final String BASE_B_5  = "displays/BASE_B_5.png";
    public static final String BASE_B_6  = "displays/BASE_B_6.png";
    public static final String BASE_B_7  = "displays/BASE_B_7.png";
    public static final String BASE_B_8  = "displays/BASE_B_8.png";
    public static final String BASE_B_9  = "displays/BASE_B_9.png";
    public static final String BASE_B_10 = "displays/BASE_B_10.png";

    // Shadow
    public static final String SHADOW = "ui/shadow.png";

    // UI
    public static final String ENEMY_MARKER  = "ui/enemy_marker.png";
    public static final String MARKER_DOT    = "ui/marker_dot.png";
    public static final String ICON_SETTINGS = "ui/icon_settings.png";
    public static final String ICON_STATS    = "ui/icon_stats.png";
    public static final String ICON_END      = "ui/icon_end.png";
    public static final String BTN_SLIDEDOWN = "ui/slidedown_btn.png";
    public static final String CIRCLE_UI     = "ui/circle_ui.png";
    public static final String CIRCLE_UI2    = "ui/circle_ui2.png";
    public static final String MILITOPIA_BTN = "ui/militopia-button.png";
    public static final String MENU_PANEL    = "ui/menu-panel.png";
    public static final String INVINCIBLE_RED = "ui/INVINCIBLE_RED.png";
    public static final String INVINCIBLE_BLUE = "ui/INVINCIBLE_BLUE.png";
    public static final String RAILWAY_ICON = "ui/RAILWAY.png";
    public static final String DIG_IN         = "displays/DIG-IN.png";
    public static final String BACKGROUND    = "game-system/militopia_background.png";
    public static final String LOGO          = "game-system/militopia-logo.png";
    public static final String TEXT_LOGO     = "game-system/militopia-text-logo.png";
    public static final String SPLASH_SCREEN = "game-system/militopia-splash-screen.png";

    // Sprite Atlases
    public static final String RECRUIT_RUN_ATLAS    = "sprites/recruit-run.atlas";
    public static final String RECRUIT_ATTACK_ATLAS = "sprites/recruit-attack.atlas";
    public static final String TANK_ATTACK_ATLAS    = "sprites/tank-attack.atlas";
    public static final String NUCLEAR_ATTACK_ATLAS = "sprites/nuclear-attack.atlas";
    public static final String GUN_NOZZLE_ATLAS     = "sprites/gun-nozzle-flash.atlas";
    public static final String RANGER_RUN_ATLAS      = "sprites/ranger-run.atlas";
    public static final String SNIPER_RUN_ATLAS      = "sprites/sniper-run.atlas";
    public static final String JUGGERNAUT_JUMP_ATLAS = "sprites/juggernaut-jump.atlas";
    public static final String HELICOPTER_MOVE_ATLAS     = "sprites/helicoper-move.atlas";
    public static final String TANK_IDLE_ATLAS           = "sprites/tank-idle.atlas";
    public static final String JUGGERNAUT_BOOSTERS_ATLAS = "sprites/juggernaut-boosters.atlas";

    // Idle / movement atlases
    public static final String RECRUIT_ATTACKING_ATLAS      = "sprites/recruit-attacking.atlas";
    public static final String DRONE_MOVING_ATLAS           = "sprites/drone-moving.atlas";
    public static final String WATER_ANIM_ATLAS             = "sprites/water-anim.atlas";
    public static final String DEEP_WATER_ANIM_ATLAS        = "sprites/deep-water-anim.atlas";
    public static final String DEER_IDLE_ATLAS              = "sprites/deer-idle.atlas";
    public static final String FISH_IDLE_ATLAS              = "sprites/fish-idle.atlas";
    public static final String HORSE_IDLE_ATLAS             = "sprites/horse-idle.atlas";
    public static final String ZEBRA_IDLE_ATLAS             = "sprites/zebra-idle.atlas";
    public static final String JAMMER_IDLE_ATLAS            = "sprites/jammer-idle.atlas";
    public static final String MUNITION_FACTORY_IDLE_ATLAS  = "sprites/munition-factory-idle.atlas";
    public static final String NUCLEAR_PLANT_IDLE_ATLAS     = "sprites/nuclear-plant-idle.atlas";
    public static final String OIL_DERRICK_IDLE_ATLAS       = "sprites/oil-derrick-idle.atlas";
    public static final String PORT_IDLE_ATLAS              = "sprites/port-idle.atlas";
    public static final String RADAR_IDLE_ATLAS             = "sprites/radar-idle.atlas";
    public static final String SOLAR_ARRAY_IDLE_ATLAS       = "sprites/solar-array-idle.atlas";

    public static final String FUNDING_ICON  = "ui/funding_icon.png";
    public static final String FUNDING_ICON2 = "ui/funding_icon2.png";

    // Fonts
    public static final String GAME_FONT = "game-system/game_font.ttf";
    public static final String RUSSO_FONT = "game-system/russo_one.ttf";
    public static final String RUSSO_FONT_BTN = "game-system/russo_one_btn.ttf"; // same file, smaller size

    public AssetManager() {
        manager = new com.badlogic.gdx.assets.AssetManager();
        loadAssets();
    }

    private void loadAssets() {
        // Terrain
        manager.load(TILE_GRASS, Texture.class);
        manager.load(TILE_WATER, Texture.class);
        manager.load(TILE_DEEPWATER, Texture.class);
        manager.load(TILE_SAND, Texture.class);
        manager.load(TILE_MOUNTAIN, Texture.class);
        manager.load(FOG_OF_WAR, Texture.class);

        // Objects / environment
        manager.load(OBJ_TREE, Texture.class);
        manager.load(OBJ_RUINS, Texture.class);
        manager.load(OBJ_OIL, Texture.class);
        manager.load(OBJ_CACTUS, Texture.class);
        manager.load(OBJ_MOUNTAIN, Texture.class);
        manager.load(STRUCT_TOWN, Texture.class);

        // Units
        manager.load(RECRUIT, Texture.class);
        manager.load(RANGER, Texture.class);
        manager.load(SNIPER, Texture.class);
        manager.load(TANK, Texture.class);
        manager.load(JUGGERNAUT, Texture.class);
        manager.load(RECON_DRONE, Texture.class);
        manager.load(SUICIDE_DRONE, Texture.class);
        manager.load(APACHE, Texture.class);
        manager.load(B2, Texture.class);
        manager.load(GUNBOAT, Texture.class);
        manager.load(DESTROYER, Texture.class);
        manager.load(CARRIER, Texture.class);
        manager.load(SUBMARINE, Texture.class);

        // Structures
        manager.load(MUNITION_FACTORY, Texture.class);
        manager.load(PORT, Texture.class);
        manager.load(SOLAR_ARRAY, Texture.class);
        manager.load(OIL_DERRICK, Texture.class);
        manager.load(NUCLEAR_PLANT, Texture.class);
        manager.load(FIELD_HOSPITAL, Texture.class);
        manager.load(RADAR_STATION, Texture.class);
        manager.load(SIGNAL_JAMMER, Texture.class);

        // Animals
        manager.load(HORSE, Texture.class);
        manager.load(FISH, Texture.class);
        manager.load(DEER, Texture.class);
        manager.load(ZEBRA, Texture.class);

        // Bases A (player 1)
        manager.load(BASE_A_1,  Texture.class);
        manager.load(BASE_A_2,  Texture.class);
        manager.load(BASE_A_3,  Texture.class);
        manager.load(BASE_A_4,  Texture.class);
        manager.load(BASE_A_5,  Texture.class);
        manager.load(BASE_A_6,  Texture.class);
        manager.load(BASE_A_7,  Texture.class);
        manager.load(BASE_A_8,  Texture.class);
        manager.load(BASE_A_9,  Texture.class);
        manager.load(BASE_A_10, Texture.class);

        // Bases B (player 2)
        manager.load(BASE_B_1,  Texture.class);
        manager.load(BASE_B_2,  Texture.class);
        manager.load(BASE_B_3,  Texture.class);
        manager.load(BASE_B_4,  Texture.class);
        manager.load(BASE_B_5,  Texture.class);
        manager.load(BASE_B_6,  Texture.class);
        manager.load(BASE_B_7,  Texture.class);
        manager.load(BASE_B_8,  Texture.class);
        manager.load(BASE_B_9,  Texture.class);
        manager.load(BASE_B_10, Texture.class);

        // Shadow
        manager.load(SHADOW, Texture.class);

        // UI
        manager.load(ENEMY_MARKER, Texture.class);
        manager.load(MARKER_DOT, Texture.class);
        manager.load(ICON_SETTINGS, Texture.class);
        manager.load(ICON_STATS, Texture.class);
        manager.load(ICON_END, Texture.class);
        manager.load(BTN_SLIDEDOWN, Texture.class);
        manager.load(CIRCLE_UI, Texture.class);
        manager.load(CIRCLE_UI2, Texture.class);
        manager.load(MILITOPIA_BTN, Texture.class);
        manager.load(MENU_PANEL, Texture.class);
        manager.load(INVINCIBLE_RED, Texture.class);
        manager.load(INVINCIBLE_BLUE, Texture.class);
        manager.load(RAILWAY_ICON, Texture.class);
        manager.load(DIG_IN, Texture.class);
        manager.load(BACKGROUND, Texture.class);
        manager.load(LOGO, Texture.class);
        manager.load(TEXT_LOGO, Texture.class);
        manager.load(SPLASH_SCREEN, Texture.class);

        // Sprite atlases
        manager.load(RECRUIT_RUN_ATLAS,    TextureAtlas.class);
        manager.load(RECRUIT_ATTACK_ATLAS, TextureAtlas.class);
        manager.load(TANK_ATTACK_ATLAS,    TextureAtlas.class);
        manager.load(NUCLEAR_ATTACK_ATLAS, TextureAtlas.class);
        manager.load(GUN_NOZZLE_ATLAS,     TextureAtlas.class);
        manager.load(RANGER_RUN_ATLAS,      TextureAtlas.class);
        manager.load(SNIPER_RUN_ATLAS,      TextureAtlas.class);
        manager.load(JUGGERNAUT_JUMP_ATLAS,      TextureAtlas.class);
        manager.load(HELICOPTER_MOVE_ATLAS,     TextureAtlas.class);
        manager.load(TANK_IDLE_ATLAS,           TextureAtlas.class);
        manager.load(JUGGERNAUT_BOOSTERS_ATLAS, TextureAtlas.class);
        manager.load(RECRUIT_ATTACKING_ATLAS,     TextureAtlas.class);
        manager.load(DRONE_MOVING_ATLAS,          TextureAtlas.class);
        manager.load(WATER_ANIM_ATLAS,            TextureAtlas.class);
        manager.load(DEEP_WATER_ANIM_ATLAS,       TextureAtlas.class);
        manager.load(DEER_IDLE_ATLAS,             TextureAtlas.class);
        manager.load(FISH_IDLE_ATLAS,             TextureAtlas.class);
        manager.load(HORSE_IDLE_ATLAS,            TextureAtlas.class);
        manager.load(ZEBRA_IDLE_ATLAS,            TextureAtlas.class);
        manager.load(JAMMER_IDLE_ATLAS,           TextureAtlas.class);
        manager.load(MUNITION_FACTORY_IDLE_ATLAS, TextureAtlas.class);
        manager.load(NUCLEAR_PLANT_IDLE_ATLAS,    TextureAtlas.class);
        manager.load(OIL_DERRICK_IDLE_ATLAS,      TextureAtlas.class);
        manager.load(PORT_IDLE_ATLAS,             TextureAtlas.class);
        manager.load(RADAR_IDLE_ATLAS,            TextureAtlas.class);
        manager.load(SOLAR_ARRAY_IDLE_ATLAS,      TextureAtlas.class);

        manager.load(FUNDING_ICON, Texture.class);
        manager.load(FUNDING_ICON2, Texture.class);

        FileHandleResolver resolver = new InternalFileHandleResolver();
        manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));

        FreetypeFontLoader.FreeTypeFontLoaderParameter fontParam = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        fontParam.fontFileName = GAME_FONT;
        fontParam.fontParameters.size = 24;
        fontParam.fontParameters.minFilter = Texture.TextureFilter.Linear;
        fontParam.fontParameters.magFilter = Texture.TextureFilter.Linear;

        manager.load(GAME_FONT, BitmapFont.class, fontParam);

        // Load Russo One (The "Heavy Tank" font)
        FreetypeFontLoader.FreeTypeFontLoaderParameter russoParam = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        russoParam.fontFileName = RUSSO_FONT;
        russoParam.fontParameters.size = 24;
        russoParam.fontParameters.minFilter = Texture.TextureFilter.Linear;
        russoParam.fontParameters.magFilter = Texture.TextureFilter.Linear;
        manager.load(RUSSO_FONT, BitmapFont.class, russoParam);

        // Load Russo One at smaller size for buttons (so text sits inside button with breathing room)
        FreetypeFontLoader.FreeTypeFontLoaderParameter russoBtnParam = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        russoBtnParam.fontFileName = RUSSO_FONT;
        russoBtnParam.fontParameters.size = 16;
        russoBtnParam.fontParameters.minFilter = Texture.TextureFilter.Linear;
        russoBtnParam.fontParameters.magFilter = Texture.TextureFilter.Linear;
        manager.load(RUSSO_FONT_BTN, BitmapFont.class, russoBtnParam);
    }

    public void finishLoading() {
        manager.finishLoading();
    }

    public TextureAtlas getAtlas(String fileName) {
        return manager.get(fileName, TextureAtlas.class);
    }

    public Texture get(String fileName) {
        if (!manager.isLoaded(fileName)) {
            return null;
        }
        Texture t = manager.get(fileName, Texture.class);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    public Skin getSkin() {
        return new Skin();
    }

    public BitmapFont getFont() {
        return manager.get(GAME_FONT, BitmapFont.class);
    }

    public BitmapFont getRussoFont() {
        return manager.get(RUSSO_FONT, BitmapFont.class);
    }

    public BitmapFont getRussoBtnFont() {
        return manager.get(RUSSO_FONT_BTN, BitmapFont.class);
    }

    public com.badlogic.gdx.graphics.g2d.TextureRegion getTerrainRegion(MapGenerator.TerrainType type) {
        String path = TILE_GRASS;
        switch (type) {
            case WATER: path = TILE_WATER; break;
            case DEEP_WATER: path = TILE_DEEPWATER; break;
            case SAND: path = TILE_SAND; break;
            case MOUNTAIN: path = TILE_MOUNTAIN; break;
            case GRASS: default: path = TILE_GRASS; break;
        }
        return new com.badlogic.gdx.graphics.g2d.TextureRegion(get(path));
    }

    public void dispose() {
        manager.dispose();
    }
}
