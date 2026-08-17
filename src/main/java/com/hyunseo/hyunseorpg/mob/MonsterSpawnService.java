package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.mythic.MythicCustomMobService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Data-driven natural replacement and direct-spawn coordinator. */
public final class MonsterSpawnService implements Listener {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final MonsterSpawnRegistry registry;
    private final MobService mobService;
    private final MobLevelScalingService levelScalingService;
    private org.bukkit.scheduler.BukkitTask directSpawnTask;

    public MonsterSpawnService(JavaPlugin plugin, ConfigService configService,
                               MonsterSpawnRegistry registry, MobService mobService,
                               MobLevelScalingService levelScalingService) {
        this.plugin = plugin;
        this.configService = configService;
        this.registry = registry;
        this.mobService = mobService;
        this.levelScalingService = levelScalingService;
    }

    public void start() {
        if (directSpawnTask != null) return;
        long interval = Math.max(20L, configService.getMonsterSpawnsLong("settings.direct-spawn-interval-ticks", 40L));
        directSpawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tryDirectSpawns, interval, interval);
    }

    public void stop() {
        if (directSpawnTask != null) directSpawnTask.cancel();
        directSpawnTask = null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onNaturalSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (!configService.getMonsterSpawnsBoolean("settings.enabled", true)) return;
        double radius = Math.max(1.0D, configService.getMonsterSpawnsDouble("settings.nearby-player-radius", 96.0D));
        int level = levelScalingService.calculateNearbyAverageLevel(event.getLocation(), radius);
        List<MonsterSpawnData> candidates = registry.findForNatural(event.getLocation(), event.getEntityType(), level);
        MonsterSpawnData selected = choose(candidates);
        if (selected == null || ThreadLocalRandom.current().nextDouble() > selected.spawnChance()) return;
        event.setCancelled(true);
        spawn(event.getLocation(), selected, level, "REPLACEMENT");
    }

    private void tryDirectSpawns() {
        if (!configService.getMonsterSpawnsBoolean("settings.enabled", true)) return;
        int maxNearby = Math.max(1, configService.getMonsterSpawnsInt("settings.direct-spawn-max-nearby", 8));
        double chance = Math.max(0.0D, Math.min(1.0D,
                configService.getMonsterSpawnsDouble("settings.direct-spawn-chance", 0.02D)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (ThreadLocalRandom.current().nextDouble() > chance) continue;
            int level = levelScalingService.calculatePlayerAverageLevel(player);
            Location location = randomSpawnLocation(player, null);
            if (location == null) continue;
            List<MonsterSpawnData> candidates = registry.findForDirect(location, level);
            MonsterSpawnData selected = choose(candidates);
            if (selected == null || ThreadLocalRandom.current().nextDouble() > selected.spawnChance()) continue;
            if (!validLocation(location, selected, player) || countNearby(location, selected) >= Math.min(maxNearby, selected.maxNearby())) {
                continue;
            }
            if (selected.maxPerChunk() > 0 && countInChunk(location, selected.monsterId()) >= selected.maxPerChunk()) continue;
            if (selected.globalCap() >= 0 && countGlobal(selected.monsterId()) >= selected.globalCap()) continue;
            spawn(location, selected, level, "ADDITIVE");
        }
    }

    private void spawn(Location location, MonsterSpawnData data, int level, String source) {
        mobService.spawnCustomMob(location, data.monsterId(), level, source).ifPresent(entity -> {
            if (entity instanceof Mob mob) {
                mob.setRemoveWhenFarAway(data.despawnWithDistance());
            }
        });
    }

    private MonsterSpawnData choose(List<MonsterSpawnData> candidates) {
        if (candidates.isEmpty()) return null;
        double total = candidates.stream().mapToDouble(MonsterSpawnData::spawnWeight).sum();
        if (total <= 0.0D) return null;
        double value = ThreadLocalRandom.current().nextDouble(total);
        for (MonsterSpawnData candidate : candidates) {
            value -= candidate.spawnWeight();
            if (value <= 0.0D) return candidate;
        }
        return candidates.get(candidates.size() - 1);
    }

    private Location randomSpawnLocation(Player player, MonsterSpawnData data) {
        World world = player.getWorld();
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble(0.0D, Math.PI * 2.0D);
            double distance = ThreadLocalRandom.current().nextDouble(12.0D, 28.0D);
            int x = player.getLocation().getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.getLocation().getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = world.getHighestBlockYAt(x, z) + 1;
            if (data != null) {
                y = Math.max(data.minY(), Math.min(data.maxY(), y));
            }
            Location location = new Location(world, x + 0.5D, y, z + 0.5D);
            if (data != null && !validLocation(location, data, player)) continue;
            Block block = location.getBlock();
            if (!block.isEmpty() || !block.getRelative(0, 1, 0).isEmpty()) continue;
            return location;
        }
        return null;
    }

    private int countNearby(Location location, MonsterSpawnData data) {
        return (int) location.getWorld().getNearbyEntities(location, data.nearbyRadius(), data.nearbyRadius(), data.nearbyRadius()).stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(entity -> data.monsterId().equals(mobService.getMobId(entity)))
                .count();
    }

    private boolean validLocation(Location location, MonsterSpawnData data, Player player) {
        if (location.getWorld() == null) return false;
        if (!data.acceptsWorld(location.getWorld().getName())
                || !data.acceptsBiome(location.getBlock().getBiome().name())) return false;
        int light = location.getBlock().getLightLevel();
        if (light < data.minLight() || light > data.maxLight()) return false;
        if (location.getY() < data.minY() || location.getY() > data.maxY()) return false;
        double distance = location.distance(player.getLocation());
        if (distance < data.minDistance() || (data.maxDistance() >= 0.0D && distance > data.maxDistance())) return false;
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);
        return (!data.requireSolidGround() || ground.getType().isSolid())
                && (!data.requireOpenSpace() || (feet.isEmpty() && head.isEmpty()));
    }

    private int countInChunk(Location location, String mobId) {
        if (location.getWorld() == null) return 0;
        return (int) java.util.Arrays.stream(location.getWorld().getChunkAt(location).getEntities())
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(entity -> mobId.equals(mobService.getMobId(entity)))
                .count();
    }

    private int countGlobal(String mobId) {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            count += (int) world.getLivingEntities().stream()
                    .filter(entity -> mobId.equals(mobService.getMobId(entity)))
                    .count();
        }
        return count;
    }
}
