# Desert Pyramid Current Implementation Status

- Working branch: `fix/desert-pyramid-full-flow-reconciliation`
- Final implementation checkpoint: `a3b595f76a3820d3af37bdf65ddf31f40344eb5f`
- Repository gate: `PASS_PENDING_INDEPENDENT_REVIEW`
- CI: new candidate workflows are running for the final implementation changes.
- Live runtime status: `LIVE_SERVER_RETEST_REQUIRED`

## Runtime integrity

`PyramidPushPillarService` treats a failed Display move as representation corruption: it records `RECOVERY_REQUIRED`, cancels Pyramid work, stops the session, and blocks completion/reward. Active Pyramid room and shaft geometry are protected from ordinary break/place events; malformed or externally damaged state fails closed and requires explicit administrative reset.

## Simplified pillar lifecycle

Pillar activation is single-shot: all configured displays must spawn, otherwise every partial display is removed and activation is frozen. `PyramidPillarRecoveryPolicy`, retry counters, and heartbeat pillar auto-recovery were removed. Normal reload reconstructs Displays only from durable logical pillar positions; `PushPillarBoard` remains the authoritative state.

## Preserved contracts

StructureRepository remains persist-first; staged shaft reveal is presentation-only and interrupted reveals fail closed. The room is radius 4 (9x9 footprint, 7x7 usable interior) with a 3x3 shaft and protected special blocks. Guardian/underground modules remain independent, and rewards retain deterministic tokens with fail-closed manual-recovery quarantine.

## Validation

Focused display-integrity and single-shot reload-policy tests plus the existing Pyramid, repository, config, and Outpost regression suites are included. Paper/client retesting remains required for visual timing, terrain-safe guardian spawn, TNT/display interaction, and repel direction.
