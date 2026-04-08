package com.militopia.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.militopia.config.GameConfig;
import com.militopia.config.StructureType;
import com.militopia.managers.AssetManager;
import com.militopia.factories.UnitFactory;
import com.militopia.map.MapGenerator;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import java.util.ArrayList;
import java.util.List;

public class MapRenderSystem extends EntitySystem {

    private final SpriteBatch batch;
    private final UnitFactory unitFactory;
    private final MapGenerator.GameMap gameMap;
    private ShapeRenderer shapeRenderer;

    private int selectedX, selectedY;
    private int bouncingX, bouncingY;
    private float bounceTimer;

    private Animation<TextureRegion> waterAnim;
    private Animation<TextureRegion> deepWaterAnim;
    private float globalWaterTime = 0f;
    private float[][] waterTileOffsets;
    private int clickedX = -1, clickedY = -1;

    private boolean fogEnabled = true;

    private static class BaseInfo {
        int x, y, owner, radius;
    }

    private final List<BaseInfo> activeBases = new ArrayList<>();

    public MapRenderSystem(SpriteBatch batch, UnitFactory factory, MapGenerator.GameMap map, AssetManager assets) {
        this.batch = batch;
        this.unitFactory = factory;
        this.gameMap = map;
        this.shapeRenderer = new ShapeRenderer();
        this.priority = 0;

        TextureAtlas waterAtlas     = assets.getAtlas(AssetManager.WATER_ANIM_ATLAS);
        TextureAtlas deepWaterAtlas = assets.getAtlas(AssetManager.DEEP_WATER_ANIM_ATLAS);
        waterAnim     = buildWaterAnim(waterAtlas);
        deepWaterAnim = buildWaterAnim(deepWaterAtlas);

        waterTileOffsets = new float[map.width][map.height];
        for (int x = 0; x < map.width; x++)
            for (int y = 0; y < map.height; y++)
                waterTileOffsets[x][y] = MathUtils.random(0f, 2f);
    }

    private Animation<TextureRegion> buildWaterAnim(TextureAtlas atlas) {
        com.badlogic.gdx.utils.Array<TextureAtlas.AtlasRegion> regions = atlas.getRegions();
        regions.sort((a, b) -> a.name.compareTo(b.name));
        return new Animation<>(2f / 24f, regions, Animation.PlayMode.LOOP);
    }

    public void updateState(int selX, int selY, int bX, int bY, float bTimer, int cX, int cY) {
        this.selectedX = selX;
        this.selectedY = selY;
        this.bouncingX = bX;
        this.bouncingY = bY;
        this.bounceTimer = bTimer;
        this.clickedX = cX;
        this.clickedY = cY;
    }

    public void setFogEnabled(boolean enabled) {
        this.fogEnabled = enabled;
    }

    @Override
    public void update(float deltaTime) {
        globalWaterTime += deltaTime;

        // --- Cache active bases for territory calculation ---
        activeBases.clear();
        ImmutableArray<Entity> baseEntities = getEngine().getEntitiesFor(
                Family.all(GridPositionComponent.class, StatsComponent.class, TypeComponent.class).get());
        for (Entity e : baseEntities) {
            TypeComponent t = e.getComponent(TypeComponent.class);
            if (t.type != TypeComponent.Type.OBJECT)
                continue;

            StatsComponent s = e.getComponent(StatsComponent.class);
            if (s.unitTypeKey != null && s.unitTypeKey.startsWith("BASE") && s.income >= 2) {
                GridPositionComponent p = e.getComponent(GridPositionComponent.class);
                BaseInfo bi = new BaseInfo();
                bi.x = p.x;
                bi.y = p.y;
                bi.owner = s.owner;
                bi.radius = s.vision;
                activeBases.add(bi);
            }
        }

        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;

        batch.begin();
        for (int x = gameMap.width - 1; x >= 0; x--) {
            for (int y = gameMap.height - 1; y >= 0; y--) {
                drawTerrainTile(x, y, xOffset, yOffset);
            }
        }
        batch.end();
        renderBordersPass();
    }

