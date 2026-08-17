package com.hyunseo.hyunseorpg.enhancement;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.random.RandomGenerator;

/** Shared, bounded option-value roll policy for promotion and reroll flows. */
public final class OptionRollService {
    private static final double EPSILON = 0.000001D;

    private OptionRollService() { }

    public record Settings(boolean enabled, String distribution, double lowerHalfWeight,
                           double upperHalfWeight, String withinRangeDistribution,
                           boolean preserveMaximumRoll) {
        public Settings {
            distribution = distribution == null ? "weighted-halves" : distribution.trim().toLowerCase();
            withinRangeDistribution = withinRangeDistribution == null
                    ? "uniform" : withinRangeDistribution.trim().toLowerCase();
            lowerHalfWeight = finitePositive(lowerHalfWeight, 3.0D);
            upperHalfWeight = finitePositive(upperHalfWeight, 1.0D);
        }

        private static double finitePositive(double value, double fallback) {
            return Double.isFinite(value) && value > 0.0D ? value : fallback;
        }
    }

    /**
     * Rolls a step-aligned value in [minimum, maximum]. lowerBound is the
     * current aggregate for a reroll, or the option minimum for a first roll.
     */
    public static OptionalDouble roll(double minimum, double maximum, double step, double lowerBound,
                                      Settings settings, RandomGenerator random) {
        if (settings == null || !settings.enabled() || random == null
                || !Double.isFinite(minimum) || !Double.isFinite(maximum)
                || !Double.isFinite(step) || !Double.isFinite(lowerBound)
                || minimum > maximum + EPSILON || step <= 0.0D) {
            return OptionalDouble.empty();
        }
        double start = Math.max(minimum, lowerBound);
        if (start > maximum + EPSILON) return OptionalDouble.empty();

        List<Double> values = stepValues(minimum, maximum, step, settings.preserveMaximumRoll());
        values = values.stream().filter(value -> value + EPSILON >= start).toList();
        if (values.isEmpty()) return OptionalDouble.empty();

        if (!"weighted-halves".equals(settings.distribution())
                || !"uniform".equals(settings.withinRangeDistribution())) {
            return OptionalDouble.of(values.get(random.nextInt(values.size())));
        }

        double midpoint = minimum + (maximum - minimum) * 0.5D;
        List<Double> lower = new ArrayList<>();
        List<Double> upper = new ArrayList<>();
        for (double value : values) {
            if (value <= midpoint + EPSILON) lower.add(value);
            else upper.add(value);
        }

        List<Double> selected;
        if (lower.isEmpty()) selected = upper;
        else if (upper.isEmpty()) selected = lower;
        else {
            double totalWeight = settings.lowerHalfWeight() + settings.upperHalfWeight();
            selected = random.nextDouble() * totalWeight < settings.lowerHalfWeight() ? lower : upper;
        }
        if (selected.isEmpty()) return OptionalDouble.empty();
        return OptionalDouble.of(selected.get(random.nextInt(selected.size())));
    }

    private static List<Double> stepValues(double minimum, double maximum, double step, boolean preserveMaximum) {
        List<Double> values = new ArrayList<>();
        long count = (long) Math.floor((maximum - minimum) / step + EPSILON);
        for (long index = 0; index <= count; index++) {
            double value = minimum + index * step;
            if (value <= maximum + EPSILON) values.add(normalize(value));
        }
        if (preserveMaximum && values.stream().noneMatch(value -> Math.abs(value - maximum) <= EPSILON)) {
            // Existing definitions are expected to be step-aligned. Keeping a
            // configured maximum reachable is safer than silently losing it.
            values.add(normalize(maximum));
        }
        return values.stream().distinct().sorted().toList();
    }

    private static double normalize(double value) {
        return Math.rint(value * 1_000_000_000D) / 1_000_000_000D;
    }
}
