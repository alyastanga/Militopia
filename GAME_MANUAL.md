# Militopia — Game Manual

## Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [Controls](#controls)
4. [Turn Structure](#turn-structure)
5. [Units](#units)
6. [Abilities](#abilities)
7. [Structures](#structures)
8. [Combat](#combat)
9. [Economy & Base Progression](#economy--base-progression)
10. [Terrain](#terrain)
11. [Fog of War](#fog-of-war)
12. [Win Conditions](#win-conditions)
13. [Game Modes & Map Generation](#game-modes--map-generation)

---

## Overview

**Militopia** is a 2-player hot-seat turn-based military strategy game played on a procedurally generated isometric map. Both players share the same machine, taking turns commanding their forces across land, sea, and air.

- **Players:** Blue (Player 1) and Red (Player 2)
- **Objective:** Destroy all enemy bases
- **Domains:** Land, Sea, and Air units operate in separate movement domains
- **Progression:** Bases level up, unlocking new units, structures, and income

---

## Getting Started

Each player starts with one **Base** and a small amount of **Funding** (5) and **XP** (500). On your first turn, you can purchase units and begin exploring the map.

**Basic loop each turn:**

1. Move your units into position
2. Attack enemy units or structures
3. Capture neutral towns and enemy bases to grow your economy
4. Spend Funding to summon new units or build structures near your bases
5. Click **End Turn** to pass to your opponent

---

## Controls

### Mouse

| Action | Input |
|--------|-------|
| Select unit or tile | Left Click |
| Deselect / cancel action | Right Click |
| Zoom in/out | Scroll Wheel |
| Pan camera | Mouse Drag |

### Keyboard

| Key | Action |
|-----|--------|
| `E` | End Turn |

### Unit Actions (click-based)

1. **Click a unit** — selects it and shows its stats in the Info Panel
2. **Click a destination tile** — moves the unit (if within movement range and passable)
3. **Click an enemy unit** — attacks (if within attack range)
4. **Click a structure** — captures it (unit must be adjacent and the structure must be neutral or enemy-owned)
5. **Ability buttons** — activate the unit's special ability (Dig In, Overwatch, Nuke, etc.)

### HUD Buttons

| Button | Function |
|--------|----------|
| **Summon** | Open menu to purchase units near a selected base |
| **Build** | Open menu to construct structures within base borders |
| **Capture** | Show capturable bases and towns adjacent to your units |
| **End Turn** | End your turn and pass to the opponent |

---

## Turn Structure

Militopia is strictly turn-based. One player acts completely before the other.

### Turn Flow

1. Active player takes all actions (move, attack, capture, build, summon)
2. Player clicks **End Turn**
3. A fade transition plays while:
   - Economy processes (income distributed, XP accumulated)
   - Fog of War updates
   - Turn counter increments
   - Active player switches
4. Camera snaps to the new player's base
5. New player begins their turn

### Action Limits

Each unit may **move once** and **act once** per turn. A unit that has acted is grayed out and cannot be used again until the next turn. All units reset at the start of their owner's turn.

> **Note:** Units summoned on the current turn cannot act until the following turn.

### Economy Processing (on turn start)

- **Turn 1:** No income or XP bonuses — free setup round
- **Turn 2+:**
  - Base XP distributed: `250 + (level − 1) × 10` per turn
  - Structure XP and income bonuses applied
  - Field Hospital healing (+3 HP to adjacent units)
  - Apache fuel ticks down; Carrier refuels adjacent air units
  - Ability cooldowns advance (e.g., Submarine Nuke cooldown)

---

## Units

Units are purchased with **Funding** from the Summon menu near a base. Higher-level bases unlock more powerful units. Each unit belongs to a domain (Land, Sea, or Air) and can only traverse compatible terrain.

### Stats Explained

| Stat | Meaning |
|------|---------|
| **HP** | Hit points; unit is destroyed when reduced to 0 |
| **ATK** | Base attack damage |
| **DEF** | Damage reduction (subtracted from incoming attacks) |
| **MOV** | Movement range in tiles per turn |
| **RNG** | Attack range in tiles (1 = melee) |
| **VIS** | Vision range in tiles (fog of war) |
| **Cost** | Funding required to purchase |

---

### Land Units

| Unit | HP | ATK | DEF | MOV | RNG | VIS | Cost | Unlocked |
|------|----|-----|-----|-----|-----|-----|------|----------|
| **Recruit** | 10 | 3 | 1 | 1 | 1 | 1 | 2 | Level 1 |
| **Ranger** | 12 | 5 | 1 | 1 | 2 | 2 | 5 | Level 2 |
| **Sniper** | 8 | 15 | 0 | 1 | 3 | 3 | 8 | Level 3 |
| **Tank (MBT)** | 30 | 12 | 5 | 2 | 3 | 3 | 15 | Level 4 |
| **Juggernaut** | 50 | 12 | 6 | 4 | 4 | 3 | — | Level 5 choice |

---

### Air Units

| Unit | HP | ATK | DEF | MOV | RNG | VIS | Cost | Unlocked |
|------|----|-----|-----|-----|-----|-----|------|----------|
| **Recon Drone** | 5 | 0 | 0 | 3 | — | 3 | 4 | Level 2 |
| **Suicide Drone** | 5 | 20 | 0 | 2 | 2 | 2 | 7 | Level 3 |
| **Apache** | 20 | 15 | 2 | 3 | 2 | 3 | 18 | Level 4 |
| **Wraith (B2)** | 45 | 18 | 3 | 3 | 3 | 3 | — | Level 5 choice |

> Air units fly over all terrain types, including mountains and water.

---

### Sea Units

| Unit | HP | ATK | DEF | MOV | RNG | VIS | Cost | Unlocked |
|------|----|-----|-----|-----|-----|-----|------|----------|
| **Gunboat** | 12 | 5 | 1 | 1 | 2 | 2 | 5 | Level 2 |
| **Destroyer** | 30 | 12 | 5 | 2 | 3 | 3 | 15 | Level 3 |
| **Carrier** | 45 | 5 | 4 | 3 | 3 | 3 | 25 | Level 4 |
| **Submarine** | 40 | 25 | 3 | 4 | 4 | 3 | — | Level 5 choice |

> Sea units can only move on Water and Deep Water tiles. A **Port** structure is required to deploy sea units.

---

### Super Units (Level 5 Choice)

When a base reaches Level 5, a one-time popup lets you choose **one** super unit:

| Super Unit | Domain | Ability |
|------------|--------|---------|
| **Juggernaut** | Land | Suppressing Fire — hits all 8 adjacent tiles at once |
| **Wraith (B2)** | Air | Stealth Cloak — permanently cloaked, nearly invisible |
| **Submarine** | Sea | Deep Dive + Nuke — cloaked with a powerful area nuke |

The chosen unit spawns immediately at the base. Only one super unit per base.

---

## Abilities

Each unit type has a unique ability, passive or active.

| Unit | Ability | Description |
|------|---------|-------------|
| **Recruit** | **Dig In** | Spend action to place a Sandbag: +3 DEF until the next enemy turn cycle |
| **Ranger** | **Overwatch** | Activate to auto-attack the first enemy that enters range during the enemy's turn |
| **Sniper** | **Camouflage** | Passive — invisible on Forest/Ruins tiles; revealed by attacking or an enemy within 1 tile |
| **Tank (MBT)** | **Blitz** | After killing a unit, the Tank may immediately move again |
| **Juggernaut** | **Suppressing Fire** | Attacks all 8 adjacent tiles simultaneously with a jump animation |
| **Recon Drone** | **High Altitude** | Passive — immune to attacks from Range-1 land units |
| **Suicide Drone** | **Kamikaze** | Destroyed immediately after attacking; dives into the target tile |
| **Apache** | **Fuel Gauge** | Limited to 5 turns of flight; crashes if fuel runs out. Carriers refuel adjacent air units |
| **Wraith (B2)** | **Stealth Cloak** | Passive — permanently cloaked; only detected within 1 tile or by a Radar Station |
| **Gunboat** | **Skirmish** | Gains 1 bonus movement tile after attacking |
| **Destroyer** | **Shore Bombardment** | +5 bonus damage against land units |
| **Carrier** | **Mobile Airfield** | Heals adjacent air units +2 HP/turn; refuels Apache fuel to maximum each turn |
| **Submarine** | **Deep Dive + Nuke** | Permanently cloaked; activate Nuke to deal 15 damage in a 1-tile radius (3-turn cooldown) |

---

## Structures

Structures are built within a base's border zone using Funding. Each structure contributes income and/or XP to its parent base every turn. Some structures have powerful special effects.

| Structure | Cost | Income/Turn | XP/Turn | Special Effect | Requirement |
|-----------|------|-------------|---------|----------------|-------------|
| **Munitions Factory** | 5 | +2 | +50 | Basic income generator | Level 1 |
| **Port** | 7 | — | +50 | Enables sea unit deployment on adjacent water | Level 2 |
| **Field Hospital** | 15 | — | +50 | Heals adjacent friendly units +3 HP/turn | Level 2 |
| **Oil Derrick** | 10 | +6 | +100 | High income; **explodes on destruction** (AOE damage) | Level 3; must be on Oil Reservoir tile |
| **Radar Station** | 20 | — | +75 | Vision range +4; reveals cloaked/invisible enemies | Level 3 |
| **Solar Array** | 8 | +3 | +75 | **Adjacency bonus:** +1 income per adjacent friendly structure | Level 4 |
| **Signal Jammer** | 25 | — | +75 | Jams enemy vision in 4-tile radius (enemies see only 1 tile); blocks stealth detection | Level 4 |
| **Nuclear Plant** | 40 | +15 | +150 | Highest income; **meltdown on destruction** converts 3×3 area to Wasteland | Level 5; coastline only |

### Bases and Towns

| Structure | Income/Turn | Notes |
|-----------|-------------|-------|
| **Base (Level 1)** | +2 | Starting structure; destroyable — if all your bases fall, you lose |
| **Base (Level 2+)** | +3 | Upgraded base with expanded borders |
| **Town** | +1 | Neutral at game start; capture by moving a unit onto it |

---

## Combat

### Damage Formula

```
Damage = (ATK + ShoreBonus) − (DEF + DigInBonus + TerrainBonus) − RangePenalty
Minimum damage: 0
```

**Modifiers:**

| Modifier | Value | Condition |
|----------|-------|-----------|
| Shore Bombardment Bonus | +5 ATK | Destroyer attacking a land unit |
| Dig In Bonus | +3 DEF | Recruit used Dig In this cycle |
| Mountain Terrain | +3 DEF | Defender on a Mountain tile |
| Tree/Forest Terrain | +1 DEF | Defender on a Tree tile |
| Base Terrain | +2 DEF | Defender on a Base tile |
| Range Penalty | −1 Damage | Ranged attacker at maximum range |

### Combat Resolution

1. Attacker damage is calculated and applied to the defender
2. If the defender is destroyed → no counterattack
3. If the defender survives **and** the attacker is within the defender's range → counter-attack fires
4. Attacker is exhausted (`hasMoved = hasActed = true`)

### Special Combat Rules

- **Recruit melee kill:** Recruit automatically advances to the defender's tile
- **Suicide Drone:** Destroyed immediately after attacking; no counterattack possible
- **Stealth attacks:** Attacks from a cloaked unit bypass counterattacks
- **Recon Drone:** Immune to attacks from Range-1 land units (High Altitude)
- **Wraith (B2) / Submarine:** Deal splash damage (1-tile radius) on hit, skipping the primary target
- **Juggernaut:** Attacks all 8 adjacent tiles simultaneously

---

## Economy & Base Progression

### Resources

The game uses two resources:

| Resource | Purpose |
|----------|---------|
| **Funding** | Spend to summon units, build structures, and capture |
| **XP** | Accumulated by bases to level them up |

### Income Sources

| Source | Funding/Turn |
|--------|-------------|
| Base (Level 1) | +2 |
| Base (Level 2+) | +3 |
| Captured Town | +1 |
| Munitions Factory | +2 |
| Solar Array | +3 (+1 per adjacent friendly structure) |
| Oil Derrick | +6 |
| Nuclear Plant | +15 |
| Hunting a wild animal (one-time) | +3 |
| Cutting a tree (one-time) | +1 |
| Cutting a cactus (one-time) | +3 |

### Base XP & Leveling

Bases accumulate XP each turn from base income and linked structures. When enough XP is collected, the base levels up, granting new units, structures, border expansion, and funding bonuses.

| Level | XP to Reach | Border Radius | Funding Bonus | Units Unlocked | Structures Unlocked |
|-------|-------------|---------------|---------------|----------------|---------------------|
| 1 | 2,000 | 1 | +5 | Recruit | Munitions Factory |
| 2 | 3,000 | 1 | — | Ranger, Recon Drone, Gunboat | Port, Field Hospital |
| 3 | 4,500 | 1 | +10 | Sniper, Suicide Drone, Destroyer | Oil Derrick, Radar Station |
| 4 | 6,750 | 2 | — | Tank, Apache, Carrier | Solar Array, Signal Jammer |
| 5 | 10,125 | 2 | +10 | **Choose 1 Super Unit** | Nuclear Plant |
| 6+ | ×1.5 each level | 2 | +10 | — | — |

### XP Per Turn (Structure Bonuses)

| Structure | XP/Turn |
|-----------|---------|
| Munitions Factory | +50 |
| Port | +50 |
| Field Hospital | +50 |
| Solar Array | +75 |
| Radar Station | +75 |
| Signal Jammer | +75 |
| Oil Derrick | +100 |
| Nuclear Plant | +150 |

Natural base growth per turn: `250 + (level − 1) × 10`

---

## Terrain

The map is generated using Simplex Noise. Terrain type determines movement passability, defense bonuses, and special interactions.

### Terrain Types

| Terrain | Passable By | Defense Bonus | Notes |
|---------|-------------|---------------|-------|
| **Grass** | Land units | — | Most common terrain |
| **Sand** | Land units | — | Desert areas |
| **Water** | Sea units | — | Shallow coastal water |
| **Deep Water** | Sea units | — | Open ocean |
| **Mountain** | Air units only | +3 DEF | Impassable to land/sea |

### Map Objects

| Object | Effect |
|--------|--------|
| **Trees** | +1 DEF to units on tile; cut for +1 Funding |
| **Cacti** | Cut for +3 Funding |
| **Ruins** | Scavenge for random rewards (one-time) |
| **Oil Reservoir** | Required tile for building an Oil Derrick |
| **Boulders** | Impassable obstacles |
| **Wild Animals** | Hunt for +3 Funding (Horses, Deer, Zebras, Fish) |

---

## Fog of War

Militopia features a **Fog of War** system that hides enemy positions and unexplored terrain.

### Vision

- Each unit reveals tiles within its **VIS** stat radius
- Structures can extend friendly vision
- Fog is recalculated every frame based on the current player's units and structures

### Vision Modifiers

| Source | Effect |
|--------|--------|
| **Radar Station** | +4 vision radius; reveals cloaked units |
| **Signal Jammer** | Hostile units within 4 tiles see only 1 tile; blocks stealth detection |

### Stealth & Detection

| Unit / Mechanic | Rule |
|-----------------|------|
| **Sniper (Camouflage)** | Invisible on Forest/Ruins; revealed by attacking or enemy within 1 tile |
| **Wraith (B2) Cloak** | Permanently invisible; detected only within 1 tile or by Radar Station |
| **Submarine Deep Dive** | Permanently invisible; detected only within 1 tile or by Radar Station |
| **Signal Jammer** | Blocks stealth detection in a 4-tile radius around the jammer |

---

## Win Conditions

### Victory

**Destroy all enemy bases.**

The game ends immediately when one player has no bases remaining. Capturing towns does not count — all enemy bases must be destroyed or captured.

### Defeat

You lose if all of your bases are captured or destroyed. There is no time limit.

### Game Over Screen

When the game ends, the Game Over screen shows:
- The winning player
- Total turn count
- Option to return to the main menu

---

## Game Modes & Map Generation

### Map Sizes

| Mode | Map Size | Description |
|------|----------|-------------|
| **Blitz** | 16 × 16 | Smaller map for faster games |
| **Marathon** | 32 × 32 | Larger map for longer strategic games |

### Procedural Map Generation

Every game generates a unique map using **Simplex Noise**:

1. **Terrain pass** — noise values determine terrain type across the map
2. **Base placement** — P1 base in the top-left quadrant, P2 base in the bottom-right, separated by at least `(width + height) / 2.5` tiles
3. **Flora** — trees and cacti scattered across land
4. **Towns** — neutral structures placed with minimum spacing
5. **Oil Reservoirs** — placed near settlements (bases and towns) for Oil Derrick access
6. **Map objects** — ruins, boulders, animals in appropriate terrain

> The same seed always produces the same map.

### Saving & Loading

Game state (units, structures, turns, base levels) is saved to `assets/saves/` as JSON. You can resume a saved game from the main menu.

---

*Militopia — Command your forces, level your bases, and conquer the map.*