    private void drawTerrainTile(int x, int y, float xOffset, float yOffset) {
        float[] coords = getIsoCoords(x, y);
        float isoX = coords[0];
        float isoY = coords[1];
        float animY = coords[2];

        TextureRegion regionToDraw = null;

        boolean isVisible = gameMap.visibleTiles[x][y];

        boolean isFog = fogEnabled && !isVisible;
        MapGenerator.TerrainType terrainType = gameMap.terrain[x][y];
        boolean isWaterTile = terrainType == MapGenerator.TerrainType.WATER || terrainType == MapGenerator.TerrainType.DEEP_WATER;
        if (isFog) {
            regionToDraw = unitFactory.fogRegion;
        } else if (!isFog && isWaterTile) {
            Animation<TextureRegion> anim = (terrainType == MapGenerator.TerrainType.DEEP_WATER) ? deepWaterAnim : waterAnim;
            regionToDraw = anim.getKeyFrame(globalWaterTime + waterTileOffsets[x][y], true);
        } else {
            regionToDraw = unitFactory.getTextureForTerrain(terrainType.ordinal());
        }

        if (regionToDraw != null) {
            float drawW = isWaterTile ? GameConfig.DRAW_WIDTH  * GameConfig.WATER_TILE_SCALE : GameConfig.DRAW_WIDTH;
            float drawH = isWaterTile ? GameConfig.DRAW_HEIGHT * GameConfig.WATER_TILE_SCALE : GameConfig.DRAW_HEIGHT;
            float drawX = isoX - xOffset - (drawW - GameConfig.DRAW_WIDTH) / 2f;
            float drawY = isoY - yOffset - (drawH - GameConfig.DRAW_HEIGHT) / 2f;

            float fogLift = isFog ? 5f : 0f;
            batch.draw(regionToDraw, drawX, drawY + animY + fogLift, drawW, drawH);
            // Second fog layer at the original (non-lifted) position for depth
            if (isFog) {
                batch.draw(regionToDraw, drawX, drawY + animY, drawW, drawH);
            }

            if (x == selectedX && y == selectedY) {
                drawHighlight(regionToDraw, drawX, drawY + animY, drawW, drawH);
            }
        }
    }

    private void renderBordersPass() {
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // --- UPDATED: Use dynamic width/height ---
        for (int x = gameMap.width - 1; x >= 0; x--) {
            for (int y = gameMap.height - 1; y >= 0; y--) {
                if (fogEnabled && !gameMap.visibleTiles[x][y]) {
                    continue;
                }

                int owner = getTileOwner(x, y);
                if (owner != 0) {
                    if (owner == 1) {
                        shapeRenderer.setColor(Color.BLUE);
                    } else if (owner == 2) {
                        shapeRenderer.setColor(Color.RED);
                    }
                    drawSmartBorders(x, y, owner);
                }
            }
        }
        drawRailPass();
        renderSelectionIndicator();
        shapeRenderer.end();
    }

    private void renderSelectionIndicator() {
        if (clickedX < 0 || clickedY < 0 || clickedX >= gameMap.width || clickedY >= gameMap.height) return;
        if (fogEnabled && !gameMap.visibleTiles[clickedX][clickedY]) return;

        float[] coords = getIsoCoords(clickedX, clickedY);
        float isoX = coords[0];
        float isoY = coords[1];
        float animY = coords[2];

        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float drawX = isoX - xOffset;
        float centerX = drawX + (GameConfig.DRAW_WIDTH / 2f);
        float surfaceLift = 10f;
        float centerY = isoY + surfaceLift + animY;
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;

        float topX = centerX,          topY = centerY + halfH;
        float rightX = centerX + halfW, rightY = centerY;
        float botX = centerX,           botY = centerY - halfH;
        float leftX = centerX - halfW,  leftY = centerY;

        float jointSize = GameConfig.SELECTION_LINE_THICKNESS / 2f;

        shapeRenderer.setColor(1f, 1f, 1f, GameConfig.SELECTION_ALPHA);
        shapeRenderer.rectLine(topX, topY, rightX, rightY, GameConfig.SELECTION_LINE_THICKNESS);
        shapeRenderer.rectLine(rightX, rightY, botX, botY, GameConfig.SELECTION_LINE_THICKNESS);
        shapeRenderer.rectLine(botX, botY, leftX, leftY, GameConfig.SELECTION_LINE_THICKNESS);
        shapeRenderer.rectLine(leftX, leftY, topX, topY, GameConfig.SELECTION_LINE_THICKNESS);
        shapeRenderer.circle(topX, topY, jointSize);
        shapeRenderer.circle(rightX, rightY, jointSize);
        shapeRenderer.circle(botX, botY, jointSize);
        shapeRenderer.circle(leftX, leftY, jointSize);
    }

