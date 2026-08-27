# Desert Pyramid Automation State

PHASE: AWAITING_REVIEW
CURRENT_BRANCH: fix/desert-pyramid-full-flow-reconciliation
CANDIDATE_HEAD: 8cbfaf08c0d1e6fe4b70b05d9c1436593d2242c9 (automation-state commit may advance the branch tip)
REPOSITORY_GATE: PASS_PENDING_INDEPENDENT_REVIEW
CI_STATE: pending GitHub Actions verification for the final documentation tip
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

VERIFIED IMPLEMENTATION:
- `PyramidUndergroundCompletionCoordinator` is the single durable pending-to-complete transaction; solved intent is persisted before the irreversible final pillar move.
- `StructureRepository` publishes index changes only after durable world snapshot success; failed save/create operations leave no false pending or ghost record.
- Restart/heartbeat recovery reconciles pending underground completion and committed rooms without replaying room reveal; stale room metadata is downgraded atomically and pillar startup failures clear transient ownership so bounded pillar-only recovery remains reachable.
- Pyramid staged shaft reveal is a durable 3x3 top-to-bottom checkpoint sequence and resumes from persisted layer progress after reload.
- Four strict vanilla chest slots (horizontal ±2/±2 from the bounds center, chest-only) map to the canonical treasure center.
- Deterministic Pyramid rewards journal before tagged exact delivery. Completed tokens and manual-recovery quarantine are durable and suppress stale queue/finalization retries, including when a delivered item is no longer in player storage.
- Reset and diagnostics expose the introduced underground, reward, and shaft-progress metadata. Guardian and underground modules remain independent.

VALIDATION:
- Prior Exploration workflow runs passed clean compile and JUnit execution.
- The final candidate tip must have a fresh successful Exploration workflow before independent review.

LIVE VALIDATION:
- LIVE_SERVER_RETEST_REQUIRED: Paper/client execution remains required for world mutation timing, visual staged reveal, terrain-safe guardian spawning, TNT/special-block protection, display interaction, and client-visible repel direction.
