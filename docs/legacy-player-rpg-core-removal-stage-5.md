# Stage 5: Legacy player RPG core removal

Stage 5 removes the player-owned character sheet while retaining RPG Level and item/content runtime.

## Dependency disposition

- **Deleted:** class selection/restrictions, class stats, allocated player stats, mana and HUD/regeneration, weapon proficiency, player skill registry/tree/levels, and their commands.
- **Moved to active runtime:** `blade_chain`, `light_greatsword`, `laser_arrow`, and `fire_arrow_rain` execute as native-enchantment content handlers. Their level comes from the actual ItemStack enchantment and no player class, mana, proficiency, or skill-tree record is consulted.
- **Retained generic runtime:** equipment metadata/attributes, `CombatService`, `CooldownService`, item input routing, temporary alchemy effects, elemental equipment, and dedicated special-equipment state.
- **Read-only compatibility:** old YAML character-sheet keys are ignored by loading and excluded from new saves. The old item `weapon_class` PDC may still be read only to infer an item category; it never restricts a player.

RPG Level remains, but level-ups no longer award stat or skill points. Mob scaling was intentionally not redesigned. Farming, delivery, quests, alchemy, and exploration remain deferred to later phases except where a removed legacy reward/requirement type had to be disconnected.

## Live-server verification

Verify an existing player save can join without a class, stat allocation, mana bar, or proficiency requirement. Exercise ordinary combat/armor/enchanting/anvil repair and rename; vanilla and elemental enhancement caps; rejection of special-weapon enhancement; native-enchantment triggers; Poseidon, Flame Axe, Thanatos, Thunder God's Axe, Solaris, Moonlit Afterglow, Thousand Eyes, and Gateway Boss. Restart and confirm old saves load without legacy-service errors. Confirm `/rpg` does not open a hub and no class/stat/special-equipment GUI or `/specialequipment craft`/menu escape hatch exists.
