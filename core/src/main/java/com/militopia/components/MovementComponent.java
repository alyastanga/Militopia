package com.militopia.components;

import com.badlogic.ashley.core.Component;

public class MovementComponent implements Component {
    public int startX, startY;   // Where we came from
    public int targetX, targetY; // Where we are going
    public float time = 0f;      // How long we've been moving
    public float duration = 0.4f; // Seconds to complete move (Lower = Faster)

    public float jumpArcHeight = 0f; // world-pixel peak height during movement (0 = no arc)

    public MovementComponent(int sX, int sY, int tX, int tY) {
        this.startX = sX;
        this.startY = sY;
        this.targetX = tX;
        this.targetY = tY;
    }
}