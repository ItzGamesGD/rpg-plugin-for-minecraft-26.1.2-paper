# Exploration validation-gated development policy

## Purpose

This policy governs future HyunseoRPG Exploration work, including Desert Pyramid, Ocean Monument, Woodland Mansion, Bastion, End content and later structure events.

It preserves the original Exploration philosophy:

```
Swamp Hut prototype
-> Pillager Outpost prototype
-> live gameplay verification
-> Exploration vertical slice validation
-> later cloud implementation
```

The purpose was to prove the real Minecraft loop before scaling it:

```
structure detection
-> variant selection
-> activation
-> event
-> objective
-> clear
-> reward
-> cleanup
```

That principle remains valid. It does not mean every later line of code must wait for a full live test.

## Why the policy must evolve

Content complexity increases materially:

```
Swamp Hut
-> simple encounter/prototype

Pillager Outpost
-> trigger + wave/phase + objective + reward

Desert Pyramid
-> module composition + staged entities + world mutation
-> generated room + partial completion + Display puzzle

Ocean Monument and later content
-> longer encounter chains + structure-relative spatial logic
-> underwater runtime interaction + cross-structure items + boss orchestration
```

A successful live test of two prototypes does not automatically validate a new runtime capability such as a room carve, body-push Display interaction or a resurrection animation.

## Capability-based validation

The unit of confidence is both the content and each runtime capability it depends on.

- A **new composition of already validated capabilities** may be implemented in cloud/Codex work.
- A **new runtime capability** is allowed to be implemented, but it carries explicit **Validation Debt** until it is checked at the appropriate runtime level.
- A capability used by one unverified content branch remains content-specific by default.

Examples of comparatively reusable validated building blocks include trigger, bounded sequence, objective tracking, spawn, reward and cleanup when their existing runtime paths apply.

Examples of new capability candidates include:

- large/structural block mutation;
- bounded room carve and rollback;
- body-push BlockDisplay interaction;
- complex resurrection lifecycle;
- underwater spatial mechanics;
- a new inventory extraction semantic;
- a new scheduler animation or multiplayer ownership rule.

## Validation levels

| Level | State | Required evidence |
| --- | --- | --- |
| 0 | DESIGNED | A design exists; no claim of implementation. |
| 1 | IMPLEMENTED | Source code exists. This alone proves no test result. |
| 2 | STATICALLY_VERIFIED | Compile, architecture/call-path review, config/registry consistency and regression-oriented inspection completed. |
| 3 | UNIT_VERIFIED | Deterministic core logic passes focused JUnit tests. Examples: transitions, candidate order, counters, grid movement and duplicate suppression. |
| 4 | PAPER_RUNTIME_VERIFIED | The capability has run correctly in actual Paper/Bukkit runtime, automatically or manually. |
| 5 | LIVE_CLIENT_VERIFIED | Gameplay behavior has been checked from a real Minecraft client. Examples: knockback feel, particle visibility, Display interaction, inventory UX, AI state, animation and multiplayer behavior. |

A build or JUnit success must never be presented as Paper Runtime or Live Client verification.

## Validation Debt

A capability is allowed to be useful before live validation. Its status must simply remain visible.

Example:

```
Capability: Pyramid Push Pillar body-interaction adapter
Implementation: IMPLEMENTED
Static: STATICALLY_VERIFIED
Unit: UNIT_VERIFIED
Paper Runtime: UNVERIFIED
Live Client: UNVERIFIED
Validation Debt: OPEN
```

Validation Debt is not automatically a bug. It means the code exists but an important runtime/gameplay assumption has not yet been proven.

### Allowed while debt is open

- Continue deterministic logic, state modelling, configuration validation and unit tests in the same content.
- Implement another content that only composes capabilities already validated for its intended use.
- Prepare a narrow adapter with explicit diagnostics and cleanup ownership.

### Restricted while debt is open

Do not build a second unverified generic layer on top of it.

Avoid this chain:

```
unverified Pyramid mechanic
-> unverified generic abstraction
-> Ocean Monument depends on it
-> another unverified abstraction
-> Woodland Mansion depends on it
```

A wrong first runtime assumption would otherwise propagate across multiple content branches.

## Generic promotion rule

A capability used by only one content implementation and not yet Paper/Live validated stays **CONTENT_SPECIFIC**.

