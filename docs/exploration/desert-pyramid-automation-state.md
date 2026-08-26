# Desert Pyramid Automation State

PHASE: AWAITING_REVIEW
CURRENT_HEAD: ffca2b137f1d1dd390d514ce063b4b609b11aa81
CANDIDATE_HEAD: ffca2b137f1d1dd390d514ce063b4b609b11aa81
LAST_REVIEWED_HEAD: 9601f93199c4baad14f7a60c3523bfd318329d4b
LAST_SAFE_HEAD: 3851be385434d314dd7fc54370c342e463316c40
REPOSITORY_GATE: FAIL
CI_STATE: REVIEW_REQUIRED
VERIFIED_SAFE_REGRESSIONS: 0
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

CANDIDATE_REPAIR:
- Addressed the committed-room recovery defect only.
- `pyramid_push_pillars` is now configured on the dedicated literal phase `pyramid_pillar_restore`, while normal post-reveal startup still uses the existing direct `continuePyramidPuzzle` hook.
- Pillar ownership marks `pyramid.puzzle.started` before world/session/display work, so a technical pillar-start failure cannot make heartbeat re-enter `pyramid_room_reveal` for an already committed room.
- A missing in-memory room session after restart is restored through `PyramidRoomService.prepare`, which validates/restores committed metadata without carving or invoking reveal.
- Pillar-start failures use runtime-owned bounded retries through `PyramidPillarRecoveryPolicy` and the existing sequence scheduler. Attempts 1-5 schedule `pyramid_pillar_restore`; attempt 6 exhausts without a room-reveal fallback.
- Added a pure recovery-policy behavioral test and updated bundled config coverage to assert the dedicated pillar-only restore phase.

FILES_CHANGED:
- src/main/java/com/hyunseo/hyunseorpg/exploration/pyramid/PyramidPillarRecoveryPolicy.java
- src/main/java/com/hyunseo/hyunseorpg/exploration/component/impl/PyramidPushPillarComponent.java
- src/main/resources/exploration/structures.yml
- src/test/java/com/hyunseo/hyunseorpg/exploration/pyramid/PyramidPillarRecoveryPolicyTest.java
- src/test/java/com/hyunseo/hyunseorpg/exploration/DesertPyramidContentTest.java

VERIFIED_SAFE_INVARIANTS:
- heartbeat is not an independent underground-completion writer
- actual entrant/movement vector drives Pyramid entry/repel
- guardian surface spawn does not depend on underground room origin
- guardian completion does not synthesize underground completion
- incomplete pillar display spawn cleans partial displays

PREVIOUS_REVIEW_RESULT:
- Candidate 9601f93199c4baad14f7a60c3523bfd318329d4b was accepted for contradictory underground-completion metadata normalization.
- No verified-safe regression incident was counted for that candidate.

KNOWN_DEFECTS_REMAINING_AFTER_THIS_CANDIDATE:
1. P0 - Restart evidence gap still requires independent review/repair: the physical pillar board can become solved before the first durable `pyramid-underground-completion-state=completion_pending` save succeeds. If that first save fails and the server stops before an in-memory retry succeeds, restart may have no durable solved evidence.
2. P0 - Reward lifetime exactly-once gap remains: `PendingRewardService.claim` can physically add an item to inventory before the completed-token tombstone and pending-removal state are durably saved. Save failure followed by crash/restart can replay the same pending reward.
3. P1 - Behavioral proof remains incomplete across the wider Pyramid matrix, including persistence failure/restart boundaries, reward tombstone failure, guardian-first/underground-first stateful flow, and exact 4/4 pillar partial-failure behavior.
4. P1 - CI must be evaluated on the current candidate/current branch state; no PASS is claimed by the modifier.
5. P1 - Final status documentation is not yet a completion certificate.
6. P1 - Bounded Outpost regression proof remains incomplete for loot-exit grace preservation, 24-40 radius behavior, next-wave scheduling, and delayed target chase.

NEXT_REQUIRED_ACTION: independent reviewer must audit candidate ffca2b137f1d1dd390d514ce063b4b609b11aa81, including the restart restoration path, bounded retry scheduling semantics, config connectivity, CI, and preservation of all VERIFIED_SAFE invariants. If accepted, return PHASE=NEEDS_FIX with the next defect only; if rejected, provide precise evidence and regression accounting.
