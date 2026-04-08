package com.militopia.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.militopia.components.*;
import com.militopia.config.GameConfig;
import com.militopia.managers.AssetManager;

public class EntityFactory {
    private PooledEngine engine;
    private TextureRegion markerRegion;
    private TextureRegion enemyMarkerRegion;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> explosionAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> hitAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> recruitAttackAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> tankAttackAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> nuclearAttackAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> gunNozzleFlashAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> recruitRunAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> rangerRunAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> sniperRunAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> juggernautJumpAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> helicopterMoveAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> tankIdleAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> juggernautBoostersAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> droneMovingAnim;

    public EntityFactory(PooledEngine engine, AssetManager assets) {
        this.engine = engine;
        this.markerRegion = new TextureRegion(assets.get(AssetManager.MARKER_DOT));
        this.enemyMarkerRegion = new TextureRegion(assets.get(AssetManager.ENEMY_MARKER));

        // --- Placeholders for animations ---
        TextureRegion placeholder = new TextureRegion(assets.get(AssetManager.MARKER_DOT));
        explosionAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.1f, placeholder);
        hitAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.05f, enemyMarkerRegion);

        // --- Animations (from packed atlases) ---
        com.badlogic.gdx.graphics.g2d.TextureAtlas recruitAttackAtlas = assets.getAtlas(AssetManager.RECRUIT_ATTACK_ATLAS);
        com.badlogic.gdx.utils.Array<TextureRegion> recruitAttackFrames = new com.badlogic.gdx.utils.Array<>();
        for (int i = 1; i <= 7; i++)
            recruitAttackFrames.add(recruitAttackAtlas.findRegion("warrior_skill4_frame" + i));
        recruitAttackAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.08f, recruitAttackFrames);

        com.badlogic.gdx.graphics.g2d.TextureAtlas tankAttackAtlas = assets.getAtlas(AssetManager.TANK_ATTACK_ATLAS);
        com.badlogic.gdx.utils.Array<TextureRegion> tankAttackFrames = new com.badlogic.gdx.utils.Array<>();
        for (int i = 0; i <= 13; i++)
            tankAttackFrames.add(tankAttackAtlas.findRegion(String.format("frame%04d", i)));
        tankAttackAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.06f, tankAttackFrames);

        com.badlogic.gdx.graphics.g2d.TextureAtlas nuclearAttackAtlas = assets.getAtlas(AssetManager.NUCLEAR_ATTACK_ATLAS);
        com.badlogic.gdx.utils.Array<TextureRegion> nuclearAttackFrames = new com.badlogic.gdx.utils.Array<>();
        for (int i = 0; i <= 12; i++)
            nuclearAttackFrames.add(nuclearAttackAtlas.findRegion(String.format("frame%04d", i)));
        nuclearAttackAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.07f, nuclearAttackFrames);

        gunNozzleFlashAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.05f,
                assets.getAtlas(AssetManager.GUN_NOZZLE_ATLAS).findRegions("frame"));

        com.badlogic.gdx.graphics.g2d.TextureAtlas runAtlas = assets.getAtlas(AssetManager.RECRUIT_RUN_ATLAS);
        com.badlogic.gdx.utils.Array<TextureRegion> runFrames = new com.badlogic.gdx.utils.Array<>();
        for (int i = 0; i <= 11; i++)
            runFrames.add(runAtlas.findRegion(String.format("recruit_run%08d", 86400 + i)));
        recruitRunAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.033f, runFrames);

        com.badlogic.gdx.graphics.g2d.TextureAtlas rangerRunAtlas = assets.getAtlas(AssetManager.RANGER_RUN_ATLAS);
        com.badlogic.gdx.utils.Array<TextureRegion> rangerRunFrames = new com.badlogic.gdx.utils.Array<>();
        for (int i = 0; i <= 11; i++)
            rangerRunFrames.add(rangerRunAtlas.findRegion(String.format("ranger_run%08d", 86445 + i)));
        rangerRunAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.033f, rangerRunFrames);

        com.badlogic.gdx.graphics.g2d.TextureAtlas sniperRunAtlas = assets.getAtlas(AssetManager.SNIPER_RUN_ATLAS);
        com.badlogic.gdx.utils.Array<TextureRegion> sniperRunFrames = new com.badlogic.gdx.utils.Array<>();
        for (int i = 0; i <= 11; i++)
            sniperRunFrames.add(sniperRunAtlas.findRegion(String.format("sniper_run%08d", 86446 + i)));
        sniperRunAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.033f, sniperRunFrames);

        com.badlogic.gdx.graphics.g2d.TextureAtlas juggernautJumpAtlas = assets.getAtlas(AssetManager.JUGGERNAUT_JUMP_ATLAS);
        com.badlogic.gdx.utils.Array<TextureRegion> juggernautJumpFrames = new com.badlogic.gdx.utils.Array<>();
        for (int i = 0; i <= 20; i++)
            juggernautJumpFrames.add(juggernautJumpAtlas.findRegion(String.format("juggernaut_jump%08d", 86484 + i)));
        juggernautJumpAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.033f, juggernautJumpFrames);

        com.badlogic.gdx.graphics.g2d.TextureAtlas helicopterMoveAtlas = assets.getAtlas(AssetManager.HELICOPTER_MOVE_ATLAS);
        com.badlogic.gdx.utils.Array<TextureRegion> helicopterMoveFrames = new com.badlogic.gdx.utils.Array<>();
        for (int i = 0; i <= 11; i++)
            helicopterMoveFrames.add(helicopterMoveAtlas.findRegion(String.format("helicopter-move%08d", 86527 + i)));
        helicopterMoveAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.033f, helicopterMoveFrames);

        com.badlogic.gdx.graphics.g2d.TextureAtlas tankIdleAtlas = assets.getAtlas(AssetManager.TANK_IDLE_ATLAS);
        com.badlogic.gdx.utils.Array<TextureRegion> tankIdleFrames = new com.badlogic.gdx.utils.Array<>();
        for (int i = 0; i <= 23; i++)
            tankIdleFrames.add(tankIdleAtlas.findRegion(String.format("tank-idle%08d", 86561 + i)));
        tankIdleAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.0833f, tankIdleFrames);

        com.badlogic.gdx.graphics.g2d.TextureAtlas juggernautBoostersAtlas = assets.getAtlas(AssetManager.JUGGERNAUT_BOOSTERS_ATLAS);
        com.badlogic.gdx.utils.Array<TextureRegion> juggernautBoostersFrames = new com.badlogic.gdx.utils.Array<>();
        for (int i = 0; i <= 9; i++)
            juggernautBoostersFrames.add(juggernautBoostersAtlas.findRegion(String.format("juggernaut-boosters%08d", 86509 + i)));
        juggernautBoostersAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.033f, juggernautBoostersFrames);

        com.badlogic.gdx.graphics.g2d.TextureAtlas droneMovingAtlas = assets.getAtlas(AssetManager.DRONE_MOVING_ATLAS);
        com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion> droneRegions = droneMovingAtlas.getRegions();
        droneRegions.sort((a, b) -> a.name.compareTo(b.name));
        droneMovingAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(2f / 24f, droneRegions, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
    }

    public com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> getRecruitRunAnim() {
        return recruitRunAnim;
    }

    public com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> getRangerRunAnim() {
        return rangerRunAnim;
    }

    public com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> getSniperRunAnim() {
        return sniperRunAnim;
    }

    public com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> getJuggernautJumpAnim() {
        return juggernautJumpAnim;
    }

    public com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> getHelicopterMoveAnim() {
        return helicopterMoveAnim;
    }

    public com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> getTankIdleAnim() {
        return tankIdleAnim;
    }

    public com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> getJuggernautBoostersAnim() {
        return juggernautBoostersAnim;
    }

    public com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> getDroneMovingAnim() {
        return droneMovingAnim;
    }

    /** Blue movement-range marker. */
    public void createMovementMarker(int x, int y) {
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(markerRegion));
        entity.add(new TypeComponent(TypeComponent.Type.MARKER));
        engine.addEntity(entity);
    }

    public void createTransformMarker(int x, int y) {
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(markerRegion));
        entity.add(new TypeComponent(TypeComponent.Type.TRANSFORM_MARKER));
        engine.addEntity(entity);
    }

    /**
     * Red attack-range marker. Rendered by UnitRenderSystem.
     * Uses the new enemy_marker texture.
     */
    public void createAttackMarker(int x, int y) {
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(enemyMarkerRegion));
        entity.add(new TypeComponent(TypeComponent.Type.ATTACK_MARKER));
        engine.addEntity(entity);
    }

    /**
     * A short-lived entity that carries floating combat feedback.
     *
     * @param text      damage amount or "BLOCKED"
     * @param worldX    iso-world X of the target tile
     * @param worldY    iso-world Y of the target tile (base; system drifts up)
     * @param isCounter true when this feedback represents a counterattack
     */
    public Entity createFloatingText(String text, float worldX, float worldY, FloatingTextComponent.Type type) {
        Entity entity = engine.createEntity();
        entity.add(new FloatingTextComponent(text, worldX, worldY, type));
        engine.addEntity(entity);
        return entity;
    }

    public void createExplosion(int x, int y) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 4)); // Above units
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));
        e.add(new TextureComponent(markerRegion)); // Placeholder base

        SpriteAnimationComponent anim = new SpriteAnimationComponent();
        anim.animation = explosionAnim;
        anim.duration = 0.5f;
        anim.autoRemove = true;
        anim.drawWidth = 9f;
        anim.drawHeight = 10f;
        e.add(anim);

        engine.addEntity(e);
    }

    public void createHit(int x, int y) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 4));
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));
        e.add(new TextureComponent(markerRegion));

        SpriteAnimationComponent anim = new SpriteAnimationComponent();
        anim.animation = hitAnim;
        anim.duration = 0.2f;
        anim.autoRemove = true;
        e.add(anim);

        engine.addEntity(e);
    }

    public void createRecruitAttack(int x, int y) {
        // Recruit is a standard 18x20 tile size
        createEffect(x, y, recruitAttackAnim, 0.6f, 12f, 14f);
    }

    public void createTankAttack(int x, int y) {
        // Tank is wider: 24x18
        createEffect(x, y, tankAttackAnim, 0.8f, 24f, 18f, 0f, 5f);
    }

    public void createNuclearAttack(int x, int y) {
        // Nuclear (Super Units) is much larger: 40x40
        createEffect(x, y, nuclearAttackAnim, 0.9f, 30f, 30f, 0f, 13f);
    }

    public void createDramaticExplosion(int x, int y) {
        // 1. Main large explosion on target tile
        createNuclearAttack(x, y);

        // 2. Adjacent little explosions on THE SAME TILE (approx 0.4s delay)
        // Lowered y-offsets as requested (from 10f base to 6f base)
        float[][] offsets = {
                { -4f, -2f }, { 4f, -2f }, { -4f, 2f }, { 4f, 2f }
        };
        for (float[] off : offsets) {
            createDelayedSmallExplosion(x, y, off[0], off[1] + 6f);
        }

        // 3. Surrounding tile explosions (all 8 neighbors in the 3x3 damage scope)
        int[][] neighbors = {
                { x + 1, y }, { x - 1, y }, { x, y + 1 }, { x, y - 1 },
                { x + 1, y + 1 }, { x + 1, y - 1 }, { x - 1, y + 1 }, { x - 1, y - 1 }
        };
        for (int[] n : neighbors) {
            // Smaller explosions on surrounding tiles with same delay
            createDelayedSmallExplosion(n[0], n[1], 0, 6f);
        }
    }

    private void createDelayedSmallExplosion(int x, int y, float offsetX, float offsetY) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 4));
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));

        SpriteAnimationComponent anim = new SpriteAnimationComponent();
        anim.animation = nuclearAttackAnim;
        anim.duration = 0.9f;
        anim.drawWidth = 12f; // Smaller scale (40%)
        anim.drawHeight = 12f;
        anim.worldOffsetX = offsetX;
        anim.worldOffsetY = offsetY;
        anim.stateTime = -0.4f; // 0.4s delay
        anim.autoRemove = true;
        e.add(anim);

        engine.addEntity(e);
    }

    public void createMuzzleFlash(int x, int y, float offsetX, float offsetY) {
        // Muzzle flash is very small: 3x3
        createEffect(x, y, gunNozzleFlashAnim, 0.45f, 3f, 3f, offsetX, offsetY);
    }

    private void createEffect(int x, int y, com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> animation,
            float duration, float width, float height) {
        createEffect(x, y, animation, duration, width, height, 0, 0);
    }

    private void createEffect(int x, int y, com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> animation,
            float duration, float width, float height, float offsetX, float offsetY) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 4));
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));

        SpriteAnimationComponent anim = new SpriteAnimationComponent();
        anim.animation = animation;
        anim.duration = duration;
        anim.drawWidth = width;
        anim.drawHeight = height;
        anim.worldOffsetX = offsetX;
        anim.worldOffsetY = offsetY;
        anim.autoRemove = true;
        e.add(anim);

        engine.addEntity(e);
    }

    // -------------------------------------------------------------------------
    // Internal helpers for world-space coordinate conversion
    // -------------------------------------------------------------------------

    /** Convert grid coords to isometric world X. */
    public static float gridToIsoX(int gx, int gy) {
        return (gx - gy) * (GameConfig.TILE_WIDTH / 2.0f);
    }

    /**
     * Convert grid coords to isometric world Y.
     * The +20 offset lifts the text above the unit sprite.
     */
    public static float gridToIsoY(int gx, int gy) {
        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;
        return (gx + gy) * (GameConfig.TILE_HEIGHT / 2.0f) - yOffset + 20f;
    }
}