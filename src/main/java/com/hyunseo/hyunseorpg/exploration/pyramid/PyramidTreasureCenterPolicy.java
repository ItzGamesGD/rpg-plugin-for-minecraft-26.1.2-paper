package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;

/** Canonical Vanilla Desert Pyramid treasure chamber center. */
public final class PyramidTreasureCenterPolicy {
    private PyramidTreasureCenterPolicy() { }

    public static Center from(StructureBounds bounds) {
        if (bounds == null) throw new IllegalArgumentException("bounds");
        return new Center((int) Math.floor(bounds.centerX()), (int) Math.floor(bounds.centerZ()));
    }

    public record Center(int x, int z) { }
}
