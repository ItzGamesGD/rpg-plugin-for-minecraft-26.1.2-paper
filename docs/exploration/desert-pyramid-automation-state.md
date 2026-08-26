# Desert Pyramid Automation State

PHASE: NEEDS_FIX
CURRENT_HEAD: 5b4721b9b5178762ff17aefbb94c73207fca71ca
CANDIDATE_HEAD: dbf7240cfe8198f09a69af2984d13d6939fa89fa
LAST_REVIEWED_HEAD: dbf7240cfe8198f09a69af2984d13d6939fa89fa
LAST_SAFE_HEAD: 3851be385434d314dd7fc54370c342e463316c40
REPOSITORY_GATE: FAIL
CI_STATE: RUNNING_HEAD_5b4721b9b5178762ff17aefbb94c73207fca71ca
VERIFIED_SAFE_REGRESSIONS: 0
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

REVIEW_RESULT:
- The ConfigMigrationService compile-only repair is statically correct: the reassigned local `value` is no longer captured by a lambda and is populated through an explicit `Map.Entry<?, ?>` loop.
- No production/test behavior was intentionally changed by that repair.
- No new regression of a previously VERIFIED_SAFE invariant was found from this repair.
- No workflow run exists for source-only candidate dbf7240cfe8198f09a69af2984d13d6939fa89fa; the subsequent handoff HEAD 5b4721b9b5178762ff17aefbb94c73207fca71ca includes that source repair and its Exploration build/JUnit workflow is still running at review time. Therefore CI cannot yet be marked PASS.

VERIFIED_SAFE_INVARIANTS:
- heartbeat is not an independent underground-completion writer
- actual entrant/movement vector drives Pyramid entry/repel
- guardian surface spawn does not depend on underground room origin
- guardian completion does not synthesize underground completion
- incomplete pillar display spawn cleans partial displays
- committed room recovery does not replay room reveal; bounded retry targets pillar restore only

KNOWN_DEFECTS:
1. P0 - Config/runtime migration phase contradiction is confirmed in current code: bundled Desert Pyramid config and recovery policy use `pyramid_push_pillars` phase `pyramid_pillar_restore`, but ConfigMigrationService still rewrites that same component to `pyramid_room_reveal`. A migrated server config can therefore undo the pillar-only recovery fix and reintroduce room-reveal routing. Repair this separately with a minimal canonical phase correction and behavioral/config migration proof.
2. P0 - Restart solved-evidence gap remains: the physical pillar board can become solved before the first durable `pyramid-underground-completion-state=completion_pending` save succeeds. If that first save fails and the server stops before the runtime retry succeeds, restart may have no durable solved evidence.
3. P0 - Reward lifetime exactly-once gap remains: PendingRewardService.claim can physically add an item to inventory before completed-token tombstone and pending-removal state are durably saved. Save failure followed by crash/restart can replay the same pending reward.
4. P1 - Behavioral proof remains incomplete across persistence failure/restart boundaries, reward tombstone failure, guardian-first/underground-first stateful flow, exact 4/4 pillar partial-failure behavior, and other required Pyramid matrix cases.
5. P1 - Bounded Outpost regression proof remains incomplete for loot-exit grace preservation, 24-40 radius behavior, next-wave scheduling, and delayed target chase.
6. P1 - Final status/current-status documentation is not yet synchronized into a valid completion certificate for the eventual passing HEAD.
7. P1 - Final config-to-runtime connectivity and adversarial whole-flow audit remain required after all repairs and a green current-HEAD CI run.

EVIDENCE:
- Compile repair candidate dbf7240cfe8198f09a69af2984d13d6939fa89fa replaces `source.forEach((key, item) -> value.put(...))` with an explicit entry loop and removes the one-shot repair workflow.
- Current ConfigMigrationService still contains `case "pyramid_push_pillars"` canonicalization to `pyramid_room_reveal`.
- Latest handoff HEAD 5b4721b9b5178762ff17aefbb94c73207fca71ca changes only the automation state document after the source repair.

NEXT_REQUIRED_ACTION: modifier must minimally fix only the confirmed ConfigMigrationService `pyramid_push_pillars` canonical phase contradiction, add a focused migration/config behavioral test proving the canonical phase remains `pyramid_pillar_restore`, commit, and return AWAITING_REVIEW. Do not start the restart solved-evidence repair before independent review of that candidate.
