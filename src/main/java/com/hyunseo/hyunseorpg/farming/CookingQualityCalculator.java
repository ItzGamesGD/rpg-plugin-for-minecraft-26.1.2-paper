package com.hyunseo.hyunseorpg.farming;

import java.util.List;

/** Pure quality aggregation for cooking. Bukkit inventory access stays outside this class. */
public final class CookingQualityCalculator {
    private CookingQualityCalculator() { }

    public static int floorAverage(List<Contribution> contributions) {
        if (contributions == null || contributions.isEmpty()) return 0;
        long weightedScore = 0L;
        long amount = 0L;
        for (Contribution contribution : contributions) {
            if (contribution == null || contribution.amount() <= 0 || contribution.score() < 0) continue;
            weightedScore += Math.multiplyExact((long) contribution.score(), contribution.amount());
            amount += contribution.amount();
        }
        if (amount <= 0L) return 0;
        return (int) Math.floor((double) weightedScore / amount);
    }

    public record Contribution(int score, int amount) { }
}
