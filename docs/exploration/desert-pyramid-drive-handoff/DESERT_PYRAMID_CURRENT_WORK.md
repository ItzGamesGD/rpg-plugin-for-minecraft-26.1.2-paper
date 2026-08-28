# DESERT PYRAMID — CURRENT WORK

Updated for Codex Cloud handoff.

## Project context

HyunseoRPG is a Paper-based Minecraft RPG plugin.

The project adds custom RPG systems including:

- exploration/structure events
- custom mobs and encounters
- alchemy/brewing
- farming progression
- custom weapons/items
- persistent structure/runtime state

Current development priority is Exploration.

For this task, focus only on the Desert Pyramid implementation.

Do not redesign unrelated systems.

Do not modify main.

Preserve existing Outpost behavior unless a shared-code regression is directly proven.

## Repository

`ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper`

## Target branch

`fix/desert-pyramid-full-flow-reconciliation`

## Audited handoff HEAD

`74a97d5bfa562358365dbcf6a633ff2baaa1264f`

## Main baseline

`413f558b500311bbcfa236e10c835eddbac0c49c`

## Execution mode

Manual review/update handoff. No repository code edits were made in the review step that produced this document.

## Repository gate

FAIL

## Live runtime gate

FAIL

`LIVE_RUNTIME_DEFECT_CONFIRMED`

## Current verified implementation improvements to preserve

- StructureRepository durability ordering: failed persistence must not become authoritative in-memory state.
- Shaft per-layer durable progress was removed; staged reveal is presentation-only and can fail closed.
- Logical pillar state is authoritative; Display coordinates are representation only.
- Missing or invalid Display move failure fails closed.
- Speculative pillar retry/reconstruction was removed.
- Normal reload reconstructs pillar Displays deterministically from durable logical state.
- Four safe disjoint pillar paths and validator were added.
- Room floor, walls, ceiling and shaft geometry protection were added.
- RECOVERY_REQUIRED central completion gate was added.
- Guardian and underground modules remain intended to be independent.
- Reward duplicate suppression remains fail-closed via deterministic token, journal/tombstone, and manual recovery quarantine.

## Confirmed static defects

### P1 — Pillar validation timing is too late

Current startup validation checks official phases/components but does not parse and validate actual Pyramid pillar definitions. `PyramidPillarConfigurationValidator` runs in the pillar component after room preparation, telegraph, staged shaft, room carve and room commit. Invalid pillar config can therefore mutate the world before being rejected.

Minimal repair target: validate bundled/active Pyramid pillar definitions during registry/module startup before listeners/tasks/world mutation, while keeping runtime validation as defense-in-depth.

### P1 — Pre-active room/shaft protection gap

Current block protection is tied to `pyramid.puzzle.active`. Protection begins only after 4/4 pillar Display startup succeeds. Prepared future room, telegraph period, staged shaft opening, opened shaft layers, and carved room before pillar activation are not protected.

Minimal repair target: protection should begin when underground room/reveal ownership is durably established and continue through active/reset/completion using authoritative room/shaft geometry. Explosion protection is not currently covered.

## Confirmed live defects

### P0 — Vanilla Desert Pyramid treasure chest recognition likely rejects real chest positions

Current predicate accepts `CHEST` only at diagonal offsets `abs(dx)==2` and `abs(dz)==2` from `floor(bounds center)`, and only `y` between `bounds.minY` and `bounds.minY+3`.

Target-version-adjacent generator evidence indicates four vanilla treasure chests are cardinal offsets around the center, not diagonal, and the underground chest Y is below the upper structure base/bounds min used by the exact predicate.

This is the highest-confidence root cause for the live finding: taking loot from the vanilla treasure chest did not trigger underground Pyramid flow.

Live coordinate validation against the deployed Paper server is still required.

### P1 — Exterior encounter presentation leaks Outpost identity

Pyramid routing itself appears to use the Pyramid guardian path, not Outpost raid waves, but command/user-facing messaging and shared raid objective language include hardcoded Outpost wording. This likely explains the live observation that the encounter visibly felt like existing Outpost raid reuse.

### P1 — Pyramid-specific blindness/darkness presentation is missing or not implemented

Production/config search found no active Pyramid-specific blindness/darkness effect component or listener.

Classify as `CONTENT_PRESENTATION_MISSING` unless a separate current spec says it was intentionally removed.

### P2 — Exploration status NONE is misleading/incomplete

The status command reports active runtimes and recent end reasons, not all persisted inactive StructureRecords.

The displayed `NONE` likely means `lastEnd=NONE` rather than no Pyramid record. This is probably independent from the chest recognition failure, but needs live trace/screenshots to confirm exact UI path.

