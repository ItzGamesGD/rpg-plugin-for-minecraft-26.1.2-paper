# Desert Pyramid Current Implementation Status

- Working branch: `fix/desert-pyramid-full-flow-reconciliation`
- Repository gate: `PASS_PENDING_INDEPENDENT_REVIEW`
- CI: GitHub Actions run [33023357959](https://github.com/ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper/actions/runs/33023357959) passed `./gradlew clean test --no-daemon` (283 tests, 2 skipped).
- Live runtime status: `LIVE_SERVER_RETEST_REQUIRED`

## Durable underground completion

The live final-pillar operation persists `pyramid-underground-completion-state=completion_pending` before it mutates the final solved board. `PyramidUndergroundCompletionCoordinator` is the sole durable pending-to-complete transaction; the pillar service owns normal live completion and restart/retry recovery calls that same coordinator. It rejects an unsolved record, is idempotent, and normalizes legacy `pyramid-underground-complete=true + completion_pending` to `complete`.

This preserves independent guardian and underground modules. Guardian completion cannot start the room/pillars, and underground completion cannot start the guardian.

## Room, shaft, and pillar recovery

The current configuration uses a room radius of 4 (9x9 footprint, 7x7 usable interior), a consistent staged 3x3 shaft, and four required pillar displays. Partial display creation is cleaned up. A committed room uses `pyramid_pillar_restore` with bounded pillar-only retry; it never replays room reveal.

Config migration canonicalizes `pyramid_push_pillars` to `pyramid_pillar_restore`, matching the bundled configuration and runtime recovery path.

## Reward transaction

Pyramid rewards are durably queued with deterministic tokens. Claim records a durable pre-delivery journal before an exact inventory insertion and tags the delivered item with that token. If final tombstone persistence fails, restart reconciliation finds the tagged item and persists the completed tombstone without another delivery. Pending, journalled, and completed tokens suppress repeated queue/finalization attempts.

## Entry and restart behavior

Pyramid entry captures the real actor and movement vector. A padded boundary triggers only on an actual outside-to-interior crossing; the perimeter itself is neutral against movement jitter. Restart recovery restores durable module, room, guardian, and reward evidence without replaying committed geometry.

## Remaining live-only validation

Run a Paper server retest for staged shaft timing, TNT/special-block protection in a generated Pyramid, real display interaction, guardian safe-spawn terrain, and client-visible repel direction. These are intentionally not claimed as live verified.
