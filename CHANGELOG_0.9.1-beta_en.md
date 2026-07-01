# HBM NTM Reforged — 0.9.1-beta (technical)

**Minecraft:** 1.12.2 · **Forge:** 14.23.5.2859+  
**File:** `HBM-NTM-Reforged-0.9.1-beta.jar`  
**Release type:** Beta

---

## Localization & encoding
- Re-saved English, Russian, Ukrainian, and Space-addon lang files as UTF-8.
- Fixed mojibake in Russian/Ukrainian entries (garbled Cyrillic in tooltips, names, and drillbit strings).
- Added `LangEncodingVerifier` checks for common encoding mistakes.

## Creative tabs & JEI sorting
- Rebuilt creative-tab sort order (`creative_tab_order.txt`) with clustering scripts and verification.
- Added search-tab sort helpers and mixins so filtered creative inventory keeps the intended order.
- Fixed JEI ingredient list ordering when `hbm` and `hbmspace` items were split into separate mod buckets (late mixin on `IngredientListElement`).
- Hid depleted RTG pellet from JEI (internal/depleted variant).

## Recipes & crafting parity
- Ported missing EE block, parts, confirm-port, and weapon-tab crafts.
- Updated machine, anvil, arc welder, assembly, crystallizer, cyclotron, hadron, magic, outgasser, press, and shredder recipe tables.
- Added `ams_muzzle` shaped crafting recipe.
- Wired new recipe classes in `CraftingManager` / registry.

## AMS Emitter
- Ported AMS Emitter multiblock (block, tile entity, GUI, container, renderer, models, textures).
- Fixed cryogel tank resetting every tick in the emitter GUI.
- Fixed AMS emitter GUI tank texture Y offset.

## Sliding blast door (keypad variant)
- Fixed ghost keypad blocks left behind when breaking `sliding_blast_door_2`.
- Store keypad positions in door TE NBT; clear keypads on destroy with proper flag handling.
- Optimized bulk destroy to avoid large world scans that caused lag spikes.

## Weapons (sedna)
- Shotgun meathook: reliable release on swap/death/disconnect; client strafe reset packet fix.
- Hold-RMB aim assist default restored; `setupStandardConfigurationNoAim()` for guns that should not aim.
- Grenade universal sub-item sort index for creative/JEI consistency.
- Sedna factory tweaks (catapult, calibers, rocket/accelerator/energy).
- Weapon render fixes (Fat Man, SPAS-12).

## Repo & tooling
- Removed accidental `com/**/*.class` artifacts from project root.
- Added/updated creative-tab and recipe audit scripts under `tools/`.

## Since 0.9.0-alpha (already on main)
Includes ITER/plasma multiblocks, fluid textures, blast-resistance rebalance, hazard fixes, sliding-door migration, SILEX/ITER JEI handlers, and broad recipe/armor/weapon parity commits pushed before this beta tag.