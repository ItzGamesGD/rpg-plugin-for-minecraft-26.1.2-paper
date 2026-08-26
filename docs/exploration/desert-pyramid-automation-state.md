# Desert Pyramid Automation State

PHASE: AWAITING_REVIEW
CURRENT_HEAD: 9601f93199c4baad14f7a60c3523bfd318329d4b
CANDIDATE_HEAD: 9601f93199c4baad14f7a60c3523bfd318329d4b
LAST_REVIEWED_HEAD: none
LAST_SAFE_HEAD: 3851be385434d314dd7fc54370c342e463316c40
REPOSITORY_GATE: FAIL
CI_STATE: PENDING
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

ADDRESSED_IN_CANDIDATE:
- Defect 1: contradictory `pyramid-underground-complete=true` plus stale/missing non-COMPLETE state no longer gets hidden by the in-memory reconciliation helper. `PyramidUndergroundCompletionState.reconcile` now returns COMPLETION_PENDING for that contradiction, so the existing repository reconciliation branch performs the durable normalization write.

FILES_CHANGED:
- src/main/java/com/hyunseo/hyunseorpg/exploration/pyramid/PyramidUndergroundCompletionState.java
- src/test/java/com/hyunseo/hyunseorpg/exploration/pyramid/PyramidUndergroundCompletionStateTest.java

TESTS_CHANGED:
- pending state remains detectable after restart when complete flag is true
- already-normalized COMPLETE remains idempotent
- true complete flag plus missing/unknown state becomes repair-pending
- unknown state without complete evidence still fails closed to UNSOLVED

KNOWN_DEFECTS:
1. Puzzle solved but first completion_pending save failure followed by restart can lose durable solved evidence.
2. Committed room + pillar start failure can re-enter pyramid_room_reveal instead of bounded pillar-only retry.
3. PendingRewardService claim can physically deliver before tombstone/pending removal is durably saved, allowing save-failure + crash duplication.
4. Behavioral tests remain incomplete/too reflective or trivial in several required paths.
5. CI for candidate HEAD has not yet produced a workflow run; latest prior known run failed.
6. Status documentation is stale.
7. Bounded Outpost regression proof is incomplete.

NEXT_REQUIRED_ACTION: independent reviewer must audit candidate 9601f93199c4baad14f7a60c3523bfd318329d4b, verify the runtime normalization branch now persists the contradiction correctly, and inspect current-HEAD CI before returning NEEDS_FIX or another terminal/review state.
