# Desert Pyramid Current Implementation Status

- Working branch: `fix/desert-pyramid-full-flow-reconciliation`
- Reconciliation baseline: `032d89afc0c8245666503d9a8d24d5f31b1da793`
- Audited source revision: `ad15ec8f0be61c8245f3cf8f0eadd0ae90de0902` (the commit immediately preceding this status-only commit)
- Status: STATICALLY_IMPLEMENTED / UNIT_EXECUTION_BLOCKED / LIVE_SERVER_RETEST_REQUIRED

## Canonical flows

Exterior: padded boundary crossing -> actual actor/direction capture -> outward repel -> Pyramid quiz -> any answer or timeout default -> guardian. Guardian completion is independent.

Underground: canonical vanilla treasure chest -> persisted anchor -> non-mutating preparation -> seven-second telegraph -> final validation -> staged top-to-bottom 3x3 shaft -> usable 7x7 interior -> four real displays -> push-pillar solve. Room reveal invokes the pillar component directly; heartbeat replay is recovery-only.

Both persisted module flags are required for final clear.

## Current runtime path

`ExplorationRuntimeManager`, `PyramidRepelComponent`, `ChoicePromptComponent`,
`PyramidGuardianComponent`, `PyramidRoomComponent`,
`PyramidRoomRevealComponent`, `PyramidPushPillarComponent`,
`PyramidPushPillarService`, and `ExplorationSequenceState`.

`BukkitExplorationPorts.compose(...)` supplies real Bukkit Display/Interaction/WorldMutation/Teleport ports while retaining HyunseoRPG mob, reward, and cleanup adapters.

## Persistent metadata and version

Current content version is `ExplorationRuntimeManager.CURRENT_PYRAMID_CONTENT_VERSION = 3`.
Records persist entry actor/direction, treasure anchor, room prepared/created/origin/radius/height,
guardian encounter state, independent guardian/underground completion, and reward transaction state.

Reward states are `reserved`, `pending`, `delivered`, and `finalized`. Pyramid rewards are durably enqueued through the existing pending-reward mailbox with a deterministic token before the structure is finalized; repeated completion is idempotent.

Legacy radius-3 or incompatible room metadata is migrated forward and stale room signatures are downgraded for safe re-preparation. A committed physical room is retained across ordinary reloads. Underground completion persistence retries with bounded runtime-owned backoff and never abandons the structure.

## Legacy boundary

`PyramidRoomService` is authoritative for live room planning/reveal. `PyramidRoomLocator`,
`PyramidRoomPreflight`, `PyramidRoomCandidate`, `PyramidModuleProgress`, and
`PyramidVariantModules` are reusable deterministic logic/test axes; they are not alternate live runtime paths.

Historical checkpoint documents are evidence only and are marked superseded.

## Validation and administration

Startup applies targeted exploration migration, loads the migrated registry, and validates the canonical seven-component Pyramid graph and required phases. Required named Pyramid phases fail closed on zero matches.

Use `/rpg exploration inspect <structure-uuid>` for bounded state/diagnostics and
`/rpg exploration reset <structure-uuid>` for one-record retryable reset.

## Tests and live boundary

Executable tests cover canonical YAML, padded cardinal/diagonal crossing, sequence reservation semantics, primitive port composition, independent module ordering, reward state transitions, bounded retry policy, runtime continuation hooks, room geometry, and push-board behavior.
primitive port composition, independent module ordering, reward state transitions, retry bounds, room geometry,
and push-board behavior. The requested command `./gradlew clean test --no-daemon` was attempted in this
workspace but cannot execute because the checkout has no Gradle wrapper (`./gradlew: No such file or directory`).
Therefore unit execution is not claimed. Paper/client restart and visual acceptance remain LIVE_SERVER_RETEST_REQUIRED.
