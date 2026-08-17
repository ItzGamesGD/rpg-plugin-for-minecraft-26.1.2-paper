# HyunseoRPG v2 Roadmap

## Product rule

Minecraft activities are content. HyunseoRPG improves and rewards them; it
must not prevent a player from mining, building, farming, exploring, or using
another weapon. A profession rewards a preferred activity, not a restriction.

## Project 1 - v2 core migration (implemented)

- Keep legacy class data for compatibility, but remove it from skill access and
  base mana calculation.
- Add lifestyle profession selection and profession-only rare material drops.
- Add weapon types and persistent weapon proficiency.
- Make skill unlocks use weapon proficiency plus skill points.
- Keep previously unlocked skills usable after migration.
- Move skill definitions to skills.yml and add weapon/profession/option config
  files.
- Add PDC storage for future equipment option attachments.

## Project 2 - shared item and equipment foundation

- Consolidate profession materials, upgrade stones, tickets, and future drops
  into one PDC item registry.
- Replace legacy class weapon issuance with generic weapon templates.
- Add equipment option slots, option materials, an application GUI, and item
  inspection.
- Connect direct stat options to StatService and combat options to CombatService.

## Project 3 - open-world activity economy

- Add real repairing, building, gathering, and fishing reward hooks.
- Add activity-specific rarity, drop tables, recipes, and player trade support.
- Keep normal worlds open; hunting grounds are optional accelerated content.
- Add elite natural spawns and data-driven material drops.

## Project 4 - weapon build expansion

- Rename legacy class specialization data to weapon specialization data.
- Add proficiency GUI, skill filtering by weapon, and skill loadout UX.
- Balance sword, bow, and spear around independently leveled weapons.
- Add axe and magic combat modules, then future weapon categories as data.

## Project 5 - dungeon and NPC content framework

- Add configured entry, floor, boss-room, reward, and return coordinates.
- Use Multiverse worlds for dungeon maps and boss-room transitions.
- Add quest, shop, dungeon-entry, and promotion NPCs after their services are stable.

## Project 6 - promotion, enhancement, and market systems

- Add optional advancement paths without disabling base activities.
- Implement enhancement materials, option rerolls, crafting, and trade sinks.
- Make elite and boss drops feed crafting and long-term equipment builds.

## Project 7 - resource pack and live content

- Assign CustomModelData ranges for weapons, materials, mobs, and visual skills.
- Add resource-pack models without changing PDC gameplay checks.
- Build actual regions, quests, mobs, dungeon maps, bosses, and recipes.
