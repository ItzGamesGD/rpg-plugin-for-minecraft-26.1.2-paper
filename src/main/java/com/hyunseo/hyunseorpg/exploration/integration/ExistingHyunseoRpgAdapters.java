package com.hyunseo.hyunseorpg.exploration.integration;

import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.mob.MobService;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adapters verified against the 2026-08-04 accessible source snapshot only.
 * Farming/alchemy are intentionally absent. Re-check signatures against the latest desktop source.
 */
public final class ExistingHyunseoRpgAdapters {
    private ExistingHyunseoRpgAdapters() { }

    public static ExplorationPorts.MobSpawnPort mobPort(MobService mobService) {
        return (mobId, location, count, options) -> {
            List<UUID> spawned = new ArrayList<>();
            Integer level = options.get("level") instanceof Number number ? Math.max(1, number.intValue()) : null;
            String normalized = mobId == null ? "" : mobId.trim().toLowerCase(java.util.Locale.ROOT);
            if (normalized.startsWith("vanilla:")) {
                String typeName = normalized.substring("vanilla:".length()).toUpperCase(java.util.Locale.ROOT);
                org.bukkit.entity.EntityType type;
                try {
                    type = org.bukkit.entity.EntityType.valueOf(typeName);
                } catch (IllegalArgumentException ignored) {
                    return List.of();
                }
                if (!type.isAlive() || location.getWorld() == null) return List.of();
                for (int i = 0; i < Math.max(1, count); i++) {
                    Entity entity = location.getWorld().spawnEntity(location, type);
                    if (entity != null) spawned.add(entity.getUniqueId());
                }
                return List.copyOf(spawned);
            }
            String customMobId = normalized.startsWith("custom:")
                    ? normalized.substring("custom:".length()) : normalized;
            if (customMobId.isBlank()) return List.of();
            for (int i = 0; i < Math.max(1, count); i++) {
                mobService.spawnCustomMob(location, customMobId, level, "EXPLORATION")
                        .ifPresent(entity -> spawned.add(entity.getUniqueId()));
            }
            return List.copyOf(spawned);
        };
    }

    public static ExplorationPorts.RewardPort itemRewardPort(RPGItemService itemService,
                                                              InventoryDeliveryService deliveryService) {
        return (player, rewardId, amount, fallback, options) -> itemService.create(rewardId, Math.max(1, amount))
                .map(item -> {
                    deliveryService.giveOrDrop(player, fallback, item);
                    return true;
                }).orElse(false);
    }
}
