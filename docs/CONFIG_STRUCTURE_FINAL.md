# HyunseoRPG Configuration Structure Finalization

## Canonical ownership

`equipment-growth.yml` is the only runtime source for equipment enhancement and promotion.

- Enhancement profiles, limits, costs, chances, and effects are read from `equipment-growth.yml`.
- Promotion grades, stars, option pools, option definitions, and enchant-slot unlocks are read from `equipment-growth.yml`.
- Shop filtering for promotion stones reads the canonical promotion stone key from `equipment-growth.yml`.
- `/rpg reload enhancement`, `/rpg reload promotion`, `/rpg reload equipment`, and `/rpg reload all` reload the canonical file before validation or registry refresh.

## Legacy compatibility

Existing items are not regenerated or deleted. Their PDC growth data remains valid. Legacy `normal-6` through `normal-10` stage identifiers are mapped to the canonical `advanced-1` through `advanced-5` stages when an item is read.

The old `enhancements.yml` file is no longer loaded by the runtime and is no longer packaged as a default resource. It can be moved safely with:

```text
/rpg migrate legacy --dry-run
/rpg migrate legacy --apply
```

The apply operation requires both canonical `enhancement` and `promotion` sections and moves `enhancements.yml` and any legacy `dungeons.yml` to `archive/legacy/<timestamp>`. This is a reversible archive operation, not item deletion.

## Operational verification

After applying the archive migration:

1. Run `/rpg doctor all` and confirm `error=0`.
2. Confirm there is no warning for `enhancements.yml`.
3. Run `/rpg reload all`.
4. Test one enhancement preview, one enhancement attempt, and one promotion preview.
5. Confirm an existing grown item retains its PDC stage, options, and enchant slots.

Gameplay verification remains a server-side test; Gradle compilation does not prove these interactions.
