# Desert Pyramid Automation State

PHASE: IMPLEMENTING
CURRENT_HEAD: 3cafab0df64aaec7761bd63a5596171cfb770665
CANDIDATE_HEAD: ffca2b137f1d1dd390d514ce063b4b609b11aa81
LAST_REVIEWED_HEAD: ffca2b137f1d1dd390d514ce063b4b609b11aa81
LAST_SAFE_HEAD: 3851be385434d314dd7fc54370c342e463316c40
REPOSITORY_GATE: FAIL
CI_STATE: FAIL
VERIFIED_SAFE_REGRESSIONS: 0
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

IMPLEMENTING_DEFECT:
- P0 compile restoration only. Repair the concrete current GitHub Actions compiler failures with minimal changes while preserving all VERIFIED_SAFE invariants.

VERIFIED_SAFE_INVARIANTS:
- heartbeat is not an independent underground-completion writer
- actual entrant/movement vector drives Pyramid entry/repel
- guardian surface spawn does not depend on underground room origin
- guardian completion does not synthesize underground completion
- incomplete pillar display spawn cleans partial displays
- committed room recovery does not replay room reveal; bounded retry targets pillar restore only

KNOWN_DEFECTS:
1. P0 - Current candidate does not compile. Known failures: missing java.util.Set resolution in ExplorationRuntimeManager; static-context call to non-static persistUndergroundCompletion in PyramidPushPillarService; invalid pending.world() calls in PyramidRoomService; unavailable Sound.BLOCK_SANDSTONE_BREAK constants; missing ExplorationComponentPhase.PYRAMID_ROOM_REVEAL enum value references.
2. P0 - Restart solved-evidence gap remains: the physical pillar board can become solved before the first durable pyramid-underground-completion-state=completion_pending save succeeds.
3. P0 - Reward lifetime exactly-once gap remains: PendingRewardService.claim can physically add an item before completed-token tombstone and pending-removal state are durably saved.
4. P1 - Behavioral proof remains incomplete across required stateful failure/restart boundaries.
5. P1 - Bounded Outpost regression proof remains incomplete.
6. P1 - Final status/current-status documentation is not yet synchronized.
7. P1 - Final config-to-runtime connectivity and adversarial whole-flow audit remain required after all repairs and green CI.

NEXT_REQUIRED_ACTION: repair compile failures only, add/adjust tests only where compile contract changed, commit a coherent candidate, then set PHASE=AWAITING_REVIEW.
