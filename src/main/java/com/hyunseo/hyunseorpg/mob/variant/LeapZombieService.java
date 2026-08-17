package com.hyunseo.hyunseorpg.mob.variant;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LeapZombieService {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final Set<UUID> tracked = new HashSet<>();
    private final Set<UUID> pendingWarnings = new HashSet<>();
    private final Map<UUID, Long> nextAllowedAt = new HashMap<>();
    private final Map<UUID, Long> fallImmuneUntil = new HashMap<>();
    private final Map<UUID, LeapState> activeLeaps = new HashMap<>();
    private BukkitTask tickTask;

    public LeapZombieService(JavaPlugin plugin, ConfigService configService) {
        this.plugin = plugin;
        this.configService = configService;
    }

    public void start() {
        if (tickTask != null) {
            return;
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    public void register(LivingEntity entity) {
        if (entity != null && entity.getType() == org.bukkit.entity.EntityType.ZOMBIE) {
            tracked.add(entity.getUniqueId());
        }
    }

    public void unregister(Entity entity) {
        if (entity == null) return;
        UUID uuid = entity.getUniqueId();
        tracked.remove(uuid);
        pendingWarnings.remove(uuid);
        activeLeaps.remove(uuid);
        nextAllowedAt.remove(uuid);
        fallImmuneUntil.remove(uuid);
    }

    public long getCooldownRemainingMillis(UUID uuid) {
        return Math.max(0L, nextAllowedAt.getOrDefault(uuid, 0L) - System.currentTimeMillis());
    }

    public boolean isLeaping(UUID uuid) {
        return activeLeaps.containsKey(uuid);
    }

    public void handleDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        UUID uuid = event.getEntity().getUniqueId();
        if (fallImmuneUntil.getOrDefault(uuid, 0L) > System.currentTimeMillis()) {
            event.setCancelled(true);
        } else {
            fallImmuneUntil.remove(uuid);
        }
    }

    public void cancelAll() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        tracked.clear();
        pendingWarnings.clear();
        nextAllowedAt.clear();
        fallImmuneUntil.clear();
        activeLeaps.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (UUID uuid : Set.copyOf(tracked)) {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof Zombie zombie) || !entity.isValid() || entity.isDead()) {
                unregister(entity);
                continue;
            }

            LeapState active = activeLeaps.get(uuid);
            if (active != null) {
                if (now >= active.expiresAtMillis() || (zombie.isOnGround() && now > active.launchedAtMillis() + 250L)) {
                    land(zombie);
                    activeLeaps.remove(uuid);
                }
                continue;
            }

            if (pendingWarnings.contains(uuid) || !canTrigger(zombie, now)) {
                continue;
            }
            Player target = findTarget(zombie);
            if (target == null) {
                continue;
            }
            beginWarning(zombie, target.getLocation());
        }
    }

    private boolean canTrigger(Zombie zombie, long now) {
        if (!configService.getMobsBoolean("zombie-variants.variants.leap.enabled", true)
                || !zombie.isOnGround()
                || zombie.isInWater()
                || zombie.isInLava()
                || zombie.isClimbing()
                || zombie.getVehicle() != null
                || !hasHeadRoom(zombie.getLocation())
                || now < nextAllowedAt.getOrDefault(zombie.getUniqueId(), 0L)) {
            return false;
        }
        return true;
    }

    private Player findTarget(Zombie zombie) {
        double minRange = Math.max(0.0D, configService.getMobsDouble(
                "zombie-variants.variants.leap.trigger.minimum-range", 3.5D));
        double maxRange = Math.max(minRange, configService.getMobsDouble(
                "zombie-variants.variants.leap.trigger.maximum-range", 8.0D));
        double minSquared = minRange * minRange;
        double maxSquared = maxRange * maxRange;

        if (zombie.getTarget() instanceof Player target && target.isOnline()
                && target.getWorld().equals(zombie.getWorld())) {
            double distanceSquared = target.getLocation().distanceSquared(zombie.getLocation());
            if (distanceSquared >= minSquared && distanceSquared <= maxSquared) {
                return target;
            }
        }

        return zombie.getWorld().getNearbyEntities(zombie.getLocation(), maxRange, maxRange, maxRange).stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .filter(Player::isOnline)
                .filter(player -> {
                    double distanceSquared = player.getLocation().distanceSquared(zombie.getLocation());
                    return distanceSquared >= minSquared && distanceSquared <= maxSquared;
                })
                .min((left, right) -> Double.compare(
                        left.getLocation().distanceSquared(zombie.getLocation()),
                        right.getLocation().distanceSquared(zombie.getLocation())))
                .orElse(null);
    }

    private void beginWarning(Zombie zombie, Location target) {
        UUID uuid = zombie.getUniqueId();
        pendingWarnings.add(uuid);
        int warningTicks = Math.max(0, configService.getMobsInt(
                "zombie-variants.variants.leap.trigger.warning-ticks", 10));
        World world = zombie.getWorld();
        world.spawnParticle(Particle.CLOUD, zombie.getLocation().add(0.0D, 0.1D, 0.0D), 6, 0.2D, 0.05D, 0.2D, 0.02D);
        world.playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 0.7F, 1.4F);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                if (!zombie.isValid() || zombie.isDead() || !zombie.isOnGround()) {
                    return;
                }
                launch(zombie, target);
            } finally {
                pendingWarnings.remove(uuid);
            }
        }, warningTicks);
    }

    private void launch(Zombie zombie, Location target) {
        Location source = zombie.getLocation();
        Vector horizontal = target.toVector().subtract(source.toVector());
        horizontal.setY(0.0D);
        if (horizontal.lengthSquared() <= 0.0001D) {
            horizontal = source.getDirection().setY(0.0D);
        }
        if (horizontal.lengthSquared() <= 0.0001D) {
            horizontal = new Vector(1.0D, 0.0D, 0.0D);
        }
        double horizontalStrength = Math.max(0.0D, configService.getMobsDouble(
                "zombie-variants.variants.leap.movement.horizontal-strength", 1.1D));
        double verticalStrength = Math.max(0.0D, configService.getMobsDouble(
                "zombie-variants.variants.leap.movement.vertical-strength", 0.65D));
        Vector velocity = horizontal.normalize().multiply(horizontalStrength);
        velocity.setY(verticalStrength);
        zombie.setVelocity(velocity);

        long now = System.currentTimeMillis();
        int cooldownTicks = Math.max(0, configService.getMobsInt(
                "zombie-variants.variants.leap.trigger.cooldown-ticks", 120));
        int immunityTicks = Math.max(0, configService.getMobsInt(
                "zombie-variants.variants.leap.movement.fall-damage-immunity-ticks", 40));
        nextAllowedAt.put(zombie.getUniqueId(), now + cooldownTicks * 50L);
        fallImmuneUntil.put(zombie.getUniqueId(), now + immunityTicks * 50L);
        activeLeaps.put(zombie.getUniqueId(), new LeapState(now, now + 5000L));
        zombie.getWorld().playSound(source, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8F, 1.1F);
    }

    private void land(Zombie zombie) {
        Location location = zombie.getLocation();
        World world = location.getWorld();
        if (world == null) return;
        double radius = Math.max(0.0D, configService.getMobsDouble(
                "zombie-variants.variants.leap.landing.radius", 1.8D));
        double damage = Math.max(0.0D, configService.getMobsDouble(
                "zombie-variants.variants.leap.landing.base-damage", 2.0D)
                + zombie.getPersistentDataContainer().getOrDefault(
                new org.bukkit.NamespacedKey(plugin, "mob_level"),
                org.bukkit.persistence.PersistentDataType.INTEGER, 1)
                * Math.max(0.0D, configService.getMobsDouble(
                "zombie-variants.variants.leap.landing.damage-per-level", 0.08D)));
        double knockback = Math.max(0.0D, configService.getMobsDouble(
                "zombie-variants.variants.leap.landing.knockback", 0.4D));
        boolean damagePlayers = configService.getMobsBoolean(
                "zombie-variants.variants.leap.landing.damage-players", true);
        boolean damageMobs = configService.getMobsBoolean(
                "zombie-variants.variants.leap.landing.damage-mobs", false);

        world.spawnParticle(Particle.CLOUD, location, 10, 0.3D, 0.05D, 0.3D, 0.04D);
        world.playSound(location, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.8F, 0.8F);
        for (Entity nearby : world.getNearbyEntities(location, radius, radius, radius)) {
            if (!(nearby instanceof LivingEntity living) || living.equals(zombie)) continue;
            if (living instanceof Player && !damagePlayers) continue;
            if (!(living instanceof Player) && !damageMobs) continue;
            living.damage(damage);
            if (knockback > 0.0D) {
                Vector direction = living.getLocation().toVector().subtract(location.toVector());
                direction.setY(0.25D);
                if (direction.lengthSquared() > 0.0D) {
                    living.setVelocity(direction.normalize().multiply(knockback));
                }
            }
        }
        int recoveryTicks = Math.max(0, configService.getMobsInt(
                "zombie-variants.variants.leap.landing.recovery-ticks", 10));
        nextAllowedAt.put(zombie.getUniqueId(), Math.max(
                nextAllowedAt.getOrDefault(zombie.getUniqueId(), 0L),
                System.currentTimeMillis() + recoveryTicks * 50L));
    }

    private boolean hasHeadRoom(Location location) {
        return location.getBlock().isPassable()
                && location.clone().add(0.0D, 1.0D, 0.0D).getBlock().isPassable()
                && location.clone().add(0.0D, 2.0D, 0.0D).getBlock().isPassable();
    }

    private record LeapState(long launchedAtMillis, long expiresAtMillis) {
    }
}
