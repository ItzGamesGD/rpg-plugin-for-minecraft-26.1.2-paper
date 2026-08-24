# Desert Pyramid implementation checkpoint

## Branch baseline

| Field | Value |
| --- | --- |
| Repository | `ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper` |
| Branch | `feature/desert-pyramid` |
| Audited HEAD | `6acf1542b2ef82f7fbd13a69739429d9c37a464b` |
| Parent development baseline | `feature/exploration-common-event-engine @ 0ffd8bb13e58bd8fd007f025366c0f9ae602f89b` |
| Remote main baseline | `main @ 413f558b500311bbcfa236e10c835eddbac0c49c` |
| Relationship to common-engine branch | 20 commits ahead, 0 behind; merge base is the common-engine HEAD |
| Relationship to main | 42 commits ahead, 0 behind; includes the common-engine changes |
| Audit date | 2026-08-24 |
| Status | `DESERT_PYRAMID_INTERMEDIATE_IMPLEMENTATION` |

This checkpoint is an evidence record for the audited remote HEAD. It does not describe a completed in-game Desert Pyramid.

## What exists

### Pyramid-specific core logic

| Area | Actual classes | Status | Evidence / boundary |
| --- | --- | --- | --- |
| Push Pillar definition | `PushPillarDefinition`, `PyramidGridPoint`, `PyramidGridDirection` | CORE_LOGIC_IMPLEMENTED, UNIT_VERIFIED | Validates required text, allowed initial/target cells and cardinal grid movement. |
| Push Pillar board | `PushPillarBoard` | CORE_LOGIC_IMPLEMENTED, UNIT_VERIFIED | Tracks logical cell occupancy, per-pillar cooldown, one-cell atomic transition, solved state and unique solved IDs. The public transition method is synchronized. |
| Invalid puzzle definitions | `PushPillarDefinition`, `PushPillarBoard` | CORE_LOGIC_IMPLEMENTED, UNIT_VERIFIED | Empty/missing allowed cells, duplicate IDs, initial cells and target cells are rejected before a board is created. There is no YAML loader or fail-safe module-disable adapter yet. |
| Room-local coordinates | `PyramidBlockPosition`, `PyramidRoomOrientation` | CORE_LOGIC_IMPLEMENTED, UNIT_VERIFIED | Transforms a Pyramid room-local coordinate through one of four orientations without retaining Bukkit World, Block or Entity references. |
| Deterministic room candidate selection | `PyramidRoomCandidate`, `PyramidRoomPreflight` | CORE_LOGIC_IMPLEMENTED, UNIT_VERIFIED | Sorts the five defined slots by fixed enum order: CENTER, NORTH, SOUTH, EAST, WEST; a supplied predicate decides usability. |
| Variant/module completion model | `PyramidVariantModules`, `PyramidModuleProgress` | CORE_LOGIC_IMPLEMENTED, UNIT_VERIFIED | Models guardian-only, underground-only and combined variants; completion is idempotent and underground-only unavailable is a no-reward terminal logical outcome. |

Tests: `PushPillarBoardTest`, `PyramidRoomPreflightTest`, and `PyramidModuleProgressTest` cover the rows above. They are pure JUnit tests, not Bukkit/Paper tests.

### Generic Exploration extensions present on this branch

| Area | Actual implementation | Status | Boundary |
| --- | --- | --- | --- |
| Initial entity AI/invulnerability | `BukkitExplorationPorts.spawnVanillaOnly`, `ExistingHyunseoRpgAdapters.prepareExplorationMob` | IMPLEMENTED, STATICALLY_VERIFIED | Existing scripted spawn options `ai` and `invulnerable` are applied to spawned living entities. No Pyramid component/config currently invokes them. |
| Named bounded sequence phases | `ExplorationSequenceScheduler`, `SequenceDelayComponent`, `SequenceWaitComponent`, `ExplorationRuntimeManager.executeNamedPhase` | IMPLEMENTED, STATICALLY_VERIFIED | Existing components may dispatch an exact configured phase name. This remains a bounded component dispatcher, not an interpreter, expression language or command runner. |
| Existing runtime lifecycle/sequence state | `ExplorationRuntimeManager`, `ExplorationSequenceState`, tracker cleanup | REUSED | Runtime ownership, delayed task cancellation, flags/counters and physical movement waiting originate in the common-engine baseline. This branch does not add Pyramid-specific lifecycle registration. |

