# Native custom enchantments — Stage 1

## Authoritative base and dependency audit

The change is based on `codex/gateway-boss-authoritative-second-pass` commit `7a0aef23703c131eded99638d042a6ef147ae1e7`. Before Stage 1, `EnchantService` stored `id@level` in `hyunseorpg:equipped_enchants`; `EquipmentEffectTriggerEngine` resolved that PDC and dispatched existing content/skill handlers. Shop/crafting IDs produced PDC books, promotion slots selected the active prefix, and `AnvilGrowthListener` replaced every normal anvil interaction with the growth menu.

## Paper plugin and command lifecycle

`paper-plugin.yml` is the sole plugin descriptor. It declares the registry bootstrapper and Paper's optional server dependency syntax for MythicMobs (`required: false`). Commands are no longer declared as Bukkit descriptor commands and production enable contains no `JavaPlugin#getCommand()` call. `PaperCommandCatalog` owns primary labels, descriptions, and aliases; `LifecycleEvents.COMMANDS` registers `PaperCommandBridge` adapters so all existing `CommandExecutor` and `TabCompleter` implementations retain their behavior.

## Registry definitions, applicability, and acquisition

`HyunseoRPGPluginBootstrap` creates all 32 `hyunseorpg:*` entries during the writable enchantment registry compose event. Bootstrap-only metadata is immutable Java data because this event runs before runtime `enchants.yml` can be loaded. A parity test requires exact active ID/max-level equality and rejects alias collisions, missing definitions, and orphan definitions.

Supported-item membership matches runtime scope. Vanilla armor tags are used where exact; bundled `hyunseorpg` item tags enumerate swords, axes, pickaxes, hoes, excavation tools, bows, crossbows, fishing rods, elytra, and mace precisely. Thus axe/hoe/pickaxe/elytra effects are not advertised for broader mining/equippable families.

Acquisition is explicit and independent from enchanting weight/cost:

- `minecraft:in_enchanting_table`: all `NON_TREASURE` definitions.
- `minecraft:tradeable` and `minecraft:on_random_loot`: all definitions.
- `minecraft:non_treasure`: the 21 common definitions.
- `minecraft:treasure`: the 11 signature/active definitions.

These additive (`replace: false`) native tag resources live in the self-contained `datapack/data/...` root beside `datapack/pack.mcmeta`. During bootstrap, `LifecycleEvents.DATAPACK_DISCOVERY` resolves `/datapack` from the plugin JAR, calls `discoverPack(..., "native-enchantments")`, and sets `autoEnableOnServerStart(true)`. Paper builds the data-pack repository before the subsequent registry compose/data reload, so the item and exclusivity tags referenced by registry builders and the acquisition tags referencing the new enchantment keys participate in the same startup load. No external world datapack, structure loot listener, or custom selection engine exists.

The co-applicable `wind_arrow` / `fire_arrow_rain` conflict group is also represented by `hyunseorpg:exclusive_set/bow_shift_left` and supplied to each registry builder with `exclusiveWith`. Other shared physical inputs belong to disjoint supported item sets and cannot coexist on one valid item. The legacy GUI check remains defense-in-depth.

## Native storage and migration

Actual `ItemStack` enchantment components are authoritative. Generated compatibility books must be `ENCHANTED_BOOK`, resolve their native target before item creation, and store the entry through `EnchantmentStorageMeta`. A book may contain vanilla entries plus exactly one Hyunseo entry; books with multiple Hyunseo entries are rejected as ambiguous rather than depending on map iteration order. Vanilla anvils directly create equipment that runtime lookup sees immediately.

Legacy PDC migration is lazy and planned before mutation. Known aliases canonicalize and levels clamp; an existing equal/higher native level counts as complete. Unknown runtime IDs and YAML-known IDs whose native target is unavailable have distinct diagnostics and remain verbatim in PDC. Mixed migration copies successes and preserves only failures. Migration only adds a Hyunseo enchant and updates/removes `equipped_enchants`; unrelated PDC and all vanilla/native entries remain untouched.

## Vanilla mechanics and legacy growth UI

`VanillaEnchantBlockListener` remains as dormant legacy code but is not registered. `AnvilGrowthListener` no longer observes `PlayerInteractEvent`; normal players open the vanilla anvil without a permission exception. The existing RPG menu remains the growth GUI entry, and its click/drag/close listeners are unchanged. Repository audit found no other table/anvil/grindstone/trade/loot command blocker registered in production. Old `vanilla-enchants.*` and `enhancement.block-vanilla-enchanting` keys are intentionally retained as dead compatibility config for later cleanup.

## Scope retained for later stages

Coin, Magic Stone, Upgrade Stone, Promotion, growth/enhancement/repair UI, Class, Skill, Mana, Stat, proficiency, RPG level, mob scaling, farming, alchemy, quest, exploration, and special-equipment runtimes remain in place. No new GUI, currency, growth engine, or structure loot engine was introduced.

## LIVE VERIFICATION REQUIRED

1. Boot Paper 26.1.2 with and without MythicMobs; confirm the `HyunseoRPG/native-enchantments` pack is discovered/enabled and all commands/aliases complete.
2. Reroll table candidates for every supported family; verify treasure entries never appear there.
3. Reroll librarian offers and generate random enchanted loot; inspect native Hyunseo books/items.
4. Apply a Hyunseo book with a vanilla enchant through a normal-player vanilla anvil and verify XP, repair, compatibility, and exclusivity.
5. Save/restart mixed vanilla/Hyunseo items and confirm components persist.
6. Exercise known, alias, missing-native, unknown, mixed, and pre-existing-higher-level legacy PDC fixtures twice.
7. Open the growth UI from the RPG menu and verify its enhancement/promotion/enchant/repair inventories still function.
