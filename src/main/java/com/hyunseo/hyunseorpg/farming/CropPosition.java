package com.hyunseo.hyunseorpg.farming;

import org.bukkit.block.Block;

import java.util.Objects;
import java.util.UUID;

/** Immutable world position used as the canonical crop key. */
public record CropPosition(UUID worldId, int x, int y, int z) {
    public CropPosition {
        Objects.requireNonNull(worldId, "worldId");
    }

    public static CropPosition of(Block block) {
        Objects.requireNonNull(block, "block");
        return new CropPosition(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }
}
