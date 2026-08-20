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
        long expiresAt = org.bukkit.Bukkit.getCurrentTick() + durationTicks;
        roots.put(id, new RootState(expiresAt));
        // Do not rely on a future PlayerMoveEvent to clean up the state. A player
        // who stops moving must still leave the root window before the next pulse.
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,
                () -> clearIfExpired(id, expiresAt), durationTicks);
    }

    /** Returns whether the player is currently rooted, expiring stale state at the boundary tick. */
    public synchronized boolean isLocked(UUID entityId) {
        RootState state = roots.get(entityId);
        if (state == null) return false;
        if (org.bukkit.Bukkit.getCurrentTick() >= state.expiresAt()) {
            roots.remove(entityId);
            return false;
        }
        return true;
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
        roots.remove(entityId);
    }

    private synchronized void clearIfExpired(UUID entityId, long expiresAt) {
        RootState state = roots.get(entityId);
        if (state != null && state.expiresAt() <= expiresAt
                && org.bukkit.Bukkit.getCurrentTick() >= state.expiresAt()) {
            roots.remove(entityId);
        }
    }

    public synchronized void clearAll() {
        roots.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) { clear(event.getEntity().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) { clear(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) { clear(event.getPlayer().getUniqueId()); }

    private record RootState(long expiresAt) { }
}
