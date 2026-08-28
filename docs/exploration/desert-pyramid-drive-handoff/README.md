# Desert Pyramid Drive Handoff

This folder mirrors the current Google Drive handoff material needed by Codex Cloud, which cannot read the Drive documents directly.

Repository: `ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper`

Branch: `fix/desert-pyramid-full-flow-reconciliation`

Main: do not modify.

## Source Drive documents

Transferred on 2026-08-28 UTC from the currently authoritative Drive handoff set:

1. `DESERT_PYRAMID_CURRENT_WORK`
2. `DESERT_PYRAMID_STABLE_SPEC`
3. `DESERT_PYRAMID_ARCHIVE_INDEX`
4. `HYUNSEORPG_WORK_OPERATING_PROTOCOL`

## Files

- `DESERT_PYRAMID_CURRENT_WORK.md` — current repair queue, live findings, and Codex Cloud next actions.
- `DESERT_PYRAMID_STABLE_SPEC.md` — stable gameplay/runtime invariants.
- `DESERT_PYRAMID_ARCHIVE_INDEX.md` — archive policy and current-document routing.
- `HYUNSEORPG_WORK_OPERATING_PROTOCOL.md` — standing Work/task execution protocol, including manual vs automation mode rules.

## Usage rule

For Desert Pyramid implementation or review work, read in this order:

1. `HYUNSEORPG_WORK_OPERATING_PROTOCOL.md` for execution-mode handling.
2. `DESERT_PYRAMID_CURRENT_WORK.md` for current branch, defects, gates, and next repair queue.
3. `DESERT_PYRAMID_STABLE_SPEC.md` only where stable contract comparison is needed.
4. `DESERT_PYRAMID_ARCHIVE_INDEX.md` only to confirm archive routing and non-authoritative status.

Do not preload historical Pyramid archive material unless `CURRENT_WORK` explicitly names a specific archived item.
