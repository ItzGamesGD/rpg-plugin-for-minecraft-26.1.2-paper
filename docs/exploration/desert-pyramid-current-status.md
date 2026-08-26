# Desert Pyramid Current Implementation Status

- Working branch: `fix/desert-pyramid-full-flow-reconciliation`
- Reconciliation baseline: `032d89afc0c8245666503d9a8d24d5f31b1da793`
- Current audited HEAD: branch tip (verify with `git rev-parse HEAD`; final report records the exact SHA)
- Status: STATICALLY_IMPLEMENTED / UNIT_EXECUTION_BLOCKED / LIVE_SERVER_RETEST_REQUIRED

## Canonical flows

Exterior: padded boundary crossing -> entry-actor capture -> outward repel -> Pyramid quiz -> any answer/default timeout -> guardian.

Underground: validated vanilla Pyramid chest -> room preparation -> delayed final validation -> 3x3 shaft and 7x7 usable room -> four real displays -> push-pillar solve.

Guardian and underground are independent. Both persisted module flags are required for clear.

## Current runtime path

`ExplorationRuntimeManager`, `PyramidRepelComponent`, `ChoicePromptComponent`,
`PyramidGuardianComponent`, `PyramidRoomComponent`,
`PyramidRoomRevealComponent`, `PyramidPushPillarComponent`,
`PyramidPushPillarService`, and `ExplorationSequenceState`.

Bukkit primitive ports are composed with HyunseoRPG mob, reward, and cleanup adapters.

## Persistent metadata

`pyramid-content-version`, `pyramid-entry-actor`, `pyramid-entry-dx`,
`pyramid-entry-dz`, `pyramid-treasure-x/y/z`,
`pyramid-room-prepared`, `pyramid-room-created`,
`pyramid-room-origin/radius/height`, `pyramid-guardian-complete`,
and `pyramid-underground-complete`.

Completed module metadata is authoritative after restart. Legacy radius-3 geometry is reset to retryable metadata while completion flags are preserved. Persisted room-created metadata is checked against bounded shell/floor/roof signatures before reuse; stale records are downgraded. Entry padding is a structure-level policy (default 4.0). Treasure anchors are restricted to canonical normal-chest slots and drive direct shaft planning.

## Legacy boundary

`PyramidRoomLocator`, `PyramidRoomPreflight`, `PyramidModuleProgress`,
and `PyramidVariantModules` remain deterministic/reusable logic or historical test axes;
the live runtime is authoritative through `PyramidRoomService` and the component path above.

Historical checkpoint documents are not current implementation evidence.

## Verification

The bundled Pyramid test was updated for the seven-component canonical configuration.
Sequence reservation/release and production primitive-port composition tests were added.
Room reveal now escalates telegraph effects at T+0/40/80/110/130 and opens 3x3 shaft layers top-to-bottom every bounded interval with rollback snapshots. Guardian/pillar technical failures stay retryable; delayed reveal failures re-arm up to five attempts. `/rpg exploration inspect <uuid>` exposes Pyramid metadata and `/rpg exploration reset <uuid>` clears only that Pyramid. Gradle execution was not available in this workspace (authenticated checkout/tooling unavailable); Paper/client verification remains a live-only requirement.

## Recovery and reward contract

Guardian encounter state is persisted as `not_started`, `quiz_pending`, `spawn_pending`, `active`, or `complete`. A failed guardian start clears the pending prompt and re-arms the next valid entrant; a completed guardian module suppresses all later spawns. Reveal preparation uses bounded retry reservations (60 ticks, five attempts); staged reveal snapshots the complete footprint and rolls back on failure. Underground completion is persisted immediately after the final pillar solve and suppresses puzzle restoration on restart.

Reward delivery records `pyramid-reward-recipient` and `pyramid-reward-state` (`delivering`, `delivered`, `finalized`) before clear. The canonical owner is the persisted entry actor (BALANCE_POLICY_PENDING); inventory overflow uses the existing pending-reward queue. A live restart test is still required to validate the adapter's crash-window behavior.

## Admin/live checklist

Use `/rpg exploration inspect <structure-uuid>` for bounded Pyramid metadata and `/rpg exploration reset <structure-uuid>` to cancel owned runtime tasks/entities and clear only that record. Live validation must cover all cardinal/diagonal entries, quiz answers and timeout, real vanilla chest slots, the seven-second telegraph, staged 3x3 opening, four displays, both module orders, and restart at each persisted checkpoint.
