package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;

/** Canonical Vanilla Desert Pyramid treasure chamber center. */
public final class PyramidTreasureCenterPolicy {
    /** Vanilla places each chest two blocks away from the chamber center. */
    public static final int CHEST_HORIZONTAL_OFFSET = 2;

    /**
     * The vanilla treasure chamber is below the Paper structure bounds. Keep
     * the vertical match bounded, but do not require one exact piece-relative
     * Y value: Paper structure bounds and the generated temple piece can have
     * a small vertical offset across target builds.
     */
    public static final int CHEST_Y_MIN_OFFSET_FROM_BOUNDS_MIN = -16;
    public static final int CHEST_Y_MAX_OFFSET_FROM_BOUNDS_MIN = -4;

    /** Kept as the canonical baseline for diagnostics and compatibility. */
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
        int minY = bounds.minY() + CHEST_Y_MIN_OFFSET_FROM_BOUNDS_MIN;
        int maxY = bounds.minY() + CHEST_Y_MAX_OFFSET_FROM_BOUNDS_MIN;
        return cardinalOffset && y >= minY && y <= maxY;
    }

    public record Center(int x, int z) { }
}
