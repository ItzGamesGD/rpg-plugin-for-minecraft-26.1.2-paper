package com.hyunseo.hyunseorpg.exploration.model;

import java.util.UUID;

/** Bukkit-free canonical anchor so persistence/tests do not retain Location objects. */
public record StructureAnchor(UUID worldId, double x, double y, double z) {
    public int chunkX() { return floorBlock(x) >> 4; }
    public int chunkZ() { return floorBlock(z) >> 4; }

    private static int floorBlock(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}
