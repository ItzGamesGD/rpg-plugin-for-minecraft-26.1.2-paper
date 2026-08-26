# Desert Pyramid Automation State

PHASE: IMPLEMENTING
CURRENT_HEAD: d00cf55f07c448ff98763066f27280006706e613
CANDIDATE_HEAD: 048d15ac61ac11ba48466206f6658e63e758f100
LAST_REVIEWED_HEAD: 048d15ac61ac11ba48466206f6658e63e758f100
LAST_SAFE_HEAD: 3851be385434d314dd7fc54370c342e463316c40
REPOSITORY_GATE: FAIL
CI_STATE: FAIL
VERIFIED_SAFE_REGRESSIONS: 0
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

IMPLEMENTING_DEFECT:
- P0 compile blocker in ConfigMigrationService Desert Pyramid canonical migration: replace the lambda that captures reassigned local `value` with a non-capturing explicit entry loop. This is intended as a compile-only repair with no behavioral change.

VERIFIED_SAFE_INVARIANTS:
- heartbeat is not an independent underground-completion writer
- actual entrant/movement vector drives Pyramid entry/repel
- guardian surface spawn does not depend on underground room origin
- guardian completion does not synthesize underground completion
- incomplete pillar display spawn cleans partial displays
- committed room recovery does not replay room reveal; bounded retry targets pillar restore only

KNOWN_DEFECTS:
1. P0 - Current CI compile blocker: ConfigMigrationService Desert Pyramid canonical migration uses a lambda that captures reassigned local `value`.
2. P0 - Restart solved-evidence gap remains: the physical pillar board can become solved before the first durable pyramid-underground-completion-state=completion_pending save succeeds. If that first save fails and the server stops before the runtime retry succeeds, restart may have no durable solved evidence.
3. P0 - Reward lifetime exactly-once gap remains: PendingRewardService.claim can physically add an item to inventory before completed-token tombstone and pending-removal state are durably saved. Save failure followed by crash/restart can replay the same pending reward.
4. P1 - Behavioral proof remains incomplete across persistence failure/restart boundaries, reward tombstone failure, guardian-first/underground-first stateful flow, exact 4/4 pillar partial-failure behavior, and other required Pyramid matrix cases.
5. P1 - Bounded Outpost regression proof remains incomplete for loot-exit grace preservation, 24-40 radius behavior, next-wave scheduling, and delayed target chase.
6. P1 - Final status/current-status documentation is not yet synchronized into a valid completion certificate for the eventual passing HEAD.
7. P1 - Final config-to-runtime connectivity and adversarial whole-flow audit remain required after all repairs and a green current-HEAD CI run.

NEXT_REQUIRED_ACTION: modifier is repairing only the ConfigMigrationService compile blocker, then must commit and hand back AWAITING_REVIEW without starting another defect.
