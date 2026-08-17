package com.hyunseo.hyunseorpg.alchemy.catalyst;

import com.hyunseo.hyunseorpg.alchemy.EffectContext;
import com.hyunseo.hyunseorpg.alchemy.EffectService;
import com.hyunseo.hyunseorpg.alchemy.EffectSourceType;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionDefinition;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Executes bounded special-catalyst deliveries through the normal EffectService. */
public final class BoundedSpecialCatalystExecutionService implements SpecialCatalystExecution, Listener {
    private final SpecialCatalystRegistry registry;
    private final int maxExecutions;
    private final JavaPlugin plugin;
    private final PotionRegistry potions;
    private final EffectService effects;
    private final Map<UUID, Request> active = new HashMap<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

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
    }

    @Override public synchronized Result execute(Request request) {
        if (request == null || request.executionId() == null || request.worldId() == null) return Result.REJECTED_POLICY;
        SpecialCatalystDefinition definition = registry.find(request.catalystId()).orElse(null);
        if (definition == null) return Result.REJECTED_NOT_REGISTERED;
        if (!definition.enabled()) return Result.REJECTED_POLICY;
        if (active.containsKey(request.executionId())) return Result.REJECTED_DUPLICATE;
        if (active.size() >= maxExecutions) return Result.REJECTED_LIMIT;
        active.put(request.executionId(), request);
        return Result.STARTED;
    }

    /** Starts a real delivery for a canonical potion consumed by an online player. */
    public synchronized Result start(UUID sourceId, String potionId, String catalystId) {
        if (plugin == null || potions == null || effects == null || sourceId == null) return Result.REJECTED_POLICY;
        Player source = Bukkit.getPlayer(sourceId);
        PotionDefinition potion = potions.find(potionId).orElse(null);
        SpecialCatalystDefinition definition = registry.find(catalystId).orElse(null);
        if (source == null || !source.isOnline() || potion == null || !potion.enabled()
                || definition == null || !definition.enabled()) return Result.REJECTED_POLICY;
        UUID executionId = UUID.randomUUID();
        Request request = new Request(executionId, potion.id(), definition.catalystId(),
                source.getWorld().getUID(), Set.of(sourceId), sourceId);
        Result result = execute(request);
        if (result != Result.STARTED) return result;
        schedule(request, potion, definition, source.getLocation().clone());
        return result;
    }

    @Override public synchronized void cancel(UUID executionId, CancelReason reason) {
        if (executionId == null) return;
        BukkitTask task = tasks.remove(executionId);
        if (task != null) task.cancel();
        active.remove(executionId);
    }

    @Override public synchronized void cancelWorld(UUID worldId, CancelReason reason) {
        if (worldId == null) return;
        new ArrayList<>(active.entrySet()).stream()
                .filter(entry -> worldId.equals(entry.getValue().worldId()))
                .map(Map.Entry::getKey)
                .forEach(id -> cancel(id, reason));
    }

    public synchronized void cancelPlayer(UUID playerId, CancelReason reason) {
        if (playerId == null) return;
        new ArrayList<>(active.entrySet()).stream()
                .filter(entry -> playerId.equals(entry.getValue().sourceId())
                        || entry.getValue().visitedTargets().contains(playerId))
                .map(Map.Entry::getKey)
                .forEach(id -> cancel(id, reason));
    }

    public synchronized void cancelAll(CancelReason reason) {
        new ArrayList<>(active.keySet()).forEach(id -> cancel(id, reason));
    }

    public synchronized int activeCount() { return active.size(); }

    private void schedule(Request request, PotionDefinition potion, SpecialCatalystDefinition definition,
                           Location origin) {
        switch (definition.kind()) {
            case SCULK -> tasks.put(request.executionId(), Bukkit.getScheduler().runTask(plugin,
                    () -> propagate(request, potion, definition, origin)));
            case ECHO -> tasks.put(request.executionId(), Bukkit.getScheduler().runTaskLater(plugin,
                    () -> echo(request, potion), definition.delayTicks()));
            case SLIME -> tasks.put(request.executionId(), Bukkit.getScheduler().runTaskTimer(plugin,
                    new Runnable() {
                        private int bounce;
                        private Location current = origin.clone();
                        private final Set<UUID> visited = new HashSet<>(request.visitedTargets());
                        @Override public void run() {
                            if (bounce++ >= definition.maxCount()) { finish(request.executionId()); return; }
                            LivingEntity next = findNext(current, visited, definition.radius());
                            if (next == null) { finish(request.executionId()); return; }
                            visited.add(next.getUniqueId());
                            apply(request, potion, next);
                            current = next.getLocation().clone();
                        }
                    }, 1L, 5L));
            case WIND_CHARGE -> tasks.put(request.executionId(), Bukkit.getScheduler().runTaskTimer(plugin,
                    new Runnable() {
                        private int ticks;
                        private final Location current = origin.clone();
                        private final Vector direction = origin.getDirection().normalize();
                        @Override public void run() {
                            ticks++;
                            if (ticks > definition.lifetimeTicks()
                                    || current.distance(origin) > definition.maxDistance()) { finish(request.executionId()); return; }
                            current.add(direction.clone().multiply(0.75D));
                            LivingEntity target = findNext(current, request.visitedTargets(), 1.5D);
                            if (target != null) { apply(request, potion, target); finish(request.executionId()); }
                        }
                    }, 1L, 1L));
        }
    }

    private void propagate(Request request, PotionDefinition potion, SpecialCatalystDefinition definition, Location origin) {
        Set<UUID> visited = new HashSet<>(request.visitedTargets());
        World world = origin.getWorld();
        if (world != null) {
            for (Entity entity : world.getNearbyEntities(origin, definition.radius(), definition.radius(), definition.radius())) {
                if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) continue;
                if (!visited.add(living.getUniqueId())) continue;
                if (visited.size() > definition.maxCount()) break;
                double factor = Math.max(definition.attenuation(),
                        1.0D - origin.distance(living.getLocation()) / Math.max(1.0D, definition.radius()));
                apply(request, potion, living, factor);
            }
        }
        finish(request.executionId());
    }

    private void echo(Request request, PotionDefinition potion) {
        if (request.sourceId() == null) { finish(request.executionId()); return; }
        Entity entity = Bukkit.getEntity(request.sourceId());
        if (entity instanceof LivingEntity living && living.isValid() && !living.isDead()) {
            apply(request, potion, living);
        }
        finish(request.executionId());
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

    private void apply(Request request, PotionDefinition potion, LivingEntity target) {
        apply(request, potion, target, 1.0D);
    }

    private void apply(Request request, PotionDefinition potion, LivingEntity target, double factor) {
        if (effects == null) return;
        var definition = effects.registry().get(potion.effectId()).orElse(null);
        if (definition == null) return;
        if (factor >= 0.999D) {
            effects.apply(target.getUniqueId(), potion.effectId(), new EffectContext(request.sourceId(),
                    EffectSourceType.OTHER, potion.id(), target.getUniqueId(), request.executionId().toString()));
            return;
        }
        int duration = Math.max(1, (int) Math.round(definition.durationTicks() * factor));
        int amplifier = Math.max(0, (int) Math.floor(definition.amplifier() * factor));
        effects.applyWithOverrides(target.getUniqueId(), potion.effectId(), new EffectContext(request.sourceId(),
                EffectSourceType.OTHER, potion.id(), target.getUniqueId(), request.executionId().toString()), duration, amplifier);
    }

    private synchronized void finish(UUID executionId) {
        BukkitTask task = tasks.remove(executionId);
        if (task != null) task.cancel();
        active.remove(executionId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) { cancelPlayer(event.getPlayer().getUniqueId(), CancelReason.ENTITY_REMOVED); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) { cancelPlayer(event.getEntity().getUniqueId(), CancelReason.ENTITY_REMOVED); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        cancelPlayer(event.getPlayer().getUniqueId(), CancelReason.WORLD_UNLOAD);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        cancelWorld(event.getWorld().getUID(), CancelReason.CHUNK_UNLOAD);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        cancelWorld(event.getWorld().getUID(), CancelReason.WORLD_UNLOAD);
    }
}
