package com.militopia.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Interpolation;

public class AnimationComponent implements Component {

    public enum Type {
        NONE,
        LUNGE,      // Melee attack movement
        PROJECTILE, // Ranged attack movement
        HIT_FLASH,  // Visual feedback on hit
        JUMP        // Juggernaut leap attack
    }

    public Type type = Type.NONE;
    public float stateTime = 0;
    public float duration = 0.3f; // Default duration
    
    // For Lunge/Projectile
    public float startX, startY;
    public float targetX, targetY;
    
    public Interpolation interpolation = Interpolation.linear;
    
    // For JUMP animation
    public float jumpStartOffX, jumpStartOffY; // visual offset at start (negative of travel delta)
    public float arcHeight = 50f;              // peak height in world pixels
    public float arcDuration = -1f;            // duration of the in-air arc phase; landing fires when arc Y drops to threshold on descent
    public boolean landingFired = false;

    // For frame-based animations (placeholder support)
    public boolean isFrameBased = false;
    public String animationKey;
    public boolean loop = false;

    public void reset() {
        type = Type.NONE;
        stateTime = 0;
        isFrameBased = false;
        animationKey = null;
        arcDuration = -1f;
        landingFired = false;
    }
}
