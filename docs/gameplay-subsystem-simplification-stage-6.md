# Stage 6: gameplay subsystem simplification

The Quest gameplay runtime, commands, listeners, managed configuration, and availability generators are retired. Legacy quest-shaped player fields are preserved only as opaque compatibility data so old saves can round-trip without interpreting, consuming, or advancing them as active Quest state.

The legacy `ExplorationModule` and its generic structure framework are also retired. Desert Pyramid, Outpost, Shipwreck, raid runtime, detection, persistence, listener, integration, command, lifecycle, and migration ownership are removed. The only preserved Exploration code is the runtime-independent Ocean Monument logical state model, retained as dormant design code for possible future reconstruction. It is not bootstrapped, registered, reloaded, started, stopped, exposed through commands, persisted by the retired framework, or active in gameplay.

Gateway Boss remains independent and active. Farming keeps vanilla block interaction plus its custom crop, food, material, crafting, and brewing inputs, and Farming Promotion remains a separate subsystem. Alchemy keeps its effect, potion, catalyst, recipe, and vanilla-use runtime without restoring an inventory GUI or blocking vanilla brewing. No replacement fishing system is introduced and vanilla fishing remains unmodified.

Stage 1–5 boundaries remain authoritative and unchanged: native Paper/Minecraft enchantments, vanilla anvil enhancement, removal of ordinary equipment Promotion, removal of the legacy Coin/Shop economy and custom GUIs, independent RPG Level data, Minecraft-owned XP, item-owned abilities, special weapons, custom mobs, cooldowns, effects, and VFX remain as previously established.
