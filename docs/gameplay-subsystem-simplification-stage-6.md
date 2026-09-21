# Stage 6: gameplay subsystem simplification

The Quest gameplay runtime, commands, listeners, managed configuration, and availability generators are retired. Legacy quest-shaped player fields are preserved only as opaque compatibility data so old saves can round-trip without interpreting, consuming, or advancing them as active Quest state.

The legacy `ExplorationModule` and its generic structure framework are also retired. Desert Pyramid, Outpost, Shipwreck, raid runtime, detection, persistence, listener, integration, command, lifecycle, and migration ownership are removed. The only preserved Exploration code is the runtime-independent Ocean Monument logical state model, retained as dormant design code for possible future reconstruction. It is not bootstrapped, registered, reloaded, started, stopped, exposed through commands, persisted by the retired framework, or active in gameplay.

Gateway Boss remains independent and active. The custom Farming and Cooking game—including crops, progression, promotion, delivery, quality, resources, item definitions, persistence, commands, and lifecycle ownership—is retired. Old save-file keys are ignored rather than destructively migrated.

Alchemy retains its effect, potion, catalyst, recipe, and potion-use backend. Its only creation frontend is the vanilla Brewing Stand: each registry recipe describes one visible transition from a base stack in a bottle slot plus the stand's ingredient stack to a canonical `PotionFactory` result. An optional catalyst is represented as a later transition using that same ingredient slot; no hidden slot or custom inventory exists. A complete result list is planned before the event is changed. A matched transition whose output cannot be created is cancelled without consuming anything, while an unmatched state is not cancelled or mutated and therefore remains entirely under vanilla brewing behavior. Bukkit performs the successful stand's single ingredient consumption, including hopper-driven operation.

No replacement fishing system is introduced and vanilla fishing remains unmodified.

Stage 1–5 boundaries remain authoritative and unchanged: native Paper/Minecraft enchantments, vanilla anvil enhancement, removal of ordinary equipment Promotion, removal of the legacy Coin/Shop economy and custom GUIs, independent RPG Level data, Minecraft-owned XP, item-owned abilities, special weapons, custom mobs, cooldowns, effects, and VFX remain as previously established.
