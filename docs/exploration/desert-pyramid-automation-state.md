# Desert Pyramid Automation State

PHASE: AWAITING_REVIEW
CURRENT_BRANCH: fix/desert-pyramid-full-flow-reconciliation
CANDIDATE_HEAD: 11d5bf957f76c168754b6689245e2dfd7df371fa
REPOSITORY_GATE: PASS_PENDING_INDEPENDENT_REVIEW
CI_STATE: final handoff push run 340 and pull-request run 341 passed
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
- Final handoff push workflow 340 and pull-request workflow 341 both passed on 11d5bf957f76c168754b6689245e2dfd7df371fa.

LIVE VALIDATION:
- LIVE_SERVER_RETEST_REQUIRED for Paper/client visuals, staged timing, terrain-safe guardian spawning, TNT/display interaction, and repel direction.
