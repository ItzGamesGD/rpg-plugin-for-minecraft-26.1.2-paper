# Stage 4: Minecraft-native gameplay authority

Stage 4 removes the legacy economy, progression navigation shell, and ordinary vanilla-boss transaction layer while preserving the Stage 1–3 enchantment and enhancement architecture.

## Removed authority

- Coin balances, services, commands, displays, activity rewards, and persistence writes.
- Magic Stone, currency fragments, conversion recipes, currency shops, extraction tickets, and custom currency repair.
- The RPG main-menu, equipment-growth inventory, enchant-support inventory, ordinary boss menu, and their holders/listeners/navigation wiring.
- Wither and Ender Dragon sessions, contribution tracking, cooldown/summon restrictions, custom settlement, offline reward queues, and common boss reward configuration.

Vanilla Wither and Ender Dragon death events, drops, and experience are no longer changed or reimplemented by HyunseoRPG. Gateway Boss and other genuinely custom event content remain independent.

## Retained authority

- The Upgrade Stone is a physical `ItemStack` crafted from one Diamond, four Redstone, and four Lapis Lazuli.
- The vanilla anvil is the only ordinary enhancement transaction and consumes exactly one Upgrade Stone plus the configured Minecraft XP-level cost.
- Native enchant registration and Minecraft enchanting, trading, loot, anvil, tooltip, and grindstone behavior remain authoritative.
- Farming and alchemy interfaces remain only where they are required by their genuinely custom gameplay. Quest access remains available through text commands rather than the removed hub.
- `PendingRewardService` remains an item-only, exactly-once delivery boundary for exploration and other physical custom rewards.

## Live verification checklist

1. Kill vanilla Wither and Ender Dragon and confirm unmodified vanilla drops and XP.
2. Confirm no custom boss bar/session, cooldown, participation gate, settlement message, or mailbox reward occurs.
3. Confirm `/rpg` displays direct command help and no legacy hub opens.
4. Verify farming and alchemy direct commands still open their required interfaces.
5. Craft an Upgrade Stone from vanilla resources.
6. Enhance once in a vanilla anvil and verify one stone and the configured XP-level cost are consumed.
7. Verify enchanting-table, librarian, loot-book, vanilla-anvil, and grindstone custom-enchant behavior.
8. Verify dedicated special weapons, Gateway Boss, and exploration exactly-once rewards.
9. Restart with old player data and confirm ignored legacy fields do not cause errors.
