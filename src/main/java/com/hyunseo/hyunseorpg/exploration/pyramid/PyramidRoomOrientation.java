package com.hyunseo.hyunseorpg.exploration.pyramid;

import java.util.Objects;

/**
 * The minimal orientation transform required for Pyramid room candidates.
 * This is not a generic structure-coordinate framework.
 */
public enum PyramidRoomOrientation {
    NORTH {
        @Override
        PyramidGridPoint transform(int x, int z) {
            return new PyramidGridPoint(x, z);
        }
    },
    EAST {
        @Override
        PyramidGridPoint transform(int x, int z) {
            return new PyramidGridPoint(-z, x);
        }
    },
    SOUTH {
        @Override
        PyramidGridPoint transform(int x, int z) {
            return new PyramidGridPoint(-x, -z);
        }
    },
    WEST {
        @Override
        PyramidGridPoint transform(int x, int z) {
            return new PyramidGridPoint(z, -x);
        }
    };

    abstract PyramidGridPoint transform(int x, int z);

    public PyramidBlockPosition resolve(PyramidBlockPosition origin, int localX, int localY, int localZ) {
        Objects.requireNonNull(origin, "origin");
        PyramidGridPoint transformed = transform(localX, localZ);
        return origin.add(transformed.x(), localY, transformed.z());
    }

    public PyramidGridPoint resolveGrid(PyramidGridPoint local) {
        Objects.requireNonNull(local, "local");
        return transform(local.x(), local.z());
    }
}
