# Native custom enchantments — stage 1

## Dependency audit

The authoritative starting commit was `7a0aef23703c131eded99638d042a6ef147ae1e7`. Fetching the private upstream was attempted before branching, but this build environment had no GitHub credentials; the checked-out commit exactly matched the supplied authoritative HEAD.

Before this stage, `EnchantService` encoded `id@level` entries in `hyunseorpg:equipped_enchants`; `EquipmentEffectTriggerEngine` resolved those entries and dispatched `content` handlers or delegated `skill` handlers to `SkillService`. Shop and crafting item IDs produced PDC-marked enchanted-book items. Promotion slots limited which encoded entries were active. `EquipmentEnchantContentService` owns the reactive combat/gather/fishing effects, while input enchants are routed from `SkillInputListener`. Equipment UUID PDC is used for per-item cooldown identity. `EnchantLoreRefreshListener`, item creation normalization, extraction, the growth GUI, and support GUI all call `EnchantService`.

## Stage-1 architecture

`HyunseoRPGPluginBootstrap` registers all 32 active definitions in Paper's writable enchantment registry before the plugin enables. Definitions include the key, display component, maximum level, weight, enchanting costs, anvil cost, supported-item tag, and active equipment slots. Runtime trigger settings remain in `enchants.yml`.

`EnchantService` now reads levels from actual item or enchanted-book enchantment components. Existing shop/crafting IDs remain a temporary lookup path, but generated books carry a stored native enchantment. The old GUI apply/extract path writes and removes native enchantments. The legacy `VanillaEnchantBlockListener` is no longer registered, so it cannot cancel tables/anvils or strip generated enchantments. Promotion-slot counts are deliberately not consulted when deciding whether a native enchantment effect is active; the slot subsystem itself remains untouched for later removal.

Legacy `equipped_enchants` is migrated lazily when an item is inspected. Known aliases are canonicalized, the highest safe level is copied to the native enchantment component, existing vanilla/native enchantments and unrelated PDC are retained, and the legacy key is removed. Unknown IDs are logged and do not damage the item. Re-reading the item is idempotent.

## Acquisition and live verification

Registry registration and supported-item tags make the entries valid for native item/book storage and vanilla-anvil application. Minecraft's acquisition membership is driven by enchantment tags (`minecraft:in_enchanting_table`, `minecraft:tradeable`, `minecraft:on_random_loot`, and treasure/non-treasure policy). **LIVE VERIFICATION REQUIRED:** validate the target Paper 26.1.2 server's enabled datapack/tag policy and perform:

1. reroll an enchanting table with sword, armor, bow, tools, fishing rod, mace, and elytra candidates;
2. reroll a librarian and inspect native enchanted-book offers;
3. generate random enchanted loot without adding structure-specific loot handlers;
4. apply a generated custom enchanted book through a vanilla anvil and confirm XP/repair behavior;
5. retain a vanilla enchantment beside a Hyunseo enchantment after save/restart;
6. hold/use old PDC equipment and verify one-time lazy migration.

No custom loot engine, acquisition GUI, currency, class, skill, stat, mana, promotion, growth, farming, alchemy, quest, or special-weapon subsystem is removed or redesigned in this stage.
