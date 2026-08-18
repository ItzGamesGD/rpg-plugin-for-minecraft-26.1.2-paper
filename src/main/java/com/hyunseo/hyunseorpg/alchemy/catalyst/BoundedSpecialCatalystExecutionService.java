package com.hyunseo.hyunseorpg.alchemy.catalyst;

import com.hyunseo.hyunseorpg.alchemy.EffectContext;
import com.hyunseo.hyunseorpg.alchemy.EffectService;
import com.hyunseo.hyunseorpg.alchemy.EffectSourceType;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionDefinition;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Executes bounded special deliveries through the normal EffectService. */
public final class BoundedSpecialCatalystExecutionService implements SpecialCatalystExecution, Listener {
    private final SpecialCatalystRegistry registry;
    private final int maxExecutions;
    private final JavaPlugin plugin;
    private final PotionRegistry potions;
    private final EffectService effects;
    private final Map<UUID, Request> active = new HashMap<>();
    private final Map<UUID, RuntimeState> runtimes = new HashMap<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private final org.bukkit.NamespacedKey executionKey;

    public BoundedSpecialCatalystExecutionService(SpecialCatalystRegistry registry, int maxExecutions) {
        this(null, registry, null, null, maxExecutions);
    }

    public BoundedSpecialCatalystExecutionService(JavaPlugin plugin, SpecialCatalystRegistry registry,
                                                  PotionRegistry potions, EffectService effects,
                                                  int maxExecutions) {
        this.plugin = plugin;
        this.registry = registry;
        this.potions = potions;
        this.effects = effects;
        this.maxExecutions = Math.max(1, maxExecutions);
        this.executionKey = plugin == null ? null : new org.bukkit.NamespacedKey(plugin, "alchemy_special_execution");
    }

    @Override
    public synchronized Result execute(Request request) {
        if (request == null || request.executionId() == null || request.worldId() == null) {
            return Result.REJECTED_POLICY;
        }
        SpecialCatalystDefinition definition = registry.find(request.catalystId()).orElse(null);
        if (definition == null) return Result.REJECTED_NOT_REGISTERED;
        if (!definition.enabled()) return Result.REJECTED_POLICY;
        if (active.containsKey(request.executionId())) return Result.REJECTED_DUPLICATE;
        if (active.size() >= maxExecutions) return Result.REJECTED_LIMIT;
        active.put(request.executionId(), request);
        return Result.STARTED;
    }

    /** Starts a real delivery for a canonical potion used by an online player. */
    public synchronized Result start(UUID sourceId, String potionId, String catalystId) {
        if (plugin == null || potions == null || effects == null || sourceId == null) return Result.REJECTED_POLICY;
        Player source = Bukkit.getPlayer(sourceId);
        PotionDefinition potion = potions.find(potionId).orElse(null);
        SpecialCatalystDefinition definition = registry.find(catalystId).orElse(null);
        if (source == null || !source.isOnline() || potion == null || !potion.enabled()
                || definition == null || !definition.enabled()) return Result.REJECTED_POLICY;
        if (definition.kind() == SpecialCatalystDefinition.Kind.SLIME
                || definition.kind() == SpecialCatalystDefinition.Kind.ECHO) return Result.REJECTED_POLICY;

        UUID executionId = UUID.randomUUID();
        Request request = new Request(executionId, potion.id(), definition.catalystId(),
                source.getWorld().getUID(), Set.of(sourceId), sourceId);
        Result result = execute(request);
        if (result != Result.STARTED) return result;
        RuntimeState state = new RuntimeState(request, potion, definition, source.getLocation().clone());
        runtimes.put(executionId, state);
        schedule(state);
        return result;
    }

