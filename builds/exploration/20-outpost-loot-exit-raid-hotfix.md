# Outpost Loot-Exit Raid Hotfix

## Root cause

The outpost runtime had a proximity activation and a choice prompt, but no inventory listener recorded an actual chest loot action. The heartbeat therefore had no `lootTaken` state to use, and physical exit always followed the generic abandon path.

## Changes

- Added `ExplorationChestLootListener` and registered it with the existing `ExplorationModule` listener set.
- Records only player actions that remove an item from the top inventory of a chest, trapped chest, barrel, or shulker box. Opening a container, dragging items into it, or moving items from the player inventory does not arm the raid.
- Added ephemeral looter/loot tick state to `ExplorationRuntime` and persisted `loot-taken`, `looter`, and `loot-taken-at-tick` on the structure record.
- Changed the canonical outpost choice prompt from proximity activation to `loot_exit`.
- After the existing physical-exit grace period, the manager executes the existing `ChoicePromptComponent`; tier selection still uses the existing `RaidWaveSpawnComponent` and `ExplorationPorts` path.
- Loot owner is preferred after restart, and an already-prompted loot state is restored through the existing lifecycle path.
- Added explicit migration for the previous canonical outpost prompt (`activate` -> `loot_exit`) without changing unrelated operator settings.

## Runtime path

`InventoryClickEvent`
→ `ExplorationChestLootListener`
→ `ExplorationRuntimeManager.markLootTaken(...)`
→ structure metadata/runtime state
→ physical exit grace
→ `LOOT_EXIT` / `ChoicePromptComponent`
→ `/rpg exploration choose`
→ `RaidWaveSpawnComponent`
→ existing custom mob spawn adapter.

## Verification

- `196 tests`
- `0 failures`
- `2 skipped` (existing live Paper-dependent tests)
- JAR: `build/libs/HyunseoRPG-0.1.0-SNAPSHOT.jar`
- SHA-256: `1E82FCDDB26BBF10436829C2C1BF5534E004D1824AB5FB6DA5B8AFCD619D8C85`

## Live verification still required

1. Enable `enabled: true` at the exploration root and `pillager_outpost.enabled: true` with non-zero selection chance in the live external config.
2. Run the explicit exploration migration apply if the external outpost component still has `phase: activate`.
3. Detect a new selected outpost, take one item from its chest, leave the abandon radius, and wait through the configured grace ticks.
4. Confirm the difficulty prompt is delivered to the looter, tier selection spawns the bounded custom wave, and no vanilla raid or Bad Omen is created.
5. Repeat with close/reopen, reload, and server restart between loot and exit to verify persisted loot state.
