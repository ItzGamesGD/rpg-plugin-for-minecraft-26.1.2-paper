# Desert Pyramid Automation State

PHASE: AWAITING_REVIEW
CURRENT_BRANCH: fix/desert-pyramid-full-flow-reconciliation
CANDIDATE_HEAD: 1b2ef4ec879075de2f018b033b81c2e88f563337
REPOSITORY_GATE: PASS_PENDING_INDEPENDENT_REVIEW
CI_STATE: push run 365 and pull-request run 366 passed
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

IMPLEMENTATION:
- Central RECOVERY_REQUIRED completion gate protects Pyramid clear/reward.
- Active room ceiling/floor/walls and full 3x3 shaft bounds are protected.
- Pillar graph uses four deterministic disjoint quadrant paths; validator rejects overlap, invalid bounds, duplicate targets, and unreachable paths.
- Pillar activation is single-shot with 4/4 spawn requirement and partial cleanup; obsolete retry/recovery machinery and heartbeat restore are removed.
- Durable logical pillar positions remain authoritative for normal reload reconstruction.
- Persist-first StructureRepository, presentation-only staged shaft reveal, module independence, and fail-closed reward quarantine remain intact.

VALIDATION:
- New validator solve-order/overlap, bundled-config, central gate, and geometry policy tests plus existing repository/Pyramid/config/Outpost suites pass in CI.

LIVE VALIDATION:
- LIVE_SERVER_RETEST_REQUIRED for Paper/client visuals, staged timing, terrain-safe guardian spawning, TNT/display interaction, and repel direction.
