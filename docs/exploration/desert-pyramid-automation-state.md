# Desert Pyramid Automation State

PHASE: IMPLEMENTING
CURRENT_HEAD: 3851be385434d314dd7fc54370c342e463316c40
CANDIDATE_HEAD: none
LAST_REVIEWED_HEAD: none
LAST_SAFE_HEAD: 3851be385434d314dd7fc54370c342e463316c40
REPOSITORY_GATE: FAIL
CI_STATE: FAIL
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

CURRENT_DEFECT: persisted `pyramid-underground-complete=true` with stale `pyramid-underground-completion-state=completion_pending` is not normalized because reconciliation converts the in-memory value to COMPLETE before the persistence branch tests whether normalization is needed.

KNOWN_DEFECTS:
1. Contradictory complete=true + completion_pending metadata normalization bug.
2. Puzzle solved but first completion_pending save failure followed by restart can lose durable solved evidence.
3. Committed room + pillar start failure can re-enter pyramid_room_reveal instead of bounded pillar-only retry.
4. PendingRewardService claim can physically deliver before tombstone/pending removal is durably saved, allowing save-failure + crash duplication.
5. Behavioral tests remain incomplete/too reflective or trivial in several required paths.
6. Latest known GitHub Actions run at 3851be385434d314dd7fc54370c342e463316c40 failed.
7. Status documentation is stale.
8. Bounded Outpost regression proof is incomplete.

NEXT_REQUIRED_ACTION: minimally repair defect 1 and add a behavioral persistence-normalization test, then hand the resulting HEAD to independent review.
