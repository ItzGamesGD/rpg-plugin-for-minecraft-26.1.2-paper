# Desert Pyramid Automation State

PHASE: AWAITING_REVIEW
CURRENT_BRANCH: fix/desert-pyramid-full-flow-reconciliation
CANDIDATE_HEAD: 990981251d1baeab5065b2df7cb66c0765af612e
REPOSITORY_GATE: PASS_PENDING_INDEPENDENT_REVIEW
CI_STATE: candidate push run 337 passed; fresh verification is required after handoff-doc updates
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

IMPLEMENTATION:
- Display move false is representation corruption: RECOVERY_REQUIRED, task cancellation, no completion/reward.
- Active Pyramid room/shaft geometry is protected from ordinary player edits.
- Pillar activation is single-shot with 4/4 spawn requirement and partial cleanup; obsolete pillar retry/recovery machinery and heartbeat restore are removed.
- Durable logical pillar positions remain authoritative for normal reload reconstruction.
- Persist-first StructureRepository, presentation-only staged shaft reveal, module independence, and fail-closed reward quarantine remain intact.

VALIDATION:
- Focused Pyramid display/protection/reload-policy tests and existing repository/Pyramid/config/Outpost suites are included.
- Candidate push workflow 337 passed; final handoff candidate must pass fresh push and pull-request workflows.

LIVE VALIDATION:
- LIVE_SERVER_RETEST_REQUIRED for Paper/client visuals, staged timing, terrain-safe guardian spawning, TNT/display interaction, and repel direction.
