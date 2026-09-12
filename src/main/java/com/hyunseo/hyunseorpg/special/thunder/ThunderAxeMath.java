package com.hyunseo.hyunseorpg.special.thunder;

import org.bukkit.util.Vector;
import java.util.*;

public final class ThunderAxeMath {
    public static final int WAVE_COUNT = 5;
    public static final int MAX_RING_PARTICLES = 48;
    private ThunderAxeMath() {}

    public static boolean fullCharge(int held, int required) { return held >= required; }
    public static int advanceHit(int current, int threshold, boolean direct) {
        if (!direct) return current;
        return current + 1 >= Math.max(1, threshold) ? 0 : current + 1;
    }
    public static boolean triggers(int current, int threshold, boolean direct) {
        return direct && current + 1 >= Math.max(1, threshold);
    }

    /** Wave one is central; waves two-five each contain five widening fan lanes. */
    public static List<Vector> fan(int wave, Vector forward, double firstDistance, double step, double angleDegrees) {
        return fan(wave, WAVE_COUNT, 5, forward, firstDistance, step, angleDegrees);
    }

    public static List<Vector> fan(int wave, int waveCount, int directionCount, Vector forward,
            double firstDistance, double step, double angleDegrees) {
        if (wave < 1 || wave > Math.max(1, waveCount)) return List.of();
        Vector flat = forward.clone().setY(0);
        if (flat.lengthSquared() == 0) flat.setZ(1);
        flat.normalize();
        double distance = firstDistance + (wave - 1) * step;
        if (wave == 1) return List.of(flat.multiply(distance));
        int lanes = Math.max(1, directionCount);
        List<Vector> result = new ArrayList<>(lanes);
        for (int lane = 0; lane < lanes; lane++) {
            double fraction = lanes == 1 ? 0 : (lane - (lanes - 1) / 2.0) / ((lanes - 1) / 2.0);
            result.add(flat.clone().rotateAroundY(Math.toRadians(angleDegrees * fraction)).multiply(distance));
        }
        return result;
    }

    /** Translates fan offsets into the immutable cast space captured at release. */
    public static List<Vector> anchoredFan(Vector castOrigin, int wave, Vector forward,
            double firstDistance, double step, double angleDegrees) {
        return fan(wave, forward, firstDistance, step, angleDegrees).stream()
                .map(offset -> castOrigin.clone().add(offset)).toList();
    }

    public static List<Vector> anchoredFan(Vector castOrigin, int wave, int waveCount, int directionCount,
            Vector forward, double firstDistance, double step, double angleDegrees) {
        return fan(wave, waveCount, directionCount, forward, firstDistance, step, angleDegrees).stream()
                .map(offset -> castOrigin.clone().add(offset)).toList();
    }

    public static List<Vector> ring(double radius, int requested) {
        int count = Math.max(8, Math.min(MAX_RING_PARTICLES, requested));
        List<Vector> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double theta = Math.PI * 2 * i / count;
            result.add(new Vector(radius * Math.cos(theta), .03 * Math.sin(theta * 3), radius * Math.sin(theta)));
        }
        return result;
    }

    public static <T> List<List<T>> distanceWaves(Map<T, Double> distances, double radius,
            int perWave, int maxTargets, int maxWaves) {
        List<T> sorted = distances.entrySet().stream().filter(e -> e.getValue() <= radius * radius)
                .sorted(Comparator.<Map.Entry<T, Double>>comparingDouble(Map.Entry::getValue)
                        .thenComparing(e -> String.valueOf(e.getKey())))
                .limit(Math.max(0, maxTargets)).map(Map.Entry::getKey).toList();
        List<List<T>> waves = new ArrayList<>();
        int size = Math.max(1, perWave);
        for (int i = 0; i < sorted.size() && waves.size() < Math.max(0, maxWaves); i += size)
            waves.add(List.copyOf(sorted.subList(i, Math.min(sorted.size(), i + size))));
        return waves;
    }

    /** Deterministic greedy chain used by tests and suitable for non-Bukkit target planning. */
    public static <T> List<T> greedyChain(T initial, Map<T, Map<T, Double>> squaredDistances,
            double radius, int maximum) {
        List<T> route = new ArrayList<>(); Set<T> visited = new HashSet<>(); visited.add(initial);
        T cursor = initial;
        while (route.size() < Math.max(0, maximum)) {
            Map<T, Double> choices = squaredDistances.getOrDefault(cursor, Map.of());
            T next = choices.entrySet().stream().filter(e -> e.getValue() <= radius * radius)
                    .filter(e -> !visited.contains(e.getKey()))
                    .min(Comparator.<Map.Entry<T, Double>>comparingDouble(Map.Entry::getValue)
                            .thenComparing(e -> String.valueOf(e.getKey())))
                    .map(Map.Entry::getKey).orElse(null);
            if (next == null) break;
            visited.add(next); route.add(next); cursor = next;
        }
        return route;
    }
}
