package com.hyunseo.hyunseorpg.player;

/** Timestamps only; kill counts remain canonical in PlayerRPGData.customMobKillCounts. */
public record MonsterDiscoveryData(long firstSeenAt, long firstKilledAt) {
    public MonsterDiscoveryData {
        firstSeenAt = Math.max(0L, firstSeenAt);
        firstKilledAt = Math.max(0L, firstKilledAt);
    }

    public MonsterDiscoveryData seen(long timestamp) {
        return new MonsterDiscoveryData(firstSeenAt > 0L ? firstSeenAt : Math.max(0L, timestamp), firstKilledAt);
    }

    public MonsterDiscoveryData killed(long timestamp) {
        long safeTimestamp = Math.max(0L, timestamp);
        return new MonsterDiscoveryData(firstSeenAt > 0L ? firstSeenAt : safeTimestamp,
                firstKilledAt > 0L ? firstKilledAt : safeTimestamp);
    }
}
