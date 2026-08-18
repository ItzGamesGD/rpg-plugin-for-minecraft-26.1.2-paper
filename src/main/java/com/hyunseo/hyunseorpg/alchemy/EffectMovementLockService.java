package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared short-lived movement lock used by effects that pulse a root/stun state. */
public final class EffectMovementLockService implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, LockState> locks = new HashMap<>();

    public EffectMovementLockService(JavaPlugin plugin) { this.plugin = plugin; }

    public synchronized void lock(LivingEntity target, long durationTicks) {
        if (target == null || target.isDead() || !target.isValid() || durationTicks <= 0) return;
        clear(target.getUniqueId());
        Location anchor = target.getLocation().clone();
        long expiresAt = Bukkit.getCurrentTick() + durationTicks;
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(target, anchor, expiresAt), 0L, 1L);
        locks.put(target.getUniqueId(), new LockState(anchor, expiresAt, task));
    }

    private synchronized void tick(LivingEntity target, Location anchor, long expiresAt) {
        LockState state = locks.get(target.getUniqueId());
        if (state == null || state.expiresAt() != expiresAt || !target.isValid()
                || target.isDead() || Bukkit.getCurrentTick() >= expiresAt) {
            clear(target.getUniqueId());
            return;
        }
        if (target.getWorld().equals(anchor.getWorld())
                && target.getLocation().distanceSquared(anchor) > 0.0001D) {
            target.teleport(anchor);
        }
        target.setVelocity(new Vector());
        target.setFallDistance(0.0F);
    }

    public synchronized void clear(UUID entityId) {
        if (entityId == null) return;
        LockState state = locks.remove(entityId);
        if (state != null && !state.task().isCancelled()) state.task().cancel();
    }

    public synchronized void clearAll() {
        new HashMap<>(locks).keySet().forEach(this::clear);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) { clear(event.getEntity().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) { clear(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) { clear(event.getPlayer().getUniqueId()); }

    private record LockState(Location anchor, long expiresAt, BukkitTask task) { }
}
