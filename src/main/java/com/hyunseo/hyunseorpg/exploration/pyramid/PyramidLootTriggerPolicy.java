package com.hyunseo.hyunseorpg.exploration.pyramid;

import java.util.Map;

/** Pure persistence policy for retrying a failed Pyramid loot trigger. */
public final class PyramidLootTriggerPolicy {
    private PyramidLootTriggerPolicy() {
    }

    /**
     * A durable loot reservation may be retried only while underground content
     * is incomplete and no reveal is already committed or in flight.
     */
    public static boolean isRetryable(Map<String, String> metadata) {
        if (metadata == null || !flag(metadata, "loot-taken")) return false;
        if (flag(metadata, "pyramid-room-created")
                || flag(metadata, "pyramid-reveal-in-progress")
                || flag(metadata, "pyramid-underground-complete")) return false;
        return !"RECOVERY_REQUIRED".equals(metadata.getOrDefault("pyramid-failure-state", ""))
                && !"STARTED".equals(metadata.getOrDefault("pyramid-loot-trigger-status", ""));
    }

    public static boolean isCommittedOrInFlight(Map<String, String> metadata) {
        if (metadata == null) return false;
        return flag(metadata, "pyramid-room-created")
                || flag(metadata, "pyramid-reveal-in-progress")
                || flag(metadata, "pyramid-underground-complete")
                || "STARTED".equals(metadata.getOrDefault("pyramid-loot-trigger-status", ""));
    }

    private static boolean flag(Map<String, String> metadata, String key) {
        return Boolean.parseBoolean(metadata.getOrDefault(key, "false"));
    }
}
