# Desert Pyramid Automation State

PHASE: AWAITING_REVIEW
CURRENT_BRANCH: fix/desert-pyramid-full-flow-reconciliation
CANDIDATE_HEAD: 9c28d3d7c41d04c33fb49c56a678f8b7fe15ee3e (subsequent status-only commits may advance the branch)
REPOSITORY_GATE: PASS_PENDING_INDEPENDENT_REVIEW
CI_STATE: PASS — GitHub Actions run 33023357959, `./gradlew clean test --no-daemon`
HARD_BLOCK: false
SAFETY_STOP: false
STOP_REASON: none

VERIFIED IMPLEMENTATION:
- `PyramidUndergroundCompletionCoordinator` is the single durable pending-to-complete transaction; the live pillar service, restart recovery, and retry all converge through it.
- The final pillar move writes durable `completion_pending` before it mutates the solved board. A failed first save leaves that move unperformed.
- Legacy `complete=true + completion_pending` is normalized by the same coordinator; an unsolved record cannot be completed by generic recovery.
- Config migration canonicalizes `pyramid_push_pillars` to `pyramid_pillar_restore`; committed rooms recover pillars without replaying room reveal.
- Deterministic exploration rewards use a durable pre-delivery journal plus an item token. Restart observes a tagged delivered item and writes the completed tombstone; queue/finalization retries suppress the same token.
- Padded Pyramid entry requires an actual outside-to-interior crossing. The neutral perimeter itself cannot jitter-trigger an encounter.

VALIDATION:
- GitHub Actions run 33023357959: PASS (clean compile and 283 JUnit tests; 2 skipped).
- A prior boundary test failure was repaired as a real inclusive-boundary behavior mismatch and rerun green.

LIVE VALIDATION:
- LIVE_SERVER_RETEST_REQUIRED: Paper/client execution remains required for world mutation timing, visual staged reveal, and in-game interaction confirmation.
