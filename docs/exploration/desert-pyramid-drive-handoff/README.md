# Desert Pyramid Drive Handoff

This folder mirrors the current Google Drive handoff material needed by Codex Cloud, which cannot read the Drive documents directly.

Repository: `ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper`

Branch: `fix/desert-pyramid-full-flow-reconciliation`

Main: do not modify.

## Source Drive search result

The Drive folder `18EGs87jL06ylRUMCap6gmVTg522yrcct` and broad `DESERT_PYRAMID` / `PYRAMID` searches returned three native Google Docs:

1. `DESERT_PYRAMID_CURRENT_WORK`
2. `DESERT_PYRAMID_STABLE_SPEC`
3. `DESERT_PYRAMID_ARCHIVE_INDEX`

The user referred to four Drive documents, but only these three Pyramid Drive documents were visible to the connector at transfer time. This README is therefore included as the fourth GitHub handoff file and records the transfer scope.

## Files

- `DESERT_PYRAMID_CURRENT_WORK.md` — current repair queue, live findings, and Codex Cloud next actions.
- `DESERT_PYRAMID_STABLE_SPEC.md` — stable gameplay/runtime invariants.
- `DESERT_PYRAMID_ARCHIVE_INDEX.md` — archive policy and current-document routing.

## Usage rule

For implementation work, read in this order:

1. `DESERT_PYRAMID_CURRENT_WORK.md`
2. `DESERT_PYRAMID_STABLE_SPEC.md` only where stable contract comparison is needed
3. `DESERT_PYRAMID_ARCHIVE_INDEX.md` only to confirm that archive material is non-authoritative by default

Do not preload historical Pyramid archive material unless `CURRENT_WORK` explicitly names a specific archived item.
