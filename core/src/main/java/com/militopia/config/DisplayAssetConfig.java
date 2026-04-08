package com.militopia.config;

import com.militopia.components.AbilitiesComponent;
import java.util.HashMap;
import java.util.Map;

public class DisplayAssetConfig {

    public static class AssetData {
        public final float width, height, offsetX, offsetY;
        public final float shadowW, shadowH, shadowOffsetX, shadowOffsetY;

        public AssetData(float width, float height, float offsetX, float offsetY,
                float shadowW, float shadowH, float shadowOffsetX, float shadowOffsetY) {
            this.width = width;
            this.height = height;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.shadowW = shadowW;
            this.shadowH = shadowH;
            this.shadowOffsetX = shadowOffsetX;
            this.shadowOffsetY = shadowOffsetY;
        }
    }

    private static final AssetData DEFAULT = new AssetData(
            GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT, 0f, 10f,
            GameConfig.TILE_WIDTH, GameConfig.TILE_HEIGHT / 2f, 0f, 0f);

    private static final Map<String, AssetData> DATA = new HashMap<>();

    static {
        // Units w h x y shw shh shx shy
        DATA.put("RECRUIT", new AssetData(5, 5, 0, 7.2f, 8, 5, 0, 0));
        DATA.put("RANGER", new AssetData(5, 5, 0, 7.2f, 8, 5, 0, 0));
        DATA.put("SNIPER", new AssetData(7, 7, 1.3f, 7f, 8, 5, 0, 0));
        DATA.put("TANK", new AssetData(12, 12, 0, 6f, 10, 10, 0, -5));
        DATA.put("JUGGERNAUT", new AssetData(12, 12, 0, 9f, 10, 10, 0, -3));

        DATA.put("RECON_DRONE", new AssetData(10, 10, 0, 12f, 10, 5, 0, 0));
        DATA.put("SUICIDE_DRONE", new AssetData(10, 10, 0, 12f, 10, 5, 0, 0));
        DATA.put("APACHE", new AssetData(12, 12, 0, 12f, 20, 20, 0, -8));
        DATA.put("B2", new AssetData(12, 12, 0, 12f, 20, 20, 0, -8));

        DATA.put("GUNBOAT", new AssetData(9, 9, 0, 6f, 12, 6, 0, 0));
        DATA.put("DESTROYER", new AssetData(13, 13, 0, 6f, 16, 10, 0, -3));
        DATA.put("CARRIER", new AssetData(13, 13, 0, 6f, 16, 10, 0, -3));
        DATA.put("SUBMARINE", new AssetData(13, 13, 0, 6f, 16, 10, 0, -3));

        // Structures
        DATA.put("MUNITION_FACTORY", new AssetData(8, 8, 0, 6f, 10, 5, 0, 0));
        DATA.put("PORT", new AssetData(15, 15, 0, 5.5f, 12, 6, 0, 0));
        DATA.put("SOLAR", new AssetData(8, 8, 0, 6.5f, 10, 5, 0, 0));
        DATA.put("OIL_DERRICK", new AssetData(8, 8, 0, 7f, 10, 5, 0, 0));
        DATA.put("NUCLEAR", new AssetData(10, 10, 0, 6f, 10, 5, 0, 0));
        DATA.put("HOSPITAL", new AssetData(8, 8, 0, 6f, 10, 5, 0, 0));
        DATA.put("RADAR", new AssetData(8, 8, 0, 7f, 10, 5, 0, 0));
        DATA.put("JAMMER", new AssetData(8, 8, 0, 7f, 10, 5, 0, 0));

        // Objects
        DATA.put("TREE", new AssetData(13, 13, 0, 6f, 10, 5, 0, 0));
        DATA.put("RUINS", new AssetData(8, 9, 0, 6f, 10, 5, 0, 0));
        DATA.put("OIL", new AssetData(10, 10, 0, 5.5f, 10, 5, 0, 0));
        DATA.put("CACTUS", new AssetData(8, 8, 0, 8f, 10, 5, 0, 0));
        DATA.put("MOUNTAIN_OBJ", new AssetData(17, 17, 0, 8f, 10, 5, 0, 0));
        DATA.put("TOWN", new AssetData(15, 16.5f, 0, 6f, 10, 5, 0, 0));

        // Base levels (shared by both players — adjust offsetY per level as needed)
        DATA.put("BASE_1", new AssetData(10, 10, 0, 7f, 14, 8, 0, 0));
        DATA.put("BASE_2", new AssetData(17, 17, 0, 8f, 14, 8, 0, 0));
        DATA.put("BASE_3", new AssetData(17, 17, 0, 8f, 14, 8, 0, 0));
        DATA.put("BASE_4", new AssetData(17, 17, 0, 8f, 14, 8, 0, 0));
        DATA.put("BASE_5", new AssetData(17, 17, 0, 8f, 14, 8, 0, 0));
        DATA.put("BASE_6", new AssetData(17, 17, 0, 8f, 14, 8, 0, 0));
        DATA.put("BASE_7", new AssetData(17, 17, 0, 8f, 14, 8, 0, 0));
        DATA.put("BASE_8", new AssetData(17, 17, 0, 8f, 14, 8, 0, 0));
        DATA.put("BASE_9", new AssetData(17, 17, 0, 8f, 14, 8, 0, 0));
        DATA.put("BASE_10", new AssetData(17, 17, 0, 8f, 14, 8, 0, 0));

        // Ability overlays
        DATA.put(AbilitiesComponent.KEY_DIG_IN, new AssetData(13f, 13f, 0f, 3f, 0, 0, 0, 0));

        // Animals
        DATA.put("HORSE", new AssetData(4, 4, 0, 6f, 5, 5, 0, 0));
        DATA.put("FISH", new AssetData(6, 6, 0, 6f, 5, 5, 0, 0));
        DATA.put("DEER", new AssetData(4, 4, 0, 6f, 5, 5, 0, 0));
        DATA.put("ZEBRA", new AssetData(4, 4, 0, 6f, 5, 5, 0, 0));
    }

    public static AssetData get(String key) {
        AssetData d = DATA.get(key);
        return d != null ? d : DEFAULT;
    }
}
