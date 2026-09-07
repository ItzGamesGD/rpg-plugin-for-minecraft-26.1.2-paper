package com.hyunseo.hyunseorpg.special.thanatos;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** Pure forward-cone filtering used by Death Sentence. */
public final class ThanatosTargeting {
    private ThanatosTargeting() { }

    public record Candidate<T>(T value, double x, double y, double z) { }

    public static <T> List<T> forward(List<Candidate<T>> candidates, double dx, double dy, double dz,
                                      double range, double minimumCosine) {
        if (!finite(dx) || !finite(dy) || !finite(dz) || !finite(range) || !finite(minimumCosine)
                || range < 0.0D) return List.of();
        double directionLength = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(directionLength) || directionLength == 0.0D) return List.of();
        List<T> result = new ArrayList<>();
        double rangeSquared = range * range;
        for (Candidate<T> candidate : candidates) {
            if (candidate == null || !finite(candidate.x()) || !finite(candidate.y()) || !finite(candidate.z())) continue;
            double lengthSquared = candidate.x() * candidate.x() + candidate.y() * candidate.y()
                    + candidate.z() * candidate.z();
            if (!Double.isFinite(lengthSquared) || lengthSquared == 0.0D || lengthSquared > rangeSquared) continue;
            double cosine = (candidate.x() * dx + candidate.y() * dy + candidate.z() * dz)
                    / (Math.sqrt(lengthSquared) * directionLength);
            if (Double.isFinite(cosine) && cosine >= minimumCosine) result.add(candidate.value());
        }
        return List.copyOf(result);
    }

    public static <T> T random(List<T> valid, RandomGenerator random) {
        return valid == null || valid.isEmpty() ? null : valid.get(random.nextInt(valid.size()));
    }

    private static boolean finite(double value) { return Double.isFinite(value); }
}
