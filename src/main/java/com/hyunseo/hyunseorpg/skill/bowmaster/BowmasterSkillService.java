package com.hyunseo.hyunseorpg.skill.bowmaster;

import com.hyunseo.hyunseorpg.classsystem.ClassStatService;
import com.hyunseo.hyunseorpg.weapon.WeaponService;
import com.hyunseo.hyunseorpg.weapon.WeaponType;
import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public final class BowmasterSkillService implements Listener {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final WeaponService weaponService;
    private final CombatService combatService;
    private final ClassStatService classStatService;
    private final Map<UUID, Integer> pendingFireArrowLevelByPlayer = new ConcurrentHashMap<>();
    private final Set<BukkitTask> tasks = ConcurrentHashMap.newKeySet();
    private final Set<BlockDisplay> beamDisplays = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();

    public BowmasterSkillService(
            JavaPlugin plugin,
            ConfigService configService,
            WeaponService weaponService,
            CombatService combatService,
            ClassStatService classStatService
    ) {
        this.plugin = plugin;
        this.configService = configService;
        this.weaponService = weaponService;
        this.combatService = combatService;
        this.classStatService = classStatService;
    }

    public void prepareFireArrow(Player player, int skillLevel) {
        if (!configService.getBoolean("bowmaster.fire-arrow.enabled", true)) {
            return;
        }

        pendingFireArrowLevelByPlayer.put(player.getUniqueId(), skillLevel);
        player.getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation(), 16, 0.35D, 0.35D, 0.35D, 0.01D);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.8F, 1.4F);
        player.sendMessage(Component.text("Fire Arrow is ready for your next bow shot.", NamedTextColor.GOLD));
    }

    public void castLaserArrow(Player player, int skillLevel) {
        if (!configService.getBoolean("bowmaster.laser-arrow.enabled", true)) {
            return;
        }

        double range = configService.getDouble("bowmaster.laser-arrow.range", 100.0D)
                + classStatService.getClassStatBonus(player, "laser_arrow", "laser_range");
        double radius = configService.getDouble("bowmaster.laser-arrow.hitbox-radius", 2.0D);
        long visualTicks = Math.max(1L, configService.getLong("bowmaster.laser-arrow.visual-ticks", 6L));
        double damage = configService.getDouble(
                "bowmaster.laser-arrow.damage",
                configService.getDouble("bowmaster.laser-arrow.damage-per-tick", 1.0D)
        );
        boolean pierceBlocks = configService.getBoolean("bowmaster.laser-arrow.pierce-blocks", false);

        Location start = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(1.2D));
        Vector direction = start.getDirection().normalize();
        Location end = start.clone().add(direction.clone().multiply(range));
        if (!pierceBlocks) {
            RayTraceResult blockTrace = player.getWorld().rayTraceBlocks(start, direction, range, FluidCollisionMode.NEVER, true);
            if (blockTrace != null && blockTrace.getHitPosition() != null) {
                end = blockTrace.getHitPosition().toLocation(player.getWorld());
            }
        }

        spawnLaserBeamSegments(start, end, visualTicks);
        drawLaserParticles(player.getWorld(), start, end);
        damageEnemiesNearLine(player, start, end, radius, damage);
        showLaserImpact(player.getWorld(), end);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0F, 1.6F);
    }

    /** Shared laser visual/line-hit implementation for non-player RPG casters. */
    public void renderMonsterLaser(LivingEntity source, Location start, Location end,
                                   long visualTicks, double radius, double damage) {
        spawnLaserBeamSegments(start, end, visualTicks);
        drawLaserParticles(source.getWorld(), start, end);
        source.getWorld().playSound(start, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8F, 1.8F);
        Vector line = end.toVector().subtract(start.toVector());
        double length = line.length();
        if (length <= 0.001D) return;
        Vector direction = line.normalize();
        Set<UUID> hitTargets = new HashSet<>();
        for (double distance = 0.0D; distance <= length; distance += Math.max(0.5D, radius * 0.5D)) {
            Location point = start.clone().add(direction.clone().multiply(distance));
            for (Entity entity : point.getWorld().getNearbyEntities(point, radius, radius, radius)) {
                if (!(entity instanceof Player target) || target.isDead() || !hitTargets.add(target.getUniqueId())) continue;
                target.damage(Math.max(0.0D, damage), source);
            }
        }
        showLaserImpact(source.getWorld(), end);
    }

    public boolean castArrowRain(Player player, int skillLevel) {
        if (!configService.getBoolean("bowmaster.arrow-rain.enabled", true)) {
            return false;
        }

        double range = configService.getDouble("bowmaster.arrow-rain.target-range", 40.0D);
        double diameter = configService.getDouble("bowmaster.arrow-rain.diameter", 5.0D);
        double radius = diameter * 0.5D;
        double fallHeight = configService.getDouble("bowmaster.arrow-rain.fall-height", 10.0D);
        int arrowCount = (int) Math.max(1L, configService.getLong("bowmaster.arrow-rain.arrow-count", 20L));
        long durationTicks = Math.max(1L, configService.getLong("bowmaster.arrow-rain.duration-ticks", 40L));
        double damage = configService.getDouble("bowmaster.arrow-rain.damage", 4.0D);
        double hitboxRadius = configService.getDouble("bowmaster.arrow-rain.hitbox-radius", 1.4D);

        Location target = findTargetPoint(player, range);
        RainPoint centerPoint = findRainPoint(target, fallHeight);
        if (centerPoint == null) {
            player.sendActionBar(Component.text("천장이 너무 낮아 불화살 비를 사용할 수 없습니다.", NamedTextColor.YELLOW));
            return false;
        }
        Location center = centerPoint.impact();
        player.getWorld().playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.0F, 0.8F);
        for (int index = 0; index < arrowCount; index++) {
            long delayTicks = Math.round(index * (durationTicks / (double) arrowCount));
            BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                RainPoint point = null;
                int retries = Math.max(1, configService.getLong("bowmaster.arrow-rain.invalid-point-retries", 5L) > Integer.MAX_VALUE
                        ? 5 : (int) configService.getLong("bowmaster.arrow-rain.invalid-point-retries", 5L));
                for (int attempt = 0; attempt < retries && point == null; attempt++) {
                    point = findRainPoint(randomPoint(center, radius), fallHeight);
                }
                if (point != null) spawnRainArrow(player, point, damage, hitboxRadius);
            }, delayTicks);
            tasks.add(task);
        }
        return true;
    }

    /** Performs the geometry-only check before SkillService consumes mana or starts cooldown. */
    public boolean canCastArrowRain(Player player) {
        if (!configService.getBoolean("bowmaster.arrow-rain.enabled", true)) return false;
        double range = configService.getDouble("bowmaster.arrow-rain.target-range", 40.0D);
        double preferred = configService.getDouble("bowmaster.arrow-rain.preferred-fall-height", 10.0D);
        return findRainPoint(findTargetPoint(player, range), preferred) != null;
    }

    public long getArrowRainInputSuppressMillis() {
        return Math.max(0L, configService.getLong("bowmaster.arrow-rain.input-suppress-millis", 2500L));
    }

    public void clearAll() {
        pendingFireArrowLevelByPlayer.clear();
        for (BukkitTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
        for (BlockDisplay display : beamDisplays) {
            if (!display.isDead()) {
                display.remove();
            }
        }
        beamDisplays.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Integer skillLevel = pendingFireArrowLevelByPlayer.get(player.getUniqueId());
        if (skillLevel == null || !weaponService.isWeaponType(event.getBow(), WeaponType.BOW)) {
            return;
        }
        pendingFireArrowLevelByPlayer.remove(player.getUniqueId());

        event.setCancelled(true);
        if (event.getProjectile() != null) {
            event.getProjectile().remove();
        }

        int arrowCount = (int) Math.max(1L, configService.getLong("bowmaster.fire-arrow.arrow-count", 10L));
        double spreadDegrees = configService.getDouble("bowmaster.fire-arrow.spread-degrees", 18.0D);
        double damage = configService.getDouble("bowmaster.fire-arrow.damage", 3.0D);
        double radius = configService.getDouble("bowmaster.fire-arrow.hitbox-radius", 0.9D);
        double speed = Math.max(0.1D, configService.getDouble("bowmaster.fire-arrow.speed", 3.2D));
        long lifetimeTicks = Math.max(1L, configService.getLong("bowmaster.fire-arrow.lifetime-ticks", 60L));
        for (int index = 0; index < arrowCount; index++) {
            double offset = arrowCount == 1 ? 0.0D : (index / (double) (arrowCount - 1) - 0.5D) * spreadDegrees;
            Vector velocity = rotateYaw(player.getEyeLocation().getDirection(), offset).normalize().multiply(speed);
            Arrow arrow = player.getWorld().spawnArrow(player.getEyeLocation(), velocity, (float) speed, 0.0F);
            arrow.setShooter(player);
            arrow.setFireTicks(120);
            arrow.setCritical(false);
            arrow.setDamage(0.0D);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            startArrowHitTask(player, arrow, radius, damage, lifetimeTicks);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
    }

    private void spawnLaserBeamSegments(Location start, Location end, long visualTicks) {
        Vector line = end.toVector().subtract(start.toVector());
        double length = line.length();
        if (length <= 0.001D) {
            return;
        }
        Vector direction = line.normalize();
        float width = (float) configService.getDouble("bowmaster.laser-arrow.visual-width", 0.8D);
        double segmentSpacing = Math.max(0.5D, configService.getDouble("bowmaster.laser-arrow.visual-segment-spacing", 1.0D));
        for (double distance = 0.0D; distance <= length; distance += segmentSpacing) {
            Location segmentLocation = start.clone().add(direction.clone().multiply(distance));
            BlockDisplay segment = start.getWorld().spawn(segmentLocation, BlockDisplay.class, entity -> {
                entity.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setInterpolationDuration(1);
                entity.setPersistent(false);
                entity.setTransformation(new Transformation(
                        new Vector3f(-width * 0.5F, -width * 0.5F, -width * 0.5F),
                        new Quaternionf(),
                        new Vector3f(width, width, width),
                        new Quaternionf()
                ));
            });
            beamDisplays.add(segment);
            BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                beamDisplays.remove(segment);
                if (!segment.isDead()) {
                    segment.remove();
                }
            }, visualTicks);
            tasks.add(task);
        }
    }

    private void drawLaserParticles(World world, Location start, Location end) {
        Vector line = end.toVector().subtract(start.toVector());
        double length = line.length();
        if (length <= 0.001D) {
            return;
        }
        Vector direction = line.normalize();
        for (double distance = 0.0D; distance <= length; distance += 1.0D) {
            Location point = start.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.END_ROD, point, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            world.spawnParticle(Particle.FLAME, point, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private void showLaserImpact(World world, Location end) {
        world.spawnParticle(Particle.EXPLOSION, end, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.spawnParticle(Particle.END_ROD, end, 24, 0.45D, 0.45D, 0.45D, 0.04D);
        world.spawnParticle(Particle.FIREWORK, end, 12, 0.35D, 0.35D, 0.35D, 0.02D);
    }

    private void damageEnemiesNearLine(Player player, Location start, Location end, double radius, double damage) {
        Vector line = end.toVector().subtract(start.toVector());
        double length = line.length();
        if (length <= 0.001D) {
            return;
        }

        Vector direction = line.normalize();
        Set<UUID> hitTargets = new HashSet<>();
        for (double distance = 0.0D; distance <= length; distance += Math.max(0.5D, radius * 0.5D)) {
            Location point = start.clone().add(direction.clone().multiply(distance));
            for (Entity entity : point.getWorld().getNearbyEntities(point, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity target) || target instanceof Player || target.isDead() || target.equals(player)) {
                    continue;
                }
                if (hitTargets.add(target.getUniqueId())) {
                    combatService.applyUltimateDamage(player, target, damage);
                }
            }
        }
    }

    private void startArrowHitTask(Player player, Arrow arrow, double radius, double damage, long lifetimeTicks) {
        Set<UUID> hitTargets = new HashSet<>();
        final long[] elapsedTicks = {0L};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            elapsedTicks[0]++;
            if (arrow.isDead() || elapsedTicks[0] > lifetimeTicks) {
                arrow.remove();
                return;
            }

            Location location = arrow.getLocation();
            location.getWorld().spawnParticle(Particle.FLAME, location, 2, 0.04D, 0.04D, 0.04D, 0.01D);
            for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity target) || target instanceof Player || target.equals(player) || target.isDead()) {
                    continue;
                }
                if (hitTargets.add(target.getUniqueId())) {
                    combatService.applyUltimateDamage(player, target, damage);
                    location.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0.0D, 1.0D, 0.0D), 10, 0.25D, 0.35D, 0.25D, 0.02D);
                }
            }
        }, 1L, 1L);
        tasks.add(task);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            task.cancel();
            tasks.remove(task);
            if (!arrow.isDead()) {
                arrow.remove();
            }
        }, lifetimeTicks + 1L);
    }

    private void spawnRainArrow(Player player, RainPoint point, double damage, double hitboxRadius) {
        Location impact = point.impact();
        Location spawnLocation = impact.clone().add(0.0D, point.fallHeight(), 0.0D);
        Arrow arrow = player.getWorld().spawnArrow(spawnLocation, new Vector(0.0D, -1.8D, 0.0D), 1.8F, 0.0F);
        arrow.setShooter(player);
        arrow.setFireTicks(120);
        arrow.setDamage(0.0D);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

        final BukkitTask[] holder = new BukkitTask[1];
        final int[] elapsed = {0};
        holder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            elapsed[0]++;
            if (!arrow.isValid() || arrow.isDead() || arrow.isOnGround() || elapsed[0] > 80) {
                Location actual = arrow.getLocation().clone();
                actual.getWorld().spawnParticle(Particle.FLAME, actual, 12, 0.35D, 0.15D, 0.35D, 0.02D);
                actual.getWorld().playSound(actual, Sound.BLOCK_FIRE_EXTINGUISH, 0.5F, 1.8F);
                for (Entity entity : actual.getWorld().getNearbyEntities(actual, hitboxRadius, hitboxRadius, hitboxRadius)) {
                    if (entity instanceof LivingEntity targetEntity && !(targetEntity instanceof Player)
                            && !targetEntity.equals(player) && !targetEntity.isDead()) {
                        combatService.applyUltimateDamage(player, targetEntity, damage);
                    }
                }
                if (!arrow.isDead()) arrow.remove();
                holder[0].cancel();
                tasks.remove(holder[0]);
            }
        }, 1L, 1L);
        tasks.add(holder[0]);
    }

    private Location findTargetPoint(Player player, double range) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();
        RayTraceResult result = player.getWorld().rayTraceBlocks(eyeLocation, direction, range, FluidCollisionMode.NEVER, true);
        Location target = result != null && result.getHitPosition() != null
                ? result.getHitPosition().toLocation(player.getWorld())
                : eyeLocation.clone().add(direction.multiply(range));
        return target;
    }

    private RainPoint findRainPoint(Location location, double preferredFallHeight) {
        if (location == null || location.getWorld() == null) return null;
        World world = location.getWorld();
        int startY = location.getBlockY();
        int down = Math.max(1, configService.getLong("bowmaster.arrow-rain.floor-search-down-blocks", 8L) > Integer.MAX_VALUE
                ? 8 : (int) configService.getLong("bowmaster.arrow-rain.floor-search-down-blocks", 8L));
        int up = Math.max(1, configService.getLong("bowmaster.arrow-rain.ceiling-search-up-blocks", 14L) > Integer.MAX_VALUE
                ? 14 : (int) configService.getLong("bowmaster.arrow-rain.ceiling-search-up-blocks", 14L));
        double minimumOpen = Math.max(1.0D, configService.getDouble("bowmaster.arrow-rain.minimum-open-height", 5.0D));
        double preferred = Math.max(1.0D, configService.getDouble("bowmaster.arrow-rain.preferred-fall-height", preferredFallHeight));
        int floorY = Integer.MIN_VALUE;
        for (int y = startY; y >= startY - down; y--) {
            if (world.getBlockAt(location.getBlockX(), y, location.getBlockZ()).isSolid()) {
                floorY = y;
                break;
            }
        }
        if (floorY == Integer.MIN_VALUE) return null;
        double impactY = floorY + 1.05D;
        int ceilingY = Integer.MIN_VALUE;
        for (int y = floorY + 1; y <= floorY + up; y++) {
            if (world.getBlockAt(location.getBlockX(), y, location.getBlockZ()).isSolid()) {
                ceilingY = y;
                break;
            }
        }
        double openHeight = ceilingY == Integer.MIN_VALUE ? Double.POSITIVE_INFINITY : ceilingY - impactY;
        if (openHeight < minimumOpen) return null;
        double fallHeight = ceilingY == Integer.MIN_VALUE
                ? preferred : Math.min(preferred, Math.max(1.0D, openHeight - 0.75D));
        return new RainPoint(new Location(world, location.getBlockX() + 0.5D, impactY,
                location.getBlockZ() + 0.5D), fallHeight);
    }

    private Location randomPoint(Location center, double radius) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = Math.sqrt(random.nextDouble()) * radius;
        return center.clone().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
    }

    private Vector rotateYaw(Vector vector, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z);
    }

    private record RainPoint(Location impact, double fallHeight) { }
}
