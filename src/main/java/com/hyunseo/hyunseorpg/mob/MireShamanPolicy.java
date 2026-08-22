package com.hyunseo.hyunseorpg.mob;

/** Pure, balance-independent guards for the Mire Shaman prototype. */
final class MireShamanPolicy {
    enum Pattern { POOL, SUMMON }

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

    static boolean summonAllowed(int currentlyActive, int maximumActive) {
        return Math.max(0, currentlyActive) < Math.max(0, maximumActive);
    }

    static Pattern choosePattern(boolean poolAvailable, boolean summonAvailable,
                                 double poolWeight, double summonWeight, double roll) {
        if (!poolAvailable && !summonAvailable) return null;
        if (!poolAvailable) return Pattern.SUMMON;
        if (!summonAvailable) return Pattern.POOL;
        double safePool = Math.max(0.0D, poolWeight);
        double safeSummon = Math.max(0.0D, summonWeight);
        if (safePool + safeSummon <= 0.0D) return Pattern.POOL;
        double normalizedRoll = Math.max(0.0D, Math.min(0.999999D, roll));
        return normalizedRoll < safePool / (safePool + safeSummon) ? Pattern.POOL : Pattern.SUMMON;
    }
}
