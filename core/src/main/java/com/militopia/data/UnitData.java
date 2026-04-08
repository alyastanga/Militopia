package com.militopia.data;

public class UnitData {
    public int x;
    public int y;
    public String type;
    public int owner;
    public String unitTypeKey;
    public int hp;
    public int maxHp;
    public boolean hasMoved;
    public boolean hasActed;
    public boolean isCloaked;
    public boolean isCloakBroken;
    public boolean isDiggingIn;
    public boolean hasUsedDigIn;
    public boolean isOverwatchActive;
    public boolean pendingSkirmishMove;
    public boolean isUnreachable;
    public int fuel;
    public int nukeCooldown;
    public float idleTimer = 0f;

    // Default constructor for JSON
    public UnitData() {
    }

    public UnitData(int x, int y, String type, int owner) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.owner = owner;
        this.unitTypeKey = type;
        this.hp = 10;
        this.maxHp = 10;
        this.hasMoved = false;
        this.hasActed = false;
        this.isCloaked = false;
        this.isCloakBroken = false;
        this.isDiggingIn = false;
        this.hasUsedDigIn = false;
        this.isOverwatchActive = false;
        this.pendingSkirmishMove = false;
        this.isUnreachable = false;
        this.fuel = -1;
        this.nukeCooldown = 0;
    }

    public UnitData(int x, int y, String type, int owner, String unitTypeKey, int hp, int maxHp,
            boolean hasMoved, boolean hasActed,
            boolean isCloaked, boolean isCloakBroken,
            boolean isDiggingIn, boolean hasUsedDigIn, boolean isOverwatchActive,
            boolean pendingSkirmishMove, boolean isUnreachable, int fuel) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.owner = owner;
        this.unitTypeKey = unitTypeKey;
        this.hp = hp;
        this.maxHp = maxHp;
        this.hasMoved = hasMoved;
        this.hasActed = hasActed;
        this.isCloaked = isCloaked;
        this.isCloakBroken = isCloakBroken;
        this.isDiggingIn = isDiggingIn;
        this.hasUsedDigIn = hasUsedDigIn;
        this.isOverwatchActive = isOverwatchActive;
        this.pendingSkirmishMove = pendingSkirmishMove;
        this.isUnreachable = isUnreachable;
        this.fuel = fuel;
        this.nukeCooldown = 0;
    }
}