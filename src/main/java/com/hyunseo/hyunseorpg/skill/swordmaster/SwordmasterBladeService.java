package com.hyunseo.hyunseorpg.skill.swordmaster;

import com.hyunseo.hyunseorpg.classsystem.ClassStatService;
import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public final class SwordmasterBladeService {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final CombatService combatService;
    private final ClassStatService classStatService;
    private final Map<UUID, List<BladeObject>> bladesByOwner = new ConcurrentHashMap<>();
    private final Set<ItemDisplay> temporaryDisplays = ConcurrentHashMap.newKeySet();
    private final Set<BukkitTask> temporaryTasks = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();
    private BukkitTask tickTask;
    private long currentTick;

    public SwordmasterBladeService(JavaPlugin plugin, ConfigService configService, CombatService combatService, ClassStatService classStatService) {
        this.plugin = plugin;
        this.configService = configService;
        this.combatService = combatService;
        this.classStatService = classStatService;
    }

    public void start() {
        if (tickTask != null) {
            return;
        }
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public boolean canCastBladeThrow(Player player, int skillLevel) {
        int bladeCount = getBladeCount(player, skillLevel);
        int maxActiveThrows = getMaxActiveThrows(player);
        int maxActiveBlades = getMaxActiveBlades(player, bladeCount, maxActiveThrows);
        List<BladeObject> ownerBlades = bladesByOwner.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayList<>());
        if (countActiveThrows(ownerBlades) >= maxActiveThrows) {
            player.sendMessage(Component.text("Active blade throws are full. Wait for existing blades to disappear.", NamedTextColor.YELLOW));
            return false;
        }
        if (ownerBlades.size() + bladeCount > maxActiveBlades) {
            player.sendMessage(Component.text("Too many active blades. Wait for existing blades to disappear.", NamedTextColor.YELLOW));
            return false;
        }
        return true;
    }

    public void castBladeThrow(Player player, int skillLevel) {
        if (!configService.getBoolean("swordmaster.blade-throw.enabled", true)) {
            return;
        }

        if (!canCastBladeThrow(player, skillLevel)) {
            return;
        }

        int bladeCount = getBladeCount(player, skillLevel);
        List<BladeObject> ownerBlades = bladesByOwner.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayList<>());

        long durationTicks = Math.max(1L, configService.getLong("swordmaster.blade-throw.duration-ticks", 200L));
        durationTicks += Math.max(0L, Math.round(classStatService.getClassStatBonus(player, "blade_throw", "duration_seconds") * 20.0D));
        double forwardOffset = configService.getDouble("swordmaster.blade-throw.forward-offset", 2.0D);
        double hitboxRadius = configService.getDouble("swordmaster.blade-throw.hitbox-radius", 0.8D);
        double minDamage = configService.getDouble("swordmaster.blade-throw.damage-min", 0.5D);
        double maxDamage = configService.getDouble("swordmaster.blade-throw.damage-max", 1.0D);
        double flightSpeed = Math.max(0.1D, configService.getDouble("swordmaster.blade-throw.throw-flight-speed", 0.7D));

        UUID throwId = UUID.randomUUID();
        Location baseLocation = player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(forwardOffset));
        for (int index = 0; index < bladeCount; index++) {
            Location destination = createThrowDestination(player);
            Vector velocity = destination.toVector().subtract(baseLocation.toVector());
            if (velocity.lengthSquared() <= 0.001D) {
                velocity = player.getLocation().getDirection();
            }
            velocity.normalize().multiply(flightSpeed);

            Location bladeLocation = baseLocation.clone().add(randomOffset(0.8D));
            Vector displayDirection = velocity.clone();
            bladeLocation.setYaw(0.0F);
            bladeLocation.setPitch(0.0F);
            ItemDisplay display = player.getWorld().spawn(bladeLocation, ItemDisplay.class, entity -> {
                entity.setItemStack(createBladeVisualItem());
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setInterpolationDuration(1);
                applyBladeTransform(entity, displayDirection);
                entity.setPersistent(false);
            });

            double damage = minDamage + random.nextDouble() * Math.max(0.0D, maxDamage - minDamage);
            ownerBlades.add(new BladeObject(
                    player.getUniqueId(),
                    throwId,
                    bladeLocation,
                    currentTick + durationTicks,
                    display,
                    damage,
                    hitboxRadius,
                    BladeState.FLYING_TO_GROUND,
                    velocity,
                    destination
            ));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.2F);
    }

    public boolean canCastBladeLaunch(Player player) {
        double activeRadius = configService.getDouble("swordmaster.blade-launch.active-radius", 60.0D);
        double searchRadius = configService.getDouble("swordmaster.blade-launch.search-radius", 10.0D);
        List<BladeObject> launchableBlades = getLaunchableBlades(player, activeRadius);
        if (launchableBlades.isEmpty()) {
            player.sendMessage(Component.text("No blades are ready to launch.", NamedTextColor.YELLOW));
            return false;
        }

        List<LivingEntity> targets = getNearbyEnemies(player.getLocation(), searchRadius);
        if (targets.isEmpty()) {
            player.sendMessage(Component.text("No enemies are in blade launch range.", NamedTextColor.YELLOW));
            return false;
        }
        return true;
    }

    public boolean hasLaunchableBlades(Player player) {
        double activeRadius = configService.getDouble("swordmaster.blade-launch.active-radius", 60.0D);
        return !getLaunchableBlades(player, activeRadius).isEmpty();
    }

    public void castBladeLaunch(Player player) {
        if (!configService.getBoolean("swordmaster.blade-launch.enabled", true)) {
            return;
        }

        double activeRadius = configService.getDouble("swordmaster.blade-launch.active-radius", 60.0D);
        double searchRadius = configService.getDouble("swordmaster.blade-launch.search-radius", 10.0D);
        List<BladeObject> launchableBlades = getLaunchableBlades(player, activeRadius);
        if (launchableBlades.isEmpty()) {
            player.sendMessage(Component.text("발사할 칼날이 주변에 없습니다.", NamedTextColor.YELLOW));
            return;
        }

        List<LivingEntity> targets = getNearbyEnemies(player.getLocation(), searchRadius);
        if (targets.isEmpty()) {
            player.sendMessage(Component.text("칼날이 노릴 적이 주변에 없습니다.", NamedTextColor.YELLOW));
            return;
        }

        double launchSpeed = Math.max(0.1D, configService.getDouble("swordmaster.blade-launch.launch-speed", 1.2D));
        double maxRange = Math.max(1.0D, configService.getDouble("swordmaster.blade-launch.max-range", 16.0D));
        long missStuckTicks = Math.max(1L, configService.getLong("swordmaster.blade-launch.miss-stuck-ticks", 40L));

        for (BladeObject blade : launchableBlades) {
            LivingEntity target = targets.get(random.nextInt(targets.size()));
            Location destination = target.getLocation().add(0.0D, target.getHeight() * 0.5D, 0.0D);
            Vector direction = destination.toVector().subtract(blade.location().toVector());
            if (direction.lengthSquared() <= 0.001D) {
                continue;
            }

            double distance = direction.length();
            if (distance > maxRange) {
                destination = blade.location().clone().add(direction.normalize().multiply(maxRange));
            }
            Vector velocity = destination.toVector().subtract(blade.location().toVector()).normalize().multiply(launchSpeed);
            blade.state(BladeState.LAUNCHED);
            blade.velocity(velocity);
            blade.destination(destination);
            teleportBladeDisplay(blade, velocity);
            double launchDistance = Math.min(distance, maxRange);
            blade.expireAt(currentTick + Math.max(missStuckTicks, Math.round(launchDistance / launchSpeed) + missStuckTicks));
            blade.clearHits();
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.7F);
    }

    public void castLightGreatsword(Player player, int skillLevel) {
        if (!configService.getBoolean("swordmaster.light-greatsword.enabled", true)) {
            return;
        }

        Location targetLocation = findUltimateTarget(player);
        long preDelayTicks = Math.max(1L, configService.getLong("swordmaster.light-greatsword.pre-delay-ticks", 40L));
        long visualFallTicks = Math.max(preDelayTicks, configService.getLong("swordmaster.light-greatsword.visual-fall-ticks", 30L));
        double skyHeight = configService.getDouble("swordmaster.light-greatsword.sky-height", 28.0D);
        Location swordLocation = targetLocation.clone().add(0.0D, skyHeight, 0.0D);
        ItemDisplay display = spawnUltimateDisplay(player, swordLocation, targetLocation);
        long swordDisplayDurationTicks = Math.max(1L, configService.getLong("swordmaster.light-greatsword.display-duration-after-impact-ticks", 100L));
        scheduleTemporaryDisplayRemoval(display, visualFallTicks + swordDisplayDurationTicks + 20L);

        player.getWorld().playSound(targetLocation, Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.2F);
        AtomicReference<BukkitTask> animationTaskReference = new AtomicReference<>();
        BukkitTask animationTask = new org.bukkit.scheduler.BukkitRunnable() {
            private long elapsedTicks;

            @Override
            public void run() {
                try {
                    if (!player.isOnline() || display.isDead()) {
                        cleanupUltimate(this, animationTaskReference.get(), display);
                        return;
                    }

                    elapsedTicks++;
                    double progress = Math.min(1.0D, elapsedTicks / (double) visualFallTicks);
                    Location currentLocation = targetLocation.clone().add(0.0D, skyHeight * (1.0D - progress) + 1.5D, 0.0D);
                    currentLocation.setYaw(0.0F);
                    currentLocation.setPitch(0.0F);
                    display.teleport(currentLocation);
                    showUltimateFallingTrail(currentLocation, targetLocation);
                    showUltimateWarning(targetLocation, progress);

                    if (elapsedTicks >= visualFallTicks) {
                        Location impactDisplayLocation = targetLocation.clone().add(0.0D, 1.5D, 0.0D);
                        impactDisplayLocation.setYaw(0.0F);
                        impactDisplayLocation.setPitch(0.0F);
                        display.teleport(impactDisplayLocation);
                        finishUltimate(this, animationTaskReference.get(), display, swordDisplayDurationTicks);
                    }
                } catch (RuntimeException exception) {
                    plugin.getLogger().log(Level.SEVERE, "Light Greatsword animation failed.", exception);
                    cleanupUltimate(this, animationTaskReference.get(), display);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        animationTaskReference.set(animationTask);
        temporaryTasks.add(animationTask);

        AtomicReference<BukkitTask> impactTaskReference = new AtomicReference<>();
        BukkitTask impactTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            temporaryTasks.remove(impactTaskReference.get());
            try {
                if (player.isOnline()) {
                    impactLightGreatsword(player, targetLocation, skillLevel);
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.SEVERE, "Light Greatsword impact failed.", exception);
            }
        }, visualFallTicks);
        impactTaskReference.set(impactTask);
        temporaryTasks.add(impactTask);
    }

    public void clearAll() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (BukkitTask task : temporaryTasks) {
            task.cancel();
        }
        temporaryTasks.clear();
        for (ItemDisplay display : temporaryDisplays) {
            if (!display.isDead()) {
                display.remove();
            }
        }
        temporaryDisplays.clear();
        for (List<BladeObject> blades : bladesByOwner.values()) {
            for (BladeObject blade : blades) {
                removeBlade(blade);
            }
        }
        bladesByOwner.clear();
    }

    /** Removes only one player's temporary blade displays and launch state. */
    public void clearPlayer(Player player) {
        if (player == null) return;
        List<BladeObject> blades = bladesByOwner.remove(player.getUniqueId());
        if (blades != null) blades.forEach(this::removeBlade);
    }

    private void tick() {
        currentTick++;

        Iterator<Map.Entry<UUID, List<BladeObject>>> ownerIterator = bladesByOwner.entrySet().iterator();
        while (ownerIterator.hasNext()) {
            Map.Entry<UUID, List<BladeObject>> entry = ownerIterator.next();
            Player owner = plugin.getServer().getPlayer(entry.getKey());
            Iterator<BladeObject> bladeIterator = entry.getValue().iterator();
            while (bladeIterator.hasNext()) {
                BladeObject blade = bladeIterator.next();
                if (currentTick >= blade.expireTick() || blade.displayEntity().isDead()) {
                    removeBlade(blade);
                    bladeIterator.remove();
                    continue;
                }
                if (owner == null || !owner.isOnline()) {
                    removeBlade(blade);
                    bladeIterator.remove();
                    continue;
                }
                if (isOutsideActiveRadius(owner, blade) && blade.state() == BladeState.STUCK) {
                    continue;
                }

                boolean remove = tickBlade(owner, blade);
                if (remove) {
                    removeBlade(blade);
                    bladeIterator.remove();
                }
            }
            if (entry.getValue().isEmpty()) {
                ownerIterator.remove();
            }
        }
    }

    private boolean tickBlade(Player owner, BladeObject blade) {
        if (blade.state() == BladeState.STUCK) {
            teleportBladeDisplay(blade, blade.velocity());
            return false;
        }

        moveBlade(blade);
        for (LivingEntity hitTarget : findHitTargets(owner, blade)) {
            if (blade.markHit(hitTarget.getUniqueId())) {
                combatService.applyPiercingSkillDamage(owner, hitTarget, blade.damage());
            }
        }

        if (hasReachedDestination(blade)) {
            long missStuckTicks = Math.max(1L, configService.getLong("swordmaster.blade-launch.miss-stuck-ticks", 40L));
            long expireTick = blade.state() == BladeState.LAUNCHED ? currentTick + missStuckTicks : blade.expireTick();
            stickBlade(blade, blade.destination(), expireTick);
        }
        return false;
    }

    private void moveBlade(BladeObject blade) {
        blade.location().add(blade.velocity());
        teleportBladeDisplay(blade, blade.velocity());
    }

    private void stickBlade(BladeObject blade, Location location, long expireTick) {
        blade.location().setX(location.getX());
        blade.location().setY(location.getY());
        blade.location().setZ(location.getZ());
        Vector stickDirection = blade.velocity().clone();
        teleportBladeDisplay(blade, stickDirection);
        blade.state(BladeState.STUCK);
        blade.velocity(stickDirection);
        blade.expireAt(expireTick);
    }

    private List<LivingEntity> findHitTargets(Player owner, BladeObject blade) {
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity nearbyEntity : blade.location().getWorld().getNearbyEntities(blade.location(), blade.hitboxRadius(), blade.hitboxRadius(), blade.hitboxRadius())) {
            if (!(nearbyEntity instanceof LivingEntity target) || !(target instanceof Enemy) || target.isDead() || target.equals(owner)) {
                continue;
            }
            targets.add(target);
        }
        return targets;
    }

    private List<BladeObject> getLaunchableBlades(Player player, double radius) {
        List<BladeObject> blades = bladesByOwner.getOrDefault(player.getUniqueId(), List.of());
        List<BladeObject> launchable = new ArrayList<>();
        double radiusSquared = radius * radius;
        for (BladeObject blade : blades) {
            if (blade.state() == BladeState.STUCK && blade.location().getWorld().equals(player.getWorld())
                    && blade.location().distanceSquared(player.getLocation()) <= radiusSquared) {
                launchable.add(blade);
            }
        }
        return launchable;
    }

    private List<LivingEntity> getNearbyEnemies(Location center, double radius) {
        List<LivingEntity> enemies = new ArrayList<>();
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity livingEntity && entity instanceof Enemy && !livingEntity.isDead()) {
                enemies.add(livingEntity);
            }
        }
        return enemies;
    }

    private boolean hasReachedDestination(BladeObject blade) {
        if (blade.destination() == null) {
            return false;
        }
        double step = Math.max(0.25D, blade.velocity().length() + 0.1D);
        return blade.location().distanceSquared(blade.destination()) <= step * step;
    }

    private Location createThrowDestination(Player player) {
        double minDistance = configService.getDouble("swordmaster.blade-throw.throw-distance-min", 7.0D);
        double maxDistance = Math.max(minDistance, configService.getDouble("swordmaster.blade-throw.throw-distance-max", 12.0D));
        double sideSpread = configService.getDouble("swordmaster.blade-throw.throw-side-spread", 5.0D);

        Vector forward = player.getLocation().getDirection().setY(0.0D);
        if (forward.lengthSquared() <= 0.001D) {
            forward = new Vector(0.0D, 0.0D, 1.0D);
        }
        forward.normalize();
        Vector right = new Vector(-forward.getZ(), 0.0D, forward.getX()).normalize();
        double distance = minDistance + random.nextDouble() * Math.max(0.0D, maxDistance - minDistance);
        double side = (random.nextDouble() * 2.0D - 1.0D) * sideSpread;
        Location destination = player.getLocation().add(forward.multiply(distance)).add(right.multiply(side));
        destination.setY(findGroundY(destination));
        return destination;
    }

    private double findGroundY(Location location) {
        World world = location.getWorld();
        int minY = world.getMinHeight();
        int startY = Math.min(world.getMaxHeight() - 1, location.getBlockY() + 5);
        for (int y = startY; y >= minY; y--) {
            if (world.getBlockAt(location.getBlockX(), y, location.getBlockZ()).getType().isSolid()) {
                return y + 1.05D;
            }
        }
        return location.getY();
    }

    private int getBladeCount(Player player, int skillLevel) {
        int level = Math.max(1, skillLevel);
        String path = "swordmaster.blade-throw.blade-count-level-" + level;
        int configured = (int) configService.getLong(path, -1L);
        int baseCount = configured > 0 ? configured : switch (level) {
            case 1 -> 10;
            case 2 -> 12;
            case 3 -> 14;
            case 4 -> 16;
            default -> 20;
        };
        int classStatBonus = (int) Math.round(classStatService.getClassStatBonus(player, "blade_throw", "blade_count"));
        return Math.max(1, baseCount + classStatBonus);
    }

    private int getMaxActiveThrows(Player player) {
        int base = (int) Math.max(1L, configService.getLong("swordmaster.blade-throw.max-active-throws", 3L));
        int classStatBonus = (int) Math.round(classStatService.getClassStatBonus(player, "blade_throw", "active_throws"));
        return Math.max(1, base + classStatBonus);
    }

    private int getMaxActiveBlades(Player player, int bladeCount, int maxActiveThrows) {
        int configured = (int) Math.max(1L, configService.getLong("swordmaster.blade-throw.max-active-blades", 60L));
        return Math.max(configured, bladeCount * maxActiveThrows);
    }

    private int countActiveThrows(List<BladeObject> blades) {
        Set<UUID> throwIds = new HashSet<>();
        for (BladeObject blade : blades) {
            throwIds.add(blade.throwId());
        }
        return throwIds.size();
    }

    private boolean isOutsideActiveRadius(Player owner, BladeObject blade) {
        double activeRadius = configService.getDouble("swordmaster.blade-launch.active-radius", 60.0D);
        return !blade.location().getWorld().equals(owner.getWorld())
                || blade.location().distanceSquared(owner.getLocation()) > activeRadius * activeRadius;
    }

    private Vector randomOffset(double radius) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = random.nextDouble() * radius;
        return new Vector(Math.cos(angle) * distance, (random.nextDouble() - 0.5D) * 0.4D, Math.sin(angle) * distance);
    }

    private ItemStack createBladeVisualItem() {
        Material material = Material.matchMaterial(configService.getString("resource-pack.skill-models.swordmaster.blade.material", "IRON_SWORD"));
        if (material == null || material.isAir()) {
            material = Material.IRON_SWORD;
        }

        ItemStack itemStack = new ItemStack(material);
        int customModelData = (int) configService.getLong("resource-pack.skill-models.swordmaster.blade.custom-model-data", 21001L);
        if (customModelData > 0) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                itemStack.setItemMeta(meta);
            }
        }
        return itemStack;
    }

    private void applyBladeTransform(ItemDisplay display, Vector direction) {
        applyDirectionalItemTransform(display, direction, "resource-pack.skill-models.swordmaster.blade.scale", 1.0D);
    }

    private void applyDirectionalItemTransform(ItemDisplay display, Vector direction, String scalePath, double defaultScale) {
        if (direction.lengthSquared() <= 0.001D) {
            return;
        }

        Vector normalized = direction.clone().normalize();
        Vector3f bladeTipAxis = new Vector3f(-1.0F, 1.0F, 0.0F).normalize();
        Vector3f bladeFaceAxis = new Vector3f(0.0F, 0.0F, 1.0F);
        Vector3f flightAxis = new Vector3f((float) normalized.getX(), (float) normalized.getY(), (float) normalized.getZ()).normalize();
        Quaternionf rotation = new Quaternionf().rotationTo(bladeTipAxis, flightAxis);
        stabilizeBladeRoll(rotation, bladeFaceAxis, flightAxis);

        float scale = (float) configService.getDouble(scalePath, defaultScale);
        display.setTransformation(new Transformation(
                new Vector3f(0.0F, 0.0F, 0.0F),
                rotation,
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        ));
    }

    private ItemStack createUltimateVisualItem() {
        return new ItemStack(Material.IRON_SWORD);
    }

    private ItemDisplay spawnUltimateDisplay(Player player, Location location, Location targetLocation) {
        ItemDisplay display = player.getWorld().spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(createUltimateVisualItem());
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setInterpolationDuration(3);
            entity.setViewRange(64.0F);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setGlowing(true);
            entity.setPersistent(false);
            applyUltimateTransform(entity);
        });
        temporaryDisplays.add(display);
        return display;
    }

   private void applyUltimateTransform(ItemDisplay display) {
    float scale = (float) configService.getDouble(
            "resource-pack.skill-models.swordmaster.light-greatsword.scale",
            5.0D
    );

    display.setTransformation(new Transformation(
            new Vector3f(0.0F, 0.0F, 0.0F),
            new Quaternionf().rotateZ((float) Math.toRadians(135.0D)),
            new Vector3f(scale, scale, scale),
            new Quaternionf()
    ));
}

    private Location findUltimateTarget(Player player) {
        double range = Math.max(1.0D, configService.getDouble("swordmaster.light-greatsword.target-range", 30.0D));
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();
        org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceBlocks(eyeLocation, direction, range);
        Location baseTarget;
        if (result != null && result.getHitPosition() != null) {
            baseTarget = result.getHitPosition().toLocation(player.getWorld());
        } else {
            baseTarget = eyeLocation.clone().add(direction.clone().multiply(range));
        }

        LivingEntity target = findNearestUltimateTarget(player, eyeLocation, direction, range);
        if (target != null) {
            return findGroundBelow(target.getLocation());
        }
        return findGroundBelow(baseTarget);
    }

    private LivingEntity findNearestUltimateTarget(Player player, Location eyeLocation, Vector direction, double range) {
        if (!configService.getBoolean("swordmaster.light-greatsword.snap-to-nearby-mob.enabled", true)) {
            return null;
        }

        double maxAngleDegrees = configService.getDouble("swordmaster.light-greatsword.snap-to-nearby-mob.max-angle-degrees", 25.0D);
        double minDot = Math.cos(Math.toRadians(Math.max(0.0D, Math.min(89.0D, maxAngleDegrees))));
        double maxLineDistance = Math.max(0.0D, configService.getDouble("swordmaster.light-greatsword.snap-to-nearby-mob.max-line-distance", 8.0D));
        double maxLineDistanceSquared = maxLineDistance * maxLineDistance;

        LivingEntity bestTarget = null;
        double bestDistanceAlongView = Double.MAX_VALUE;
        for (Entity entity : player.getWorld().getNearbyEntities(eyeLocation, range, range, range)) {
            if (!(entity instanceof LivingEntity livingEntity) || livingEntity instanceof Player || livingEntity.isDead()) {
                continue;
            }

            Location candidateLocation = livingEntity.getLocation().add(0.0D, livingEntity.getHeight() * 0.5D, 0.0D);
            Vector toCandidate = candidateLocation.toVector().subtract(eyeLocation.toVector());
            double distance = toCandidate.length();
            if (distance <= 0.001D || distance > range) {
                continue;
            }

            Vector normalizedToCandidate = toCandidate.clone().normalize();
            double dot = normalizedToCandidate.dot(direction);
            if (dot < minDot) {
                continue;
            }

            double distanceAlongView = toCandidate.dot(direction);
            if (distanceAlongView < 0.0D) {
                continue;
            }
            Vector closestPointOnView = eyeLocation.toVector().add(direction.clone().multiply(distanceAlongView));
            double lineDistanceSquared = candidateLocation.toVector().distanceSquared(closestPointOnView);
            if (lineDistanceSquared > maxLineDistanceSquared) {
                continue;
            }

            if (distanceAlongView < bestDistanceAlongView) {
                bestDistanceAlongView = distanceAlongView;
                bestTarget = livingEntity;
            }
        }
        return bestTarget;
    }

    private void showUltimateWarning(Location center, double progress) {
        double radius = configService.getDouble("swordmaster.light-greatsword.radius", 10.0D);
        int points = 36;
        for (int index = 0; index < points; index += 3) {
            double angle = Math.PI * 2.0D * index / points;
            Location particleLocation = center.clone().add(Math.cos(angle) * radius, 0.15D, Math.sin(angle) * radius);
            center.getWorld().spawnParticle(Particle.END_ROD, particleLocation, 1, 0.0D, 0.02D, 0.0D, 0.0D);
        }
    }

    private void showUltimateFallingTrail(Location swordLocation, Location targetLocation) {
        swordLocation.getWorld().spawnParticle(Particle.END_ROD, swordLocation, 8, 0.35D, 0.35D, 0.35D, 0.01D);
        swordLocation.getWorld().spawnParticle(Particle.CRIT, swordLocation, 4, 0.25D, 0.25D, 0.25D, 0.02D);
        Location beamLocation = targetLocation.clone().add(0.0D, Math.max(1.0D, swordLocation.getY() - targetLocation.getY()) * 0.5D, 0.0D);
        double beamHeight = Math.max(0.5D, swordLocation.getY() - targetLocation.getY()) * 0.5D;
        swordLocation.getWorld().spawnParticle(Particle.END_ROD, beamLocation, 3, 0.08D, beamHeight, 0.08D, 0.0D);
    }

    private void impactLightGreatsword(Player player, Location center, int skillLevel) {
        double radius = configService.getDouble("swordmaster.light-greatsword.radius", 10.0D);
        double damage = configService.getDouble("swordmaster.light-greatsword.damage", 20.0D)
                + classStatService.getClassStatBonus(player, "light_greatsword", "damage");
        spawnUltimateGroundMark(center);
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0.0D, 1.0D, 0.0D), 80, radius * 0.35D, 1.5D, radius * 0.35D, 0.03D);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.7F);
        center.getWorld().playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.0F, 1.4F);

        double radiusSquared = radius * radius;
        double verticalRange = Math.max(radius, configService.getDouble("swordmaster.light-greatsword.vertical-damage-range", 16.0D));
        int hitCount = 0;
        for (LivingEntity target : center.getWorld().getLivingEntities()) {
            if (target instanceof Player || target.isDead()) {
                continue;
            }
            Location targetGroundLocation = target.getLocation();
            double horizontalDistanceSquared = square(targetGroundLocation.getX() - center.getX()) + square(targetGroundLocation.getZ() - center.getZ());
            if (horizontalDistanceSquared > radiusSquared) {
                continue;
            }
            if (Math.abs(targetGroundLocation.getY() - center.getY()) > verticalRange) {
                continue;
            }
            combatService.applyUltimateDamage(player, target, damage);
            hitCount++;
        }
        if (player.isOp()) {
            player.sendMessage(Component.text("Light Greatsword hit " + hitCount + " target(s).", NamedTextColor.GRAY));
        }
    }

    private void spawnUltimateGroundMark(Location center) {
        Material material = Material.matchMaterial(configService.getString("resource-pack.skill-models.swordmaster.light-greatsword-ground.material", "PAPER"));
        if (material == null || material.isAir()) {
            material = Material.PAPER;
        }
        ItemStack itemStack = new ItemStack(material);
        int customModelData = (int) configService.getLong("resource-pack.skill-models.swordmaster.light-greatsword-ground.custom-model-data", 21003L);
        if (customModelData > 0) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                itemStack.setItemMeta(meta);
            }
        }

        Location markLocation = center.clone().add(0.0D, 0.05D, 0.0D);
        markLocation.setYaw(0.0F);
        markLocation.setPitch(0.0F);
        ItemDisplay markDisplay = center.getWorld().spawn(markLocation, ItemDisplay.class, entity -> {
            entity.setItemStack(itemStack);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setPersistent(false);
            float scale = (float) configService.getDouble("resource-pack.skill-models.swordmaster.light-greatsword-ground.scale", 8.0D);
            entity.setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf().rotateX((float) Math.toRadians(90.0D)),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()
            ));
        });
        temporaryDisplays.add(markDisplay);
        long lifetimeTicks = Math.max(1L, configService.getLong("swordmaster.light-greatsword.ground-mark-duration-ticks", 100L));
        scheduleTemporaryDisplayRemoval(markDisplay, lifetimeTicks);
    }

    private Location findGroundBelow(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int startY = Math.min(world.getMaxHeight() - 1, Math.max(location.getBlockY(), world.getMinHeight()));
        for (int y = startY; y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isSolid()) {
                return new Location(world, x + 0.5D, y + 1.05D, z + 0.5D);
            }
        }
        int highestY = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5D, highestY + 1.05D, z + 0.5D);
    }

    private double square(double value) {
        return value * value;
    }

    private void cleanupUltimate(org.bukkit.scheduler.BukkitRunnable runnable, BukkitTask task, ItemDisplay display) {
        cleanupTemporaryDisplay(runnable, task, display);
    }

    private void finishUltimate(org.bukkit.scheduler.BukkitRunnable runnable, BukkitTask task, ItemDisplay display, long removeDelayTicks) {
        runnable.cancel();
        if (task != null) {
            temporaryTasks.remove(task);
        }
        scheduleTemporaryDisplayRemoval(display, removeDelayTicks);
    }

    private void scheduleTemporaryDisplayRemoval(ItemDisplay display, long delayTicks) {
        AtomicReference<BukkitTask> taskReference = new AtomicReference<>();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            temporaryTasks.remove(taskReference.get());
            removeTemporaryDisplay(display);
        }, Math.max(1L, delayTicks));
        taskReference.set(task);
        temporaryTasks.add(task);
    }

    private void cleanupTemporaryDisplay(org.bukkit.scheduler.BukkitRunnable runnable, BukkitTask task, ItemDisplay display) {
        runnable.cancel();
        if (task != null) {
            temporaryTasks.remove(task);
        }
        removeTemporaryDisplay(display);
    }

    private void removeTemporaryDisplay(ItemDisplay display) {
        temporaryDisplays.remove(display);
        if (!display.isDead()) {
            display.remove();
        }
    }


    private void stabilizeBladeRoll(Quaternionf rotation, Vector3f bladeFaceAxis, Vector3f flightAxis) {
        Vector3f currentFaceAxis = rotation.transform(new Vector3f(bladeFaceAxis));
        Vector3f desiredFaceAxis = projectedUpAxis(flightAxis);
        float dot = clamp(currentFaceAxis.dot(desiredFaceAxis), -1.0F, 1.0F);
        float signedAngle = (float) Math.atan2(new Vector3f(currentFaceAxis).cross(desiredFaceAxis).dot(flightAxis), dot);
        Quaternionf roll = new Quaternionf().rotateAxis(signedAngle, flightAxis.x, flightAxis.y, flightAxis.z);
        roll.mul(rotation, rotation);
    }

    private Vector3f projectedUpAxis(Vector3f flightAxis) {
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f projected = up.sub(new Vector3f(flightAxis).mul(up.dot(flightAxis)));
        if (projected.lengthSquared() < 0.001F) {
            Vector3f fallback = new Vector3f(1.0F, 0.0F, 0.0F);
            projected = fallback.sub(new Vector3f(flightAxis).mul(fallback.dot(flightAxis)));
        }
        return projected.normalize();
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void teleportBladeDisplay(BladeObject blade, Vector direction) {
        Location displayLocation = blade.location().clone();
        displayLocation.setYaw(0.0F);
        displayLocation.setPitch(0.0F);
        blade.displayEntity().teleport(displayLocation);
        applyBladeTransform(blade.displayEntity(), direction);
    }

    private void removeBlade(BladeObject blade) {
        if (!blade.displayEntity().isDead()) {
            blade.displayEntity().remove();
        }
    }
}
