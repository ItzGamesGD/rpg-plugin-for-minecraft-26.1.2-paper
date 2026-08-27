# Desert Pyramid Automation State

PHASE: AWAITING_REVIEW
CURRENT_BRANCH: fix/desert-pyramid-full-flow-reconciliation
CANDIDATE_HEAD: a3b595f76a3820d3af37bdf65ddf31f40344eb5f
REPOSITORY_GATE: PASS_PENDING_INDEPENDENT_REVIEW
CI_STATE: final candidate workflows running
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
- Focused Pyramid display/reload policy tests and existing repository/Pyramid/config/Outpost suites are included.
- Final candidate CI is required to pass on the updated branch.

LIVE VALIDATION:
- LIVE_SERVER_RETEST_REQUIRED for Paper/client visuals, staged timing, terrain-safe guardian spawning, TNT/display interaction, and repel direction.
