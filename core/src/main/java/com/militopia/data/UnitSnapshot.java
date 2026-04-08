package com.militopia.data;

import com.militopia.components.StatsComponent;

/**
 * Immutable snapshot of a single unit's state at a point in time.
 * Used by TurnHistoryManager to restore units (including dead ones) on undo.
 */
public class UnitSnapshot {

    public String unitTypeKey; // Factory key e.g. "RECRUIT", "TANK"
    public int x, y;
    public int owner;
    public int currentHP;
    public boolean hasActed;
    public boolean hasMoved;
    public StatsComponent.MoveType moveType;

    // --- Ability States ---
    public boolean isDiggingIn;
    public boolean hasUsedDigIn;
    public boolean isOverwatchActive;
    public boolean isCloaked;
    public boolean isCloakBroken;
    public boolean pendingSkirmishMove;
    public boolean isUnreachable;
    public int fuel;
    public int nukeCooldown;
    public float idleTimer = 0f;

    /** No-arg constructor for LibGDX Json serialization */
    public UnitSnapshot() {
        this.unitTypeKey = "";
        this.x = 0;
        this.y = 0;
        this.owner = 0;
        this.currentHP = 0;
        this.hasActed = false;
        this.hasMoved = false;
        this.moveType = null;
        this.isDiggingIn = false;
        this.hasUsedDigIn = false;
        this.isOverwatchActive = false;
        this.isCloaked = false;
        this.isCloakBroken = false;
        this.pendingSkirmishMove = false;
        this.isUnreachable = false;
        this.fuel = -1;
        this.nukeCooldown = 0;
    }

    public UnitSnapshot(String unitTypeKey, int x, int y, int owner,
            int currentHP, boolean hasActed, boolean hasMoved,
            StatsComponent.MoveType moveType, boolean isDiggingIn,
            boolean hasUsedDigIn, boolean isOverwatchActive,
            boolean isCloaked, boolean isCloakBroken, boolean pendingSkirmishMove,
            boolean isUnreachable, int fuel, int nukeCooldown) {
        this.unitTypeKey = unitTypeKey;
        this.x = x;
        this.y = y;
        this.owner = owner;
        this.currentHP = currentHP;
        this.hasActed = hasActed;
        this.hasMoved = hasMoved;
        this.moveType = moveType;
        this.isDiggingIn = isDiggingIn;
        this.hasUsedDigIn = hasUsedDigIn;
        this.isOverwatchActive = isOverwatchActive;
        this.isCloaked = isCloaked;
        this.isCloakBroken = isCloakBroken;
        this.pendingSkirmishMove = pendingSkirmishMove;
        this.isUnreachable = isUnreachable;
        this.fuel = fuel;
        this.nukeCooldown = nukeCooldown;
    }
}
