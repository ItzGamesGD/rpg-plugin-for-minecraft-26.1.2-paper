package com.hyunseo.hyunseorpg.farming;

import org.bukkit.block.Block;

/** Boundary between logical crops and their current block representation. */
public interface CropBlockAdapter {
    boolean canPlant(Block soil, Block target, CropDefinition definition);

    boolean isRepresentation(Block block, CropDefinition definition);

    /** Returns whether the block is the dependent upper representation. */
    default boolean isUpperRepresentation(Block block, CropDefinition definition) {
        return false;
    }

    /** Validates the lower and, when configured, upper representation together. */
    default boolean isCompleteRepresentation(Block canonicalBlock, CropDefinition definition) {
        return isRepresentation(canonicalBlock, definition)
                && (!definition.twoBlock()
                || isUpperRepresentation(canonicalBlock.getRelative(0, 1, 0), definition));
    }

    boolean applyStage(Block block, CropDefinition definition, int stage);

    /** Removes only this crop's visual representation, without vanilla drops. */
    boolean clearRepresentation(Block canonicalBlock, CropDefinition definition);
}