    /** Handles the initial and subsequent splash events for the bounded slime bounce delivery. */
    public synchronized void handleSlimeSplash(ThrownPotion potionEntity, String potionId,
                                                UUID sourceId, Collection<LivingEntity> affected) {
        if (plugin == null || potions == null || effects == null || potionEntity == null
                || potionId == null || potionId.isBlank() || executionKey == null) return;

        String raw = potionEntity.getPersistentDataContainer().get(executionKey, PersistentDataType.STRING);
        RuntimeState state = raw == null ? null : runtimes.get(parseUuid(raw));
        if (state != null) {
            if (!state.processedProjectiles.add(potionEntity.getUniqueId())) return;
            processSlimeSplash(state, potionEntity, affected);
            return;
        }

        SpecialCatalystDefinition definition = registry.find("slime").orElse(null);
        PotionDefinition potion = potions.find(potionId).orElse(null);
        if (definition == null || !definition.enabled() || definition.kind() != SpecialCatalystDefinition.Kind.SLIME
                || potion == null || !potion.enabled()) return;

        UUID executionId = UUID.randomUUID();
        Request request = new Request(executionId, potion.id(), definition.catalystId(),
                potionEntity.getWorld().getUID(), Set.of(), sourceId);
        if (execute(request) != Result.STARTED) return;
        state = new RuntimeState(request, potion, definition, potionEntity.getLocation().clone());
        state.bounceCount = 1;
        state.processedProjectiles.add(potionEntity.getUniqueId());
        runtimes.put(executionId, state);
        tasks.put(executionId, Bukkit.getScheduler().runTaskLater(plugin,
                () -> finish(executionId), Math.max(20L,
                        (long) definition.lifetimeTicks() * definition.maxCount() + 20L)));
        processSlimeSplash(state, potionEntity, affected);
    }

