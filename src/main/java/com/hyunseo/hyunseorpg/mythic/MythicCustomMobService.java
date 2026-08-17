package com.hyunseo.hyunseorpg.mythic;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.mob.MobService;
import com.hyunseo.hyunseorpg.mob.MobRegistry;
import com.hyunseo.hyunseorpg.mob.MobTagService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** YAML-driven behavior layer for the four MythicMobs-backed MVP monsters. */
public final class MythicCustomMobService implements Listener {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final MythicMobRegistry registry;
    private final MythicMobIntegrationService mythicIntegration;
    private final MobService mobService;
    private final MobRegistry mobRegistry;
    private final MobTagService mobTagService;
    private final NamespacedKey customMobKey;
    private final NamespacedKey projectileTypeKey;
    private final NamespacedKey projectileSourceKey;
    private final Map<UUID, RuntimeMob> activeMobs = new ConcurrentHashMap<>();
    private final Map<UUID, ProjectileState> projectiles = new ConcurrentHashMap<>();
    private final Map<UUID, FreezeState> freezeStates = new ConcurrentHashMap<>();
    private final Map<UUID, FrozenRootState> frozenRootStates = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTaskHandle> dashTasks = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> dashTargetIds = new ConcurrentHashMap<>();
    private final Set<org.bukkit.scheduler.BukkitTask> tasks = ConcurrentHashMap.newKeySet();
    private final Set<UUID> debugPlayers = ConcurrentHashMap.newKeySet();
    private org.bukkit.scheduler.BukkitTask tickTask;
    private long ticks;

    public MythicCustomMobService(JavaPlugin plugin, ConfigService configService,
                                  MythicMobRegistry registry, MythicMobIntegrationService mythicIntegration,
                                  MobService mobService, MobRegistry mobRegistry) {
        this.plugin = plugin;
        this.configService = configService;
        this.registry = registry;
        this.mythicIntegration = mythicIntegration;
        this.mobService = mobService;
        this.mobRegistry = mobRegistry;
        this.mobTagService = mobService.getMobTagService();
        this.customMobKey = new NamespacedKey(plugin, "mythic_custom_mob");
        this.projectileTypeKey = new NamespacedKey(plugin, "mythic_projectile_type");
        this.projectileSourceKey = new NamespacedKey(plugin, "mythic_projectile_source");
    }

