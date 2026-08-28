package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;

/** Canonical Vanilla Desert Pyramid treasure chamber center. */
public final class PyramidTreasureCenterPolicy {
    /** Vanilla places each chest two blocks away from the chamber center. */
    public static final int CHEST_HORIZONTAL_OFFSET = 2;

    /**
     * The treasure chamber is generated below the structure piece's bounding
     * box: its chest layer is eleven blocks below the piece's minimum Y.
     */
    public static final int CHEST_Y_OFFSET_FROM_BOUNDS_MIN = -11;

    private PyramidTreasureCenterPolicy() { }

    public static Center from(StructureBounds bounds) {
        if (bounds == null) throw new IllegalArgumentException("bounds");
        return new Center((int) Math.floor(bounds.centerX()), (int) Math.floor(bounds.centerZ()));
    }

    /**
     * Matches the four cardinal Vanilla treasure chests. The trigger slots are
     * deliberately exact so arbitrary containers inside a Pyramid cannot start
     * the underground flow.
     */
    public static boolean isVanillaTreasureSlot(StructureBounds bounds, int x, int y, int z) {
        if (bounds == null) return false;
        Center center = from(bounds);
        int dx = Math.abs(x - center.x());
        int dz = Math.abs(z - center.z());
        boolean cardinalOffset = (dx == CHEST_HORIZONTAL_OFFSET && dz == 0)
                || (dx == 0 && dz == CHEST_HORIZONTAL_OFFSET);
        return cardinalOffset && y == bounds.minY() + CHEST_Y_OFFSET_FROM_BOUNDS_MIN;
    }

    public record Center(int x, int z) { }
}
