package com.militopia.config;

/**
 * Named constants for all magic numbers used in combat resolution.
 * Centralizing these makes tuning easier and avoids silent bugs from
 * scattered literals.
 */
public class CombatConstants {

    private CombatConstants() {
    }

    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Jump animation (Juggernaut)
    // -----------------------------------------------------------------------
    public static final float JUMP_ARC_HEIGHT = 25f;
    public static final float JUMP_ARC_DURATION = 0.5f;  // how long the in-air arc lasts; post-landing frames fill the rest
    public static final float JUMP_DURATION = 0.7f;      // full animation duration including post-landing frames
    public static final float JUMP_LANDING_THRESHOLD = 2f; // arc Y (px) below which landing is triggered on descent

    // -----------------------------------------------------------------------
    // Attack animations
    // -----------------------------------------------------------------------
    public static final float LUNGE_MELEE_DURATION = 0.4f;
    public static final float LUNGE_RANGED_DURATION = 0.15f;
    public static final float LUNGE_MELEE_DISTANCE = 1.5f; // multiplier on iso delta
    public static final float LUNGE_RANGED_RECOIL = -0.1f; // small pop-back for ranged

    // -----------------------------------------------------------------------
    // Muzzle flash offsets
    // -----------------------------------------------------------------------
    public static final float MUZZLE_OFFSET_X_RIGHT = 3.5f;
    public static final float MUZZLE_OFFSET_X_LEFT = -3.5f;
    public static final float MUZZLE_OFFSET_Y = -2.5f;

    // -----------------------------------------------------------------------
    // Resource rewards
    // -----------------------------------------------------------------------
    /** Funding gained when a unit cuts down a tree. */
    public static final int TREE_CUT_FUNDING = 1;
    /** Funding gained when a unit cuts down a cactus. */
    public static final int CACTUS_CUT_FUNDING = 3;
    /** Funding gained when a unit hunts an animal. */
    public static final int ANIMAL_HUNT_FUNDING = 3;

    // -----------------------------------------------------------------------
    // Terrain defense bonuses
    // -----------------------------------------------------------------------
    public static final int TERRAIN_BONUS_MOUNTAIN = 3;
    public static final int TERRAIN_BONUS_TREE = 1;
    public static final int TERRAIN_BONUS_BASE = 2;

    // -----------------------------------------------------------------------
    // Ability constants
    // -----------------------------------------------------------------------
    public static final int DIG_IN_DEFENSE_BONUS = 3;
    public static final int MAX_RANGE_ATTACK_PENALTY = 1;
    public static final int SHORE_BOMBARDMENT_BONUS = 5; // Destroyer vs Land

    // -----------------------------------------------------------------------
    // Fog of War / Vision
    // -----------------------------------------------------------------------
    /** Radius of the jamming zone projected by a Signal Jammer structure. */
    public static final int JAMMER_RADIUS = 4;
    /** Bonus vision tiles added to a Radar Station's base vision stat. */
    public static final int RADAR_VISION_BONUS = 4;
    /** Vision radius forced on any unit standing inside a jammer zone. */
    public static final int JAMMER_SUPPRESSED_VISION = 1;
    /** Radius within which any unit can spot a cloaked (stealthed) enemy. */
    public static final int STEALTH_DETECTION_RADIUS = 1;

    // -----------------------------------------------------------------------
    // Juggernaut jump — super unit interaction
    // -----------------------------------------------------------------------
    /** Damage multiplier applied to the primary super-unit target on a Juggernaut jump. */
    public static final float JUMP_SUPER_UNIT_DAMAGE_MULTIPLIER = 1.5f;

    // -----------------------------------------------------------------------
    // AoE splash (Juggernaut jump landing, B2, Submarine)
    // -----------------------------------------------------------------------
    /** Chebyshev radius of AoE splash applied on Juggernaut jump landing and B2/Submarine attacks. */
    public static final int AOE_SPLASH_RADIUS = 1;
    /** Damage multiplier applied to AoE splash tiles for B2 and Submarine attacks (reduces splash damage). */
    public static final float AOE_SPLASH_DAMAGE_MULTIPLIER = 1f / 1.5f;

    // -----------------------------------------------------------------------
    // Entity search limits
    // -----------------------------------------------------------------------
    /** Maximum search radius used by findNearestFreeTile when placing units. */
    public static final int NEAREST_FREE_TILE_MAX_RADIUS = 10;
}
