package com.hyunseo.hyunseorpg.farming;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Snapshot used by the Stage 4 harvest boundary. */
public record HarvestContext(
        Player player,
        String cropId,
        CropPosition canonicalPosition,
        boolean mature,
        HarvestCause cause,
        ItemStack tool,
        boolean hoe,
        FarmingStage farmingStage,
        String transactionId,
        int baseDropAmount,
        int seedDropAmount
) {
    public HarvestContext {
        tool = tool == null ? null : tool.clone();
        farmingStage = farmingStage == null ? FarmingStage.BASIC : farmingStage;
    }
}
