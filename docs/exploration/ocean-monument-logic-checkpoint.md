# Ocean Monument Logic-Only Checkpoint

## Baseline

| Field | Value |
|---|---|
| Repository | `ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper` |
| Branch | `feature/ocean-monument-logic` |
| Baseline branch | `main` |
| Baseline HEAD | `413f558b500311bbcfa236e10c835eddbac0c49c` |
| Logic commit | `83213c121457af9fecd88ed5158dac3bb9ef78b9` |
| Audit date | 2026-08-24 |
| Parent development baseline | Outpost follow-up on `main`: `fix(exploration): force raid target chase after delay` |

`main` was selected because it is the latest branch containing the Outpost follow-up fix while remaining free of both the common-event-engine branch and all Desert Pyramid-specific changes. The branch is deliberately **not** based on `feature/desert-pyramid`; therefore it contains none of Push Pillar, Pyramid room, Pyramid module-progress, or Pyramid persistence work.

## Design sources reviewed

- `src/main/resources/exploration/structures.yml` at the baseline: the `ocean_monument` definition exists but is disabled and has no variants.
- `project_sources/14-3-.txt`, section 5.12: the confirmed flow is three fake diamond-block seals, a warning before the third seal, a transition to a top-level combat area, monument army/Guardian combat, and the Deep-Sea Tidecaster as the boss-equivalent.
- `docs/exploration/exploration-validation-development-policy.md` and `docs/exploration/desert-pyramid-implementation-checkpoint.md` on `feature/desert-pyramid @ 301916858e0afe144d3e08a3408b02667c7bed87`: reviewed as policy/checkpoint references only, not inherited as code.

No design source identifies a specific cross-structure item or its Ocean Monument effect. Such an item is intentionally not represented in the logic model.

## Strict gap audit

| Capability | Classification | Current result |
|---|---|---|
| Three-seal completion and duplicate suppression | LOGIC_ONLY_NEW | Implemented in `OceanMonumentProgress`. |
| Pre-final-seal warning gate | LOGIC_ONLY_NEW | Logical acknowledgement is implemented; the warning presentation is not. |
| Phase progression and fail-safe rejection | LOGIC_ONLY_NEW | Implemented as a deterministic state machine. |
| Encounter objective completion/count | LOGIC_ONLY_NEW | Implemented with immutable required IDs and exactly-once completion. |
| Deep-Sea Tidecaster eligibility/completion | LOGIC_ONLY_NEW | Logical eligibility and completion only. No entity request is executed. |
| Clear/reward eligibility | LOGIC_ONLY_NEW | Logical eligibility and one claim gate only. |
| Existing Exploration runtime/sequence/objective/reward lifecycle | REUSE | Not invoked on this branch; a future runtime adapter must compose these verified capabilities. |
| Monument discovery, bounds, orientation, and random internal seal slots | RUNTIME_ADAPTER_REQUIRED | No world/structure access was added. |
| Fake BlockDisplay seals and player interaction | RUNTIME_ADAPTER_REQUIRED | No Display, Interaction, listener, or scheduler code was added. |
| Warning/title, relocation, ceiling presentation, entity waves, boss spawn, reward delivery, cleanup, persistence | RUNTIME_ADAPTER_REQUIRED | Explicitly deferred. |
| Three-dimensional underwater combat, water-column effects, underwater velocity/knockback, pathfinding | NEW_CAPABILITY_CANDIDATE | Deferred; no common abstraction was created. |
| Cross-structure item identity and optional route/reward semantics | DESIGN_UNRESOLVED | No confirmed item contract exists in the reviewed design. |
| Monument-specific seal/encounter progression | CONTENT_SPECIFIC | Kept in the Ocean package; no generic seal/wave/boss engine. |

## Pure logic implemented

`src/main/java/com/hyunseo/hyunseorpg/exploration/ocean/` contains only Java value/state logic and imports no `org.bukkit` type.

- `MonumentPhase`: minimal lifecycle and terminal phases.
- `MonumentActionResult`: immutable accepted/rejected transition result and reason.
- `OceanMonumentProgress`: fixed three-seal model, final-warning gate, objective completion, boss eligibility, clear eligibility, logical reward claim suppression, and abandon lockout.

The model stores only logical string IDs and sets. It does not store locations, world state, player/entity UUIDs, scheduled tasks, or persistent records.

## Verification matrix

| Capability | Implementation | Static | Unit | Paper runtime | Live client |
|---|---|---|---|---|---|
| Seal state and final-warning gate | IMPLEMENTED | VERIFIED | VERIFIED | NOT_IMPLEMENTED | UNVERIFIED |
| Objective/boss/clear state | IMPLEMENTED | VERIFIED | VERIFIED | NOT_IMPLEMENTED | UNVERIFIED |
| Duplicate suppression | IMPLEMENTED | VERIFIED | VERIFIED | NOT_IMPLEMENTED | UNVERIFIED |
| Logical final-reward claim gate | IMPLEMENTED | VERIFIED | VERIFIED | NOT_IMPLEMENTED | UNVERIFIED |
| Monument discovery and spatial resolution | NOT_IMPLEMENTED | N/A | N/A | UNVERIFIED | UNVERIFIED |
| Fake seals, player interaction, relocation, combat and reward delivery | NOT_IMPLEMENTED | N/A | N/A | UNVERIFIED | UNVERIFIED |

Static verification here means source inspection: no common production source was changed, no registry/configuration/persistence file was changed, and the new package has no Bukkit/Paper import. Unit verification is GitHub Actions run #46, job #97365205058: `clean build + JUnit` completed successfully.

## Validation debt

Open validation debt remains for:

- actual Monument detection, bounds, orientation, and deterministic/random safe internal slots;
- BlockDisplay/Interaction seal lifecycle and actual player interaction;
- warning and relocation presentation;
- ceiling opening/block mutation;
- army/Guardian/Deep-Sea Tidecaster entity lifecycle;
- underwater velocity, knockback, vertical combat, and water-column mechanics;
- actual reward delivery, StructureRecord persistence, cleanup, participants, and multiplayer behavior.

## Deliberate exclusions

This branch adds no Bukkit listener, scheduler task, entity spawn, AI, particles, sounds, titles, movement/status effect, teleport, block mutation, structure scan, inventory listener, reward call, persistence change, configuration/registry entry, or cleanup adapter.

No `OceanMonumentEngine`, `OceanMonumentEventManager`, `UnderwaterEngine`, `GenericBossEngine`, `GenericWaveEngine`, `GenericSealEngine`, `GenericStructureRoomEngine`, or scripting layer was created.

## Next integration step

After the representative Pyramid validation work has established the relevant runtime capabilities, perform a separate Ocean Monument runtime GAP audit. Keep any proven common capability in a standalone generic commit; keep monument-only adapters/content mechanics out of the common baseline until a second validated use case exists.
