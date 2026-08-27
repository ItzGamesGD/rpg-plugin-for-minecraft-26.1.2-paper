# Desert Pyramid Automation State

PHASE: AWAITING_REVIEW
CURRENT_BRANCH: fix/desert-pyramid-full-flow-reconciliation
CANDIDATE_HEAD: 617a0aa0d90786744ac29c5323c6b9c42e6db19c
REPOSITORY_GATE: PASS_PENDING_INDEPENDENT_REVIEW
CI_STATE: latest candidate workflow verification in progress
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

VERIFIED IMPLEMENTATION:
- PyramidUndergroundCompletionCoordinator is the sole durable pending-to-complete authority; recovery-required structures fail closed.
- Final pillar intent and logical position persist before irreversible board mutation.
- StructureRepository publishes index changes only after durable world snapshot success; failed save/create leaves no false pending or ghost record.
- Shaft staging is presentation-only (3x3 top-to-bottom); interrupted/ambiguous reveals and damaged room signatures fail closed without speculative reconstruction.
- Durable logical pillar positions are authoritative; reload restores displays from them only.
- Four strict vanilla chest slots (horizontal ±2/±2 from bounds center, chest-only) map to one canonical treasure center.
- Deterministic reward journal/tombstones and manual-recovery quarantine prevent duplicate delivery after ambiguous persistence.
- Reset and diagnostics expose underground, reward, logical pillar, and recovery-required metadata. Guardian and underground modules remain independent.

VALIDATION:
- Earlier workflows passed compile and JUnit; workflows for the latest candidate are running.

LIVE VALIDATION:
- LIVE_SERVER_RETEST_REQUIRED: Paper/client execution remains required for world mutation timing, staged visuals, terrain-safe guardian spawning, TNT/special-block protection, display interaction, and repel direction.
