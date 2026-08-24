package com.hyunseo.hyunseorpg.exploration.pyramid;

/**
 * Integer world-block position used by bounded, pyramid-specific room planning.
 * It deliberately stores no World or chunk reference.
 */
public record PyramidBlockPosition(int x, int y, int z) {
    public PyramidBlockPosition add(int dx, int dy, int dz) {
        return new PyramidBlockPosition(x + dx, y + dy, z + dz);
    }
}
