# Desert Pyramid Automation State

PHASE: AWAITING_REVIEW
CURRENT_BRANCH: fix/desert-pyramid-full-flow-reconciliation
CANDIDATE_HEAD: e2098cd260890a1e946b81b15fb1bad5a803ece6 (implementation tip; docs commits may advance the branch tip)
REPOSITORY_GATE: PASS_PENDING_INDEPENDENT_REVIEW
CI_STATE: final-tip GitHub Actions verification in progress for the simplified recovery implementation
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

VERIFIED IMPLEMENTATION:
- `PyramidUndergroundCompletionCoordinator` is the single durable pending-to-complete transaction; solved intent is persisted before the irreversible final pillar move.
- `StructureRepository` publishes index changes only after durable world snapshot success; failed save/create operations leave no false pending or ghost record.
- Restart/heartbeat recovery reconciles pending underground completion and committed rooms without replaying room reveal. Corrupt committed-room signatures and ambiguous staged reveals fail closed as RECOVERY_REQUIRED; no speculative room reconstruction is attempted.
- Pyramid staged shaft reveal is presentation-only (3x3 top-to-bottom during one runtime); per-layer durable checkpoints were removed to avoid contradictory physical/durable progress.
- Four strict vanilla chest slots (horizontal ±2/±2 from the bounds center, chest-only) map to the canonical treasure center.
- Deterministic Pyramid rewards journal before tagged exact delivery. Completed tokens and manual-recovery quarantine are durable and suppress stale queue/finalization retries, including when a delivered item is no longer in player storage.
- Reset and diagnostics expose underground, reward, and recovery-required metadata. Guardian and underground modules remain independent.

VALIDATION:
- Prior Exploration workflow runs passed clean compile and JUnit execution.
- The final candidate tip must have a fresh successful Exploration workflow before independent review.

LIVE VALIDATION:
- LIVE_SERVER_RETEST_REQUIRED: Paper/client execution remains required for world mutation timing, visual staged reveal, terrain-safe guardian spawning, TNT/special-block protection, display interaction, and client-visible repel direction.
