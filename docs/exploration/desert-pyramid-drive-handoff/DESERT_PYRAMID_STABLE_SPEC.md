# DESERT PYRAMID — STABLE AUTHORITATIVE SPEC

Source: Google Drive document `DESERT_PYRAMID_STABLE_SPEC` (`1r8gdE3e5pp4wiLdY49FeyLTxqvdF91JIuj7hz-jm0E8`).
Transferred to GitHub for Codex Cloud access on 2026-08-28 UTC.

HyunseoRPG Exploration

## PURPOSE

This document contains the stable design and runtime invariants for Desert Pyramid. It is authoritative for intended behavior. Historical audits and old recovery requirements are NOT authoritative unless DESERT_PYRAMID_CURRENT_WORK explicitly references them.

## 1. CORE ARCHITECTURE

Normal play must be deterministic and must not create invalid state.
Normal server/plugin reload supports only minimal deterministic reconstruction from durable logical state.
Unsupported player interaction is rejected or ignored.
External corruption or impossible runtime/world state FAILS CLOSED.
Do not speculatively reconstruct damaged rooms, shafts, Displays, or puzzle progress.

## 2. EXTERIOR / QUIZ / GUARDIAN FLOW

Player outside padded Pyramid boundary
→ attempts entry from any direction
→ actual entering player and actual movement/entry direction captured
→ repel toward the side from which the player entered
→ quiz starts
→ ANY answer/default routing
→ guardian encounter
→ guardian spawns at a safe ABOVE-GROUND location
→ guardian objective
→ guardian module completion persisted.

Repel direction must use actual entry/movement direction, not underground room origin.
Guardian must never spawn underground.
Guardian completion must never reveal the underground room, start pillars, or synthesize underground completion.

## 3. TREASURE / UNDERGROUND FLOW

Valid intended Vanilla Desert Pyramid treasure chest interaction
→ canonical treasure chamber center derived
→ safe room/shaft preflight
→ room prepared
→ approximately 7-second telegraph
→ staged 3x3 shaft opening from top toward underground room
→ underground room becomes accessible
→ pillar puzzle starts directly
→ puzzle solved
→ underground module completion persisted.

All four intended Vanilla treasure chests must resolve to the same canonical treasure center. Arbitrary Pyramid containers are rejected. Trigger chest coordinate and shaft center are different concepts.

## 4. ROOM GEOMETRY

Required usable interior: 7x7.
If walls consume the outer ring, total footprint must be 9x9.
Room height must support player movement, pillar Displays, pillar movement, and visibility.

## 5. SHAFT

Shaft footprint is 3x3 across preflight, safety validation, snapshot where retained, carve, staged reveal, and final geometry.
Do not destroy the whole treasure chamber floor.
Protect TNT and prohibited/special blocks according to the Pyramid safety contract.
Staged reveal is presentation; do not create an elaborate crash-reconstruction state machine for individual visual layers.

## 6. PILLAR PUZZLE — AUTHORITATIVE LOGICAL STATE

Logical pillar position is authoritative, e.g. pillar ID → predefined pathIndex.
Display coordinates are NOT authoritative game state.
Supported player input validates a legal transition, updates logical index, and moves the representation to the predefined coordinate for that index.
Do not infer logical state from arbitrary Display coordinates.
A pillar must not be able to leave its predefined path through normal code.
Wrong but legal puzzle moves are valid gameplay, not corruption.

If four pillars are configured, all 4/4 must be created before puzzle activation. If creation partially succeeds and then fails, clean up created representations and do not activate the puzzle.

## 7. NORMAL RELOAD

Durable logical state is the source of truth.
On ordinary reload, recreate disposable physical representations deterministically from durable logical state where needed.
Do not inspect arbitrary world state and guess progress.

## 8. CORRUPTION / IMPOSSIBLE STATE

Examples: required Display unexpectedly missing during active play, required room geometry damaged, required puzzle block modified, impossible pillar state, invalid ownership, room signature mismatch.
Required behavior:
detect invariant violation
→ cancel Pyramid runtime tasks for the affected progression
→ freeze progression
→ mark FAILED or RECOVERY_REQUIRED using the smallest compatible mechanism
→ log exact reason
→ no completion/reward
→ explicit administrative reset required.

Do not automatically reveal again, re-carve shaft, reconstruct the room, guess pillar position, synthesize completion, or silently continue.

## 9. PREFLIGHT

Prefer prevention over recovery. Before activation validate required space, room geometry feasibility, four pillar placements and paths, shaft candidate/safety, protected blocks, Display creation prerequisites, and puzzle configuration consistency. Failure before activation must fail closed rather than partially start.

## 10. MODULE INDEPENDENCE

Guardian and underground are independent persistent modules.
Guardian-first order must work.
Underground-first order must work.
Neither module may start, complete, overwrite, or synthesize the other.
Overall Pyramid completion becomes eligible only when both are complete.

## 11. PERSISTENCE OWNERSHIP

Failed durable persistence must never masquerade as a successful logical commit.
Underground/shaft writes must load the latest authoritative StructureRecord and merge only metadata they own. A stale captured StructureRecord must never overwrite guardian or other unrelated metadata.
Avoid duplicate writers and overlapping recovery writers.

## 12. REWARD

Use one deterministic Pyramid reward token/obligation. Duplicate automatic physical reward delivery is forbidden.
If physical delivery/finalization becomes genuinely ambiguous, prefer fail-closed/manual recovery over automatically issuing another reward.
Do not try to infer where an already delivered item went.
Keep only the token/tombstone/journal mechanisms necessary for duplicate suppression and correct normal transaction ordering.

## 13. CLEARED / FAILED BEHAVIOR

CLEARED Pyramid must not reactivate.
FAILED/RECOVERY_REQUIRED Pyramid must not continue progression or reward until explicit administrative intervention/reset.
Do not silently reset behind the player.

## 14. OUTPOST SCOPE

Outpost is not an active feature-development target here. Only bounded regression verification/fixes are allowed for previously intended behavior: loot-exit grace not erased by proximity activation; intended grace/raid transition; next-wave scheduling after objective completion; delayed raid target chase. No Outpost redesign or recovery expansion.

## 15. DESIGN PRINCIPLE

PREVENT INVALID NORMAL STATE
+
DETERMINISTIC NORMAL RELOAD
+
FAIL CLOSED ON EXTERNAL CORRUPTION

NOT:
DETECT EVERYTHING
+
RECONSTRUCT EVERYTHING
+
RETRY EVERYTHING.

Stable pillar graph rule: Desert Pyramid uses at least three pillars (default four). Each pillar has a deterministic cardinal path inside the 7x7 usable grid; allowed-cell sets and target cells are pairwise disjoint so every legal solve order remains solvable. Configuration validation rejects overlap or unreachable paths before activation.
