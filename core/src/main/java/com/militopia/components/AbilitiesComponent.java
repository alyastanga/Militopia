package com.militopia.components;

import com.badlogic.ashley.core.Component;

/**
 * Stores status and state for unique unit/structure abilities.
 */
public class AbilitiesComponent implements Component {

    // --- Ability key constants ---
    public static final String KEY_DIG_IN = "DIG_IN";
    public static final String KEY_OVERWATCH = "OVERWATCH";

    // --- State flags ---
    public boolean isDiggingIn = false; // Recruit
    public boolean hasUsedDigIn = false; // Recruit
    public boolean isOverwatchActive = false; // Ranger (must be manually activated)
    public boolean isCloaked = false; // Sniper / Wraith / Submarine / B2
    public boolean pendingSkirmishMove = false; // Gunboat: 1-tile move after attacking
    public boolean isUnreachable = false; // Unit is unreachable (immune to melee) — renders at half opacity
    public boolean isCloakBroken = false; // Set to true after attacking, reset at start of next turn

    // --- Resources / Cooldowns ---
    public int fuel = -1; // Apache (-1 = N/A)
    public int nukeCooldown = 0; // Submarine
    public float idleTimer = 0f; // Tank idle animation timer

    // --- Ability Specific Data ---
    public int fuelMax = 5;

    public AbilitiesComponent() {
    }

    /** Convenience for initializing with fuel */
    public AbilitiesComponent(int initialFuel) {
        this.fuel = initialFuel;
        this.fuelMax = initialFuel;
    }
}