    private float[] getIsoCoords(int x, int y) {
        float isoX = (x - y) * (GameConfig.TILE_WIDTH / 2.0f);
        float isoY = (x + y) * (GameConfig.TILE_HEIGHT / 2.0f);
        float animY = 0;
        if (x == bouncingX && y == bouncingY) {
            float progress = bounceTimer / GameConfig.BOUNCE_DURATION;
            animY = (float) Math.sin(progress * Math.PI) * GameConfig.BOUNCE_HEIGHT;
        }
        return new float[] { isoX, isoY, animY };
    }

    private void drawHighlight(TextureRegion t, float x, float y, float w, float h) {
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
        batch.setColor(0.4f, 0.4f, 0.4f, 1f);
        batch.draw(t, x, y, w, h);
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    private void drawSmartBorders(int x, int y, int currentOwner) {
        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float isoX = (x - y) * (GameConfig.TILE_WIDTH / 2.0f);
        float isoY = (x + y) * (GameConfig.TILE_HEIGHT / 2.0f);
        float drawX = isoX - xOffset;
        float centerX = drawX + (GameConfig.DRAW_WIDTH / 2f);
        float surfaceLift = 10f;
        float centerY = isoY + surfaceLift;
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;
        float topX = centerX;
        float topY = centerY + halfH;
        float rightX = centerX + halfW;
        float rightY = centerY;
        float botX = centerX;
        float botY = centerY - halfH;
        float leftX = centerX - halfW;
        float leftY = centerY;
        float thick = 2.0f;
        float jointSize = thick / 2f;

        if (getTileOwner(x, y + 1) != currentOwner) {
            shapeRenderer.rectLine(leftX, leftY, topX, topY, thick);
            shapeRenderer.circle(leftX, leftY, jointSize);
            shapeRenderer.circle(topX, topY, jointSize);
        }
        if (getTileOwner(x + 1, y) != currentOwner) {
            shapeRenderer.rectLine(rightX, rightY, topX, topY, thick);
            shapeRenderer.circle(rightX, rightY, jointSize);
            shapeRenderer.circle(topX, topY, jointSize);
        }
        if (getTileOwner(x, y - 1) != currentOwner) {
            shapeRenderer.rectLine(rightX, rightY, botX, botY, thick);
            shapeRenderer.circle(rightX, rightY, jointSize);
            shapeRenderer.circle(botX, botY, jointSize);
        }
        if (getTileOwner(x - 1, y) != currentOwner) {
            shapeRenderer.rectLine(leftX, leftY, botX, botY, thick);
            shapeRenderer.circle(leftX, leftY, jointSize);
            shapeRenderer.circle(botX, botY, jointSize);
        }
    }

    private int getTileOwner(int x, int y) {
        if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height) {
            return 0;
        }

        // Check against cached bases
        for (BaseInfo bi : activeBases) {
            int dx = Math.abs(bi.x - x);
            int dy = Math.abs(bi.y - y);
            if (dx <= bi.radius && dy <= bi.radius) {
                return bi.owner;
            }
        }

        return 0;
    }

    private boolean isBase(int x, int y, int player) {
        MapGenerator.ObjectType obj = gameMap.objects[x][y];
        if (player == 1) {
            return obj == MapGenerator.ObjectType.BASE_P1;
        }
        if (player == 2) {
            return obj == MapGenerator.ObjectType.BASE_P2;
        }
        return false;
    }

