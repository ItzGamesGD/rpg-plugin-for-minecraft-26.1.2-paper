# Stage 6: gameplay subsystem simplification

The quest runtime, command, listener, managed configuration, and availability generators are removed. Legacy quest-shaped player fields remain read-only compatibility data so old saves do not fail to load; no runtime consumes or advances them.

The large exploration event module is no longer bootstrapped or reloaded. Its reusable world/event implementation is retained as dormant code pending a future datapack-native decision; Gateway Boss remains independent and active.

Farming keeps vanilla block interaction plus custom crop, food, material, crafting, and brewing inputs. Alchemy keeps its effect, potion, catalyst, recipe, and vanilla-use runtime without restoring an inventory GUI or blocking vanilla brewing. No replacement fishing system is introduced and vanilla fishing remains unmodified.

Stage 1–5 boundaries remain authoritative: native enchantments, vanilla anvil enhancement, independent RPG Level data, Minecraft-owned XP, item-owned abilities, special weapons, custom mobs, cooldowns, effects, and VFX remain active.
