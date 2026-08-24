package com.hyunseo.hyunseorpg.exploration.pyramid;

/**
 * The only legal push directions for the desert-pyramid pillar puzzle.
 * The puzzle is deliberately cardinal-only; diagonal physics is not part of
 * the logical grid.
 */
public enum PyramidGridDirection {
    NORTH(0, -1),
    SOUTH(0, 1),
    EAST(1, 0),
    WEST(-1, 0);

    private final int x;
    private final int z;

    PyramidGridDirection(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }
}
