package com.hyunseo.hyunseorpg.exploration.listener;

import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntimeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.concurrent.atomic.AtomicLong;

/** Physical boundary crossing is separated from teleport so ABANDONED is never caused by a system move. */
public final class ExplorationPlayerMovementListener implements Listener {
    private final ExplorationRuntimeManager runtimes;
    private final AtomicLong tickCounter;

    public ExplorationPlayerMovementListener(ExplorationRuntimeManager runtimes, AtomicLong tickCounter) {
        this.runtimes = runtimes;
        this.tickCounter = tickCounter;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) return;
        var from = event.getFrom();
        var to = event.getTo();
        if (to == null) return;
        // PlayerMoveEvent also fires for look-only updates. Sequence movement waits need exact XYZ changes,
        // not a block-coordinate approximation that discards legitimate short movement.
        if (Double.compare(from.getX(), to.getX()) == 0
                && Double.compare(from.getY(), to.getY()) == 0
                && Double.compare(from.getZ(), to.getZ()) == 0) return;
        runtimes.onPhysicalMove(event.getPlayer(), from, to, tickCounter.get());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        runtimes.onTeleport(event.getPlayer(), tickCounter.get());
    }
}
