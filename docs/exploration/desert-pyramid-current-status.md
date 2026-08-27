# Desert Pyramid Current Implementation Status

- Working branch: `fix/desert-pyramid-full-flow-reconciliation`
- Candidate implementation checkpoint: `363e570753facd730932fc08858f835dd4a23126`
- Repository gate: `PASS_PENDING_INDEPENDENT_REVIEW`
- CI: fresh final-tip GitHub Actions verification is running `./gradlew clean test --no-daemon`.
- Live runtime status: `LIVE_SERVER_RETEST_REQUIRED`

## Durable underground completion

`PyramidUndergroundCompletionCoordinator` is the sole durable pending-to-complete authority. The final pillar move persists `completion_pending` and its logical position before the irreversible board mutation; failed repository saves cannot publish false in-memory durability. Recovery-required structures are fail-closed and cannot complete or reward.

## Repository and simplified recovery

`StructureRepository` persists a complete world snapshot before publishing index mutations, so failed saves leave no candidate or ghost record. Shaft staging is presentation-only without per-layer durable checkpoints. Interrupted or ambiguous reveals, damaged committed-room signatures, missing displays, and impossible pillar state are marked `RECOVERY_REQUIRED`; no speculative room, shaft, or pillar reconstruction or replay is attempted. Ordinary reload restores displays only from durable logical pillar positions.

## Room, pillar, and entry contracts

The room uses radius 4 (9x9 footprint with a 7x7 usable interior), a consistent 3x3 shaft, protected special blocks, staged top-to-bottom reveal, and four required pillar displays. Partial display creation is cleaned before activation. Entry records the actual actor and movement vector; guardian spawn remains above ground and independent of underground state. The four strict vanilla chest offsets are horizontal ±2/±2 from bounds center and share one canonical center.

## Reward transaction

Pyramid rewards use deterministic tokens and a durable pre-delivery claim journal. Completed tombstones survive restart. If delivery occurs but final persistence is ambiguous, the token is durably quarantined as `manual-recovery-required` and can never be automatically reissued, including after Pyramid finalization retry. This fail-closed policy favors no duplicate physical rewards over automatic recovery from externally moved items.

## Legacy/reset and live validation

Migration canonicalizes `pyramid_push_pillars` to `pyramid_pillar_restore`. Reset and diagnostics include completion, reward, logical pillar, and recovery-required metadata. Paper/client retesting remains required for visual timing, terrain-safe guardian spawn, display interaction, TNT protection, and client-visible repel direction.
