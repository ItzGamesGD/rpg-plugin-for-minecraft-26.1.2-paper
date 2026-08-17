package com.hyunseo.hyunseorpg.mob.variant;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BombZombieService {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final Set<BombTask> activeTasks = new HashSet<>();

    public BombZombieService(JavaPlugin plugin, ConfigService configService) {
        this.plugin = plugin;
        this.configService = configService;
    }

    public void apply(LivingEntity zombie) {
        EntityEquipment equipment = zombie.getEquipment();
        if (equipment == null) {
            return;
        }

        boolean replaceExisting = configService.getMobsBoolean(
                "zombie-variants.variants.bomb.helmet.replace-existing", true);
        ItemStack current = equipment.getHelmet();
        if (!replaceExisting && current != null && !current.getType().isAir()) {
            equipment.setHelmetDropChance(0.0F);
        } else {
            Material material = Material.matchMaterial(configService.getMobsString(
                    "zombie-variants.variants.bomb.helmet.material", "TNT"));
            equipment.setHelmet(new ItemStack(material == null ? Material.TNT : material));
            equipment.setHelmetDropChance(0.0F);
        }
        if (zombie instanceof Mob mob) {
            mob.setCanPickupItems(false);
        }
    }

    public void removeVisualHelmet(LivingEntity zombie) {
        EntityEquipment equipment = zombie.getEquipment();
        if (equipment != null && equipment.getHelmet() != null
                && equipment.getHelmet().getType() == Material.TNT) {
            equipment.setHelmet(null);
            equipment.setHelmetDropChance(0.0F);
        }
        if (zombie instanceof Mob mob) {
            mob.setCanPickupItems(true);
        }
    }

    public void scheduleExplosion(Location source, int level) {
        if (source == null || source.getWorld() == null) {
            return;
        }
        BombTask task = new BombTask(source.clone(), Math.max(1, level));
        activeTasks.add(task);
        task.runTaskTimer(plugin, 0L, 5L);
    }

    public void cancelAll() {
        for (BombTask task : Set.copyOf(activeTasks)) {
            task.cancel();
        }
        activeTasks.clear();
    }

    private final class BombTask extends BukkitRunnable {
        private final Location location;
        private final int level;
        private int elapsedTicks;

        private BombTask(Location location, int level) {
            this.location = location;
            this.level = level;
        }

        @Override
        public void run() {
            World world = location.getWorld();
            if (world == null || Bukkit.getWorld(world.getUID()) == null) {
                finish();
                return;
            }

            int delay = Math.max(0, configService.getMobsInt(
                    "zombie-variants.variants.bomb.death-explosion.delay-ticks", 30));
            if (elapsedTicks == 0) {
                world.playSound(location, Sound.BLOCK_LEVER_CLICK, 0.8F, 1.4F);
            } else if (elapsedTicks == 10) {
                world.spawnParticle(Particle.FLAME, location.clone().add(0.0D, 0.5D, 0.0D), 8, 0.25D, 0.25D, 0.25D, 0.02D);
            } else if (elapsedTicks == 20) {
                world.playSound(location, Sound.BLOCK_LEVER_CLICK, 1.0F, 1.8F);
            }

            if (elapsedTicks >= delay) {
                explode(world);
                finish();
                return;
            }
            elapsedTicks += 5;
        }

        private void explode(World world) {
            double radius = Math.max(0.0D, configService.getMobsDouble(
                    "zombie-variants.variants.bomb.death-explosion.radius", 3.0D));
            double radiusSquared = radius * radius;
            double baseDamage = Math.max(0.0D, configService.getMobsDouble(
                    "zombie-variants.variants.bomb.death-explosion.base-damage", 4.0D));
            double damagePerLevel = Math.max(0.0D, configService.getMobsDouble(
                    "zombie-variants.variants.bomb.death-explosion.damage-per-level", 0.15D));
            double knockback = Math.max(0.0D, configService.getMobsDouble(
                    "zombie-variants.variants.bomb.death-explosion.knockback", 0.6D));
            boolean damagePlayers = configService.getMobsBoolean(
                    "zombie-variants.variants.bomb.death-explosion.damage-players", true);
            boolean damageMobs = configService.getMobsBoolean(
                    "zombie-variants.variants.bomb.death-explosion.damage-mobs", false);

            world.spawnParticle(Particle.EXPLOSION, location, 1);
            world.spawnParticle(Particle.CLOUD, location, 16, radius * 0.2D, 0.2D, radius * 0.2D, 0.08D);
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);

            double rawDamage = Math.max(0.0D, baseDamage + level * damagePerLevel);
            for (Entity nearby : world.getNearbyEntities(location, radius, radius, radius)) {
                if (!(nearby instanceof LivingEntity living) || living instanceof ArmorStand) {
                    continue;
                }
                if (living.getLocation().distanceSquared(location) > radiusSquared) {
                    continue;
                }
                if (living instanceof Player && !damagePlayers) {
                    continue;
                }
                if (!(living instanceof Player) && !damageMobs) {
                    continue;
                }

                double distance = Math.sqrt(living.getLocation().distanceSquared(location));
                double falloff = Math.max(0.3D, 1.0D - (distance / Math.max(0.1D, radius)) * 0.7D);
                living.damage(rawDamage * falloff);
                if (knockback > 0.0D) {
                    Vector direction = living.getLocation().toVector().subtract(location.toVector());
                    direction.setY(0.35D);
                    if (direction.lengthSquared() > 0.0D) {
                        living.setVelocity(direction.normalize().multiply(knockback));
                    }
                }
            }
        }

        private void finish() {
            activeTasks.remove(this);
            cancel();
        }
    }
}
