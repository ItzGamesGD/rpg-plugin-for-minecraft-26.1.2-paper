package com.hyunseo.hyunseorpg.special.flame;

import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;

/** Pure, server-independent contracts used by the Flame Axe runtime. */
public final class FlameAxeMath {
    private static final double EPSILON = 1.0E-10;

    private FlameAxeMath() {}

    public static boolean isFullCharge(int heldTicks, int requiredTicks) {
        return heldTicks >= Math.max(1, requiredTicks);
    }

    public static boolean mayHit(double distance, double radius, double rotationSinceHit,
                                 double requiredRotation, int noDamageTicks) {
        return distance <= radius && rotationSinceHit >= requiredRotation && noDamageTicks <= 0;
    }

    public static boolean maySelectTarget(UUID id, Set<UUID> visited, int maxDestinations) {
        return id != null && visited != null && visited.size() < Math.max(0, maxDestinations)
                && !visited.contains(id);
    }

    /** Turns current toward desired by at most maxRadians and never emits a non-finite vector. */
    public static Vector steer(Vector current, Vector desired, double maxRadians) {
        if (!finite(current) || current.lengthSquared() < EPSILON) return new Vector();
        if (!finite(desired) || desired.lengthSquared() < EPSILON) return current.clone();
        double speed = current.length();
        Vector from = current.clone().normalize();
        Vector to = desired.clone().normalize();
        double dot = Math.max(-1D, Math.min(1D, from.dot(to)));
        double angle = Math.acos(dot);
        if (angle <= Math.max(0D, maxRadians)) return to.multiply(speed);
        Vector axis = from.clone().crossProduct(to);
        if (axis.lengthSquared() < EPSILON) {
            axis = from.clone().crossProduct(Math.abs(from.getY()) < .9 ? new Vector(0, 1, 0) : new Vector(1, 0, 0));
        }
        if (axis.lengthSquared() < EPSILON) return current.clone();
        Vector result = from.rotateAroundAxis(axis.normalize(), Math.max(0D, maxRadians)).multiply(speed);
        return finite(result) ? result : current.clone();
    }

    private static boolean finite(Vector vector) {
        return vector != null && Double.isFinite(vector.getX()) && Double.isFinite(vector.getY())
                && Double.isFinite(vector.getZ());
    }
}
