# Desert Pyramid Current Implementation Status

- Working branch: `fix/desert-pyramid-full-flow-reconciliation`
- Candidate implementation checkpoint: `b06538966e48cbb63d900fd40feea8d946ed5b17` (documentation commits may advance the tip)
- Repository gate: `PASS_PENDING_INDEPENDENT_REVIEW`
- CI: the candidate implementation runs the Exploration workflow `./gradlew clean test --no-daemon`; the final documentation tip must be rechecked by GitHub Actions.
- Live runtime status: `LIVE_SERVER_RETEST_REQUIRED`

## Durable underground completion

`PyramidUndergroundCompletionCoordinator` is the sole durable pending-to-complete authority. The final pillar move persists `completion_pending` before the irreversible board mutation; a failed repository save does not publish a false in-memory record. Restart and heartbeat recovery reconcile only pending/legacy contradictory evidence through the same coordinator, and repeated reconciliation is idempotent.

## Repository and shaft recovery

`StructureRepository` persists the complete world snapshot before publishing index mutations, so failed saves leave no candidate or ghost record. Staged Pyramid shaft progress is persisted after each 3x3 layer and resumed from that checkpoint after reload; committed rooms restore pillars only and never replay room carving.

## Room, pillar, and entry contracts

The room uses radius 4 (9x9 footprint with a 7x7 usable interior), a consistent 3x3 shaft, protected special blocks, staged top-to-bottom reveal, and four required pillar displays. Partial display creation is cleaned before bounded pillar-only retry. Entry records the actual actor and movement vector; guardian spawn remains above ground and independent of underground state. The four canonical vanilla chest offsets are strict chest-only slots at horizontal ±2/±2 from the bounds center (the target Paper geometry) and share one canonical center.

## Reward transaction

Pyramid rewards use deterministic tokens. A durable claim journal is written before exact tagged inventory delivery. Completed tombstones survive restart; stale mailbox rows are ignored when a completion tombstone exists. If delivery occurs but final persistence fails, restart either observes the tagged item and completes the tombstone or moves the token to durable `manual-recovery-required` quarantine when the item is no longer observable. Quarantined tokens are fail-closed and can never be automatically reissued, including after Pyramid finalization retry.

## Legacy/reset and live validation

Migration canonicalizes `pyramid_push_pillars` to `pyramid_pillar_restore`. Reset and diagnostics include completion state, reward state, staged shaft progress, and stale-room metadata cleanup. Paper/client retesting remains required for visual timing, terrain-safe guardian spawn, display interaction, TNT protection, and client-visible repel direction.
