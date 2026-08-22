package com.hyunseo.hyunseorpg.exploration.model;

/** Inclusive integer structure bounds copied from the server structure descriptor. */
public record StructureBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public StructureBounds {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("invalid structure bounds");
        }
    }

    public double centerX() { return (minX + maxX) / 2.0D; }
    public double centerY() { return (minY + maxY) / 2.0D; }
    public double centerZ() { return (minZ + maxZ) / 2.0D; }

    /** Squared Euclidean distance to the box; zero when inside. */
    public double distanceSquaredTo(double x, double y, double z) {
        double dx = axisDistance(x, minX, maxX);
        double dy = axisDistance(y, minY, maxY);
        double dz = axisDistance(z, minZ, maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public int minChunkX() { return minX >> 4; }
    public int maxChunkX() { return maxX >> 4; }
    public int minChunkZ() { return minZ >> 4; }
    public int maxChunkZ() { return maxZ >> 4; }

    private static double axisDistance(double value, double min, double max) {
        if (value < min) return min - value;
        if (value > max) return value - max;
        return 0.0D;
    }
}