Do not turn first-use Pyramid mechanics into `GenericExcavationEngine`, `GenericPhysicalPuzzleEngine`, `GenericGridPuzzleEngine`, `GenericRoomGenerationEngine` or a comparable framework.

Generic promotion normally requires all of the following:

1. a second real content use case;
2. a clear common contract shared by both uses;
3. sufficient Paper/runtime evidence for the first implementation;
4. confirmation that a small existing-component extension cannot solve it.

## Cloud parallel development

Parallel cloud work is permitted, but the boundary matters.

### Actively allowed

- strict GAP audits;
- deterministic content logic;
- module and phase models;
- objective conditions;
- candidate selection;
- reward rules;
- configuration validation;
- pure coordinate calculations;
- JUnit/static tests;
- documentation and validation-debt records.

### Allowed carefully

- adapters over an already validated Exploration component;
- minimal persistence extensions;
- existing spawn, reward, sequence and cleanup integration.

The adapter must not invent a new runtime assumption unnoticed.

### Prefer serial validation first

When these are new capabilities, validate them in one representative content before broad reuse:

- actual Bukkit world mutation;
- new entity lifecycle pattern;
- new player interaction model;
- new Display interaction;
- new inventory semantic;
- complex scheduled animation;
- new multiplayer ownership behavior;
- new structure detection assumption.

## Recommended workflow

1. Perform a strict GAP audit.
2. Classify each need as REUSE, SMALL_EXTENSION, NEW_GENERIC_COMPONENT or CONTENT_SPECIFIC.
3. Implement deterministic core logic.
4. Complete static and unit verification.
5. Integrate with actual Bukkit/Paper runtime.
6. Record Validation Debt.
7. Live-test a representative content.
8. Fix the demonstrated common-engine gap, if one exists.
9. Promote the verified capability deliberately.
10. Reuse it in later content.

## Pyramid's role

Desert Pyramid is the first complex Exploration stress test:

```
Swamp Hut
-> simple prototype

Pillager Outpost
-> vertical-slice, phase/wave prototype

Desert Pyramid
-> module composition, staged entities, world mutation,
   room generation, partial completion and Display puzzle
```

Its integration and live-validation findings are a future baseline for Ocean Monument and Woodland Mansion. Until then, Pyramid-specific room and Push Pillar logic must not become inherited generic infrastructure.

Ocean Monument and Woodland Mansion work is still allowed before Pyramid is live-verified, for example GAP audits, deterministic event/room-selection logic and unit tests. The restriction is on chaining new generic abstractions over Pyramid's unverified capabilities.

## Branch and regression policy

New content branches should originate from a verified common Exploration baseline whenever practical.

If a content branch discovers a true common capability:

1. audit whether it is actually generic;
2. isolate it in a clear commit;
3. run static/unit regression checks;
4. manage it as a common-baseline candidate;
5. branch later content from that baseline, not from the source content's full branch.

Do not let Ocean Monument inherit a content-specific Pyramid mechanic merely because Pyramid happened to be its parent branch.

Every new capability review considers regressions in:

- Swamp Hut;
- Pillager Outpost;
- Shipwreck where present;
- structure detection and variant selection;
- StructureRecord/persistence;
- runtime lifecycle;
- objectives;
- reward;
- clear, abandon and cleanup.

## Diagnostics

Paper integration automation is desirable but not a prerequisite for development. Use targeted diagnostics to reduce later live-test cost.

Useful event-level fields include structure/runtime ID, variant/module, phase transition, objective transition, scheduled action, task cancellation, tracked entity, block mutation, reward, clear, abandon, cleanup and fail-safe exception.

Do not log every tick or every PlayerMoveEvent.

## Completion-report standard

Exploration reports should include a matrix like:

| Field | Example |
| --- | --- |
| Capability | Push Pillar logical grid |
| Implementation | YES |
| Static | PASS |
| Unit | PASS |
| Paper Runtime | NOT_REQUIRED_FOR_PURE_LOGIC |
| Live Client | UNVERIFIED |
| Validation Debt | body-interaction adapter remains open |
| Regression | existing Exploration suite status |

## Policy summary

1. New compositions of verified capabilities may be implemented in cloud work.
2. New capabilities may also be implemented, but their Validation Debt must be explicit.
3. Do not build an unverified generic abstraction chain on top of unverified runtime capabilities.

This is neither a ban on implementation before live testing nor permission to treat static success as unlimited gameplay proof. It is a managed path for turning representative live-tested capabilities into reliable Exploration infrastructure.
