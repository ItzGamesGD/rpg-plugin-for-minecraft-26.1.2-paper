package com.hyunseo.hyunseorpg.exploration.listener;

import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntimeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/** Prevents normal player edits to an active Desert Pyramid puzzle's owned geometry. */
public final class PyramidPuzzleProtectionListener implements Listener {
    private final ExplorationRuntimeManager runtimes;

    public PyramidPuzzleProtectionListener(ExplorationRuntimeManager runtimes) {
        this.runtimes = runtimes;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (runtimes.isPyramidPuzzleProtected(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (runtimes.isPyramidPuzzleProtected(event.getBlock())) {
            event.setCancelled(true);
        }
    }
}
