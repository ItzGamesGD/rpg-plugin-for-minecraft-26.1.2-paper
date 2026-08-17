package com.hyunseo.hyunseorpg.farming;

/** Pure catch-up calculation for elapsed crop growth while a chunk was not ticking. */
public final class CropGrowthCalculator {
    private CropGrowthCalculator() {
    }

    public static GrowthResult catchUp(int currentStage, int maxStage, long now,
                                       long nextGrowthAt, long secondsPerStage) {
        int safeMax = Math.max(0, maxStage);
        int safeCurrent = Math.max(0, Math.min(currentStage, safeMax));
        if (safeCurrent >= safeMax) {
            return new GrowthResult(safeMax, Long.MAX_VALUE,
                    safeCurrent != safeMax || nextGrowthAt != Long.MAX_VALUE);
        }
        if (nextGrowthAt == Long.MAX_VALUE || now < nextGrowthAt) {
            return new GrowthResult(safeCurrent, Math.max(0L, nextGrowthAt),
                    safeCurrent != currentStage || nextGrowthAt < 0L);
        }

        long interval = safeMillis(secondsPerStage);
        long elapsed = now - nextGrowthAt;
        long dueSteps = elapsed / interval;
        if (dueSteps < Long.MAX_VALUE) dueSteps++;
        long allowedSteps = safeMax - safeCurrent;
        long steps = Math.min(dueSteps, allowedSteps);
        int nextStage = (int) (safeCurrent + steps);
        if (nextStage >= safeMax) {
            return new GrowthResult(safeMax, Long.MAX_VALUE,
                    safeMax != currentStage || nextGrowthAt != Long.MAX_VALUE);
        }
        long nextDue = safeAdd(nextGrowthAt, safeMultiply(steps, interval));
        return new GrowthResult(nextStage, nextDue,
                nextStage != currentStage || nextDue != nextGrowthAt);
    }

    private static long safeMillis(long seconds) {
        long safeSeconds = Math.max(1L, seconds);
        return safeSeconds > Long.MAX_VALUE / 1000L
                ? Long.MAX_VALUE : safeSeconds * 1000L;
    }

    private static long safeMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static long safeAdd(long left, long right) {
        if (right > Long.MAX_VALUE - left) return Long.MAX_VALUE;
        return left + right;
    }

    public record GrowthResult(int stage, long nextGrowthAt, boolean changed) {
    }
}
