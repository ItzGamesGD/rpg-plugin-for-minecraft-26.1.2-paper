package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Shared short-lived movement lock used by effects that pulse a root/stun state. */
public final class EffectMovementLockService implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, RootState> roots = new HashMap<>();

    public EffectMovementLockService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts or refreshes a short movement lock for any LivingEntity.
     *
     * <p>Player movement is constrained by PlayerMoveEvent. Mob movement is paused by
     * temporarily disabling AI, while preserving the original hasAI() value.</p>
     */
    public synchronized void lock(LivingEntity target, long durationTicks) {
        if (target == null || target.isDead() || !target.isValid() || durationTicks <= 0L) {
            debug("lock skipped target=" + (target == null ? "null" : target.getUniqueId())
                    + " reason=invalid-or-nonpositive-duration");
            return;
        }

        UUID id = target.getUniqueId();
        long now = Bukkit.getCurrentTick();
        long expiresAt = now + durationTicks;
        RootState existing = roots.get(id);
        Boolean originalHasAI = existing == null ? originalHasAI(target) : existing.originalHasAI();

        if (target instanceof Mob mob && Boolean.TRUE.equals(originalHasAI)) {
            mob.setAI(false);
        }
        target.setVelocity(new Vector());

        // A re-lock must never treat the already-disabled AI as the original state.
        long effectiveExpiry = existing == null ? expiresAt : Math.max(existing.expiresAt(), expiresAt);
        RootState state = new RootState(effectiveExpiry, originalHasAI);
        roots.put(id, state);
        debug("lock target=" + id
                + " type=" + target.getType()
                + " tick=" + now
                + " duration=" + durationTicks
                + " expiresAt=" + effectiveExpiry
                + " originalHasAI=" + originalHasAI
                + " refreshed=" + (existing != null));

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> clearIfExpired(id, effectiveExpiry), durationTicks);
    }

    /** Returns whether the entity is currently rooted, releasing stale state at the boundary tick. */
    public synchronized boolean isLocked(UUID entityId) {
        RootState state = roots.get(entityId);
        if (state == null) return false;

        long now = Bukkit.getCurrentTick();
        if (now >= state.expiresAt()) {
            roots.remove(entityId);
            release(entityId, state, now, "boundary");
            return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        RootState before = stateOf(id);
        boolean positional = isPositionalMove(event);
        if (before != null) {
            debug("player-move phase=HIGHEST player=" + id
                    + " tick=" + Bukkit.getCurrentTick()
                    + " cancelled=" + event.isCancelled()
                    + " isLocked=" + isLocked(id)
                    + " expiresAt=" + before.expiresAt()
                    + " positional=" + positional);
        }

        // A previously cancelled event must remain cancelled. The diagnostic path above
        // still records it so an unrelated listener can be identified in live testing.
        if (event.isCancelled() || !isLocked(id)) return;
        if (event.getTo() == null || event.getFrom().getWorld() == null
                || !event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            clear(id);
            return;
        }
        if (!positional) return;

        // Root only blocks positional movement. Preserve yaw/pitch so the camera remains usable.
        org.bukkit.Location constrained = event.getFrom().clone();
        constrained.setYaw(event.getTo().getYaw());
        constrained.setPitch(event.getTo().getPitch());
        event.setTo(constrained);
        debug("player-move phase=HIGHEST action=setTo player=" + id
                + " tick=" + Bukkit.getCurrentTick()
                + " positional=" + positional
                + " finalPositional=" + isPositionalMove(event));
    }

    /**
     * Observes the final event destination after later listeners have run.
     * This is diagnostic-only and intentionally does not mutate the event at MONITOR.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onMoveMonitor(PlayerMoveEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        RootState state = stateOf(id);
        if (state == null) return;
        debug("player-move phase=MONITOR player=" + id
                + " tick=" + Bukkit.getCurrentTick()
                + " cancelled=" + event.isCancelled()
                + " isLocked=" + isLocked(id)
                + " expiresAt=" + state.expiresAt()
                + " positional=" + isPositionalMove(event)
                + " finalTo=" + describe(event.getTo()));
    }

    public synchronized void clear(UUID entityId) {
        if (entityId == null) return;
        RootState state = roots.remove(entityId);
        if (state == null) return;
        release(entityId, state, Bukkit.getCurrentTick(), "clear");
    }

    private synchronized void clearIfExpired(UUID entityId, long expectedExpiry) {
        RootState state = roots.get(entityId);
        if (state == null || state.expiresAt() != expectedExpiry) return;
        if (Bukkit.getCurrentTick() < state.expiresAt()) return;
        roots.remove(entityId);
        release(entityId, state, Bukkit.getCurrentTick(), "scheduled");
    }

    public synchronized void clearAll() {
        Set<UUID> ids = new HashSet<>(roots.keySet());
        ids.forEach(this::clear);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        clear(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        clear(event.getPlayer().getUniqueId());
    }

    boolean debugEnabled() {
        return plugin.getConfig().getBoolean("alchemy-runtime-debug.enabled", false);
    }

    void debug(String message) {
        if (debugEnabled()) {
            plugin.getLogger().info("[alchemy-runtime] " + message);
        }
    }

    private synchronized RootState stateOf(UUID id) {
        return roots.get(id);
    }

    private Boolean originalHasAI(LivingEntity target) {
        return target instanceof Mob mob ? mob.hasAI() : null;
    }

    private void release(UUID id, RootState state, long now, String reason) {
        Entity entity = Bukkit.getEntity(id);
        if (entity instanceof Mob mob && state.originalHasAI() != null
                && mob.isValid() && !mob.isDead()) {
            mob.setAI(state.originalHasAI());
        }
        debug("release uuid=" + id
                + " tick=" + now
                + " reason=" + reason
                + " removed=true"
                + " restoredAI=" + state.originalHasAI());
    }

    private boolean isPositionalMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getWorld() == null
                || !event.getFrom().getWorld().equals(event.getTo().getWorld())) return true;
        return event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ();
    }

    private String describe(org.bukkit.Location location) {
        if (location == null) return "null";
        return location.getX() + "," + location.getY() + "," + location.getZ()
                + ",yaw=" + location.getYaw() + ",pitch=" + location.getPitch();
    }

    /** Structured shock-root state used by the production effect debug surface. */
    public synchronized String debug(UUID entityId, long currentTick) {
        RootState state = roots.get(entityId);
        boolean active = state != null && currentTick < state.expiresAt();
        return "shockRootActive=" + active
                + " shockRootStart=" + (state == null ? -1L : currentTick)
                + " shockRootEnd=" + (state == null ? -1L : state.expiresAt())
                + " currentTick=" + currentTick;
    }

    private record RootState(long expiresAt, Boolean originalHasAI) { }
}
