package com.hyunseo.hyunseorpg.mob;

/** Pure, balance-independent guards for the Mire Shaman prototype. */
final class MireShamanPolicy {
    private MireShamanPolicy() {
    }

    static boolean canReclaim(boolean reclaimUsed, double healthRatio, int ownedPoolCount, double threshold) {
        return !reclaimUsed
                && ownedPoolCount > 0
                && healthRatio <= Math.max(0.0D, Math.min(1.0D, threshold));
    }

    static int summonCount(int currentlyActive, int configuredCount, int maximumActive) {
        int remaining = Math.max(0, maximumActive - Math.max(0, currentlyActive));
        return Math.min(remaining, Math.max(0, configuredCount));
    }
}
