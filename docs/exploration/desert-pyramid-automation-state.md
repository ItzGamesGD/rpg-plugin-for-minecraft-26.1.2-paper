# Desert Pyramid Automation State

PHASE: AWAITING_REVIEW
CURRENT_HEAD: 048d15ac61ac11ba48466206f6658e63e758f100
CANDIDATE_HEAD: 048d15ac61ac11ba48466206f6658e63e758f100
LAST_REVIEWED_HEAD: ffca2b137f1d1dd390d514ce063b4b609b11aa81
LAST_SAFE_HEAD: 3851be385434d314dd7fc54370c342e463316c40
REPOSITORY_GATE: FAIL
CI_STATE: IN_PROGRESS
VERIFIED_SAFE_REGRESSIONS: 0
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

IMPLEMENTATION_RESULT:
- Repaired only the concrete compile failures reported by GitHub Actions run 32992044724.
- Added the missing PYRAMID_ROOM_REVEAL phase enum required by the existing Pyramid room-reveal components.
- Restored java.util.Set import used by guardian restart recovery.
- Made PyramidPushPillarService.Session an inner instance class so the existing pillar solve path can call its owning service's non-static persistUndergroundCompletion transaction.
- Replaced invalid PendingReveal world() method calls with the existing world field.
- Replaced unavailable BLOCK_SANDSTONE_BREAK constants with the already-supported BLOCK_SAND_BREAK sound for the same sand-collapse presentation.
- No intended Pyramid state-machine, persistence ordering, room geometry, guardian logic, or Outpost behavior was changed by this compile-only repair.

FILES_CHANGED_FOR_CANDIDATE:
- src/main/java/com/hyunseo/hyunseorpg/exploration/component/ExplorationComponentPhase.java
- src/main/java/com/hyunseo/hyunseorpg/exploration/component/impl/PyramidRoomComponent.java
- src/main/java/com/hyunseo/hyunseorpg/exploration/component/impl/PyramidPushPillarService.java
- src/main/java/com/hyunseo/hyunseorpg/exploration/pyramid/PyramidRoomService.java
- src/main/java/com/hyunseo/hyunseorpg/exploration/runtime/ExplorationRuntimeManager.java

TESTS_CHANGED:
- none; this candidate restores compilation of already-configured/runtime-referenced contracts. The full existing suite is running in GitHub Actions for the candidate.

CI_EVIDENCE:
- GitHub Actions run 33004531302 targets exact candidate 048d15ac61ac11ba48466206f6658e63e758f100.
- At handoff time the run is still IN_PROGRESS; do not treat CI as PASS until the independent verifier sees a completed successful current-candidate run.

VERIFIED_SAFE_INVARIANTS:
- heartbeat is not an independent underground-completion writer
- actual entrant/movement vector drives Pyramid entry/repel
- guardian surface spawn does not depend on underground room origin
- guardian completion does not synthesize underground completion
- incomplete pillar display spawn cleans partial displays
- committed room recovery does not replay room reveal; bounded retry targets pillar restore only

KNOWN_DEFECTS:
1. P0 - Restart solved-evidence gap remains: the physical pillar board can become solved before the first durable pyramid-underground-completion-state=completion_pending save succeeds. If that first save fails and the server stops before the runtime retry succeeds, restart may have no durable solved evidence.
2. P0 - Reward lifetime exactly-once gap remains: PendingRewardService.claim can physically add an item to inventory before completed-token tombstone and pending-removal state are durably saved. Save failure followed by crash/restart can replay the same pending reward.
3. P1 - Behavioral proof remains incomplete across persistence failure/restart boundaries, reward tombstone failure, guardian-first/underground-first stateful flow, exact 4/4 pillar partial-failure behavior, and other required Pyramid matrix cases. Reflection/existence and trivial arithmetic/list tests do not count as behavioral proof.
4. P1 - Bounded Outpost regression proof remains incomplete for loot-exit grace preservation, 24-40 radius behavior, next-wave scheduling, and delayed target chase.
5. P1 - Final status/current-status documentation is not yet synchronized into a valid completion certificate for the eventual passing HEAD.
6. P1 - Final config-to-runtime connectivity and adversarial whole-flow audit remain required after all repairs and a green current-HEAD CI run.

NEXT_REQUIRED_ACTION: independent verifier should review candidate 048d15ac61ac11ba48466206f6658e63e758f100 and the completed result of Actions run 33004531302. If accepted, modifier should next address the restart solved-evidence P0. Do not modify main and do not broaden into unrelated Exploration redesign.
