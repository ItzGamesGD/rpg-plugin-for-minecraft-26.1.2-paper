# Exploration Live-Test Checkpoint

- Checkpoint branch: `codex/exploration-build-junit-fix`
- Commit: `a436ebc` (`test: validate scout component list entry`)
- Build result: SUCCESS
- Automated tests: 172 total, 0 failures, 2 skipped
- JAR: `2026-08-20_097_exploration-latest-a436ebc.jar`
- SHA-256: `BB5D80EC1AFCCBDC173074FC8FEC31EB628825AEEC5E285A2355B0EA5B64ED85`
- Server plugins folder: not overwritten

## Live Status

- Paper live verification: PENDING
- This checkpoint was extracted and built in isolation; it was not merged into `main`.

## Required Live Tests

1. Plugin enable and exploration module initialization.
2. Detect a real Paper structure key and verify bounds/location.
3. Confirm no runtime is created before player approach.
4. Confirm exactly one runtime is created after approach.
5. Confirm duplicate activation is blocked.
6. Confirm persistence record creation, reload retention, restart restoration, and reconnect deduplication.
7. Test scripted swamp-hut witch and pillager-outpost scout-wave spawning.
8. Verify spawn location, objective registration, entity death/removal, clear, abandon, grace period, cleanup, and end reason.
9. Repeat plugin disable, world unload, and server restart cleanup checks.

## Classification

- Static/build gate: PASS
- Automated verification: PASS_WITH_SKIPPED_TESTS
- Paper live verification: LIVE_UNVERIFIED
