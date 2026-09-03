package com.hyunseo.hyunseorpg.special.moonlit;

import java.util.random.RandomGenerator;

/** Pure geometry used by Moon Shadow; Bukkit state deliberately stays outside this class. */
public final class MoonlitAfterglowMath {
    private MoonlitAfterglowMath() { }

    public record Point(double x, double y, double z) {
        public Point add(Point other) { return new Point(x + other.x, y + other.y, z + other.z); }
        public Point multiply(double scale) { return new Point(x * scale, y * scale, z * scale); }
        public double length() { return Math.sqrt(x * x + y * y + z * z); }
    }

    /** Resolves raw WASD state against horizontal camera basis; null means stationary fallback. */
    public static Point movementDirection(boolean forward, boolean backward, boolean left, boolean right,
                                          Point facing, Point rightBasis) {
        double longitudinal = (forward ? 1.0 : 0.0) - (backward ? 1.0 : 0.0);
        double lateral = (right ? 1.0 : 0.0) - (left ? 1.0 : 0.0);
        double x = facing.x * longitudinal + rightBasis.x * lateral;
        double z = facing.z * longitudinal + rightBasis.z * lateral;
        double length = Math.sqrt(x * x + z * z);
        return length < 1.0e-8 ? null : new Point(x / length, 0, z / length);
    }

    public static Point sphericalOffset(RandomGenerator random, double minimumRadius, double maximumRadius) {
        if (!Double.isFinite(minimumRadius) || !Double.isFinite(maximumRadius)
                || minimumRadius < 0 || maximumRadius < minimumRadius) {
            throw new IllegalArgumentException("invalid radial bounds");
        }
        double z = random.nextDouble(-1.0, 1.0);
        double azimuth = random.nextDouble(0.0, Math.PI * 2.0);
        double horizontal = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        double radius = random.nextDouble(minimumRadius, Math.nextUp(maximumRadius));
        return new Point(horizontal * Math.cos(azimuth), z, horizontal * Math.sin(azimuth)).multiply(radius);
    }
}
