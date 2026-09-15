# Stage 4: Minecraft-native economy

Base: `b746a936fd5c601e65bfd03956efb8a42a0eff33` (Stage 1–3 authoritative HEAD).

## Dependency disposition

- **Removed:** Coin balance/service/display, activity currency rewards, Magic Stone and both legacy fragment currencies, currency shops, extraction/repair currency transactions, and their commands/configuration.
- **Retained:** the Upgrade Stone as a physical `ItemStack`, vanilla-anvil XP costs, useful elemental and boss crafting materials, native enchant acquisition, and all Stage 1–3 special-equipment behavior.
- **Deferred:** quest, farming, delivery, alchemy, and the main RPG navigation shell remain for their dedicated cleanup stages. Their legacy serialized coin fields are read only as ignored compatibility data and never awarded or saved anew.
- **Minecraft replacement:** Upgrade Stones are crafted from configurable recipe data using Diamond, Redstone, and Lapis Lazuli. Bosses award Minecraft experience and configurable physical Upgrade Stones/useful cores.

Ordinary mining, farming, fishing, hunting, logging, and husbandry no longer produce plugin currency or generic progression fragments. Vanilla drops remain authoritative.

## Live verification checklist

1. Log in with old player data and restart without errors.
2. Confirm old coin/fragment values have no gameplay effect.
3. Verify mining, logging, farming, fishing, and mob kills award no plugin currency.
4. Kill each boss and verify XP, Upgrade Stone, and useful core drops.
5. Craft an Upgrade Stone from vanilla resources.
6. Enhance once in a vanilla anvil and verify one stone and the configured XP-level cost are consumed.
7. Verify enchanting-table, librarian, loot-book, and vanilla-anvil custom enchant acquisition.
8. Verify dedicated special weapons and Gateway Boss behavior.
9. Confirm no currency-only shop or menu entry is reachable.