    /** Applies echo twice to the exact entities affected by one real splash collision. */
    public synchronized void handleEchoSplash(String potionId, UUID sourceId,
                                               Collection<LivingEntity> affected) {
        if (plugin == null || potions == null || effects == null || potionId == null || potionId.isBlank()) return;
        SpecialCatalystDefinition definition = registry.find("echo_shard").orElse(null);
        PotionDefinition potion = potions.find(potionId).orElse(null);
        if (definition == null || !definition.enabled() || definition.kind() != SpecialCatalystDefinition.Kind.ECHO
                || potion == null || !potion.enabled()) return;

        UUID executionId = UUID.randomUUID();
        Player source = sourceId == null ? null : Bukkit.getPlayer(sourceId);
        UUID worldId = source == null ? null : source.getWorld().getUID();
        if (worldId == null && affected != null) {
            worldId = affected.stream().filter(entity -> entity != null && entity.getWorld() != null)
                    .map(entity -> entity.getWorld().getUID()).findFirst().orElse(null);
        }
        if (worldId == null) return;
        Request request = new Request(executionId, potion.id(), definition.catalystId(), worldId, Set.of(), sourceId);
        if (execute(request) != Result.STARTED) return;
        Location origin = source == null && affected != null && !affected.isEmpty()
                ? affected.iterator().next().getLocation().clone()
                : source == null ? null : source.getLocation().clone();
        if (origin == null) { active.remove(executionId); return; }
        RuntimeState state = new RuntimeState(request, potion, definition, origin);
        if (affected != null) {
            for (LivingEntity target : affected) {
                if (target == null || target.isDead() || !target.isValid()
                        || (sourceId != null && sourceId.equals(target.getUniqueId()))) continue;
                if (apply(state, target)) state.visited.add(target.getUniqueId());
            }
        }
        if (state.visited.isEmpty()) { active.remove(executionId); return; }
        runtimes.put(executionId, state);
        tasks.put(executionId, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            synchronized (BoundedSpecialCatalystExecutionService.this) {
                RuntimeState current = runtimes.get(executionId);
                if (current == null) return;
                for (UUID targetId : new HashSet<>(current.visited)) {
                    Entity entity = Bukkit.getEntity(targetId);
                    if (entity instanceof LivingEntity target && target.isValid() && !target.isDead()
                            && target.getWorld().equals(current.origin.getWorld())) apply(current, target);
                }
                finish(executionId);
            }
        }, definition.delayTicks()));
    }

    private void processSlimeSplash(RuntimeState state, ThrownPotion potionEntity,
                                     Collection<LivingEntity> affected) {
        if (state == null || !active.containsKey(state.request.executionId())) return;
        if (affected != null) {
            for (LivingEntity target : affected) {
                if (target != null && !target.isDead() && target.isValid()) apply(state, target);
            }
        }
        if (state.bounceCount >= state.definition.maxCount()) {
            finish(state.request.executionId());
            return;
        }
        spawnSlimeBounce(state, potionEntity);
    }

    private void spawnSlimeBounce(RuntimeState state, ThrownPotion previous) {
        Location location = previous.getLocation().clone().add(0.0D, 0.18D, 0.0D);
        World world = location.getWorld();
        if (world == null) { finish(state.request.executionId()); return; }
        ThrownPotion bounce = world.spawn(location, ThrownPotion.class);
        bounce.setItem(previous.getItem().clone());
        Player source = source(state);
        if (source != null) bounce.setShooter(source);
        Vector velocity = previous.getVelocity().clone();
        if (velocity.lengthSquared() < 0.01D) {
            velocity = source == null ? new Vector(0.0D, 0.45D, 0.0D)
                    : source.getLocation().getDirection().normalize();
        }
        velocity.setY(Math.max(0.28D, Math.abs(velocity.getY()) * 0.72D + 0.18D));
        velocity.setX(velocity.getX() * 0.78D);
        velocity.setZ(velocity.getZ() * 0.78D);
        bounce.setVelocity(velocity);
        bounce.getPersistentDataContainer().set(executionKey, PersistentDataType.STRING,
                state.request.executionId().toString());
        state.bounceProjectiles.add(bounce.getUniqueId());
        state.bounceCount++;
    }

    @Override public synchronized void cancel(UUID executionId, CancelReason reason) { finish(executionId); }

    @Override
    public synchronized void cancelWorld(UUID worldId, CancelReason reason) {
        if (worldId == null) return;
        new ArrayList<>(active.entrySet()).stream()
                .filter(entry -> worldId.equals(entry.getValue().worldId()))
                .map(Map.Entry::getKey).forEach(this::finish);
    }

    public synchronized void cancelPlayer(UUID playerId, CancelReason reason) {
        if (playerId == null) return;
        new ArrayList<>(active.entrySet()).stream()
                .filter(entry -> playerId.equals(entry.getValue().sourceId())
                        || entry.getValue().visitedTargets().contains(playerId))
                .map(Map.Entry::getKey).forEach(this::finish);
    }

    public synchronized void cancelAll(CancelReason reason) {
        new ArrayList<>(active.keySet()).forEach(this::finish);
    }

    public synchronized int activeCount() { return active.size(); }

    private void schedule(RuntimeState state) {
        SpecialCatalystDefinition definition = state.definition;
        switch (definition.kind()) {
            case SCULK -> {
                spawnCloud(state, state.origin);
                tasks.put(state.request.executionId(), Bukkit.getScheduler().runTaskTimer(plugin,
                        () -> tickSculk(state.request.executionId()), 1L, 5L));
            }
            case ECHO -> { /* Driven by PotionSplashEvent via handleEchoSplash. */ }
            case FIREBALL -> launchFireball(state);
            case WIND_CHARGE -> tasks.put(state.request.executionId(), Bukkit.getScheduler().runTaskTimer(plugin,
                    new Runnable() {
                        private int ticks;
                        private final Location current = state.origin.clone();
                        private final Vector direction = state.origin.getDirection().normalize();
                        @Override public void run() {
                            ticks++;
                            if (ticks > definition.lifetimeTicks()
                                    || current.distance(state.origin) > definition.maxDistance()) {
                                finish(state.request.executionId());
                                return;
                            }
                            current.add(direction.clone().multiply(0.75D));
                            LivingEntity target = findNext(current, state.visited, 1.5D);
                            if (target != null) {
                                state.visited.add(target.getUniqueId());
                                apply(state, target);
                                finish(state.request.executionId());
                            }
                        }
                    }, 1L, 1L));
            case SLIME -> { /* Driven by PotionSplashEvent via handleSlimeSplash. */ }
        }
    }

    private void tickSculk(UUID executionId) {
        RuntimeState state = runtimes.get(executionId);
        if (state == null || state.origin.getWorld() == null) { finish(executionId); return; }
        if (state.spawnedClouds >= state.definition.maxCount()) { finish(executionId); return; }
        boolean propagated = false;
        for (UUID cloudId : new HashSet<>(state.clouds)) {
            Entity entity = Bukkit.getEntity(cloudId);
            if (!(entity instanceof AreaEffectCloud cloud) || !cloud.isValid()) {
                state.clouds.remove(cloudId);
                continue;
            }
            for (Entity nearby : cloud.getWorld().getNearbyEntities(cloud.getLocation(),
                    state.definition.radius(), state.definition.radius(), state.definition.radius())) {
                if (!(nearby instanceof LivingEntity target) || target.isDead() || !target.isValid()) continue;
                if (!state.visited.add(target.getUniqueId())) continue;
                if (state.origin.getWorld() != target.getWorld()
                        || state.origin.distance(target.getLocation()) > state.definition.maxTotalDistance()) continue;
                propagated = true;
                apply(state, target, attenuation(state, cloud.getLocation(), target.getLocation()));
                if (state.spawnedClouds < state.definition.maxCount()) spawnCloud(state, target.getLocation());
            }
        }
        if (!propagated) finish(executionId);
    }

    private AreaEffectCloud spawnCloud(RuntimeState state, Location location) {
        if (location == null || location.getWorld() == null
                || state.spawnedClouds >= state.definition.maxCount()) return null;
        AreaEffectCloud cloud = location.getWorld().spawn(location, AreaEffectCloud.class);
        cloud.setRadius((float) state.definition.radius());
        cloud.setDuration(Math.max(20, state.definition.lifetimeTicks()));
        cloud.setWaitTime(0);
        cloud.setDurationOnUse(0);
        cloud.setRadiusOnUse(0.0F);
        cloud.setReapplicationDelay(10);
        if (executionKey != null) cloud.getPersistentDataContainer().set(executionKey,
                PersistentDataType.STRING, state.request.executionId().toString());
        state.clouds.add(cloud.getUniqueId());
        state.spawnedClouds++;
        return cloud;
    }

    private void launchFireball(RuntimeState state) {
        Player source = source(state);
        if (source == null || executionKey == null) { finish(state.request.executionId()); return; }
        SmallFireball fireball = source.launchProjectile(SmallFireball.class);
        fireball.setIsIncendiary(false);
        fireball.setYield(0.0F);
        fireball.getPersistentDataContainer().set(executionKey, PersistentDataType.STRING,
                state.request.executionId().toString());
        state.projectileId = fireball.getUniqueId();
        tasks.put(state.request.executionId(), Bukkit.getScheduler().runTaskLater(plugin,
                () -> finish(state.request.executionId()), Math.max(20, state.definition.lifetimeTicks())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof SmallFireball fireball) || executionKey == null) return;
        String raw = fireball.getPersistentDataContainer().get(executionKey, PersistentDataType.STRING);
        if (raw == null) return;
        RuntimeState state = runtimes.get(parseUuid(raw));
        if (state == null) return;
        Location impact = event.getHitEntity() instanceof LivingEntity target
                ? target.getLocation() : fireball.getLocation();
        deliverFireball(state, impact);
        finish(state.request.executionId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFireballDirectDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof SmallFireball fireball && hasExecutionIdentity(fireball)) {
            event.setDamage(0.0D);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFireballExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof SmallFireball fireball) || !hasExecutionIdentity(fireball)) return;
        String raw = fireball.getPersistentDataContainer().get(executionKey, PersistentDataType.STRING);
        RuntimeState state = runtimes.get(parseUuid(raw));
        if (state != null) {
            deliverFireball(state, fireball.getLocation());
            finish(state.request.executionId());
        }
        event.blockList().clear();
        event.setYield(0.0F);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFireballIgnite(BlockIgniteEvent event) {
        if (event.getIgnitingEntity() instanceof SmallFireball fireball && hasExecutionIdentity(fireball)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFireballCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof SmallFireball fireball && hasExecutionIdentity(fireball)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSculkCloudApply(AreaEffectCloudApplyEvent event) {
        if (executionKey == null) return;
        String raw = event.getEntity().getPersistentDataContainer().get(executionKey, PersistentDataType.STRING);
        if (raw != null && runtimes.containsKey(parseUuid(raw))) event.setCancelled(true);
    }

    private boolean hasExecution(SmallFireball fireball) {
        UUID executionId = executionId(fireball);
        return executionId != null && runtimes.containsKey(executionId);
    }

    private boolean hasExecutionIdentity(SmallFireball fireball) {
        return executionId(fireball) != null;
    }

    private UUID executionId(SmallFireball fireball) {
        if (executionKey == null) return null;
        String raw = fireball.getPersistentDataContainer().get(executionKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try { return UUID.fromString(raw); } catch (IllegalArgumentException ignored) { return null; }
    }

    private void deliverFireball(RuntimeState state, Location impact) {
        if (state.delivered || impact == null || impact.getWorld() == null) return;
        state.delivered = true;
        World world = impact.getWorld();
        world.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
        world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.15F);
        for (Entity nearby : world.getNearbyEntities(impact, state.definition.radius(),
                state.definition.radius(), state.definition.radius())) {
            if (!(nearby instanceof LivingEntity target) || target.isDead()) continue;
            if (target == source(state)) continue;
            apply(state, target);
        }
    }

    private double attenuation(RuntimeState state, Location from, Location to) {
        return Math.max(state.definition.attenuation(),
                1.0D - from.distance(to) / Math.max(1.0D, state.definition.radius()));
    }

    private LivingEntity findNext(Location location, Set<UUID> visited, double radius) {
        World world = location.getWorld();
        if (world == null) return null;
        for (Entity entity : world.getNearbyEntities(location, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) continue;
            if (!visited.contains(living.getUniqueId())) return living;
        }
        return null;
    }

    private boolean apply(RuntimeState state, LivingEntity target) { return apply(state, target, 1.0D); }

    private boolean apply(RuntimeState state, LivingEntity target, double factor) {
        if (effects == null || target == null || target.isDead()) return false;
        var definition = effects.registry().get(state.potion.effectId()).orElse(null);
        if (definition == null || !definition.enabled()) return false;
        EffectSourceType sourceType = switch (state.definition.kind()) {
            case SCULK -> EffectSourceType.POTION_LINGERING;
            case ECHO, SLIME -> EffectSourceType.POTION_SPLASH;
            case FIREBALL -> EffectSourceType.POTION_FIREBALL;
            default -> EffectSourceType.POTION_DRINK;
        };
        int duration = Math.max(1, (int) Math.round(definition.durationTicks() * factor));
        int amplifier = Math.max(0, (int) Math.floor(definition.amplifier() * factor));
        return effects.applyWithOverrides(target.getUniqueId(), state.potion.effectId(),
                new EffectContext(state.request.sourceId(), sourceType, state.potion.id(),
                        target.getUniqueId(), state.request.executionId().toString()), duration, amplifier);
    }

    private Player source(RuntimeState state) {
        Entity entity = Bukkit.getEntity(state.request.sourceId());
        return entity instanceof Player player && player.isOnline() ? player : null;
    }

    private synchronized void finish(UUID executionId) {
        if (executionId == null) return;
        BukkitTask task = tasks.remove(executionId);
        if (task != null) task.cancel();
        RuntimeState state = runtimes.remove(executionId);
        if (state != null) {
            for (UUID cloudId : state.clouds) {
                Entity entity = Bukkit.getEntity(cloudId);
                if (entity instanceof AreaEffectCloud cloud) cloud.remove();
            }
            for (UUID bounceId : state.bounceProjectiles) {
                Entity entity = Bukkit.getEntity(bounceId);
                if (entity != null) entity.remove();
            }
            if (state.projectileId != null) {
                Entity projectile = Bukkit.getEntity(state.projectileId);
                if (projectile != null) projectile.remove();
            }
        }
        active.remove(executionId);
    }

    private UUID parseUuid(String raw) {
        try { return UUID.fromString(raw); } catch (IllegalArgumentException ignored) { return new UUID(0, 0); }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) { cancelPlayer(event.getPlayer().getUniqueId(), CancelReason.ENTITY_REMOVED); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) { cancelPlayer(event.getEntity().getUniqueId(), CancelReason.ENTITY_REMOVED); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) { cancelPlayer(event.getPlayer().getUniqueId(), CancelReason.WORLD_UNLOAD); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) { cancelWorld(event.getWorld().getUID(), CancelReason.CHUNK_UNLOAD); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) { cancelWorld(event.getWorld().getUID(), CancelReason.WORLD_UNLOAD); }

    private static final class RuntimeState {
        private final Request request;
        private final PotionDefinition potion;
        private final SpecialCatalystDefinition definition;
        private final Location origin;
        private final Set<UUID> visited = new HashSet<>();
        private final Set<UUID> clouds = new HashSet<>();
        private final Set<UUID> processedProjectiles = new HashSet<>();
        private final Set<UUID> bounceProjectiles = new HashSet<>();
        private int spawnedClouds;
        private int bounceCount;
        private UUID projectileId;
        private boolean delivered;

        private RuntimeState(Request request, PotionDefinition potion,
                             SpecialCatalystDefinition definition, Location origin) {
            this.request = request;
            this.potion = potion;
            this.definition = definition;
            this.origin = origin;
            this.visited.addAll(request.visitedTargets());
        }
    }
}
