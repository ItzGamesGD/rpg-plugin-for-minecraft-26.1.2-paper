# Vanilla anvil enhancement — Stage 2

## Authoritative base and baseline

Stage 2 starts from the completed Stage-1 native-enchantment head `1632923297987633a290caa195a8a828c859d314` on the supplied workspace branch. It preserves `paper-plugin.yml`, Paper lifecycle commands, bundled native-enchantment datapack discovery, native ItemStack enchant storage, and lazy legacy enchant migration. The baseline `./gradlew clean test --no-daemon` was attempted before branching; this environment's proxy returned HTTP 403 while resolving Paper API, before compilation.

## Dependency audit

- `EquipmentEnhancementService` owns `hyunseorpg:enhancement_level`, generated enhancement lore, profile effects used by `CombatService`, tool/farming speed hooks, and admin force-level behavior. Previously it asked `EquipmentPromotionService` for its cap and exposed coin/chance/failure APIs.
- `EnhancementRegistry` maps vanilla materials/custom IDs to stat profiles, interpolated the old +50 stone/coin/chance curves, and reads `equipment-growth.yml`.
- `EquipmentGrowthPolicy` and `EquipmentTierService` classify supported materials, grades, endgame state, and registered special equipment. Promotion, enchant-slot, and repair policy remain for later stages.
- `SpecialEquipmentRegistry` contains both mid-tier elemental equipment and dedicated special/endgame equipment. Stage 2 therefore adds an explicit configured elemental boundary rather than treating every registry member alike.
- `EquipmentInstanceService` UUID and all ItemMeta/PDC, damage, model, display, lore, vanilla enchants, and Stage-1 native enchants survive because the anvil preview starts with `equipment.clone()` and changes only enhancement PDC/generated lore.
- `EquipmentGrowthGuiService` previously spent coins/stones and rolled chance/failure. Its enhancement screen is now unreachable; promotion, enchant compatibility, repair, support, and later-removal classes remain intact.
- `AnvilGrowthListener` already stopped hijacking anvil blocks in Stage 1 and continues to handle only legacy growth inventories. `RPGMenuService` remains the growth-menu entry.
- Existing item crafting/shop/drop paths still create `basic_upgrade_stone`; Stage 2 only consumes it as the physical second anvil input and does not redesign its acquisition.

## New transaction

`VanillaAnvilEnhancementListener` intervenes only when the second input slot (inventory index 1) contains the registered `basic_upgrade_stone`. All other second-slot inputs and outputs remain untouched for vanilla rename, material/item repair, and vanilla/native enchanted books. For the custom combination it clones the first input (index 0), resolves the next level, writes `+1`, sets repair-material cost to exactly one, and sets a configured Minecraft XP-level repair cost. Paper/Minecraft owns output clicks, insufficient-level rejection, creative behavior, shift/number-key transactions, input consumption, preview invalidation, and concurrent container state; the plugin does not implement a parallel click transaction.

The enhancement is deterministic. Coin cost is zero, success is 100%, and fail count/bonus are no longer written or read. XP costs are levels (not raw points): +1–5 costs 1, +6–10 costs 2, +11–20 costs 3, +21–30 costs 4, and +31–40 costs 5 by default.

## Classification and caps

- `VANILLA`: any supported ordinary vanilla or starter equipment, enchanted or not; configurable cap, default +30.
- `ELEMENTAL`: IDs explicitly listed under `enhancement.elemental-item-ids`, resolved through existing RPG item and special registries; configurable cap, default +40.
- `SPECIAL`: other registered special equipment, including dedicated Thanatos, Solaris, Thunder, Poseidon and Moonlit equipment; no output.
- `ENDGAME`: grade/endgame policy result; no output.
- `UNSUPPORTED`: non-equipment; no output.

The cap no longer reads promotion grade/star, promotion stage, promotion options, or enchant slots. Existing promotion classes/data remain for Stage 3 removal.

## Vanilla and native-enchantment preservation

The result clone retains amount-independent item state: durability, repair cost, instance UUID, item ID, custom model data, display name, other lore, unrelated PDC, vanilla enchantments, and registered Hyunseo enchantments. `applySuccessfulEnhancement` changes only `enhancement_level`, schema metadata when needed, and the generated `강화:` lore line. Stage-1 `equipped_enchants` migration remains migration-only and is not reintroduced as authoritative storage.

## LIVE VERIFICATION REQUIRED

1. Open an anvil as a normal player and test vanilla rename and material/item repair.
2. Apply a vanilla enchanted book and a Hyunseo native enchanted book.
3. Combine a vanilla sword with a stack of upgrade stones; confirm +1, exactly one stone, and configured XP-level deduction.
4. Repeat through +30 and confirm no +31 preview.
5. Enhance each configured elemental weapon through +40 and confirm no +41 preview.
6. Confirm Thanatos, Solaris, Thunder God's Axe, Poseidon, Moonlit Afterglow and other dedicated/endgame items produce no enhancement output.
7. Confirm damage, custom model data, name, lore, instance UUID, unrelated PDC, vanilla enchantments and Hyunseo enchantments survive.
8. Restart and verify level and enchant persistence.
9. Exercise normal click, shift-click, number-key, double-click, cursor replacement, drag, stacked stones, insufficient levels, creative, stale preview, repeated output and inventory close.

## Deliberately retained

Promotion/Promotion Stone, Coin, Magic Stone/fragments, legacy growth/repair/support classes, Class, Skill, Mana, Stats, proficiency, RPG level, mob scaling, farming, alchemy, quest, exploration, shops and special-weapon progression remain. No content gate, mob scaling, global XP curve, ore, recipe, rune, or endgame system was added.
