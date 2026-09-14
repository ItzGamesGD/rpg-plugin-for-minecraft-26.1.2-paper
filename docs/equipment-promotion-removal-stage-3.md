# Equipment progression restructuring — Stage 3

## Scope and dependency decision

Stage 3 removes the ordinary equipment Promotion layer after enhancement became an independent vanilla-anvil transaction. The removed layer owned grade/star progression, random options, rerolls, Promotion Stones, and custom-enchant slot unlocks. It previously influenced combat stats, durability, repair price, special-equipment eligibility, and growth/support GUIs; those dependencies are now absent.

The farming-stage and farming-hoe systems are intentionally unchanged. They are a farming subsystem, not the retired ordinary equipment Promotion layer.

## Runtime model

Ordinary and elemental equipment now retain only their independent enhancement level. Vanilla equipment caps at +30, elemental equipment caps at +40, and dedicated special/endgame equipment remains excluded. Enhancement still uses one Upgrade Stone and configured Minecraft XP levels in the vanilla anvil with deterministic success.

Native vanilla and `hyunseorpg:*` enchantments remain ItemStack enchantments. Promotion no longer grants or limits enchant slots, and the native registry/datapack, compatibility policies, vanilla anvil application, and migration shims from Stages 1–2 remain intact.

## Removed surface

- Equipment promotion service, model/grade, option roller, holder, GUI routes, reroll support, and debug command path.
- Promotion Stone and reroll-ticket item definitions, recipes, shop/reward/drop entries.
- Promotion sections in equipment growth/support/special-equipment configuration and their validators/migrations.
- Promotion bonuses in combat, tools, durability, repair, and special-equipment calculations.
- Promotion-based special-equipment requirements and custom-enchant slot metadata.
- The dormant custom enhancement inventory and its direct item-mutation transaction; the vanilla anvil listener is now the only normal enhancement authority.
- The unused `equipment-options.yml` registry/service/config surface and obsolete tier-specific `max-enhancement: 50` ceiling.

Existing item PDC is not destructively rewritten: obsolete grade/star/option/slot fields are ignored. Equipment identity, enhancement level, durability, native enchantments, unrelated PDC, and special-equipment identity are preserved.

`EnhancementClassificationService` is the sole enhancement-capability authority. The configured elemental IDs resolve to `ELEMENTAL` and max +40 even when their definitions are hosted by the special-equipment registry; dedicated weapons resolve to `SPECIAL`/`ENDGAME` and max 0. Special-equipment lore is derived from this same calculated maximum rather than a second `growth.enhancement-enabled` boolean.

## Live verification

1. Open a vanilla anvil; verify repair, rename, vanilla books, and Hyunseo native books.
2. Enhance ordinary equipment with one Upgrade Stone and configured XP levels; verify +30 cap.
3. Enhance elemental equipment; verify +40 cap and special equipment rejection.
4. Verify no equipment Promotion/reroll menu appears and obsolete Promotion Stones perform no progression action.
5. Use a legacy promoted item; verify it does not crash and can still enhance while identity/enchantments remain intact.
6. Verify representative special equipment (Poseidon, Flame Axe, Thanatos, Thunder God's Axe, Solaris, Moonlit Afterglow, Thousand Eyes) and Gateway Boss behavior.
7. Restart and repeat legacy-item, enhancement, and native-enchantment checks.
