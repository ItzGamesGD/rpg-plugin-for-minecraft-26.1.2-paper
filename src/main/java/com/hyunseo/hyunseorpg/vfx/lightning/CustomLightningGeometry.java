package com.hyunseo.hyunseorpg.vfx.lightning;

import org.bukkit.util.Vector;
import java.util.*;

/** World-independent jagged path generation, kept separate from display rendering and combat. */
public final class CustomLightningGeometry {
    private static final double MIN_DISTANCE = 0.01;
    private CustomLightningGeometry() {}

    public record Segment(Vector start, Vector end, int depth) {}

    public static List<Segment> generate(Vector start, Vector end, CustomLightningParameters parameters, long seed) {
        if (!finite(start) || !finite(end)) return List.of();
        Vector delta = end.clone().subtract(start);
        double distance = delta.length();
        if (distance < MIN_DISTANCE) return List.of();
        Random random = new Random(seed);
        List<Segment> result = new ArrayList<>();
        List<Vector> main = path(start, end, parameters.segmentLength(), parameters.pathJitter(), random);
        appendSegments(main, 0, result);
        addBranches(main, delta.clone().normalize(), 1, parameters, random, result);
        return List.copyOf(result);
    }

    private static List<Vector> path(Vector start, Vector end, double segmentLength, double jitter, Random random) {
        Vector delta = end.clone().subtract(start);
        double distance = delta.length();
        Vector forward = delta.clone().normalize();
        Vector reference = Math.abs(forward.getY()) < .9 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector side = forward.clone().crossProduct(reference).normalize();
        Vector up = side.clone().crossProduct(forward).normalize();
        int count = Math.max(1, (int) Math.ceil(distance / segmentLength));
        List<Vector> points = new ArrayList<>(count + 1);
        for (int i = 0; i <= count; i++) {
            double t = i / (double) count;
            Vector point = start.clone().add(delta.clone().multiply(t));
            if (i > 0 && i < count) {
                double envelope = Math.sin(Math.PI * t);
                point.add(side.clone().multiply(signed(random) * jitter * envelope));
                point.add(up.clone().multiply(signed(random) * jitter * .65 * envelope));
            }
            points.add(point);
        }
        return points;
    }

    private static void addBranches(List<Vector> parent, Vector forward, int depth,
            CustomLightningParameters p, Random random, List<Segment> result) {
        if (depth > p.maxBranchDepth() || p.branchLength() <= 0) return;
        for (int i = 1; i < parent.size() - 1; i++) {
            if (random.nextDouble() >= p.branchChance() / depth) continue;
            Vector origin = parent.get(i);
            Vector randomVector = new Vector(signed(random), signed(random), signed(random));
            Vector perpendicular = randomVector.subtract(forward.clone().multiply(randomVector.dot(forward)));
            if (perpendicular.lengthSquared() < 1.0e-6) perpendicular = new Vector(1, 0, 0);
            perpendicular.normalize();
            double length = p.branchLength() / depth * (.65 + random.nextDouble() * .35);
            Vector branchDirection = forward.clone().multiply(.35).add(perpendicular).normalize();
            Vector end = origin.clone().add(branchDirection.multiply(length));
            List<Vector> branch = path(origin, end, p.segmentLength() * .8, p.pathJitter() * .55, random);
            appendSegments(branch, depth, result);
            addBranches(branch, end.clone().subtract(origin).normalize(), depth + 1, p, random, result);
        }
    }

    private static void appendSegments(List<Vector> points, int depth, List<Segment> output) {
        for (int i = 1; i < points.size(); i++) output.add(new Segment(points.get(i - 1), points.get(i), depth));
    }

    private static double signed(Random random) { return random.nextDouble() * 2 - 1; }
    private static boolean finite(Vector vector) {
        return Double.isFinite(vector.getX()) && Double.isFinite(vector.getY()) && Double.isFinite(vector.getZ());
    }
}
