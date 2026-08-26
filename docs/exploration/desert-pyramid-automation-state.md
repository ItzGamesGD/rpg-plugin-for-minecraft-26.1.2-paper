# Desert Pyramid Automation State

PHASE: NEEDS_FIX
CURRENT_HEAD: 5aa6c6904a39d6414a212361b077fe284bd96fa1
CANDIDATE_HEAD: 9601f93199c4baad14f7a60c3523bfd318329d4b
LAST_REVIEWED_HEAD: 9601f93199c4baad14f7a60c3523bfd318329d4b
LAST_SAFE_HEAD: 3851be385434d314dd7fc54370c342e463316c40
REPOSITORY_GATE: FAIL
CI_STATE: NO_CURRENT_HEAD_RUN
VERIFIED_SAFE_REGRESSIONS: 0
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

VERIFIED_SAFE_INVARIANTS:
- heartbeat is not an independent underground-completion writer
- actual entrant/movement vector drives Pyramid entry/repel
- guardian surface spawn does not depend on underground room origin
- guardian completion does not synthesize underground completion
- incomplete pillar display spawn cleans partial displays

REVIEW_RESULT:
- Candidate 9601f93199c4baad14f7a60c3523bfd318329d4b correctly fixes Defect 1 without modifying any previously verified-safe implementation path. `PyramidUndergroundCompletionState.reconcile(true, non-COMPLETE)` now returns COMPLETION_PENDING, so `ExplorationRuntimeManager.reconcilePendingPyramidUndergroundCompletion` enters its existing `completeFlag && !complete` normalization branch and durably writes `pyramid-underground-completion-state=complete` plus the current content version.
- The changed production surface is limited to the completion-state enum; the companion tests cover contradictory pending/missing/unknown states and already-normalized COMPLETE idempotency.
- No verified-safe regression incident is counted for this candidate.
- There is no GitHub Actions workflow run for candidate 9601f93199c4baad14f7a60c3523bfd318329d4b, so CI cannot be marked PASS.

KNOWN_DEFECTS:
1. P0 - Restart evidence gap remains: the physical pillar board can become solved before the first durable `pyramid-underground-completion-state=completion_pending` save succeeds. If that first save fails and the server stops before an in-memory retry succeeds, restart has no durable solved evidence and may reconstruct the puzzle as unsolved.
2. P0 - Committed-room recovery remains unsafe: after the room is committed, a pillar-start failure can leave `pyramid.puzzle.started=false`; heartbeat still re-enters the `pyramid_room_reveal` phase rather than a bounded pillar-only restoration path. A committed carved room must never be subjected to buried/pre-reveal validation again.
3. P0 - Reward lifetime exactly-once gap remains: `PendingRewardService.claim` can physically add an item to inventory before the completed-token tombstone and pending-removal state are durably saved. Save failure followed by crash/restart can replay the same pending reward.
4. P1 - Behavioral proof remains incomplete: several required contracts still rely on reflection, enum-only transitions, or trivial arithmetic/list tests instead of repository/runtime-state failure-and-restart behavior. Required coverage still includes persistence failure/restart boundaries, committed-room pillar-only recovery, reward tombstone failure, guardian-first/underground-first stateful flow, and exact 4/4 pillar partial-failure behavior.
5. P1 - CI remains unproven for the accepted candidate: GitHub Actions reports no run for 9601f93199c4baad14f7a60c3523bfd318329d4b. A successful relevant current-candidate/current-head run is required before completion.
6. P1 - Status documentation remains stale relative to the automation candidate/current branch history and cannot serve as the final completion certificate yet.
7. P1 - Bounded Outpost regression proof remains incomplete for loot-exit grace preservation, 24-40 radius behavior, next-wave scheduling, and delayed target chase.

NEXT_REQUIRED_ACTION: modifier should address P0 Defect 2 first: make committed-room recovery pillar-only and bounded, with a behavioral test proving that a committed room never re-enters room reveal after pillar-start failure. After one coherent repair, commit and return PHASE=AWAITING_REVIEW for independent review. Do not combine the reward transaction rewrite into the same repair.
