package com.hyunseo.hyunseorpg.exploration.integration;

import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.mob.MobService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
    private static final ExplorationEntityCleanupPolicy CLEANUP_POLICY = new ExplorationEntityCleanupPolicy();

    private ExistingHyunseoRpgAdapters() { }

    public static ExplorationPorts.MobSpawnPort mobPort(MobService mobService) {
        return new ExplorationPorts.MobSpawnPort() {
            @Override
            public Collection<UUID> spawn(String mobId, Location location, int count, Map<String, Object> options) {
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
                    if (entity instanceof LivingEntity living) {
                        prepareExplorationMob(living, options);
                        spawned.add(entity.getUniqueId());
                    }
                }
                    return List.copyOf(spawned);
            }
            String customMobId = normalized.startsWith("custom:")
                    ? normalized.substring("custom:".length()) : normalized;
            if (customMobId.isBlank()) return List.of();
            for (int i = 0; i < Math.max(1, count); i++) {
                if (customMobId.equals("ravager_rider")) {
                    mobService.spawnOutpostRavagerRider(location, level, "EXPLORATION")
                            .forEach(entity -> {
                                prepareExplorationMob(entity, options);
                                spawned.add(entity.getUniqueId());
                            });
                    continue;
                }
                mobService.spawnCustomMob(location, customMobId, level, "EXPLORATION")
                        .ifPresent(entity -> {
                            prepareExplorationMob(entity, options);
                            spawned.add(entity.getUniqueId());
                        });
            }
                return List.copyOf(spawned);
            }

            private void prepareExplorationMob(LivingEntity entity, Map<String, Object> options) {
                if (entity == null) return;
                entity.setGlowing(Boolean.parseBoolean(String.valueOf(
                        options.getOrDefault("glowing", "true"))));
                if (options.containsKey("ai")) entity.setAI(Boolean.parseBoolean(String.valueOf(options.get("ai"))));
                if (options.containsKey("invulnerable")) {
                    entity.setInvulnerable(Boolean.parseBoolean(String.valueOf(options.get("invulnerable"))));
                }
                Object rawTarget = options.get("target-player-uuid");
                if (rawTarget == null) return;
                try {
                    Player target = Bukkit.getPlayer(UUID.fromString(String.valueOf(rawTarget)));
                    UUID targetId = UUID.fromString(String.valueOf(rawTarget));
                    long chaseAfter = options.get("raid-chase-after-ticks") instanceof Number number
                            ? Math.max(0L, number.longValue()) : 0L;
                    mobService.getMobTagService().markExplorationRaidTarget(entity, targetId,
                            Bukkit.getCurrentTick() + chaseAfter);
                    if (entity instanceof Mob mob && target != null && target.isOnline() && !target.isDead()
                            && target.getWorld().equals(entity.getWorld())) {
                        mob.setTarget(target);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Invalid debug/runtime target IDs fail closed.
                }
            }

            @Override
            public SpawnDetails describe(UUID entityId) {
                Entity entity = Bukkit.getEntity(entityId);
                if (!(entity instanceof LivingEntity living)) {
                    return new SpawnDetails(entityId, entity == null ? "UNKNOWN" : entity.getType().name(), "", "", "");
                }
                return new SpawnDetails(entityId, living.getType().name(), mobService.getMobId(living),
                        mobService.getMobTagService().getCustomMobId(living),
                        mobService.getMobTagService().getSpawnSource(living));
            }
        };
    }

    public static ExplorationPorts.StructureEntityCleanupPort entityCleanupPort(MobService mobService) {
        return (world, bounds, entityType) -> {
            if (world == null || bounds == null || entityType == null) return 0;
            Location center = new Location(world, bounds.centerX(), bounds.centerY(), bounds.centerZ());
            double x = Math.max(1.0D, (bounds.maxX() - bounds.minX()) / 2.0D + 1.0D);
            double y = Math.max(1.0D, (bounds.maxY() - bounds.minY()) / 2.0D + 1.0D);
            double z = Math.max(1.0D, (bounds.maxZ() - bounds.minZ()) / 2.0D + 1.0D);
            int removed = 0;
            for (Entity entity : world.getNearbyEntities(center, x, y, z)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (!CLEANUP_POLICY.shouldRemove(entity.getType(), entityType,
                        mobService.isRpgMob(living), mobService.getMobTagService().isCustomMob(living),
                        bounds, entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ())) continue;
                entity.remove();
                removed++;
            }
            return removed;
        };
    }

    public static ExplorationPorts.RewardPort itemRewardPort(RPGItemService itemService,
                                                              InventoryDeliveryService deliveryService) {
        return new ExplorationPorts.RewardPort() {
            @Override
            public boolean grant(Player player, String rewardId, int amount, Location fallback,
                                 Map<String, Object> options) {
                ItemStack item = createItem(itemService, rewardId, amount);
                if (item == null) return false;
                deliveryService.giveOrDrop(player, fallback, item);
                return true;
            }

            @Override
            public boolean enqueueDurable(Player player, String rewardId, int amount, Location fallback,
                                          Map<String, Object> options, String idempotencyToken) {
                ItemStack item = createItem(itemService, rewardId, amount);
                if (item == null || player == null) return false;
                UUID token = UUID.nameUUIDFromBytes((idempotencyToken == null ? "" : idempotencyToken)
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return deliveryService.queueItemOnce(player, token, item, "exploration-pyramid:" + idempotencyToken);
            }

            private ItemStack createItem(RPGItemService service, String rewardId, int amount) {
                int safeAmount = Math.max(1, amount);
                String normalized = rewardId == null ? "" : rewardId.trim();
                if (normalized.regionMatches(true, 0, "vanilla:", 0, "vanilla:".length())) {
                    try {
                        Material material = Material.valueOf(normalized.substring("vanilla:".length())
                                .trim().toUpperCase(java.util.Locale.ROOT));
                        return material.isItem() ? new ItemStack(material, safeAmount) : null;
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                }
                return service.create(normalized, safeAmount).orElse(null);
            }
        };
    }}
