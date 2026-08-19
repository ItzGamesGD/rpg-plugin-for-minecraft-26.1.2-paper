package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared short-lived movement lock used by effects that pulse a root/stun state. */
public final class EffectMovementLockService implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, RootState> roots = new HashMap<>();

    public EffectMovementLockService(JavaPlugin plugin) { this.plugin = plugin; }

    public synchronized void lock(LivingEntity target, long durationTicks) {
        if (!(target instanceof org.bukkit.entity.Player player)
                || player.isDead() || !player.isValid() || durationTicks <= 0) return;
        UUID id = player.getUniqueId();
        long start = org.bukkit.Bukkit.getCurrentTick();
        long end = start + durationTicks;
        clear(id);
        UUID generation = UUID.randomUUID();
        RootState state = new RootState(start, end, generation);
        roots.put(id, state);
        if (plugin != null) {
            state.unlockTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                synchronized (EffectMovementLockService.this) {
                    RootState current = roots.get(id);
                    if (current != null && current.generation.equals(generation)) {
                        roots.remove(id);
                    }
                }
            }, durationTicks);
        }
    }

    /** Returns whether the player is currently rooted, expiring stale state at the boundary tick. */
    public synchronized boolean isLocked(UUID entityId) {
        RootState state = roots.get(entityId);
        if (state == null) return false;
        long currentTick = org.bukkit.Bukkit.getCurrentTick();
        if (currentTick >= state.endTick) {
            roots.remove(entityId);
            if (state.unlockTask != null) state.unlockTask.cancel();
            return false;
        }
        return currentTick >= state.startTick;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!isLocked(event.getPlayer().getUniqueId())) return;
        if (event.getTo() == null || !event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            clear(event.getPlayer().getUniqueId());
            return;
        }
        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getY() == event.getTo().getY()
                && event.getFrom().getZ() == event.getTo().getZ()) return;

        // Root only blocks positional movement. Preserve yaw/pitch so the camera remains usable.
        org.bukkit.Location constrained = event.getFrom().clone();
        constrained.setYaw(event.getTo().getYaw());
        constrained.setPitch(event.getTo().getPitch());
        event.setTo(constrained);
    }

    public synchronized void clear(UUID entityId) {
        if (entityId == null) return;
        RootState state = roots.remove(entityId);
        if (state != null && state.unlockTask != null) state.unlockTask.cancel();
    }

    public synchronized void clearAll() {
        for (RootState state : roots.values()) {
            if (state.unlockTask != null) state.unlockTask.cancel();
        }
        roots.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) { clear(event.getEntity().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) { clear(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) { clear(event.getPlayer().getUniqueId()); }

    public synchronized String debug(UUID entityId, long currentTick) {
        RootState state = roots.get(entityId);
        boolean active = state != null && currentTick >= state.startTick && currentTick < state.endTick;
        return "shockRootActive=" + active
                + " shockRootStart=" + (state == null ? -1 : state.startTick)
                + " shockRootEnd=" + (state == null ? -1 : state.endTick)
                + " currentTick=" + currentTick;
    }

    private static final class RootState {
        private final long startTick;
        private final long endTick;
        private final UUID generation;
        private org.bukkit.scheduler.BukkitTask unlockTask;

        private RootState(long startTick, long endTick, UUID generation) {
            this.startTick = startTick;
            this.endTick = endTick;
            this.generation = generation;
        }
    }
}