    private boolean hasRail(int x, int y) {
        if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height) return false;
        return gameMap.rails[x][y];
    }

    private boolean hasBase(int x, int y) {
        if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height) return false;
        MapGenerator.ObjectType obj = gameMap.objects[x][y];
        if (obj == null) return false;
        String name = obj.name();
        return name.startsWith("BASE_P1") || name.startsWith("BASE_P2");
    }

    private void drawRailPass() {
        float thick = GameConfig.SELECTION_LINE_THICKNESS;
        float jointSize = thick / 2f;
        shapeRenderer.setColor(0.65f, 0.65f, 0.65f, 1.0f); // slightly brighter steel gray

        for (int x = gameMap.width - 1; x >= 0; x--) {
            for (int y = gameMap.height - 1; y >= 0; y--) {
                if (!gameMap.rails[x][y]) continue;
                if (fogEnabled && !gameMap.visibleTiles[x][y]) continue;

                float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
                float[] coords = getIsoCoords(x, y);
                float cx = coords[0] - xOffset + (GameConfig.DRAW_WIDTH / 2f);
                float cy = coords[1] + 10f + coords[2]; // surface lift + bounce anim
                float halfW = GameConfig.TILE_WIDTH / 2.0f;
                float halfH = GameConfig.TILE_HEIGHT / 2.0f;

                float neX = cx + halfW / 2f, neY = cy + halfH / 2f; // grid +X midpoint
                float seX = cx + halfW / 2f, seY = cy - halfH / 2f; // grid -Y midpoint
                float swX = cx - halfW / 2f, swY = cy - halfH / 2f; // grid -X midpoint
                float nwX = cx - halfW / 2f, nwY = cy + halfH / 2f; // grid +Y midpoint

                boolean toNE = hasRail(x + 1, y) || hasBase(x + 1, y);
                boolean toSE = hasRail(x, y - 1) || hasBase(x, y - 1);
                boolean toSW = hasRail(x - 1, y) || hasBase(x - 1, y);
                boolean toNW = hasRail(x, y + 1) || hasBase(x, y + 1);

                if (toNE || toSE || toSW || toNW) {
                    shapeRenderer.circle(cx, cy, jointSize);
                    
                    if (toNE) {
                        float tx = neX, ty = neY;
                        if (hasBase(x + 1, y)) { tx = cx + halfW; ty = cy + halfH; }
                        shapeRenderer.rectLine(cx, cy, tx, ty, thick);
                        shapeRenderer.circle(tx, ty, jointSize);
                    }
                    if (toSE) {
                        float tx = seX, ty = seY;
                        if (hasBase(x, y - 1)) { tx = cx + halfW; ty = cy - halfH; }
                        shapeRenderer.rectLine(cx, cy, tx, ty, thick);
                        shapeRenderer.circle(tx, ty, jointSize);
                    }
                    if (toSW) {
                        float tx = swX, ty = swY;
                        if (hasBase(x - 1, y)) { tx = cx - halfW; ty = cy - halfH; }
                        shapeRenderer.rectLine(cx, cy, tx, ty, thick);
                        shapeRenderer.circle(tx, ty, jointSize);
                    }
                    if (toNW) {
                        float tx = nwX, ty = nwY;
                        if (hasBase(x, y + 1)) { tx = cx - halfW; ty = cy + halfH; }
                        shapeRenderer.rectLine(cx, cy, tx, ty, thick);
                        shapeRenderer.circle(tx, ty, jointSize);
                    }
                } else {
                    // Isolated rail: draw a straight line through the middle of the tile
                    // Use a slightly longer segment than just edge-midpoints to make it clear it's a line
                    float diagX = halfW * 0.4f;
                    float diagY = halfH * 0.4f;
                    shapeRenderer.rectLine(cx - diagX, cy - diagY, cx + diagX, cy + diagY, thick);
                    shapeRenderer.circle(cx - diagX, cy - diagY, jointSize);
                    shapeRenderer.circle(cx + diagX, cy + diagY, jointSize);
                }
            }
        }
    }
}
