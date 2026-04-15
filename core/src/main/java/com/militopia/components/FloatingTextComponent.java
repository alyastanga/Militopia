package com.militopia.components;

import com.badlogic.ashley.core.Component;

/**
 * Attached to a short-lived entity that displays a floating damage number or
 * "BLOCKED" label above a unit in world-space.
 *
 * The UnitRenderSystem renders and ages these entities, removing them when
 * {@code timer >= MAX_TIME}.
 */
public class FloatingTextComponent implements Component {

    /** How long the text floats before disappearing (seconds). */
    public static final float MAX_TIME = 1.0f;

    /** The text to display, e.g. "5" or "BLOCKED". */
    public enum Type {
        DAMAGE,
        BLOCKED,
        XP,
        FUNDING
    }

    public String text;
    public float worldX, worldY;
    public Type type;
    public float timer = 0f;
    public float alpha = 1f;

    /** Grid tile coordinates of the source unit/structure — used for fog culling. */
    public int gridX = -1, gridY = -1;

    public FloatingTextComponent(String text, float worldX, float worldY, Type type) {
        this.text = text;
        this.worldX = worldX;
        this.worldY = worldY;
        this.type = type;
    }

    public FloatingTextComponent(String text, float worldX, float worldY, Type type, int gridX, int gridY) {
        this(text, worldX, worldY, type);
        this.gridX = gridX;
        this.gridY = gridY;
    }
}