The audited successful CI run compiled and tested this branch with `./gradlew clean test --no-daemon`. That is STATICALLY_VERIFIED/UNIT_VERIFIED evidence only where the respective tests exist; it is not Paper-runtime or client-gameplay evidence.

## Adapter and integration audit

### Room preflight boundary

`PyramidRoomPreflight.firstUsable` accepts a `Predicate<PyramidRoomCandidate>`. No Bukkit adapter supplies that predicate.

Therefore the following are **NOT_IMPLEMENTED**:

- actual desert-pyramid anchor or vanilla treasure-room resolution;
- room bounding-box construction;
- build-height validation;
- AIR, CAVE_AIR, VOID_AIR, WATER or LAVA rejection;
- container, block entity, spawner, portal or protected-block rejection;
- temporary-mutation ownership conflict checks;
- deterministic preflight persistence/reuse after runtime recreation.

The existing code does not load chunks or mutate blocks in the preflight model, which is correct for its current pure-logic scope.

### Registry, bootstrap and persistence

`src/main/resources/exploration/structures.yml` contains a disabled `desert_pyramid` structure record with `variants: {}`. The root Exploration module is also disabled in that resource.

`ExplorationModule` registers only the existing generic component set. It registers no Pyramid component, listener, preflight service, room-carve adapter, Push Pillar runtime or module orchestrator.

`PyramidModuleProgress` is an in-memory value object. It is **not** attached to `ExplorationRuntime`, `StructureRecord` metadata, repository storage, reload restoration, or partial-completion recovery. No Pyramid reward policy is registered.

### Existing world and presentation ports

`BukkitExplorationPorts` can spawn vanilla entities, BlockDisplays, Interactions and one-block temporary mutations. `ExplorationPorts.PuzzlePort` remains NOOP in the safe defaults. No code converts a player movement delta/contact region into `PushPillarBoard.tryMove`, and no code renders a pillar or route for the board.

## Content feature audit

| Capability | Current status | Evidence / missing connection |
| --- | --- | --- |
| Entrance Guardian trigger | NOT_IMPLEMENTED | No Pyramid variant/component/listener registration. |
| Safe outward knockback | NOT_IMPLEMENTED | No Pyramid location/orientation or knockback action. |
| Joke question and answer delay | NOT_IMPLEMENTED | Named phases exist, but no presentation component/config/content flow exists. |
| Movement lock, Blindness, Darkness | NOT_IMPLEMENTED | No Pyramid adapter or component invocation. |
| Husk ring placement | NOT_IMPLEMENTED | Spawn options exist, but no ring placement or Pyramid spawn definition exists. |
| Staged Husk spawn | PARTIAL | Generic AI/invulnerable spawn options exist; no Husk content flow or later activation exists. |
| First physical-movement activation | PARTIAL | Common engine detects physical XYZ movement and can wait for it; no Pyramid staged entity activation is attached. |
| Resurrection lineage/pending objective/rise | NOT_IMPLEMENTED | No Guardian runtime state, death hook, scheduler animation or cleanup integration. |
| Entrance seal/open | NOT_IMPLEMENTED | Existing temporary block primitive is reusable but no Pyramid use is registered. |
| Actual chest extraction detection | NOT_IMPLEMENTED | Existing `ExplorationChestLootListener` and manager path are Outpost-specific. |
| Actual vanilla Pyramid bounds and treasure-room resolution | NOT_IMPLEMENTED | Generic structure detection can identify registered structures; no Pyramid room resolver exists. |
| Bukkit safety-shell scan | NOT_IMPLEMENTED | Only pure candidate predicate boundary exists. |
| Room carve and bounded rollback | NOT_IMPLEMENTED | No mutation plan, commit or rollback code. |
| Collapse and safe fall | NOT_IMPLEMENTED | No floor mutation, fall-damage scope or participant policy. |
| BlockDisplay pillar rendering and particle route | NOT_IMPLEMENTED | Generic ports exist; no board adapter/presentation task. |
| Player contact and movement delta to push | NOT_IMPLEMENTED | `PushPillarBoard` has no Bukkit listener connection. |
| Target response/secret passage | NOT_IMPLEMENTED | No target action or mutation registration. |
| Reward integration | NOT_IMPLEMENTED | Reward port/component exists but Pyramid variants are empty. |
| Variant registration | NOT_IMPLEMENTED | Logical variant enum exists only; YAML has no variants. |
| StructureRecord persistence | NOT_IMPLEMENTED | No Pyramid module completion metadata or runtime restoration. |
| Multiplayer behavior | NOT_IMPLEMENTED | Board synchronization is unit-level protection only; no participant/contact/loot runtime integration. |

