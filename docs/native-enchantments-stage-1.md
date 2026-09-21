# Native custom enchantments — Stage 1

## Vanilla-authoritative catalogue follow-up

The active `hyunseorpg:*` catalogue now contains only mechanics Minecraft does not already provide.
Former copies of Protection, Fire/Blast/Projectile Protection, Thorns, Respiration, Aqua Affinity,
Swift Sneak, Depth Strider, Soul Speed, Frost Walker, and Unbreaking remain registered only as
unsupported, non-acquirable serialization shims. Lazy normalization converts their native or old
`equipped_enchants` representation to the corresponding `minecraft:*` enchant using the greatest
existing level, clamped to the vanilla maximum. An incompatible legacy conversion is retained inert
and logged rather than discarded. Swift Sneak bridge PDC is migration input only and is removed
after conversion.

New custom books are plain `ENCHANTED_BOOK` stacks with native stored enchantments and no custom
display name or generated lore. Equipment uses Minecraft's native tooltip; old generated enchant
lore is cleanup-only. Application is exclusively the vanilla anvil, and grindstone previews are
normalized so removed custom enchants cannot leave ghost lore/PDC.

Paper's registry entry-add lifecycle removes only the Infinity↔Mending and
Protection↔Fire Protection keys from those four entries' existing exclusive sets. It does not alter
max levels or any other protection conflict. Wind Arrow↔Fire Arrow Rain remains a native custom
exclusive set.

## Authoritative base and dependency audit

The change is based on `codex/gateway-boss-authoritative-second-pass` commit `7a0aef23703c131eded99638d042a6ef147ae1e7`. Before Stage 1, `EnchantService` stored `id@level` in `hyunseorpg:equipped_enchants`; `EquipmentEffectTriggerEngine` resolved that PDC and dispatched existing content/skill handlers. Shop/crafting IDs produced PDC books, promotion slots selected the active prefix, and `AnvilGrowthListener` replaced every normal anvil interaction with the growth menu.

## Paper plugin and command lifecycle

`paper-plugin.yml` is the sole plugin descriptor. It declares the registry bootstrapper and Paper's optional server dependency syntax for MythicMobs (`required: false`). Commands are no longer declared as Bukkit descriptor commands and production enable contains no `JavaPlugin#getCommand()` call. `PaperCommandCatalog` owns primary labels, descriptions, and aliases; `LifecycleEvents.COMMANDS` registers `PaperCommandBridge` adapters so all existing `CommandExecutor` and `TabCompleter` implementations retain their behavior.

## Registry definitions, applicability, and acquisition

`HyunseoRPGPluginBootstrap` creates 20 active, genuinely new `hyunseorpg:*` entries plus 12 inert serialization shims during the writable enchantment registry compose event. Bootstrap-only metadata is immutable Java data because this event runs before runtime `enchants.yml` can be loaded. A parity test requires exact active ID/max-level equality and rejects alias collisions, missing definitions, and orphan definitions.

Supported-item membership matches runtime scope. Vanilla armor tags are used where exact; bundled `hyunseorpg` item tags enumerate swords, axes, pickaxes, hoes, excavation tools, bows, crossbows, fishing rods, elytra, and mace precisely. Thus axe/hoe/pickaxe/elytra effects are not advertised for broader mining/equippable families.

Acquisition is explicit and independent from enchanting weight/cost:

- `minecraft:in_enchanting_table`: all `NON_TREASURE` definitions.
- `minecraft:tradeable` and `minecraft:on_random_loot`: all definitions.
- `minecraft:non_treasure`: the 9 common definitions.
- `minecraft:treasure`: the 11 signature/active definitions.

These additive (`replace: false`) native tag resources live in the self-contained `datapack/data/...` root beside `datapack/pack.mcmeta`. During bootstrap, `LifecycleEvents.DATAPACK_DISCOVERY` resolves `/datapack` from the plugin JAR, calls `discoverPack(..., "native-enchantments")`, and sets `autoEnableOnServerStart(true)`. Paper builds the data-pack repository before the subsequent registry compose/data reload, so the item and exclusivity tags referenced by registry builders and the acquisition tags referencing the new enchantment keys participate in the same startup load. No external world datapack, structure loot listener, or custom selection engine exists.

The co-applicable `wind_arrow` / `fire_arrow_rain` conflict group is also represented by `hyunseorpg:exclusive_set/bow_shift_left` and supplied to each registry builder with `exclusiveWith`. Other shared physical inputs belong to disjoint supported item sets and cannot coexist on one valid item. There is no gameplay equip GUI or plugin-owned application transaction.

## Native storage and migration

Actual `ItemStack` enchantment components are authoritative. Generated compatibility books must be `ENCHANTED_BOOK`, resolve their native target before item creation, and store the entry through `EnchantmentStorageMeta`. A book may contain vanilla entries plus exactly one Hyunseo entry; books with multiple Hyunseo entries are rejected as ambiguous rather than depending on map iteration order. Vanilla anvils directly create equipment that runtime lookup sees immediately.

Legacy PDC migration is lazy and planned before mutation. Active aliases canonicalize and levels clamp; retired vanilla copies merge into their `minecraft:*` targets using maximum-not-sum semantics. Unknown, missing-target, and incompatible entries remain encoded and receive diagnostics. Native retired entries use the same policy on equipment and stored-enchantment books. Unrelated PDC and enchantments remain untouched.

## Vanilla mechanics and legacy growth UI

`VanillaEnchantBlockListener` remains as dormant legacy code but is not registered. `AnvilGrowthListener` no longer observes `PlayerInteractEvent`; normal players open the vanilla anvil without a permission exception. Growth-menu enchant buttons now direct players to the enchanting table/vanilla anvil, and the former equip method and book-consuming GUI route are absent. Repository audit found no other table/anvil/grindstone/trade/loot command blocker registered in production. Old `vanilla-enchants.*` and `enhancement.block-vanilla-enchanting` keys are intentionally retained as dead compatibility config for later cleanup.

## Scope retained for later stages

At Stage 1 completion, Coin, Magic Stone, Upgrade Stone, Promotion, growth/enhancement/repair UI, Class, Skill, Mana, Stat, proficiency, RPG level, mob scaling, farming, alchemy, quest, exploration, and special-equipment runtimes remained in place. Stage 2 later moved enhancement to the vanilla anvil and Stage 3 removes ordinary equipment Promotion without changing the native-enchantment architecture.

## LIVE VERIFICATION REQUIRED

1. Boot Paper 26.1.2 with and without MythicMobs; confirm the `HyunseoRPG/native-enchantments` pack is discovered/enabled and all commands/aliases complete.
2. Reroll table candidates for every supported family; verify treasure entries never appear there.
3. Reroll librarian offers and generate random enchanted loot; inspect native Hyunseo books/items.
4. Apply a Hyunseo book with a vanilla enchant through a normal-player vanilla anvil and verify XP, repair, compatibility, and exclusivity.
5. Save/restart mixed vanilla/Hyunseo items and confirm components persist.
6. Exercise known, alias, missing-native, unknown, mixed, and pre-existing-higher-level legacy PDC fixtures twice.
7. Open the growth UI and verify enchant buttons provide vanilla-workstation guidance and no ordinary equipment Promotion route remains.
