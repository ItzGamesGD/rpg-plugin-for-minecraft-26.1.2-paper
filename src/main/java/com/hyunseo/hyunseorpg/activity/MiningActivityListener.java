package com.hyunseo.hyunseorpg.activity;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/** Protects the activity reward ledger across normal, explosive, and piston changes. */
public final class MiningActivityListener implements Listener {
    private final MiningActivityService service;
    private final ActivityBlockRepository placedBlocks;

    public MiningActivityListener(MiningActivityService service, ActivityBlockRepository placedBlocks) {
        this.service = service;
        this.placedBlocks = placedBlocks;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        service.handleBreak(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        service.recordPlacement(event.getBlockPlaced());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(placedBlocks::remove);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().forEach(placedBlocks::remove);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(placedBlocks::isRecorded)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(placedBlocks::isRecorded)) event.setCancelled(true);
    }
}
