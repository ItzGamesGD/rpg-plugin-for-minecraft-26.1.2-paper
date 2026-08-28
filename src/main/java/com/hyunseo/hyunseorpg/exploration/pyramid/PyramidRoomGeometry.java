package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;

/** Authoritative deterministic room shell and access-shaft bounds. */
public record PyramidRoomGeometry(
        PyramidBlockPosition origin,
        int roomRadius,
        int roomHeight,
        int shaftCenterX,
        int shaftCenterZ,
        int shaftRadius,
        int shaftBottomY,
        int shaftTopY
) {
    public static final int SHAFT_RADIUS = 1;

    public PyramidRoomGeometry {
        if (origin == null) throw new IllegalArgumentException("origin");
        if (roomRadius < 1 || roomHeight < 1) throw new IllegalArgumentException("invalid room size");
        if (shaftRadius != SHAFT_RADIUS) throw new IllegalArgumentException("Desert Pyramid shaft must be 3x3");
        if (shaftBottomY != origin.y() + roomHeight) {
            throw new IllegalArgumentException("shaft must begin at room ceiling");
        }
        if (shaftTopY < shaftBottomY) throw new IllegalArgumentException("shaft top below room ceiling");
    }

    public static PyramidRoomGeometry of(PyramidBlockPosition origin, int roomRadius,
                                         int roomHeight, StructureBounds structureBounds) {
        if (structureBounds == null) throw new IllegalArgumentException("structureBounds");
        return new PyramidRoomGeometry(origin, roomRadius, roomHeight, origin.x(), origin.z(), SHAFT_RADIUS,
                origin.y() + roomHeight, structureBounds.minY() - 1);
    }

    public static PyramidRoomGeometry resolved(PyramidBlockPosition origin, int roomRadius, int roomHeight,
                                               int shaftCenterX, int shaftCenterZ,
                                               int shaftBottomY, int shaftTopY) {
        return new PyramidRoomGeometry(origin, roomRadius, roomHeight, shaftCenterX, shaftCenterZ,
                SHAFT_RADIUS, shaftBottomY, shaftTopY);
    }

    public boolean containsRoomShellOrInterior(int x, int y, int z) {
        return Math.abs(x - origin.x()) <= roomRadius
                && Math.abs(z - origin.z()) <= roomRadius
                && y >= origin.y() - 1 && y <= origin.y() + roomHeight;
    }

    public boolean containsShaft(int x, int y, int z) {
        return Math.abs(x - shaftCenterX) <= shaftRadius
                && Math.abs(z - shaftCenterZ) <= shaftRadius
                && y >= shaftBottomY && y <= shaftTopY;
    }

    public boolean protectedBlock(int x, int y, int z) {
        return containsRoomShellOrInterior(x, y, z) || containsShaft(x, y, z);
    }
}
