package com.hyunseo.hyunseorpg.exploration.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Bukkit-only primitives. Custom RPG mobs and rewards intentionally remain external adapters. */
public final class BukkitExplorationPorts {
    private BukkitExplorationPorts() { }

    public static ExplorationPorts safeDefaults(JavaPlugin plugin) {
        ExplorationPorts.DisplayPort displays = new ExplorationPorts.DisplayPort() {
            @Override public UUID spawn(String kind, Location location, Map<String, Object> options) {
                return spawnDisplay(kind, location, options);
            }
            @Override public boolean move(UUID entityId, Location location) {
                return moveDisplay(entityId, location);
            }
            @Override public boolean remove(UUID entityId) {
                return removeEntity(entityId);
            }
        };
        return new ExplorationPorts(
                BukkitExplorationPorts::spawnVanillaOnly,
                displays,
                BukkitExplorationPorts::spawnInteraction,
                BukkitExplorationPorts::temporaryBlock,
                (player, location, options) -> player.teleport(location),
                ExplorationPorts.RewardPort.NOOP,
                ExplorationPorts.PuzzlePort.NOOP,
                ExplorationPorts.StructureEntityCleanupPort.NOOP);
    }

    /** Compose real Bukkit primitives with HyunseoRPG domain adapters. */
    public static ExplorationPorts compose(JavaPlugin plugin,
                                           ExplorationPorts.MobSpawnPort mobs,
                                           ExplorationPorts.RewardPort rewards,
                                           ExplorationPorts.StructureEntityCleanupPort cleanup) {
        ExplorationPorts primitives = safeDefaults(plugin);
        return new ExplorationPorts(
                mobs,
                primitives.displays(),
                primitives.interactions(),
                primitives.worldMutations(),
                primitives.teleports(),
                rewards,
                primitives.puzzles(),
                cleanup);
    }

    private static Collection<UUID> spawnVanillaOnly(String rawId, Location location, int count, Map<String, Object> options) {
        String id = rawId == null ? "" : rawId.trim().toLowerCase(Locale.ROOT);
        if (!id.startsWith("vanilla:") || location.getWorld() == null) return List.of();
        EntityType type;
        try { type = EntityType.valueOf(id.substring("vanilla:".length()).toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { return List.of(); }
        if (!type.isAlive()) return List.of();
        List<UUID> result = new ArrayList<>();
        for (int i = 0; i < Math.max(1, count); i++) {
            Entity entity = location.getWorld().spawnEntity(location, type);
            if (entity instanceof org.bukkit.entity.LivingEntity living) {
                if (options.containsKey("ai")) living.setAI(Boolean.parseBoolean(String.valueOf(options.get("ai"))));
                if (options.containsKey("invulnerable")) {
                    living.setInvulnerable(Boolean.parseBoolean(String.valueOf(options.get("invulnerable"))));
                }
            }
            result.add(entity.getUniqueId());
        }
        return List.copyOf(result);
    }

    private static UUID spawnDisplay(String kind, Location location, Map<String, Object> options) {
        if (location.getWorld() == null || !"block".equalsIgnoreCase(kind)) return null;
        Material material = Material.matchMaterial(String.valueOf(options.getOrDefault("material", "STONE")));
        if (material == null || !material.isBlock()) return null;
        BlockDisplay display = location.getWorld().spawn(location, BlockDisplay.class);
        display.setBlock(material.createBlockData());
        display.setGlowing(Boolean.parseBoolean(String.valueOf(options.getOrDefault("glowing", "false"))));
        return display.getUniqueId();
    }

    private static boolean moveDisplay(UUID entityId, Location location) {
        if (entityId == null || location == null || location.getWorld() == null) return false;
        Entity entity = Bukkit.getEntity(entityId);
        if (!(entity instanceof BlockDisplay display) || !entity.getWorld().equals(location.getWorld())) return false;
        return display.teleport(location);
    }

    private static boolean removeEntity(UUID entityId) {
        Entity entity = entityId == null ? null : Bukkit.getEntity(entityId);
        if (entity == null) return false;
        entity.remove();
        return true;
    }

    private static UUID spawnInteraction(String interactionId, Location location, Map<String, Object> options) {
        if (location.getWorld() == null) return null;
        Interaction interaction = location.getWorld().spawn(location, Interaction.class);
        interaction.setInteractionWidth(number(options.get("width"), 1.0F));
        interaction.setInteractionHeight(number(options.get("height"), 1.0F));
        interaction.setResponsive(Boolean.parseBoolean(String.valueOf(options.getOrDefault("responsive", "true"))));
        interaction.addScoreboardTag("hyunseorpg_exploration_interaction");
        interaction.addScoreboardTag("exploration_id_" + safeTag(interactionId));
        return interaction.getUniqueId();
    }

    private static Runnable temporaryBlock(Location location, Map<String, Object> options) {
        if (location.getWorld() == null) return () -> { };
        Material material = Material.matchMaterial(String.valueOf(options.getOrDefault("material", "BARRIER")));
        if (material == null || !material.isBlock()) return () -> { };
        BlockData before = location.getBlock().getBlockData().clone();
        location.getBlock().setType(material, false);
        UUID worldId = location.getWorld().getUID();
        int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
        return () -> {
            var world = Bukkit.getWorld(worldId);
            if (world != null) world.getBlockAt(x, y, z).setBlockData(before, false);
        };
    }

    private static float number(Object value, float fallback) {
        if (value instanceof Number number) return Math.max(0.01F, number.floatValue());
        try { return value == null ? fallback : Math.max(0.01F, Float.parseFloat(String.valueOf(value))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static String safeTag(String value) {
        String source = value == null ? "unknown" : value.toLowerCase(Locale.ROOT);
        return source.replaceAll("[^a-z0-9_\\-.]", "_");
    }
}