## Additional review findings

- Structure detection maps `minecraft:desert_pyramid` to `desert_pyramid` and config enables Pyramid, but discovery is chunk-load based. Already-loaded or old chunks may remain unindexed until chunk reload or explicit scan.
- Chest trigger uses `InventoryClickEvent` at `MONITOR`/`ignoreCancelled`. Opening alone intentionally does not trigger; item removal is required. Normal click/shift/hotbar/drop paths are mostly covered. Drag/other automated transfer paths are not proof-covered.
- `markLootTaken` calls `activate()`, but a failed activation can leave the loot trigger path as a silent no-op at the trigger site.
- No detailed accept/reject log exists for Pyramid chest coordinate/type rejection.

## Minimal repair queue for next Codex Cloud implementation

1. Fix Pyramid treasure chest geometry predicate to match actual Paper 1.20.6/target server Desert Pyramid chest coordinates and Y/bounds semantics. Add tests for all four real vanilla chests, rejection of arbitrary containers, and shared canonical center.
2. Add startup-time Pyramid pillar config validation before any room/shaft world mutation. Ensure invalid active config fails module/registry validation early.
3. Extend Pyramid geometry protection to the pre-active underground ownership window: prepared/telegraph/reveal/staged shaft/carved room before pillar activation. Include actual shaft vertical range and room ceiling. Decide whether explosion block filtering belongs in the same protection contract.
4. Repair Outpost-specific user-facing wording in Pyramid exterior flow while preserving shared engine reuse where safe.
5. Implement or explicitly scope Pyramid darkness/blindness presentation. If intended, wire it to Pyramid quiz/guardian flow and test it.
6. Improve status/diagnostics so live testers can distinguish no record, inactive persisted record, active runtime, selected vanilla/no-content variant, and `lastEnd=NONE`.
7. Add integration-level tests for `markLootTaken -> PYRAMID_LOOT_TRIGGER`, exact treasure geometry, status lookup, pre-active protection lifecycle, and startup validator timing.

## Live retest prerequisites

- Confirm deployed server JAR/head is `74a97d5bfa562358365dbcf6a633ff2baaa1264f` or newer repair head.
- Confirm data-folder `exploration/structures.yml` is migrated/current and Pyramid is enabled with current phases.
- Use a newly generated Pyramid or force reindex/reload of the exact Pyramid chunks; old already-loaded chunks may not have records.
- Capture actual StructureRecord id/type/variant/state/bounds/metadata for the tested Pyramid.
- Capture actual Paper chest block coordinates and types for all four treasure chests.
- Test both item-removal paths and status command output with logs enabled.

## Live trace log checklist

- Structure candidate detected and record created.
- Exploration activation for `desert_pyramid`.
- Pyramid entry boundary crossed and repel vector logged.
- Quiz armed and answer/default routed to guardian spawn.
- Guardian spawn attempted at above-ground exterior location.
- Chest accepted or rejected with coordinate/type reason after repair instrumentation.
- Loot armed and `PYRAMID_LOOT_TRIGGER` executed.
- Room preflight prepared.
- Reveal scheduled and staged reveal started/completed.
- Pillar activation started with 4/4 Displays.
- Guardian and underground module completion each persisted independently.
- Central completion gate checked failure state before reward/CLEARED.

## Test coverage gaps

- Current pillar validator tests are useful unit proof, including solve-order coverage.
- Current geometry protection tests are mostly helper/policy proof, not full lifecycle proof for pre-active reveal windows.
- Current central failure gate tests are mostly helper proof, not full service/integration proof.
- No strong integration proof for real structure detection, status lookup, real inventory event semantics, treasure chest geometry predicate, `markLootTaken` to room reveal, Pyramid-specific exterior presentation, or darkness/blindness.

## Shared-root conclusions

- Status `NONE` + treasure chest not recognized: likely independent or insufficient evidence. Chest failure has a strong geometry root cause; status `NONE` is likely a status projection/diagnostic issue.
- Outpost-like encounter + missing blindness/darkness: likely related at presentation level, but code root causes differ. Outpost wording leak is separate from missing Pyramid-specific effect implementation.

## Google/GitHub document rule for next handoff

- Do not append large historical transcripts.
- Keep this current work file as the concise authoritative current-state handoff.
- Do not modify `DESERT_PYRAMID_STABLE_SPEC` unless the stable gameplay contract changes.
- Do not modify main.

## Current state for Codex Cloud

Repository-addressable remaining defects: PRESENT.

Primary blocker: live underground trigger likely disconnected because real vanilla chest geometry is not accepted.

Next action: implementation repair on branch `fix/desert-pyramid-full-flow-reconciliation`, then new CI and live retest.
