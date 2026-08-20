package com.hyunseo.hyunseorpg.alchemy;

/** Pure timing rules for the interval-based Shock pulse/root contract. */
final class ShockTimingPolicy {
    private ShockTimingPolicy() { }

    static long interval(long configuredInterval) {
        // An interval of one tick cannot contain a strictly shorter positive root.
        return Math.max(2L, configuredInterval);
    }

    static long firstPulseAt(long currentTick, long configuredInterval) {
        return currentTick + interval(configuredInterval);
    }

    static long nextPulseAt(long currentTick, long configuredInterval) {
        return currentTick + interval(configuredInterval);
    }

    static long effectiveRootDuration(long configuredDuration, long configuredInterval) {
        long interval = interval(configuredInterval);
        return Math.max(1L, Math.min(Math.max(1L, configuredDuration), interval - 1L));
    }
}
