package com.hyunseo.hyunseorpg.exploration.listener;

import com.hyunseo.hyunseorpg.exploration.component.impl.PyramidPushPillarService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.concurrent.atomic.AtomicLong;

/** Forwards physical movement to the Pyramid-specific contact adapter. */
public final class PyramidPushPillarListener implements Listener {
    private final PyramidPushPillarService puzzles;
    private final AtomicLong tickCounter;

    public PyramidPushPillarListener(PyramidPushPillarService puzzles, AtomicLong tickCounter) {
        this.puzzles = puzzles;
        this.tickCounter = tickCounter;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent || event.getTo() == null) return;
        var from = event.getFrom();
        var to = event.getTo();
        if (Double.compare(from.getX(), to.getX()) == 0
                && Double.compare(from.getY(), to.getY()) == 0
                && Double.compare(from.getZ(), to.getZ()) == 0) return;
        puzzles.onMove(event.getPlayer(), from, to, tickCounter.get());
    }
}
