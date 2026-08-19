package com.hyunseo.hyunseorpg.exploration.listener;

import com.hyunseo.hyunseorpg.exploration.detection.StructureDetectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ChunkLoadExplorationListener implements Listener {
    private final StructureDetectionService detection;

    public ChunkLoadExplorationListener(StructureDetectionService detection) { this.detection = detection; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        detection.scanChunk(event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
    }
}
