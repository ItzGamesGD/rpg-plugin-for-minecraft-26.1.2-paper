# Desert Pyramid Current Implementation Status

- Working branch: `fix/desert-pyramid-full-flow-reconciliation`
- Final candidate implementation checkpoint: `a11cc5ef20f9660d2f5382ec0c71324bbe9f6b0f`
- Repository gate: `PASS_PENDING_INDEPENDENT_REVIEW`
- CI: latest final-tip push and pull-request workflows passed (runs 293/294).
- Live runtime status: `LIVE_SERVER_RETEST_REQUIRED`

## Durable underground completion

`PyramidUndergroundCompletionCoordinator` is the sole durable pending-to-complete authority. The final pillar move persists `completion_pending` and its logical position before irreversible board mutation; failed saves cannot publish false durability. Recovery-required structures fail closed and cannot complete or reward.

## Repository and simplified recovery

`StructureRepository` persists a complete world snapshot before publishing index mutations, so failed saves leave no candidate or ghost record. Shaft staging is presentation-only without per-layer durable checkpoints. Interrupted or ambiguous reveals, damaged committed-room signatures, missing displays, and impossible pillar state become `RECOVERY_REQUIRED`; no speculative room, shaft, or pillar reconstruction/replay occurs. Ordinary reload restores displays only from durable logical pillar positions.

## Room, pillar, and entry contracts

The room uses radius 4 (9x9 footprint with a 7x7 usable interior), a consistent 3x3 shaft, protected special blocks, staged top-to-bottom reveal, and four required pillar displays. Entry records the actual actor and movement vector; guardian spawn remains above ground and independent of underground state. Four strict vanilla chest offsets (horizontal ±2/±2 from bounds center, chest-only) share one canonical center.

## Reward transaction

Pyramid rewards use deterministic tokens and a durable pre-delivery claim journal. Completed tombstones survive restart. If delivery occurs but final persistence is ambiguous, the token is durably quarantined as `manual-recovery-required` and can never be automatically reissued, including after Pyramid finalization retry. This fail-closed policy favors no duplicate physical rewards over speculative recovery.

## Legacy/reset and live validation

Migration canonicalizes `pyramid_push_pillars` to `pyramid_pillar_restore`. Reset and diagnostics include completion, reward, logical pillar, and recovery-required metadata. Paper/client retesting remains required for visual timing, terrain-safe guardian spawn, display interaction, TNT protection, and client-visible repel direction.
