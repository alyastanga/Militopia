package com.militopia.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class IdleAnimationComponent implements Component {
    public Animation<TextureRegion> animation;
    public float stateTime = 0f;

    /** If true, loops forever with no pause (used for fish). */
    public boolean looping = false;

    /** True while waiting in the inter-cycle pause. */
    public boolean paused = true;

    /** Seconds remaining in the current pause. Set to random 2-3s on spawn. */
    public float pauseTimer = 0f;

    /**
     * If true, drawn as an overlay on top of the static sprite (only during playing phase).
     * Static sprite still shows when paused. Used for tank idle.
     * If false (default), replaces the static sprite entirely; frame 0 shown when paused.
     */
    public boolean overlayOnly = false;

    /** If true, flips the frame horizontally to match the entity's facing direction. */
    public boolean respectsFacing = false;

    /** Extra draw offset applied on top of cfg.offsetX/Y (used for tank idle positioning). */
    public float drawOffsetX = 0f;
    public float drawOffsetY = 0f;
}
