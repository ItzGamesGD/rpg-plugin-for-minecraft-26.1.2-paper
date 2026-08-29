<!-- SUPERSEDED HISTORICAL CHECKPOINT
AUDITED HEAD: d1bc561501deae0461e7976c4a8b3a50a29e17c9
NOT CURRENT IMPLEMENTATION STATE. See desert-pyramid-current-status.md.
-->

# Desert Pyramid strict gap audit

Baseline: `feature/exploration-common-event-engine` at `0ffd8bb13e58bd8fd007f025366c0f9ae602f89b`.
This audit precedes any Pyramid content implementation.

| Capability | Classification | Existing code / API | Decision |
| --- | --- | --- | --- |
| Structure activation, duplicate runtime protection, abandon, clear | REUSE | `runtime/ExplorationRuntimeManager` | Structure-runtime-owned behavior already exists. |
| Delay and delayed ownership | REUSE | `component/impl/SequenceDelayComponent`, `ExplorationSequenceState` | Runtime cleanup cancels registered tasks. |
| Objective clear/count gate | REUSE | `component/impl/SequenceWaitComponent`, `ExplorationRuntime.objectiveEntities()` | Bounded wait only; no predicate language. |
| Flag/counter gate | REUSE | `SequenceStateComponent`, `ExplorationSequenceState` | Named flags and integer counters only. |
| Actual XYZ movement gate | REUSE | `ExplorationPlayerMovementListener`, `SequenceWaitComponent` | Yaw/pitch-only events are ignored. |
| Reward / cleanup | REUSE | `RewardDropComponent`, `RuntimeObjectTracker` | Must preserve exactly-once structure completion policy. |
| Temporary seal / snapshot restore | REUSE | `TemporarySealComponent`, `WorldMutationPort.applyTemporary` | Use runtime tracker cleanup; no second mutation engine. |
| Teleport / interaction / display | REUSE | `ForcedRelocationComponent`, `InteractionTargetComponent`, `DisplayTargetComponent` | Reuse existing ports. |
| Vanilla spawn with initial AI/invulnerability state | SMALL_EXTENSION | `ScriptedSpawnComponent`, `BukkitExplorationPorts` | Extend existing spawn options only if the actual guardian flow needs it. |
| Ring placement | SMALL_EXTENSION | `ScriptedSpawnComponent`, `ComponentLocations` | Add a placement option only after concrete Pyramid coordinates are fixed. |
| Safe knockback, movement lock, blindness/darkness, safe fall | SMALL_EXTENSION | Existing status/movement modules must be audited at implementation time | No Pyramid copy or new effect service. |
| Actual chest item extraction | SMALL_EXTENSION | `ExplorationChestLootListener`, `ExplorationRuntimeManager.markLootTaken` | Generalize its proven extraction semantics; chest-open alone remains insufficient. |
| Pyramid room candidate/safety/carve/rollback | PYRAMID_SPECIFIC | none | Bounded, deterministic Pyramid operation; do not create an excavation engine. |
| Push-pillar logical grid, display/contact, occupancy | PYRAMID_SPECIFIC | Display/interaction ports are reusable | Isolate as Pyramid mechanic; do not build a grid-puzzle engine. |
| Resurrection lineage and rise animation | PYRAMID_SPECIFIC | runtime task/entity tracking reusable | Keep resurrection state in the Guardian module; evaluate generic promotion only with a second real content user. |
| Pyramid variant module completion persistence | SMALL_EXTENSION | `StructureRecord` metadata/state policy | Store only guardian/underground completion if current restart policy requires it. |
| Room topology, pyramid excavation framework, generic scripting language | PYRAMID_SPECIFIC | n/a | Explicitly out of scope. |

## Boundaries

Persistent structure state is distinct from runtime flags/counters/tasks. Scheduled callbacks, animation progress, display UUIDs, and arbitrary objects are not persisted. The eventual Pyramid implementation must re-run bounded preflight/final validation before any world mutation and leave chunk-unload/live timing behavior as live-server verification.
