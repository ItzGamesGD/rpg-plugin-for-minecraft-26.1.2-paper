package com.hyunseo.hyunseorpg.skill.lancer;

import com.hyunseo.hyunseorpg.classsystem.ClassStatService;
import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class LancerSkillService {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final CombatService combatService;
    private final ClassStatService classStatService;
    private final Set<BukkitTask> tasks = ConcurrentHashMap.newKeySet();

    public LancerSkillService(JavaPlugin plugin, ConfigService configService, CombatService combatService, ClassStatService classStatService) {
        this.plugin = plugin;
        this.configService = configService;
        this.combatService = combatService;
        this.classStatService = classStatService;
    }

    public void castSpearThrow(Player player, int skillLevel) {
        if (!configService.getBoolean("lancer.spear-throw.enabled", true)) {
            return;
        }

        double speed = configService.getDouble("lancer.spear-throw.speed", 2.5D);
        double damage = configService.getDouble("lancer.spear-throw.damage", 12.5D)
                + classStatService.getClassStatBonus(player, "spear_throw", "damage");
        double hitboxRadius = configService.getDouble("lancer.spear-throw.hitbox-radius", 1.0D);
        long lifetimeTicks = Math.max(1L, configService.getLong("lancer.spear-throw.lifetime-ticks", 60L));
        spawnPluginTrident(player, player.getEyeLocation(), player.getEyeLocation().getDirection().normalize().multiply(speed), damage, hitboxRadius, lifetimeTicks);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0F, 1.2F);
    }

    public void castCharge(Player player, double overrideDamage) {
        castCharge(player, overrideDamage, 0.0D);
    }

    private void castCharge(Player player, double overrideDamage, double extraSpeed) {
        if (!configService.getBoolean("lancer.charge.enabled", true)) {
            return;
        }

        double distance = configService.getDouble("lancer.charge.distance", 10.0D);
        long durationTicks = Math.max(1L, configService.getLong("lancer.charge.duration-ticks", 8L));
        double speedBonus = extraSpeed + classStatService.getClassStatBonus(player, "charge", "speed");
        double damage = overrideDamage > 0.0D ? overrideDamage : configService.getDouble("lancer.charge.damage", 10.0D);
        double hitboxRadius = configService.getDouble("lancer.charge.hitbox-radius", 1.4D);
        Vector direction = player.getEyeLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() <= 0.001D) {
            direction = player.getLocation().getDirection().setY(0.0D);
        }
        direction.normalize();
        Vector velocity = direction.clone().multiply((distance / durationTicks) + speedBonus);
        Set<UUID> hitTargets = new HashSet<>();

        final long[] elapsedTicks = {0L};
        AtomicReference<BukkitTask> taskReference = new AtomicReference<>();
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            elapsedTicks[0]++;
            if (!player.isOnline() || player.isDead() || elapsedTicks[0] > durationTicks) {
                slowHorizontalVelocity(player);
                cancelTask(taskReference.get());
                return;
            }

            Location next = player.getLocation().add(velocity);
            if (next.getBlock().getType().isSolid()) {
                slowHorizontalVelocity(player);
                cancelTask(taskReference.get());
                return;
            }
            player.setVelocity(velocity);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0.0D, 1.0D, 0.0D), 1);
            damageNearby(player, player.getLocation(), hitboxRadius, damage, hitTargets);
        }, 0L, 1L);
        taskReference.set(task);
        tasks.add(task);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0F, 1.2F);
    }

    public void castSpearBreakthrough(Player player, int skillLevel) {
        if (!configService.getBoolean("lancer.spear-breakthrough.enabled", true)) {
            return;
        }

        int spearCount = (int) Math.max(1L, configService.getLong("lancer.spear-breakthrough.spear-count", 10L)
                + Math.round(classStatService.getClassStatBonus(player, "spear_breakthrough", "spear_count")));
        double spearSpeed = configService.getDouble("lancer.spear-breakthrough.spear-speed", 2.0D);
        double spearDamage = configService.getDouble("lancer.spear-breakthrough.spear-damage", 5.0D);
        double chargeDamage = configService.getDouble("lancer.spear-breakthrough.charge-damage", 10.0D);
        double breakthroughChargeSpeed = classStatService.getClassStatBonus(player, "spear_breakthrough", "charge_speed");
        long preDelayTicks = Math.max(1L, configService.getLong("lancer.spear-breakthrough.pre-delay-ticks", 40L));
        Vector baseDirection = player.getEyeLocation().getDirection().normalize();

        for (int index = 0; index < spearCount; index++) {
            double offset = spearCount == 1 ? 0.0D : (index / (double) (spearCount - 1) - 0.5D) * 22.0D;
            Vector direction = rotateYaw(baseDirection, offset).normalize();
            Location spawnLocation = player.getEyeLocation().add(direction.clone().multiply(1.2D));
            spawnPluginTrident(player, spawnLocation, direction.multiply(spearSpeed), spearDamage, 1.0D, preDelayTicks);
        }

        BukkitTask chargeTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tasks.removeIf(BukkitTask::isCancelled);
            castCharge(player, chargeDamage, breakthroughChargeSpeed);
        }, preDelayTicks);
        tasks.add(chargeTask);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0F, 1.4F);
    }

    public void clearAll() {
        for (BukkitTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
    }

    private void spawnPluginTrident(Player player, Location location, Vector velocity, double damage, double hitboxRadius, long lifetimeTicks) {
        Trident trident = player.getWorld().spawn(location, Trident.class, entity -> {
            entity.setShooter(player);
            entity.setVelocity(velocity);
            entity.setDamage(0.0D);
            entity.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
            entity.setPierceLevel(10);
        });

        Set<UUID> hitTargets = new HashSet<>();
        final long[] elapsedTicks = {0L};
        AtomicReference<BukkitTask> taskReference = new AtomicReference<>();
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            elapsedTicks[0]++;
            if (trident.isDead() || elapsedTicks[0] > lifetimeTicks) {
                trident.remove();
                cancelTask(taskReference.get());
                return;
            }
            trident.getWorld().spawnParticle(Particle.CRIT, trident.getLocation(), 2, 0.04D, 0.04D, 0.04D, 0.01D);
            damageNearby(player, trident.getLocation(), hitboxRadius, damage, hitTargets);
        }, 1L, 1L);
        taskReference.set(task);
        tasks.add(task);
    }

    private void damageNearby(Player player, Location center, double radius, double damage, Set<UUID> hitTargets) {
        World world = center.getWorld();
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || target instanceof Player || target.isDead() || target.equals(player)) {
                continue;
            }
            if (hitTargets.add(target.getUniqueId())) {
                combatService.applyUltimateDamage(player, target, damage);
            }
        }
    }

    private Vector rotateYaw(Vector vector, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z);
    }

    private void slowHorizontalVelocity(Player player) {
        Vector current = player.getVelocity();
        player.setVelocity(new Vector(current.getX() * 0.25D, current.getY(), current.getZ() * 0.25D));
    }

    private void cancelTask(BukkitTask task) {
        if (task == null) {
            return;
        }
        task.cancel();
        tasks.remove(task);
    }
}