## Architecture assessment

### Generic-abstraction restraint

No `PyramidEngine`, `PyramidEventManager`, `GenericExcavationEngine`, `GenericGridPuzzleEngine`, `GenericSokobanEngine`, `GenericRoomGenerationEngine`, or `GenericWorldTransactionManager` exists in the audited diff.

The room preflight and Push Pillar board are explicitly Pyramid-specific. The generic changes are limited to existing Exploration sequence dispatch and spawn options.

### Open issues and validation debt

1. **Actual Pyramid structure/treasure-room assumption** — no Paper adapter has proven vanilla bounds, chest identity or candidate coordinates.
2. **World-mutation safety** — bounded validation, carve plan, rollback, lifetime classification and conflict ownership are unimplemented.
3. **Actual loot semantics** — the existing listener is Outpost-specific and has not been generalized to Pyramid chest extraction.
4. **Push Pillar interaction** — logical transitions are unit-tested, but BlockDisplay rendering, player contact, movement delta, multiplayer feel and task cleanup have no runtime adapter.
5. **Guardian lifecycle** — staged spawn primitive exists, but activation, resurrection lineage, rise animation, pending objective accounting and cleanup are unimplemented.
6. **Partial completion persistence** — logical module state is not stored; restart/reload policy is therefore unresolved.
7. **Configuration and reward contract** — no enabled variants, component definitions or Pyramid reward policy exist.

## Verification matrix

| Capability | Logic | Unit | Paper Runtime | Live Client |
| --- | --- | --- | --- | --- |
| Common sequence wait/physical movement | implemented in common baseline | verified for state/movement filtering | unverified | unverified |
| Named component phase dispatch | implemented | not directly unit-tested on this branch | unverified | unverified |
| Initial AI/invulnerable spawn options | implemented | not directly unit-tested | unverified | unverified |
| Push Pillar logical grid | implemented | verified | no adapter | unverified |
| Room candidate ordering/local transform | implemented | verified | no adapter | unverified |
| Module completion model | implemented | verified | no persistence/runtime adapter | unverified |
| Bukkit safety-shell scan | not implemented | N/A | N/A | N/A |
| Room carve/rollback/collapse/safe fall | not implemented | N/A | N/A | N/A |
| Guardian encounter/resurrection | not implemented | N/A | N/A | N/A |
| Actual loot trigger, variants, reward | not implemented | N/A | N/A | N/A |
| Display/contact puzzle adapter | not implemented | N/A | N/A | N/A |

## Final assessment

```
B_IMPLEMENTATION_STARTED = YES
B_CORE_LOGIC_IMPLEMENTED = PARTIAL
B_ACTUAL_CONTENT_INTEGRATED = NO
B_STATICALLY_COMPLETE = NO
B_PAPER_RUNTIME_VERIFIED = NO
B_LIVE_CLIENT_VERIFIED = NO
```

The next implementation step is not another generic engine. It is a Pyramid-specific runtime slice: register an explicit disabled test variant, resolve/validate a real Pyramid room and chest preflight, then integrate one bounded capability at a time with runtime diagnostics and a separate validation-debt record.
