# Militopia

> A two-player, turn-based military strategy game on a procedurally generated isometric map.

![Java](https://img.shields.io/badge/Java-8%2F17-blue) ![libGDX](https://img.shields.io/badge/libGDX-1.14.0-red) ![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-lightgrey) ![Platform](https://img.shields.io/badge/Platform-Windows%20%2F%20macOS%20%2F%20Linux-green)

---

## Table of Contents

1. [Overview](#overview)
2. [Download](#download)
3. [Screenshots](#screenshots)
4. [Getting Started](#getting-started)
5. [LAN Multiplayer](#lan-multiplayer)
6. [Controls](#controls)
7. [Gameplay at a Glance](#gameplay-at-a-glance)
8. [The Map](#the-map)
9. [Turn System](#turn-system)
10. [Economy](#economy)
11. [Base Progression](#base-progression)
12. [Units](#units)
13. [Unit Abilities](#unit-abilities)
14. [Structures](#structures)
15. [Combat](#combat)
16. [Capture Mechanics](#capture-mechanics)
17. [Fog of War](#fog-of-war)
18. [Ruins (Scavenging)](#ruins-scavenging)
19. [Save & Load](#save--load)
20. [HUD](#hud)
21. [Tech Stack](#tech-stack)
22. [Developer Notes](#developer-notes)
23. [Roadmap](#roadmap)
24. [License](#license)

---

## Overview

**Militopia** is a hot-seat, 2-player strategy game where both players share the same machine and take alternating turns. Each player captures territory, manages an economy, builds structures, and deploys military units across Land, Sea, and Air domains — all with the goal of **destroying every enemy base**.

The game features a tactically deep combat system with Fog of War, base progression, unique unit abilities, and a procedurally generated isometric map powered by Simplex noise.

---

## Download

Grab the latest release from the [Releases page](https://github.com/Hanzm10/Militopia/releases/latest).

| File | Platform | Requirements |
|------|----------|--------------|
| `Militopia-v1.0.0-windows.zip` | Windows 64-bit | None — Java bundled |
| `Militopia-v1.0.0-mac-arm64.zip` | macOS Apple Silicon (M1/M2/M3) | None — Java bundled |
| `Militopia-v1.0.0-universal.jar` | Windows / macOS / Linux | Java 8+ required |

### Running the pre-built release

**Windows**
1. Download and extract `Militopia-v1.0.0-windows.zip`
2. Open the `roast` folder
3. Double-click `Militopia.exe`

**macOS (Apple Silicon)**
1. Download and extract `Militopia-v1.0.0-mac-arm64.zip`
2. Right-click `Militopia.app` → **Open**
3. If blocked by Gatekeeper: System Settings → Privacy & Security → **Open Anyway**

**Universal JAR**
```bash
java -jar Militopia-v1.0.0-universal.jar
```

---

## Screenshots

> Screenshots coming soon.

---

## Getting Started

> Want to just play? See [Download](#download) above.

### Prerequisites (building from source)

- **Java 8+** (or Java 17) installed and on your `PATH`.

Verify your Java installation:

```bash
java -version
```

### 1. Clone the repository

```bash
git clone https://github.com/Hanzm10/Militopia.git
cd Militopia
```

### 2. Run the game

```bash
# Windows
gradlew.bat lwjgl3:run

# macOS / Linux
./gradlew lwjgl3:run
```

### 3. Build a distributable

```bash
# Cross-platform JAR (requires Java on target machine)
./gradlew lwjgl3:jar

# Standalone bundles (Java bundled — no install needed)
./gradlew lwjgl3:packageWinX64      # Windows
./gradlew lwjgl3:packageMacM1       # macOS Apple Silicon
./gradlew lwjgl3:packageMacX64      # macOS Intel
./gradlew lwjgl3:packageLinuxX64    # Linux
```

Output:
- JAR → `lwjgl3/build/libs/`
- Bundles → `lwjgl3/build/construo/`

> **Tip:** On macOS you may need to run `chmod +x gradlew` once before the `./gradlew` commands work.

---

## LAN Multiplayer

Militopia supports **LAN play** — each player runs the game on their own machine on the same network.

### Hosting a game

1. Launch the game and select **LAN** from the main menu.
2. Configure your player name, game seed, and map mode.
3. Click **Host Game** — the lobby waits for a client to connect (TCP port 7777).
4. Once the client joins, the game starts automatically.

### Joining a game

1. Launch the game and select **LAN** from the main menu.
2. Switch to the **Join** tab — hosts on the same network are auto-discovered via UDP broadcast.
3. Click **Join** next to a discovered host, or enter the host's IP address manually.
4. The host sends the initial game state and both players enter the game.

> **Note:** In LAN mode each player sees only their own fog of war — there is no shared screen like hot-seat.

---

## Controls

| Action | Input |
|---|---|
| Pan camera | Click and drag (left mouse button) |
| Zoom | Scroll wheel (0.2× – 2.0×) |
| Select unit / tile | Left click |
| Deselect | Right click |
| End Turn | Click the **End Turn** button in the HUD |

---

## Gameplay at a Glance

| Feature | Detail |
|---|---|
| Players | 2 — Hot-seat (same machine) or LAN (separate machines) |
| Player Colors | Player 1 = Blue · Player 2 = Red |
| Map | Procedurally generated per seed |
| Game Modes | Blitz (16×16) · Marathon (32×32) |
| Win Condition | Destroy all enemy bases |

---

## The Map

### Terrain Types

| Terrain | Passable By |
|---|---|
| `GRASS` | Land units |
| `SAND` | Land units |
| `WATER` | Sea units only |
| `DEEP_WATER` | Sea units only |
| `MOUNTAIN` | Impassable (Air units fly over) |

### Map Objects

- **Bases (P1/P2)** — One per player, placed in opposite quadrants. Capturable.
- **Towns** — Neutral structures; capture them for +1 income/turn.
- **Oil Reservoirs** — Required tile for building an Oil Derrick.
- **Ruins** — Interactive tiles with randomized scavenging rewards.
- **Trees** — Can be cut down for +1 Funding. Provide +1 defense bonus.
- **Cacti, Boulders** — Aesthetic terrain objects.

### Wild Animals

Aesthetic fauna spawn around bases and during capture events:

- Horse — Grass tiles
- Deer — Near Trees
- Zebra — Sand tiles
- Fish — Water/Deep Water

> Units can **hunt animals** for **+3 Funding** each.

---

## Turn System

1. The **active player** takes all their actions (move, attack, capture, build, summon).
2. They click **"End Turn"** when done.
3. A fade transition plays while the economy resolves and fog of war updates.
4. The camera snaps to the new player's base and gameplay continues.

> **Note:** Income is **not** distributed on Turn 1 — the first round is free.

---

## Economy

Each player has a **Funding** pool. At the start of each turn (after Turn 1), income from all owned structures is added.

### Income Sources

| Source | Income / Turn |
|---|---|
| Base (Level 1) | +2 |
| Base (Level 2+) | +3 |
| Captured Town | +1 |
| Munitions Factory | +2 |
| Solar Array | +3 |
| Oil Derrick | +6 |
| Nuclear Plant | +15 |
| Cutting a Tree | +1 (one-time) |
| Hunting an Animal | +3 (one-time) |

---

## Base Progression

Bases accumulate **XP** each turn and level up to unlock new units and structures.

### XP Growth Per Turn

- **Natural Growth:** `250 + (level - 1) × 10` XP/turn
- **Structure Bonus:** Each linked structure contributes additional XP/turn

### Level Table

| Level | XP Required | Income | Border Radius | Funding Bonus | Units Unlocked | Structures Unlocked |
|---|---|---|---|---|---|---|
| **1** | 2,000 | +2 | 1 | +5 | Recruit | Munitions Factory |
| **2** | 3,000 | +3 | 1 | — | Ranger, Recon Drone, Gunboat | Port, Field Hospital |
| **3** | 4,500 | +3 | 1 | +10 | Sniper, Suicide Drone, Destroyer | Oil Derrick, Radar Station |
| **4** | 6,750 | +3 | **2** | — | Tank (MBT), Apache, Carrier | Solar Array, Signal Jammer |
| **5** | 10,125 | +3 | 2 | +10 | *(choose 1 Super Unit)* | Nuclear Plant |
| **6+** | ×1.5 each | +3 | 2 | +10 | — | — |

> **Level 5 Super Unit:** At Level 5, a choice popup appears letting the player select one super unit to unlock for that base (Juggernaut, Wraith/B2, or Submarine). One is spawned immediately.

> A **Level-Up popup** appears on every promotion showing funding bonuses and newly unlocked content.

---

## Units

### Action Rules

- Each unit may **move once** and **act once** (attack, capture, or use ability) per turn.
- After acting, a unit is **grayed out** and cannot act again that turn.
- All units reset at the **start of the owner's turn**.
- Units summoned this turn **cannot act** until the next turn (`hasActed = true` on spawn).

### Movement Domains

| Domain | Passable Terrain |
|---|---|
| `LAND` | Grass, Sand |
| `SEA` | Water, Deep Water |
| `AIR` | All terrain (ignores ground obstacles) |

---

### Land Units

| Unit | HP | ATK | DEF | MOV | RNG | VIS | Cost | Unlock |
|---|---|---|---|---|---|---|---|---|
| **Recruit** | 10 | 3 | 1 | 1 | 1 | 1 | 2 | Level 1 |
| **Ranger** | 12 | 5 | 1 | 1 | 2 | 2 | 5 | Level 2 |
| **Sniper** | 8 | 15 | 0 | 1 | 3 | 3 | 8 | Level 3 |
| **Tank (MBT)** | 30 | 12 | 5 | 2 | 3 | 3 | 15 | Level 4 |
| **Juggernaut** | 50 | 12 | 6 | 4 | 4 | 3 | — | Level 5 (choice) |

### Air Units

| Unit | HP | ATK | DEF | MOV | RNG | VIS | Cost | Unlock |
|---|---|---|---|---|---|---|---|---|
| **Recon Drone** | 5 | 0 | 0 | 3 | 0 | 3 | 4 | Level 2 |
| **Suicide Drone** | 5 | 20 | 0 | 2 | 2 | 2 | 7 | Level 3 |
| **Apache** | 20 | 15 | 2 | 3 | 2 | 3 | 18 | Level 4 |
| **Wraith (B2)** | 45 | 18 | 3 | 3 | 3 | 3 | — | Level 5 (choice) |

### Sea Units

| Unit | HP | ATK | DEF | MOV | RNG | VIS | Cost | Unlock |
|---|---|---|---|---|---|---|---|---|
| **Gunboat** | 10 | 5 | 2 | 2 | 2 | 2 | 6 | Level 2 |
| **Destroyer** | 30 | 15 | 3 | 3 | 3 | 3 | 13 | Level 3 |
| **Carrier** | 45 | 5 | 4 | 3 | 3 | 3 | 25 | Level 4 |
| **Submarine** | 40 | 25 | 3 | 4 | 4 | 3 | — | Level 5 (choice) |

> **Cost `—`** = Super units unlocked via Level 5 base promotion, not purchased directly.

---

## Unit Abilities

Every unit has a unique special ability.

| Unit | Ability | Effect |
|---|---|---|
| **Recruit** | Dig In | Spend 1 action to gain +3 Defense (places a Sandbag on the tile) |
| **Ranger** | Overwatch | Manually activate to auto-attack the first enemy that enters range during the enemy's turn (1 trigger per turn) |
| **Sniper** | Camouflage | Invisible on Forest/Ruins tiles; revealed on attack or when enemy is adjacent (within 1 tile) |
| **Tank (MBT)** | Blitz | If an attack kills a unit, the Tank may move again immediately |
| **Juggernaut** | Jump Strike | Leaps to the target tile; deals AoE damage to all enemies on landing |
| **Recon Drone** | High Altitude | Immune to all damage from range-1 land units |
| **Suicide Drone** | Kamikaze | Destroyed immediately after attacking; dives into target tile on attack |
| **Apache** | Fuel Gauge | Has 5 turns of fuel; crashes if not refueled. A Carrier refuels adjacent air units each turn |
| **Wraith (B2)** | Stealth Cloak | Invisible until it attacks; only detected within 1 tile or by a Radar Station |
| **Gunboat** | Skirmish | Gains 1 bonus movement after attacking |
| **Destroyer** | Shore Bombardment | Deals +5 bonus damage to Land units |
| **Carrier** | Mobile Airfield | Heals and refuels adjacent Air units at turn start |
| **Submarine** | Deep Dive | Cloaked until it attacks; regular attack splashes all adjacent tiles (radius 1) |

---

## Structures

Structures are built within a base's **border zone** and linked to the parent base, contributing XP per turn.

| Structure | Cost | Income | XP/Turn | Special Effect |
|---|---|---|---|---|
| **Munitions Factory** | 5 | +2 | +50 | — |
| **Port** | 7 | 0 | +50 | Enables Sea unit summoning on adjacent Water tiles |
| **Field Hospital** | 15 | 0 | +50 | Heals adjacent units at the start of each turn |
| **Solar Array** | 8 | +3 | +75 | Adjacency Bonus: +1 income for each adjacent friendly structure |
| **Radar Station** | 20 | 0 | +75 | Scanner: Vision range +4; reveals cloaked/invisible enemies in radius |
| **Signal Jammer** | 25 | 0 | +75 | Static: Jams enemy vision in a 4-tile radius (suppressed units see only 1 tile); blocks stealth detection |
| **Oil Derrick** | 10 | +6 | +100 | Must be built on an Oil Reservoir tile. Indestructible: cannot be destroyed by attacks |
| **Nuclear Plant** | 40 | +15 | +150 | Coastline only. Indestructible: cannot be destroyed by attacks |

---

## Combat

### Damage Formula

```
Damage = ATK - DEF + terrain_bonus - range_penalty
```

### Terrain Defense Bonuses

| Tile | Defense Bonus |
|---|---|
| Mountain | +3 |
| Forest (Tree) | +1 |
| Base | +2 |

### Range Penalty

- Attacking at **maximum range** incurs a **−1 damage penalty**.

---

## Capture Mechanics

- Move a unit onto an **unowned or enemy Base/Town** to trigger the capture menu.
- Capturing a **Town** converts it to a Base for the capturing player (+50 XP, income starts).
- Capturing an **enemy Base** transfers it fully (+250 XP). The base retains its current level.
- After capture, the capturing unit's `hasActed = true`.
- Capturing a base **spawns wild animals** in the surrounding area.

---

## Fog of War

- Each player sees only tiles within the **vision range** of their own units and structures.
- Fog updates on every turn transition.
- **Stealth/Cloak** (Sniper Camouflage, Wraith/B2, Submarine) hides units even within enemy vision unless the enemy is **within 1 tile** or a **Radar Station** is in range.
- A **Signal Jammer** forces any unit inside its 4-tile radius to see only 1 tile.

---

## Ruins (Scavenging)

When a unit stands on a **Ruins** tile, they can perform a **Scavenge** action (consumes the unit's turn). The ruins are permanently removed and a random reward is granted:

| Chance | Outcome |
|---|---|
| 20% | +15 Funding |
| 20% | +1,000 XP |
| 20% | Spawns a Recon Drone (can act immediately) |
| 20% | Spawns a Sniper on a nearby land tile (or +15 Funding if no space) |
| 20% | Spawns a Destroyer at the nearest water tile (or +1,000 XP if landlocked) |

---

## Save & Load

- Game state saves to **JSON** under `saves/<gameName>_<seed>.json`.
- Persisted data includes: seed, player names, turn count, XP, funding, map state, unit positions, and per-unit action flags (`hasMoved`, `hasActed`).
- Loading fully reconstructs all entities and preserves mid-turn action limits.

---

## HUD

| Element | Description |
|---|---|
| **Top Bar** | Turn counter · Active player name · Funding `[current] (+income/turn)` · XP total |
| **Unit Info Panel** | Sliding panel showing selected unit's name, HP, stats, and action buttons |
| **Summon Menu** | Slide-in panel listing purchasable units with costs (grayed out if insufficient funds) |
| **Build Menu** | Slide-in panel for constructing structures within base territory |
| **Capture Menu** | Slide-in panel for capturing adjacent structures |
| **Level-Up Popup** | Animated popup on base promotion with bonuses and unlocks |
| **Super Unit Popup** | Choice popup at Level 5 to select and spawn a super unit |
| **Floating Text** | XP, Funding, and event text floats above tiles during gameplay |
| **Game Over Screen** | Displayed when all enemy bases are captured; returns to main menu |

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| **Java** | 8 / 17 | Core language |
| **libGDX** | 1.14.0 | Game framework (graphics, input, audio) |
| **Ashley ECS** | 1.7.4 | Entity Component System |
| **LWJGL 3** | — | Desktop (Windows) backend |
| **gdx-freetype** | 1.14.0 | TrueType font rendering |
| **Box2D** | 1.14.0 | Physics (via gdx-box2d) |
| **Gradle** | 8.x | Build system |
| **JUnit Jupiter** | 5.10.0 | Unit testing |
| **Mockito** | 5.11.0 | Test mocking |

---

## Developer Notes

### Testing Mode

Set `GameConfig.TESTING_MODE = true` to **disable action-per-turn enforcement** — useful for rapidly testing combat and mechanics.

### Project Structure

```
Militopia/
├── core/                   # All game logic
│   └── src/main/java/com/militopia/
│       ├── config/         # Static configs (unit stats, base levels, combat constants)
│       ├── components/     # Ashley ECS components
│       ├── systems/        # ECS systems (combat, fog, render, economy...)
│       ├── factories/      # Entity creation (UnitFactory, EntityFactory)
│       ├── screen/         # Screens (Menu, Game, NewGame, LoadGame, GameOver)
│       ├── managers/       # Asset, Audio, Save, Turn managers
│       └── data/           # Game state and save data models
├── lwjgl3/                 # Desktop launcher
├── assets/                 # Textures, sounds, fonts, skins
└── saves/                  # Runtime save files (auto-generated)
```

### Key Source Files

| File | Purpose |
|---|---|
| `config/UnitStatConfig.java` | Canonical unit stats (HP, ATK, DEF, MOV, RNG, VIS, Cost) |
| `config/BaseLevelConfig.java` | Base XP thresholds, income, unlocks per level |
| `config/CombatConstants.java` | Damage modifiers, terrain bonuses, ability constants |
| `systems/CombatSystem.java` | Full attack resolution, ability triggers, animations |
| `systems/FogSystem.java` | Fog of War visibility computation |
| `systems/WinConditionSystem.java` | Monitors base counts; triggers Game Over |
| `systems/ScavengeSystem.java` | Ruins scavenging rewards logic |
| `factories/UnitFactory.java` | Unit/structure/base entity creation and level-up |
| `screen/GameScreen.java` | Main gameplay screen and systems orchestration |

---

## Roadmap

| Phase | Description | Status |
|---|---|---|
| 0 | Foundation — libGDX + Ashley ECS + isometric renderer | Complete |
| 1 | Turn Engine & Economy | Complete |
| 2 | Base Progression & Level-Up | Complete |
| 3 | Capture & Territory + Fog of War | Complete |
| 4 | Full Unit Roster & Combat | Complete |
| 4.1 | Unit & Building Abilities | Complete |
| 5 | Specialized Structures | Complete |
| 6 | Win / Loss Conditions | Complete |
| 7 | Exploration & Persistence (Ruins, Oil, Save/Load) | Complete |
| 8 | Polish & UX (Animations, SFX, BGM, Floating Text) | In Progress |
| 9 | Advanced Mechanics (Railways) | Planned |

---

## License

This project is a personal project. All rights reserved by the author unless otherwise stated.
