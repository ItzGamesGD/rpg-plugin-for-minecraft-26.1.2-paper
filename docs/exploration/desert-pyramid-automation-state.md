# Desert Pyramid Automation State

PHASE: NEEDS_FIX
CURRENT_HEAD: b41b5d4a71f60e303eb620dd59881015a16edfa8
CANDIDATE_HEAD: ffca2b137f1d1dd390d514ce063b4b609b11aa81
LAST_REVIEWED_HEAD: ffca2b137f1d1dd390d514ce063b4b609b11aa81
LAST_SAFE_HEAD: 3851be385434d314dd7fc54370c342e463316c40
REPOSITORY_GATE: FAIL
CI_STATE: FAIL
VERIFIED_SAFE_REGRESSIONS: 0
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

REVIEW_RESULT:
- Candidate ffca2b137f1d1dd390d514ce063b4b609b11aa81 is accepted for the committed-room pillar-only recovery defect.
- The dedicated `pyramid_pillar_restore` phase is connected by config to `pyramid_push_pillars`, while the normal post-reveal happy path still invokes the pillar component directly through `continuePyramidPuzzle`.
- Pillar ownership now marks `pyramid.puzzle.started` before room/session/display restoration. A committed room therefore does not become eligible for heartbeat `pyramid_room_reveal` replay after a technical pillar-start failure.
- When the in-memory room session is absent after restart, `PyramidRoomService.prepare` is used only to restore/validate already-committed room metadata; it does not carve or reveal a committed room.
- Retry policy is bounded to attempts 1-5 and targets only `pyramid_pillar_restore`; attempt 6 exhausts without a reveal fallback.
- No previously VERIFIED_SAFE invariant was changed into a new defect by this candidate. VERIFIED_SAFE_REGRESSIONS remains 0.

CI_EVIDENCE:
- GitHub Actions run 32992044724 for candidate ffca2b137f1d1dd390d514ce063b4b609b11aa81 completed with conclusion `failure` during `:compileJava`.
- Current compiler errors include: missing `java.util.Set` import/use resolution in `ExplorationRuntimeManager`; static-context call to non-static `persistUndergroundCompletion` in `PyramidPushPillarService`; invalid `pending.world()` calls in `PyramidRoomService`; unavailable `Sound.BLOCK_SANDSTONE_BREAK` constants in Pyramid room code; and missing `ExplorationComponentPhase.PYRAMID_ROOM_REVEAL` enum values referenced by Pyramid components.
- These are repository-addressable compile defects, not HARD_BLOCK.

VERIFIED_SAFE_INVARIANTS:
- heartbeat is not an independent underground-completion writer
- actual entrant/movement vector drives Pyramid entry/repel
- guardian surface spawn does not depend on underground room origin
- guardian completion does not synthesize underground completion
- incomplete pillar display spawn cleans partial displays
- committed room recovery does not replay room reveal; bounded retry targets pillar restore only

KNOWN_DEFECTS:
1. P0 - Current candidate does not compile. Repair the concrete GitHub Actions compiler failures with minimal changes and preserve all VERIFIED_SAFE invariants. Do not treat warnings/deprecations as the blocker unless they become errors.
2. P0 - Restart solved-evidence gap remains: the physical pillar board can become solved before the first durable `pyramid-underground-completion-state=completion_pending` save succeeds. If that first save fails and the server stops before the in-memory retry succeeds, restart may have no durable solved evidence.
3. P0 - Reward lifetime exactly-once gap remains: `PendingRewardService.claim` can physically add an item to inventory before completed-token tombstone and pending-removal state are durably saved. Save failure followed by crash/restart can replay the same pending reward.
4. P1 - Behavioral proof remains incomplete across persistence failure/restart boundaries, reward tombstone failure, guardian-first/underground-first stateful flow, exact 4/4 pillar partial-failure behavior, and other required Pyramid matrix cases. Reflection/existence and trivial arithmetic/list tests do not count as behavioral proof.
5. P1 - Bounded Outpost regression proof remains incomplete for loot-exit grace preservation, 24-40 radius behavior, next-wave scheduling, and delayed target chase.
6. P1 - Final status/current-status documentation is not yet synchronized into a valid completion certificate for the eventual passing HEAD.
7. P1 - Final config-to-runtime connectivity and adversarial whole-flow audit remain required after all repairs and a green current-HEAD CI run.

NEXT_REQUIRED_ACTION: modifier should first repair the current compile failures as one minimal coherent candidate, obtain/allow a current-candidate GitHub Actions run, then hand off with PHASE=AWAITING_REVIEW. After compilation is restored, the next functional P0 is the restart solved-evidence gap. Do not modify main and do not broaden into unrelated Exploration redesign.
