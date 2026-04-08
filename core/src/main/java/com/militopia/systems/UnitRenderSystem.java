package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.militopia.components.*;
import com.militopia.config.DisplayAssetConfig;
import com.militopia.config.GameConfig;
import com.militopia.config.StructureType;
import com.militopia.config.UnitType;
import com.militopia.map.MapGenerator;
import com.militopia.utils.ZComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnitRenderSystem extends EntitySystem {

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private ImmutableArray<Entity> entities;
    private ZComparator comparator;

    private final MapGenerator.GameMap gameMap;
    private boolean fogEnabled = true;
    private int activePlayer = 1;

    private int selectedX = -1, selectedY = -1;
    private int bouncingX = -1, bouncingY = -1;
    private float bounceTimer = 0;

    private com.badlogic.gdx.graphics.g2d.TextureRegion shadowRegion;
    private com.badlogic.gdx.graphics.g2d.TextureRegion invincibleRed, invincibleBlue;
    private com.badlogic.gdx.graphics.g2d.TextureRegion digInRegion;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> recruitRunAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> rangerRunAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> sniperRunAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> juggernautJumpAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> helicopterMoveAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> juggernautBoostersAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> droneMovingAnim;

    public void setDroneMovingAnim(
            com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim) {
        this.droneMovingAnim = anim;
    }

    public void setInvincibleRegions(com.badlogic.gdx.graphics.g2d.TextureRegion red,
            com.badlogic.gdx.graphics.g2d.TextureRegion blue) {
        this.invincibleRed = red;
        this.invincibleBlue = blue;
    }

    public void setShadowRegion(com.badlogic.gdx.graphics.g2d.TextureRegion r) {
        this.shadowRegion = r;
    }

    public void setDigInRegion(com.badlogic.gdx.graphics.g2d.TextureRegion r) {
        this.digInRegion = r;
    }

    public void setRecruitRunAnim(
            com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim) {
        this.recruitRunAnim = anim;
    }

    public void setRangerRunAnim(
            com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim) {
        this.rangerRunAnim = anim;
    }

    public void setSniperRunAnim(
            com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim) {
        this.sniperRunAnim = anim;
    }

    public void setJuggernautJumpAnim(
            com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim) {
        this.juggernautJumpAnim = anim;
    }

    public void setHelicopterMoveAnim(
            com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim) {
        this.helicopterMoveAnim = anim;
    }

    public void setJuggernautBoostersAnim(
            com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim) {
        this.juggernautBoostersAnim = anim;
    }

    // Reusable removal list (avoids per-frame allocation accumulation)
    private final Array<Entity> toRemove = new Array<>();

    public UnitRenderSystem(SpriteBatch batch, MapGenerator.GameMap map, BitmapFont font) {
        this.batch = batch;
        this.gameMap = map;
        this.font = font;
        this.comparator = new ZComparator();
        this.shapeRenderer = new ShapeRenderer();
        this.priority = 1;
    }

    public void setPlayer(int playerID) {
        this.activePlayer = playerID;
    }

    public void setFogEnabled(boolean enabled) {
        this.fogEnabled = enabled;
    }

    public void updateState(int selX, int selY, int bX, int bY, float bTimer) {
        this.selectedX = selX;
        this.selectedY = selY;
        this.bouncingX = bX;
        this.bouncingY = bY;
        this.bounceTimer = bTimer;
    }

    @Override
    public void addedToEngine(com.badlogic.ashley.core.Engine engine) {
        entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class)
                .one(TextureComponent.class, SpriteAnimationComponent.class, com.militopia.components.IdleAnimationComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        // Sorted sprite pass
        List<Entity> sorted = new ArrayList<>();
        for (Entity e : entities)
            sorted.add(e);
        Collections.sort(sorted, comparator);

        batch.begin();
        for (Entity e : sorted) {
            drawEntity(e, deltaTime);
        }
        batch.end();

        drawBaseOverlay();
        drawFloatingTexts(deltaTime);

        // Remove dead + expired floating-text entities (done after all rendering)
        for (Entity e : toRemove) {
            getEngine().removeEntity(e);
        }
        toRemove.clear();
    }

    // -------------------------------------------------------------------------
    // Entity drawing
    // -------------------------------------------------------------------------

    private void drawEntity(Entity e, float deltaTime) {
        GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
        TextureComponent tex = e.getComponent(TextureComponent.class);
        MovementComponent move = e.getComponent(MovementComponent.class);
        TypeComponent typeC = e.getComponent(TypeComponent.class);
        StatsComponent stats = e.getComponent(StatsComponent.class);
        DeathAnimComponent death = e.getComponent(DeathAnimComponent.class);
        SpriteAnimationComponent spriteAnim = e.getComponent(SpriteAnimationComponent.class);
        com.militopia.components.AnimationComponent animComp = e
                .getComponent(com.militopia.components.AnimationComponent.class);
        com.militopia.components.IdleAnimationComponent idleAnim = e
                .getComponent(com.militopia.components.IdleAnimationComponent.class);
        if (idleAnim != null) {
            if (idleAnim.looping) {
                idleAnim.stateTime += deltaTime;
            } else if (idleAnim.paused) {
                idleAnim.pauseTimer -= deltaTime;
                if (idleAnim.pauseTimer <= 0f) {
                    idleAnim.paused = false;
                    idleAnim.stateTime = 0f;
                }
            } else {
                idleAnim.stateTime += deltaTime;
                if (idleAnim.animation != null && idleAnim.stateTime >= idleAnim.animation.getAnimationDuration()) {
                    idleAnim.paused = true;
                    idleAnim.pauseTimer = MathUtils.random(2f, 3f);
                    idleAnim.stateTime = 0f;
                }
            }
        }

        boolean isMarker = (typeC.type == TypeComponent.Type.MARKER
                || typeC.type == TypeComponent.Type.TRANSFORM_MARKER);
        boolean isAttackMarker = (typeC.type == TypeComponent.Type.ATTACK_MARKER);

        // Fog and Stealth culling
        if (!isMarker && !isAttackMarker) {
            boolean isMyUnit = (stats != null && stats.owner == activePlayer);

            if (fogEnabled) {
                if (pos.x >= 0 && pos.x < gameMap.width && pos.y >= 0 && pos.y < gameMap.height) {
                    if (!gameMap.visibleTiles[pos.x][pos.y] && !isMyUnit) {
                        return;
                    }
                }
            }

            // Stealth check (Enemy view only)
            if (!isMyUnit && stats != null) {
                AbilitiesComponent abilities = e.getComponent(AbilitiesComponent.class);
                boolean isStealth = (abilities != null && abilities.isCloaked);

                // Sniper Camouflage: Grass/Forest/Mountain?
                // The prompt says "forest/mountains". We use Tree objects or Mountain terrain.
                if (stats.unitType == UnitType.SNIPER) {
                    MapGenerator.TerrainType terrain = gameMap.terrain[pos.x][pos.y];
                    MapGenerator.ObjectType obj = gameMap.objects[pos.x][pos.y];
                    if (terrain == MapGenerator.TerrainType.MOUNTAIN || obj == MapGenerator.ObjectType.TREE
                            || obj == MapGenerator.ObjectType.RUINS || obj == MapGenerator.ObjectType.MOUNTAIN_OBJ) {
                        isStealth = true;
                    }
                }

                // If unit has already acted/attacked, it is revealed
                if (stats.hasActed || (abilities != null && abilities.isCloakBroken)) {
                    isStealth = false;
                }

                if (isStealth && !gameMap.detectedTiles[pos.x][pos.y]) {
                    // Hidden!
                    return;
                }
            }
        }

        // --- Death animation ---
        if (death != null) {
            death.timer += deltaTime;
            if (death.timer >= DeathAnimComponent.DURATION) {
                death.done = true;
                toRemove.add(e);
                return; // don't draw after fully faded
            }
            float progress = death.timer / DeathAnimComponent.DURATION;
            float alpha = 1f - progress;
            // Flash red: alternate between red and white every 0.1s
            boolean flashRed = ((int) (death.timer / 0.1f) % 2 == 0);
            if (flashRed) {
                batch.setColor(1f, 0f, 0f, alpha);
            } else {
                batch.setColor(1f, 1f, 1f, alpha);
            }
            // --- Iso position (with movement lerp during death) ---
            float isoX, isoY;
            if (move != null) {
                float moveAlpha = Math.min(move.time / move.duration, 1.0f);
                float startIsoX = (move.startX - move.startY) * (GameConfig.TILE_WIDTH / 2.0f);
                float startIsoY = (move.startX + move.startY) * (GameConfig.TILE_HEIGHT / 2.0f);
                float endIsoX = (move.targetX - move.targetY) * (GameConfig.TILE_WIDTH / 2.0f);
                float endIsoY = (move.targetX + move.targetY) * (GameConfig.TILE_HEIGHT / 2.0f);
                isoX = MathUtils.lerp(startIsoX, endIsoX, moveAlpha) + pos.visualOffsetX;
                isoY = MathUtils.lerp(startIsoY, endIsoY, moveAlpha) + pos.visualOffsetY;
            } else {
                isoX = (pos.x - pos.y) * (GameConfig.TILE_WIDTH / 2.0f) + pos.visualOffsetX;
                isoY = (pos.x + pos.y) * (GameConfig.TILE_HEIGHT / 2.0f) + pos.visualOffsetY;
            }

            String deathKey = (stats != null) ? stats.unitTypeKey : "";
            DisplayAssetConfig.AssetData deathCfg = DisplayAssetConfig.get(deathKey);
            float xOff = (deathCfg.width - GameConfig.TILE_WIDTH) / 2f;
            float yOff = (deathCfg.height - GameConfig.TILE_HEIGHT) / 2f;
            batch.draw(tex.region, isoX - xOff + deathCfg.offsetX, isoY - yOff + deathCfg.offsetY, deathCfg.width,
                    deathCfg.height);
            batch.setColor(Color.WHITE);
            return;
        }

        // --- Iso position (with movement lerp) ---
        float isoX, isoY;
        if (move != null) {
            float alpha = Math.min(move.time / move.duration, 1.0f);
            float startIsoX = (move.startX - move.startY) * (GameConfig.TILE_WIDTH / 2.0f);
            float startIsoY = (move.startX + move.startY) * (GameConfig.TILE_HEIGHT / 2.0f);
            float endIsoX = (move.targetX - move.targetY) * (GameConfig.TILE_WIDTH / 2.0f);
            float endIsoY = (move.targetX + move.targetY) * (GameConfig.TILE_HEIGHT / 2.0f);
            isoX = MathUtils.lerp(startIsoX, endIsoX, alpha) + pos.visualOffsetX;
            isoY = MathUtils.lerp(startIsoY, endIsoY, alpha) + pos.visualOffsetY;
        } else {
            isoX = (pos.x - pos.y) * (GameConfig.TILE_WIDTH / 2.0f) + pos.visualOffsetX;
            isoY = (pos.x + pos.y) * (GameConfig.TILE_HEIGHT / 2.0f) + pos.visualOffsetY;
        }

        // Bounce animation
        float animY = 0;
        if (move != null && move.jumpArcHeight > 0) {
            float progress = Math.min(move.time / move.duration, 1.0f);
            animY = (float) Math.sin(progress * Math.PI) * move.jumpArcHeight;
        } else if (move == null && pos.x == bouncingX && pos.y == bouncingY) {
            float progress = bounceTimer / GameConfig.BOUNCE_DURATION;
            animY = (float) Math.sin(progress * Math.PI) * GameConfig.BOUNCE_HEIGHT;
        }

        String assetKey = (stats != null) ? stats.unitTypeKey : "";
        if (stats != null && (assetKey.equals("BASE_P1") || assetKey.equals("BASE_P2"))) {
            assetKey = "BASE_" + Math.min(stats.level, 10);
        }
        DisplayAssetConfig.AssetData cfg = DisplayAssetConfig.get(assetKey);
        float drawW = cfg.width;
        float drawH = cfg.height;
        float xOffset = (drawW - GameConfig.TILE_WIDTH) / 2f;
        float yOffset = (drawH - GameConfig.TILE_HEIGHT) / 2f;
        float verticalOff = isMarker || isAttackMarker ? 5f : cfg.offsetY;

        // Shadow — drawn on the ground plane beneath units
        boolean isAnimal = e.getComponent(AnimalComponent.class) != null;
        if (shadowRegion != null && (typeC.type == TypeComponent.Type.UNIT || isAnimal)) {
            float shadowW = cfg.shadowW;
            float shadowH = cfg.shadowH;
            batch.setColor(0f, 0f, 0f, GameConfig.SHADOW_ALPHA);
            batch.draw(shadowRegion,
                    isoX - shadowW / 2f + GameConfig.TILE_WIDTH / 2f + GameConfig.SHADOW_OFFSET_X + cfg.shadowOffsetX,
                    isoY + GameConfig.SHADOW_OFFSET_Y + cfg.shadowOffsetY + animY,
                    shadowW, shadowH);
            batch.setColor(Color.WHITE);
        }

        // --- Colour tinting ---
        // (unreachable/tintAlpha logic removed: Recon Drones now 100% opacity)

        boolean isJumpingEarly = (animComp != null
                && animComp.type == com.militopia.components.AnimationComponent.Type.JUMP
                && stats != null && stats.unitType == UnitType.JUGGERNAUT
                && juggernautJumpAnim != null);

        if (isAttackMarker) {
            // New enemy marker (draw with original colours)
            batch.setColor(Color.WHITE);
        } else if (isMarker) {
            batch.setColor(Color.WHITE);
        } else if (typeC.type == TypeComponent.Type.UNIT) {
            if (!GameConfig.TESTING_MODE && stats != null && stats.hasActed && move == null && !isJumpingEarly) {
                if (stats.owner == 1)
                    batch.setColor(0.3f, 0.3f, 0.6f, 1.0f);
                else if (stats.owner == 2)
                    batch.setColor(0.6f, 0.3f, 0.3f, 1.0f);
                else
                    batch.setColor(0.3f, 0.3f, 0.3f, 1.0f);
            } else if (stats != null && stats.owner == 2) {
                batch.setColor(1.0f, 0.6f, 0.6f, 1.0f);
            } else if (stats != null && stats.owner == 1) {
                batch.setColor(0.6f, 0.6f, 1.0f, 1.0f);
            } else {
                batch.setColor(1f, 1f, 1f, 1.0f);
            }
        } else {
            batch.setColor(Color.WHITE);
        }

        boolean isMovingRecruit = (move != null && stats != null && stats.unitType == UnitType.RECRUIT
                && recruitRunAnim != null);
        boolean isMovingRanger = (move != null && stats != null && stats.unitType == UnitType.RANGER
                && rangerRunAnim != null);
        boolean isMovingSniper = (move != null && stats != null && stats.unitType == UnitType.SNIPER
                && sniperRunAnim != null);
        boolean isMovingApache = (move != null && stats != null && stats.unitType == UnitType.APACHE
                && helicopterMoveAnim != null);
        boolean isMovingDrone = (move != null && stats != null && stats.unitType == UnitType.RECON_DRONE
                && droneMovingAnim != null);
        com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> activeRunAnim = isMovingRecruit
                ? recruitRunAnim
                : isMovingRanger ? rangerRunAnim
                        : isMovingSniper ? sniperRunAnim
                                : isMovingApache ? helicopterMoveAnim
                                        : isMovingDrone ? droneMovingAnim
                                                : null;

        boolean isJumping = isJumpingEarly;

        if (tex != null && tex.region != null && activeRunAnim == null && !isJumping
                && (idleAnim == null || idleAnim.overlayOnly)) {
            batch.draw(tex.region,
                    isoX - xOffset + cfg.offsetX,
                    isoY - yOffset + verticalOff + animY,
                    drawW, drawH);
        }

        if (idleAnim != null && idleAnim.animation != null && !(idleAnim.overlayOnly && idleAnim.paused)) {
            float idleT = (idleAnim.looping || !idleAnim.paused) ? idleAnim.stateTime : 0f;
            com.badlogic.gdx.graphics.g2d.TextureRegion idleFrame = idleAnim.animation.getKeyFrame(idleT, idleAnim.looping);
            if (idleFrame != null) {
                float frameX = isoX - xOffset + cfg.offsetX + idleAnim.drawOffsetX;
                float frameW = drawW;
                if (idleAnim.respectsFacing && tex != null && tex.region != null && tex.region.isFlipX()) {
                    frameX += drawW;
                    frameW = -drawW;
                }
                batch.draw(idleFrame, frameX, isoY - yOffset + verticalOff + animY + idleAnim.drawOffsetY, frameW, drawH);
            }
        }

        if (activeRunAnim != null) {
            com.badlogic.gdx.graphics.g2d.TextureRegion runFrame = activeRunAnim.getKeyFrame(move.time, true);
            if (runFrame != null) {
                boolean facingLeft = tex != null && tex.region != null && tex.region.isFlipX();
                float frameX = isoX - xOffset + cfg.offsetX;
                float frameW = drawW;
                if (facingLeft) {
                    frameX += drawW;
                    frameW = -drawW;
                }
                batch.draw(runFrame, frameX, isoY - yOffset + verticalOff + animY, frameW, drawH);
            }
        }

        // --- Juggernaut boosters (drawn behind/below the jump sprite) ---
        if (isJumping && juggernautBoostersAnim != null && animComp.stateTime >= 3 * 0.033f) {
            float boosterStateTime = animComp.stateTime - 3 * 0.033f;
            com.badlogic.gdx.graphics.g2d.TextureRegion boosterFrame = juggernautBoostersAnim
                    .getKeyFrame(boosterStateTime, false);
            if (boosterFrame != null) {
                batch.setColor(Color.WHITE);
                boolean facingLeft = tex != null && tex.region != null && tex.region.isFlipX();
                float frameX = isoX - xOffset + cfg.offsetX + GameConfig.JUGGERNAUT_BOOSTERS_OFFSET_X;
                float frameW = drawW;
                if (facingLeft) {
                    frameX += drawW;
                    frameW = -drawW;
                }
                batch.draw(boosterFrame, frameX, isoY - yOffset + verticalOff + animY + GameConfig.JUGGERNAUT_BOOSTERS_OFFSET_Y,
                        frameW, drawH);
            }
        }

        if (isJumping) {
            com.badlogic.gdx.graphics.g2d.TextureRegion jumpFrame = juggernautJumpAnim.getKeyFrame(animComp.stateTime,
                    false);
            if (jumpFrame != null) {
                boolean facingLeft = tex != null && tex.region != null && tex.region.isFlipX();
                float frameX = isoX - xOffset + cfg.offsetX;
                float frameW = drawW;
                if (facingLeft) {
                    frameX += drawW;
                    frameW = -drawW;
                }
                batch.draw(jumpFrame, frameX, isoY - yOffset + verticalOff + animY, frameW, drawH);
            }
        }

        if (spriteAnim != null && spriteAnim.animation != null && spriteAnim.stateTime >= 0) {
            com.badlogic.gdx.graphics.g2d.TextureRegion frame = spriteAnim.animation.getKeyFrame(spriteAnim.stateTime,
                    spriteAnim.loop);
            if (frame != null) {
                float dW = (spriteAnim.drawWidth > 0) ? spriteAnim.drawWidth : GameConfig.DRAW_WIDTH;
                float dH = (spriteAnim.drawHeight > 0) ? spriteAnim.drawHeight : GameConfig.DRAW_HEIGHT;

                // Center the sprite on the tile diamond center (isoY + TILE_HEIGHT/2).
                // Pure effect entities have no TextureComponent so don't inherit the unit's
                // verticalOff — that offset is for lifting unit sprites above the ground.
                float effectVerticalOff = (tex != null) ? verticalOff : GameConfig.TILE_HEIGHT / 2f;
                float currentXOffset = (dW - GameConfig.TILE_WIDTH) / 2f;
                float currentYOffset = (dH - GameConfig.TILE_HEIGHT) / 2f;

                batch.draw(frame,
                        isoX - currentXOffset + spriteAnim.worldOffsetX,
                        isoY - currentYOffset + effectVerticalOff + animY + spriteAnim.worldOffsetY,
                        dW, dH);
            }
        }

        batch.setColor(Color.WHITE);

        // Dig-In sandbag VFX: drawn in front of the unit sprite
        if (digInRegion != null && typeC.type == TypeComponent.Type.UNIT
                && stats != null && stats.unitType == UnitType.RECRUIT) {
            AbilitiesComponent abilities = e.getComponent(AbilitiesComponent.class);
            if (abilities != null && abilities.isDiggingIn) {
                DisplayAssetConfig.AssetData sbCfg = DisplayAssetConfig.get(AbilitiesComponent.KEY_DIG_IN);
                batch.draw(digInRegion,
                        isoX - sbCfg.width / 2f + GameConfig.TILE_WIDTH / 2f + sbCfg.offsetX,
                        isoY + sbCfg.offsetY + animY,
                        sbCfg.width, sbCfg.height);
            }
        }

        // Selection glow
        if ((tex != null || idleAnim != null) && !isMarker && !isAttackMarker && pos.x == selectedX && pos.y == selectedY
                && activeRunAnim == null && !isJumping) {
            Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
            batch.setColor(0.4f, 0.4f, 0.4f, 1f);
            com.badlogic.gdx.graphics.g2d.TextureRegion glowRegion = (idleAnim != null && idleAnim.animation != null)
                    ? idleAnim.animation.getKeyFrame((idleAnim.looping || !idleAnim.paused) ? idleAnim.stateTime : 0f, idleAnim.looping)
                    : (tex != null ? tex.region : null);
            if (glowRegion != null) batch.draw(glowRegion,
                    isoX - xOffset + cfg.offsetX,
                    isoY - yOffset + verticalOff + animY,
                    drawW, drawH);
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(Color.WHITE);
        }

        // --- Invincibility/Cloaking Indicator ---
        boolean isMyUnit = (stats != null && stats.owner == activePlayer);
        if (isMyUnit && tex != null && stats != null && (stats.unitType == UnitType.SNIPER
                || stats.unitType == UnitType.B2 || stats.unitType == UnitType.SUBMARINE)) {
            com.badlogic.gdx.graphics.g2d.TextureRegion indicator = (stats.owner == 1) ? invincibleBlue : invincibleRed;
            if (indicator != null) {
                AbilitiesComponent abilities = e.getComponent(AbilitiesComponent.class);
                boolean isCurrentlyCloaked = (abilities != null && abilities.isCloaked && !abilities.isCloakBroken);

                // Sniper dynamic stealth check
                if (stats.unitType == UnitType.SNIPER && (abilities == null || !abilities.isCloakBroken)) {
                    MapGenerator.TerrainType terrain = gameMap.terrain[pos.x][pos.y];
                    MapGenerator.ObjectType obj = gameMap.objects[pos.x][pos.y];
                    if (terrain == MapGenerator.TerrainType.MOUNTAIN || obj == MapGenerator.ObjectType.TREE
                            || obj == MapGenerator.ObjectType.RUINS || obj == MapGenerator.ObjectType.MOUNTAIN_OBJ) {
                        isCurrentlyCloaked = true;
                    }
                }

                // Opacity: 100% if currently cloaked, 50% if cloak is broken or sniper in the
                // open
                float alpha = isCurrentlyCloaked ? 1.0f : 0.5f;

                // Position: upper right of the unit sprite
                float indicatorSize = 2.5f;
                float indicatorX = isoX - xOffset + cfg.offsetX + drawW; // Shifted right
                float indicatorY = isoY - yOffset + verticalOff + animY + drawH - 1f; // Shifted higher

                batch.setColor(1f, 1f, 1f, alpha);
                batch.draw(indicator, indicatorX, indicatorY, indicatorSize, indicatorSize);
                batch.setColor(Color.WHITE);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Floating damage text
    // -------------------------------------------------------------------------

    private void drawFloatingTexts(float deltaTime) {
        ImmutableArray<Entity> floaters = getEngine().getEntitiesFor(
                Family.all(FloatingTextComponent.class).get());

        if (floaters.size() == 0)
            return;

        batch.begin();

        float originalScaleX = font.getData().scaleX;
        float originalScaleY = font.getData().scaleY;
        font.getData().setScale(0.22f);

        for (Entity e : floaters) {
            FloatingTextComponent ft = e.getComponent(FloatingTextComponent.class);

            // Colour based on text type
            switch (ft.type) {
                case BLOCKED:
                    font.setColor(1f, 0.85f, 0f, ft.alpha); // Gold
                    break;
                case XP:
                    font.setColor(0.2f, 0.8f, 1f, ft.alpha); // Light Blue/Cyan
                    break;
                case FUNDING:
                    font.setColor(0.2f, 1f, 0.2f, ft.alpha); // Green
                    break;
                case DAMAGE:
                default:
                    font.setColor(1f, 0.2f, 0.2f, ft.alpha); // Red
                    break;
            }

            GlyphLayout layout = new GlyphLayout(font, ft.text);
            font.draw(batch, ft.text,
                    ft.worldX - layout.width / 2f,
                    ft.worldY);
        }

        font.getData().setScale(originalScaleX, originalScaleY);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Base overlay (unchanged from original)
    // -------------------------------------------------------------------------

    private void drawBaseOverlay() {
        if (selectedX == -1 || selectedY == -1)
            return;
        Entity selectedEntity = null;
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (pos.x == selectedX && pos.y == selectedY) {
                if (e.getComponent(TypeComponent.class).type == TypeComponent.Type.UNIT) {
                    selectedEntity = e;
                    break;
                }
                selectedEntity = e;
            }
        }
        if (selectedEntity != null) {
            StatsComponent stats = selectedEntity.getComponent(StatsComponent.class);
            TypeComponent type = selectedEntity.getComponent(TypeComponent.class);
            if (stats != null && type.type == TypeComponent.Type.OBJECT
                    && (stats.owner == 1 || stats.owner == 2)
                    && stats.unitTypeKey != null && stats.unitTypeKey.startsWith("BASE")) {
                float isoX = (selectedX - selectedY) * (GameConfig.TILE_WIDTH / 2.0f);
                float isoY = (selectedX + selectedY) * (GameConfig.TILE_HEIGHT / 2.0f);
                float xOff = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
                float yOff = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;
                float dX = isoX - xOff + GameConfig.DRAW_WIDTH / 2f;
                float dY = isoY - yOff + 15;
                drawXPBar(dX, dY - 8, stats);
                // drawBaseName removed per user request
            }
        }
    }

    private void drawXPBar(float x, float y, StatsComponent stats) {
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float width = 32f, height = 4f, radius = 2f;
        float progress = MathUtils.clamp(stats.currentBaseXP / stats.maxBaseXP, 0, 1);
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        drawRoundedRect(x - width / 2, y, width, height, radius);
        if (progress > 0) {
            shapeRenderer.setColor(stats.owner == 1 ? new Color(0.2f, 0.4f, 1f, 1f) : new Color(1f, 0.2f, 0.2f, 1f));
            float barWidth = Math.max(width * progress, 2f);
            drawRoundedRect(x - width / 2, y, barWidth, height, radius);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
    }

    private void drawRoundedRect(float x, float y, float width, float height, float radius) {
        float r = Math.min(radius, Math.min(width / 2, height / 2));
        shapeRenderer.rect(x + r, y + r, width - 2 * r, height - 2 * r);
        shapeRenderer.rect(x + r, y, width - 2 * r, r);
        shapeRenderer.rect(x + width - r, y + r, r, height - 2 * r);
        shapeRenderer.rect(x + r, y + height - r, width - 2 * r, r);
        shapeRenderer.rect(x, y + r, r, height - 2 * r);
        shapeRenderer.arc(x + r, y + r, r, 180f, 90f, 16);
        shapeRenderer.arc(x + width - r, y + r, r, 270f, 90f, 16);
        shapeRenderer.arc(x + width - r, y + height - r, r, 0f, 90f, 16);
        shapeRenderer.arc(x + r, y + height - r, r, 90f, 90f, 16);
    }

    private void drawBaseName(float x, float y, String name) {
        batch.begin();
        float ox = font.getData().scaleX, oy = font.getData().scaleY;
        font.getData().setScale(0.25f);
        GlyphLayout layout = new GlyphLayout(font, name);
        font.draw(batch, name, x - layout.width / 2, y);
        font.getData().setScale(ox, oy);
        batch.end();
    }
}
