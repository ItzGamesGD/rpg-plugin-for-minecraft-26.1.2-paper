package com.hyunseo.hyunseorpg.exploration.listener;

import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntimeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/** Prevents normal player edits to an active Desert Pyramid puzzle's owned geometry. */
public final class PyramidPuzzleProtectionListener implements Listener {
    /** Pure policy used by both Bukkit handlers: only owned geometry is cancelled. */
    public static boolean shouldCancelBlockEdit(boolean protectedBlock) { return protectedBlock; }

    private final ExplorationRuntimeManager runtimes;

    public PyramidPuzzleProtectionListener(ExplorationRuntimeManager runtimes) {
        this.runtimes = runtimes;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (shouldCancelBlockEdit(runtimes.isPyramidPuzzleProtected(event.getBlock()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (shouldCancelBlockEdit(runtimes.isPyramidPuzzleProtected(event.getBlock()))) {
            event.setCancelled(true);
        }
    }
}
