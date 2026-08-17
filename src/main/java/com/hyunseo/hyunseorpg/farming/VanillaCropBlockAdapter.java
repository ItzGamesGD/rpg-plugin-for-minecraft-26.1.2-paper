package com.hyunseo.hyunseorpg.farming;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;

/** Temporary Stage 1 representation; replaceable by a resource-pack/block adapter later. */
public final class VanillaCropBlockAdapter implements CropBlockAdapter {
    @Override
    public boolean canPlant(Block soil, Block target, CropDefinition definition) {
        return soil != null && target != null && definition.soil().contains(soil.getType())
                && target.getType() == Material.AIR
                && (!definition.twoBlock() || target.getRelative(0, 1, 0).getType() == Material.AIR);
    }

    @Override
    public boolean isRepresentation(Block block, CropDefinition definition) {
        return block != null && block.getType() == definition.displayBlock();
    }

    @Override
    public boolean isUpperRepresentation(Block block, CropDefinition definition) {
        return definition.twoBlock() && isRepresentation(block, definition);
    }

    @Override
    public boolean applyStage(Block block, CropDefinition definition, int stage) {
        if (block == null) return false;
        if (!applySingleStage(block, definition, stage, true)) return false;
        if (!definition.twoBlock()) return true;
        Block upper = block.getRelative(0, 1, 0);
        if (upper.getType() != Material.AIR && !isUpperRepresentation(upper, definition)) return false;
        return applySingleStage(upper, definition, stage, false);
    }

    @Override
    public boolean clearRepresentation(Block canonicalBlock, CropDefinition definition) {
        if (canonicalBlock == null) return false;
        boolean cleared = false;
        if (definition.twoBlock()) {
            Block upper = canonicalBlock.getRelative(0, 1, 0);
            // Remove the dependent block first so vanilla physics cannot drop it
            // after the lower block loses its support.
            if (isUpperRepresentation(upper, definition)) {
                upper.setType(Material.AIR, false);
                cleared = true;
            }
        }
        if (canonicalBlock.getType() == definition.displayBlock()) {
            canonicalBlock.setType(Material.AIR, false);
            cleared = true;
        }
        return cleared;
    }

    private boolean applySingleStage(Block block, CropDefinition definition, int stage, boolean lower) {
        if (block.getType() != definition.displayBlock()) {
            if (!lower && block.getType() != Material.AIR) return false;
            block.setType(definition.displayBlock(), false);
        }
        if (!(block.getBlockData() instanceof Ageable ageable)) return stage == 0;
        int maximumAge = ageable.getMaximumAge();
        int age = Math.min(maximumAge, Math.max(0,
                Math.round((float) stage / definition.maxStage() * maximumAge)));
        ageable.setAge(age);
        block.setBlockData(ageable, false);
        return true;
    }
}