    public void start() {
        if (tickTask != null) return;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (tickTask != null) tickTask.cancel();
        tickTask = null;
        for (org.bukkit.scheduler.BukkitTask task : tasks) task.cancel();
        tasks.clear();
        for (BukkitTaskHandle handle : dashTasks.values()) handle.task().cancel();
        dashTasks.clear();
        dashTargetIds.clear();
        for (UUID id : activeMobs.keySet()) {
            Entity entity = Bukkit.getEntity(id);
            if (entity instanceof LivingEntity living && "riptide_drowned".equals(getCustomMobId(living))) {
                living.setRiptiding(false);
            }
        }
        for (UUID id : projectiles.keySet()) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && !entity.isDead()) entity.remove();
        }
        activeMobs.clear();
        projectiles.clear();
        freezeStates.clear();
        frozenRootStates.clear();
        debugPlayers.clear();
    }

    public List<String> getConfiguredMobIds() {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>(registry.getConfiguredBehaviorMobIds());
        ids.addAll(mobRegistry.getBehaviorMobIds());
        addAliasIfConfigured(ids, "custom_breeze", "explosive_breeze");
        return List.copyOf(ids);
    }

    public boolean isCustomMob(LivingEntity entity) {
        return entity != null && entity.getPersistentDataContainer().has(customMobKey, PersistentDataType.STRING);
    }

    public String getCustomMobId(LivingEntity entity) {
        String stored = entity.getPersistentDataContainer().getOrDefault(customMobKey, PersistentDataType.STRING, "");
        String canonical = canonicalId(stored);
        if (!canonical.equals(stored) && !canonical.isBlank()) {
            entity.getPersistentDataContainer().set(customMobKey, PersistentDataType.STRING, canonical);
        }
        return canonical;
    }

    public Optional<LivingEntity> spawnConfigured(Location location, String rawId, Integer levelOverride) {
        String id = canonicalId(rawId);
        ConfigurationSection section = findConfig(id);
        if (section == null) return Optional.empty();
        EntityType type = parseEntityType(section.getString("base-type", section.getString("vanilla-type", ""))).orElse(null);
        if (type == null || !type.isAlive() || location.getWorld() == null) return Optional.empty();

        LivingEntity entity = (registry.get(id).isPresent() ? mythicIntegration.spawn(id, location) : Optional.<LivingEntity>empty()).orElseGet(() -> {
            Entity spawned = location.getWorld().spawnEntity(location, type);
            return spawned instanceof LivingEntity living ? living : null;
        });
        if (entity == null) return Optional.empty();

        int level = Math.max(1, levelOverride == null ? section.getInt("level", 1) : levelOverride);
        String display = section.getString("display-name", id);
        List<String> tags = new ArrayList<>(section.getStringList("tags"));
        tags.add("mythic");
        tags.add("custom");
        if (section.getBoolean("elite", false)) tags.add("elite");
        if (section.getBoolean("boss", false)) tags.add("boss");
        mobService.markAsRpgMob(entity, level, id, display, tags);
        mobTagService.markCustomMob(entity, id, "MYTHIC", section.getString("drop-table", ""));
        entity.getPersistentDataContainer().set(customMobKey, PersistentDataType.STRING, id);
        entity.getPersistentDataContainer().set(new NamespacedKey(plugin, "mythic_mob_id"), PersistentDataType.STRING, id);
        applyStats(entity, section, level);
        if ("riptide_drowned".equals(id)) entity.setRiptiding(false);
        entity.setGlowing(false);
        activeMobs.put(entity.getUniqueId(), new RuntimeMob(id, 0L, 0L));
        debug("spawn " + id + " uuid=" + entity.getUniqueId());
        return Optional.of(entity);
    }

    public int clearCustomMobs() {
        int count = 0;
        for (UUID id : new ArrayList<>(activeMobs.keySet())) {
            Entity entity = Bukkit.getEntity(id);
            if (entity instanceof LivingEntity living && "riptide_drowned".equals(getCustomMobId(living))) {
                living.setRiptiding(false);
            }
            if (entity != null && !entity.isDead()) { entity.remove(); count++; }
            activeMobs.remove(id);
        }
        for (UUID sourceId : new ArrayList<>(dashTasks.keySet())) {
            Entity entity = Bukkit.getEntity(sourceId);
            if (entity instanceof LivingEntity living) cancelDash(living);
        }
        return count;
    }

    public void setDebug(Player player, boolean enabled) {
        if (enabled) debugPlayers.add(player.getUniqueId()); else debugPlayers.remove(player.getUniqueId());
        player.sendMessage((enabled ? "Mythic custom mob debug enabled." : "Mythic custom mob debug disabled."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onNaturalSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        Location location = event.getLocation();
        for (String id : canonicalConfiguredMobIds()) {
            ConfigurationSection section = findConfig(id);
            if (section == null || !section.getBoolean("spawn.natural.enabled", false)) continue;
            if (parseEntityType(section.getString("base-type", section.getString("vanilla-type", ""))).orElse(null) != event.getEntityType()) continue;
            if (!isAllowedSpawn(location, section) || ThreadLocalRandom.current().nextDouble() > section.getDouble("spawn.natural.chance", 0.0D)) continue;
            int nearby = 0;
            for (UUID uuid : activeMobs.keySet()) {
                Entity tracked = Bukkit.getEntity(uuid);
                if (tracked instanceof LivingEntity living && living.getWorld().equals(location.getWorld())
                        && living.getLocation().distanceSquared(location) <= 32.0D * 32.0D
                        && id.equals(getCustomMobId(living))) nearby++;
            }
            if (nearby >= section.getInt("spawn.natural.max-nearby", 16)) continue;
            event.setCancelled(true);
            spawnConfigured(location, id, null);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (isTrackedProjectile(projectile)) return;
        if (projectile.getShooter() instanceof LivingEntity source && isCustomMob(source)
                && config(source).getBoolean("behavior.disable-vanilla-projectiles", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && isCustomMob(entity)
                && config(entity).getBoolean("behavior.disable-vanilla-fire", true)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileState state = projectiles.remove(projectile.getUniqueId());
        if (state == null) return;
        event.setCancelled(true);
        Entity sourceEntity = Bukkit.getEntity(state.sourceId());
        if (!(sourceEntity instanceof LivingEntity source)) { projectile.remove(); return; }
        ConfigurationSection section = config(source);
        String type = state.type();
        if ("frost-snowball".equals(type)) {
            if (event.getHitEntity() instanceof Player player) applyFrostHit(source, player, section);
        } else if ("breeze-orb".equals(type)) {
            explodeBreezeOrb(source, projectile.getLocation(), section);
        }
        projectile.remove();
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        activeMobs.remove(entity.getUniqueId());
        BukkitTaskHandle handle = dashTasks.remove(entity.getUniqueId());
        if (handle != null) handle.task().cancel();
        dashTargetIds.remove(entity.getUniqueId());
        if ("riptide_drowned".equals(getCustomMobId(entity))) entity.setRiptiding(false);
        debug("death " + getCustomMobId(entity));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        freezeStates.remove(event.getPlayer().getUniqueId());
        frozenRootStates.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        freezeStates.remove(event.getPlayer().getUniqueId());
        frozenRootStates.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        freezeStates.remove(event.getPlayer().getUniqueId());
        frozenRootStates.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        FrozenRootState state = frozenRootStates.get(event.getPlayer().getUniqueId());
        if (state == null) return;
        if (state.expiresAt() <= ticks || !event.getPlayer().getWorld().equals(state.origin().getWorld())) {
            frozenRootStates.remove(event.getPlayer().getUniqueId());
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (state.lockHorizontal() && (to.getX() != from.getX() || to.getZ() != from.getZ())) {
            Location locked = from.clone();
            locked.setYaw(to.getYaw());
            locked.setPitch(to.getPitch());
            if (!state.lockVertical()) locked.setY(to.getY());
            event.setTo(locked);
        } else if (state.lockVertical() && to.getY() != from.getY()) {
            Location locked = to.clone();
            locked.setY(from.getY());
            event.setTo(locked);
        }
    }

    private void tick() {
        ticks++;
        for (Map.Entry<UUID, RuntimeMob> entry : new ArrayList<>(activeMobs.entrySet())) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || entity.isDead() || !entity.isValid()) {
                activeMobs.remove(entry.getKey());
                continue;
            }
            RuntimeMob state = entry.getValue();
            ConfigurationSection section = config(living);
            Player target = nearestPlayer(living, section.getDouble("behavior.recognition-range", 24.0D));
            String id = getCustomMobId(living);
            if (target == null) continue;
            if (state.busyUntil() > ticks || state.nextAttack() > ticks) continue;
            if ("frost_blaze".equals(id)) {
                beginFrostAttack(living, target, section, state);
            } else if ("explosive_breeze".equals(id)) {
                fireBreezeOrb(living, target, section);
                activeMobs.put(living.getUniqueId(), state.withNext(ticks + secondsToTicks(section.getDouble("behavior.attack-interval", 4.0D))));
            } else if ("riptide_drowned".equals(id)) {
                if (startDash(living, target, section)) {
                    activeMobs.put(living.getUniqueId(), state.withNext(ticks + secondsToTicks(section.getDouble("behavior.cooldown", 7.0D))));
                }
            }
        }
        for (Map.Entry<UUID, ProjectileState> entry : new ArrayList<>(projectiles.entrySet())) {
            if ("breeze-orb".equals(entry.getValue().type())) {
                Entity entity = Bukkit.getEntity(entry.getKey());
                if (entity instanceof Projectile projectile && !projectile.isDead()) {
                    projectile.setVelocity(entry.getValue().velocity().clone());
                }
            }
            if (entry.getValue().expiresAt() <= ticks) {
                Entity projectile = Bukkit.getEntity(entry.getKey());
                if (projectile != null && !projectile.isDead()) projectile.remove();
                projectiles.remove(entry.getKey());
            }
        }
        for (Map.Entry<UUID, FreezeState> entry : new ArrayList<>(freezeStates.entrySet())) {
            if (entry.getValue().expiresAt() <= ticks) freezeStates.remove(entry.getKey());
        }
        for (Map.Entry<UUID, FrozenRootState> entry : new ArrayList<>(frozenRootStates.entrySet())) {
            if (entry.getValue().expiresAt() <= ticks) frozenRootStates.remove(entry.getKey());
        }
    }

    private void fireFrostSnowball(LivingEntity source, Player target, ConfigurationSection section) {
        Location start = source.getEyeLocation().add(source.getEyeLocation().getDirection().normalize().multiply(0.7D));
        Snowball ball = source.getWorld().spawn(start, Snowball.class);
        markProjectile(ball, "frost-snowball", source, section.getDouble("behavior.projectile-speed", 1.6D), target.getLocation().add(0, 1, 0));
        source.getWorld().playSound(source.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.7F, 1.6F);
    }

    private void beginFrostAttack(LivingEntity source, Player target, ConfigurationSection section, RuntimeMob state) {
        long interval = secondsToTicks(section.getDouble("behavior.attack-interval", 2.5D));
        long chargeTicks = Math.max(1L, section.getLong("behavior.charge-ticks", 20L));
        ConfigurationSection volley = section.getConfigurationSection("behavior.volley");
        int count = Math.max(1, volley == null ? 1 : volley.getInt("count", 1));
        long volleyInterval = Math.max(1L, volley == null ? 6L : volley.getLong("interval-ticks", 6L));
        long busy = chargeTicks + Math.max(0, count - 1) * volleyInterval;
        activeMobs.put(source.getUniqueId(), state.withNext(ticks + interval).withBusy(ticks + busy));
        source.getWorld().playSound(source.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.65F, 1.35F);
        source.getWorld().spawnParticle(Particle.FLAME, source.getEyeLocation(),
                Math.max(1, section.getInt("behavior.charge-particle-count", 8)), 0.25, 0.25, 0.25, 0.02);
        for (int index = 0; index < count; index++) {
            long delay = chargeTicks + index * volleyInterval;
            org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!source.isDead() && source.isValid() && target.isOnline() && !target.isDead()
                        && source.getWorld().equals(target.getWorld())) {
                    fireFrostSnowball(source, target, section);
                }
            }, delay);
            tasks.add(task);
        }
    }

    private void fireBreezeOrb(LivingEntity source, Player target, ConfigurationSection section) {
        Location start = source.getEyeLocation().add(source.getEyeLocation().getDirection().normalize().multiply(0.6D));
        WindCharge orb = source.getWorld().spawn(start, WindCharge.class);
        markProjectile(orb, "breeze-orb", source, section.getDouble("behavior.projectile-speed", 0.9D), target.getLocation().add(0, 1, 0));
        source.getWorld().playSound(source.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.7F, 0.8F);
    }

    private void markProjectile(Projectile projectile, String type, LivingEntity source, double speed, Location target) {
        projectile.getPersistentDataContainer().set(projectileTypeKey, PersistentDataType.STRING, type);
        projectile.getPersistentDataContainer().set(projectileSourceKey, PersistentDataType.STRING, source.getUniqueId().toString());
        projectile.setShooter(source);
        Vector direction = target.toVector().subtract(projectile.getLocation().toVector()).normalize();
        projectile.setVelocity(direction.multiply(Math.max(0.1D, speed)));
        projectiles.put(projectile.getUniqueId(), new ProjectileState(
                source.getUniqueId(), type, ticks + 100L, direction.multiply(Math.max(0.01D, speed))));
    }

    private void applyFrostHit(LivingEntity source, Player player, ConfigurationSection section) {
        if (isShieldingSource(player, source)) return;
        double damage = section.getDouble("behavior.projectile-damage", 3.0D);
        player.damage(damage, source);
        FreezeState old = freezeStates.get(player.getUniqueId());
        int stack = old == null ? 1 : old.stack() + 1;
        int max = Math.max(1, section.getInt("behavior.freeze.max-stacks", 4));
        long timeout = ticks + secondsToTicks(section.getDouble("behavior.freeze.stack-timeout", 6.0D));
        if (stack >= max) {
            double visualDuration = section.getDouble("behavior.freeze.visual-freeze-seconds",
                    section.getDouble("behavior.freeze.duration", 1.2D));
            double rootDuration = section.getDouble("behavior.freeze.root-duration-seconds", visualDuration);
            player.setFreezeTicks((int) Math.round(visualDuration * 20.0D));
            frozenRootStates.put(player.getUniqueId(), new FrozenRootState(
                    player.getLocation().clone(),
                    ticks + secondsToTicks(rootDuration),
                    section.getBoolean("behavior.freeze.lock-horizontal", true),
                    section.getBoolean("behavior.freeze.lock-vertical", false)));
            freezeStates.remove(player.getUniqueId());
        } else {
            freezeStates.put(player.getUniqueId(), new FreezeState(stack, timeout));
        }
    }

    private boolean isShieldingSource(Player player, LivingEntity source) {
        if (!player.isBlocking()) return false;
        Vector toSource = source.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();
        return player.getEyeLocation().getDirection().normalize().dot(toSource) > 0.1D;
    }

    private void explodeBreezeOrb(LivingEntity source, Location location, ConfigurationSection section) {
        double radius = section.getDouble("behavior.explosion.radius", 2.5D);
        double damage = section.getDouble("behavior.explosion.damage", 5.0D);
        location.getWorld().spawnParticle(Particle.EXPLOSION, location, 1);
        for (Player player : playersNear(location, radius)) {
            player.damage(damage, source);
            knockback(player, location, section.getDouble("behavior.explosion.knockback", 0.8D));
        }
    }

    private boolean startDash(LivingEntity source, Player target, ConfigurationSection section) {
        double distance = source.getLocation().distance(target.getLocation());
        if (distance < section.getDouble("behavior.dash.min-distance", 5.0D)
                || distance > section.getDouble("behavior.dash.max-distance", 18.0D)) return false;
        if (dashTasks.containsKey(source.getUniqueId())) return false;
        long warning = Math.max(1L, Math.round(section.getDouble("behavior.dash.warning-seconds", 0.8D) * 20.0D));
        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (source.isDead() || !source.isValid() || target.isDead() || !target.isOnline()
                    || !source.getWorld().equals(target.getWorld())) {
                cancelDash(source);
                return;
            }
            Vector direction = target.getLocation().add(0, 0.5, 0).toVector().subtract(source.getLocation().toVector()).normalize();
            if (source.getLocation().getBlock().getType().isSolid()) direction.setY(Math.min(direction.getY(), 0.35D));
            dashTargetIds.put(source.getUniqueId(), target.getUniqueId());
            runDash(source, target, direction, section);
        }, warning);
        tasks.add(task);
        dashTasks.put(source.getUniqueId(), new BukkitTaskHandle(task));
        return true;
    }

    private void runDash(LivingEntity source, Player target, Vector direction, ConfigurationSection section) {
        double speed = section.getDouble("behavior.dash.speed", 1.0D);
        double maxDistance = section.getDouble("behavior.dash.max-travel", 16.0D);
        double damage = section.getDouble("behavior.dash.damage", 7.0D);
        double knockback = section.getDouble("behavior.dash.knockback", 0.8D);
        Location origin = source.getLocation().clone();
        Set<UUID> hit = new HashSet<>();
        final int[] elapsed = {0};
        source.setRiptiding(true);
        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            elapsed[0]++;
            if (source.isDead() || !source.isValid() || target.isDead() || !target.isOnline()
                    || !source.getWorld().equals(target.getWorld())
                    || source.getLocation().distance(origin) >= maxDistance || elapsed[0] > 30
                    || source.getLocation().getBlock().getType().isSolid()) {
                cancelDash(source);
                return;
            }
            source.setVelocity(direction.clone().multiply(speed));
            for (Player player : playersNear(source.getLocation(), 1.25D)) {
                if (hit.add(player.getUniqueId())) {
                    player.damage(damage, source);
                    knockback(player, source.getLocation(), knockback);
                }
            }
        }, 0L, 1L);
        tasks.add(task);
        dashTasks.put(source.getUniqueId(), new BukkitTaskHandle(task));
    }

    private void cancelDash(LivingEntity source) {
        BukkitTaskHandle handle = dashTasks.remove(source.getUniqueId());
        if (handle != null) { handle.task().cancel(); tasks.remove(handle.task()); }
        dashTargetIds.remove(source.getUniqueId());
        if ("riptide_drowned".equals(getCustomMobId(source))) source.setRiptiding(false);
        source.setVelocity(new Vector());
    }

    private void applyStats(LivingEntity entity, ConfigurationSection section, int level) {
        double multiplier = 1.0D + Math.max(0.0D, level - 1) * section.getDouble("stats.level-multiplier", 0.0D);
        setAttribute(entity, Attribute.MAX_HEALTH, section.getDouble("stats.max-health", 0.0D) * multiplier, true);
        setAttribute(entity, Attribute.ATTACK_DAMAGE, section.getDouble("stats.attack-damage", 0.0D) * multiplier, false);
        setAttribute(entity, Attribute.MOVEMENT_SPEED, section.getDouble("stats.movement-speed", 0.0D), false);
        setAttribute(entity, Attribute.ARMOR, section.getDouble("stats.armor", 0.0D), false);
    }

    private void setAttribute(LivingEntity entity, Attribute attribute, double value, boolean setHealth) {
        if (value <= 0.0D) return;
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        instance.setBaseValue(value);
        if (setHealth) entity.setHealth(Math.min(value, instance.getValue()));
    }

    private boolean isAllowedSpawn(Location location, ConfigurationSection section) {
        Block current = location.getBlock();
        Block below = current.getRelative(0, -1, 0);
        if (current.isLiquid() || below.isLiquid()) return false;
        if (section.getBoolean("spawn.natural.requires-ground", true) && !below.getType().isSolid()) return false;
        if (!section.getBoolean("spawn.natural.requires-ground", true) && current.getType().isSolid()) return false;
        if (location.getY() < section.getDouble("spawn.natural.min-y", -64.0D) || location.getY() > section.getDouble("spawn.natural.max-y", 320.0D)) return false;
        List<String> worlds = section.getStringList("spawn.natural.worlds.allow");
        List<String> deniedWorlds = section.getStringList("spawn.natural.worlds.deny");
        String worldName = location.getWorld().getName();
        if (!worlds.isEmpty() && worlds.stream().noneMatch(value -> value.equalsIgnoreCase(worldName))) return false;
        if (deniedWorlds.stream().anyMatch(value -> value.equalsIgnoreCase(worldName))) return false;
        double minPlayerDistance = Math.max(0.0D, section.getDouble("spawn.natural.min-player-distance", 0.0D));
        double maxPlayerDistance = section.getDouble("spawn.natural.max-player-distance", -1.0D);
        if (minPlayerDistance > 0.0D || maxPlayerDistance >= 0.0D) {
            boolean inRange = false;
            for (Player player : location.getWorld().getPlayers()) {
                double distance = player.getLocation().distance(location);
                if (distance >= minPlayerDistance && (maxPlayerDistance < 0.0D || distance <= maxPlayerDistance)) {
                    inRange = true;
                    break;
                }
            }
            if (!inRange) return false;
        }
        String biome = location.getBlock().getBiome().name().toUpperCase(Locale.ROOT);
        List<String> allow = section.getStringList("spawn.natural.biomes.allow");
        List<String> deny = section.getStringList("spawn.natural.biomes.deny");
        if (!allow.isEmpty() && allow.stream().noneMatch(value -> value.equalsIgnoreCase(biome))) return false;
        if (deny.stream().anyMatch(value -> value.equalsIgnoreCase(biome))) return false;
        return true;
    }

    private Player nearestPlayer(LivingEntity source, double range) {
        Player nearest = null;
        double closest = range * range;
        for (Entity entity : source.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof Player player) || player.isDead() || !player.isValid()) continue;
            double distance = source.getLocation().distanceSquared(player.getLocation());
            if (distance < closest) { closest = distance; nearest = player; }
        }
        return nearest;
    }

    private List<Player> playersNear(Location location, double radius) {
        return location.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
                .filter(Player.class::isInstance).map(Player.class::cast).filter(player -> !player.isDead()).toList();
    }

    private void knockback(Player player, Location source, double amount) {
        Vector direction = player.getLocation().toVector().subtract(source.toVector()).normalize();
        direction.setY(Math.max(0.15D, direction.getY()));
        player.setVelocity(player.getVelocity().add(direction.multiply(amount)));
    }

    private boolean isTrackedProjectile(Projectile projectile) {
        return projectile.getPersistentDataContainer().has(projectileTypeKey, PersistentDataType.STRING);
    }

    private ConfigurationSection config(LivingEntity entity) {
        ConfigurationSection section = findConfig(getCustomMobId(entity));
        if (section == null) {
            throw new IllegalStateException("Missing Mythic custom mob config: " + getCustomMobId(entity));
        }
        return section;
    }

    private ConfigurationSection findConfig(String id) {
        String canonical = canonicalId(id);
        if (registry.getSection(canonical).isPresent()) return registry.getSection(canonical).get();
        return mobRegistry.getSection(canonical).orElse(null);
    }

    private long secondsToTicks(double seconds) { return Math.max(1L, Math.round(seconds * 20.0D)); }
    private String normalize(String id) { return id == null ? "" : id.trim().toLowerCase(Locale.ROOT); }
    private String canonicalId(String id) {
        return "custom_breeze".equals(normalize(id)) ? "explosive_breeze" : normalize(id);
    }
    private List<String> canonicalConfiguredMobIds() {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>(registry.getConfiguredBehaviorMobIds());
        ids.addAll(mobRegistry.getBehaviorMobIds());
        return ids.stream()
                .map(this::canonicalId)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)).stream().toList();
    }
    private void addAliasIfConfigured(Set<String> ids, String alias, String canonical) {
        if (ids.contains(canonical)) ids.add(alias);
    }
    private java.util.Optional<EntityType> parseEntityType(String value) {
        try { return java.util.Optional.of(EntityType.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'))); }
        catch (RuntimeException ignored) { return java.util.Optional.empty(); }
    }
    private void debug(String message) {
        if (debugPlayers.isEmpty()) return;
        plugin.getLogger().fine("[MythicCustomMob] " + message);
    }

    private record RuntimeMob(String id, long nextAttack, long busyUntil) {
        RuntimeMob withNext(long value) { return new RuntimeMob(id, value, busyUntil); }
        RuntimeMob withBusy(long value) { return new RuntimeMob(id, nextAttack, value); }
    }
    private record ProjectileState(UUID sourceId, String type, long expiresAt, Vector velocity) { }
    private record FreezeState(int stack, long expiresAt) { }
    private record FrozenRootState(Location origin, long expiresAt, boolean lockHorizontal, boolean lockVertical) { }
    private record BukkitTaskHandle(org.bukkit.scheduler.BukkitTask task) { }
}
