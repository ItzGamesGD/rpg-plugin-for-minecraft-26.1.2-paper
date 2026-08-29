package com.hyunseo.hyunseorpg.exploration.pyramid;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PyramidLootTriggerPolicyTest {
    @Test
    void persistedLootWithoutUndergroundContentRemainsRetryable() {
        assertTrue(PyramidLootTriggerPolicy.isRetryable(Map.of("loot-taken", "true")));
        assertTrue(PyramidLootTriggerPolicy.isRetryable(Map.of(
                "loot-taken", "true", "pyramid-room-prepared", "true",
                "pyramid-loot-trigger-status", "RETRYABLE")));
    }

    @Test
    void committedOrInFlightContentCannotBeTriggeredTwice() {
        assertFalse(PyramidLootTriggerPolicy.isRetryable(Map.of(
                "loot-taken", "true", "pyramid-room-created", "true")));
        assertFalse(PyramidLootTriggerPolicy.isRetryable(Map.of(
                "loot-taken", "true", "pyramid-reveal-in-progress", "true")));
        assertFalse(PyramidLootTriggerPolicy.isRetryable(Map.of(
                "loot-taken", "true", "pyramid-loot-trigger-status", "STARTED")));
        assertTrue(PyramidLootTriggerPolicy.isCommittedOrInFlight(Map.of(
                "loot-taken", "true", "pyramid-loot-trigger-status", "STARTED")));
    }

    @Test
    void recoveryRequiredStateFailsClosed() {
        assertFalse(PyramidLootTriggerPolicy.isRetryable(Map.of(
                "loot-taken", "true", "pyramid-failure-state", "RECOVERY_REQUIRED")));
        assertFalse(PyramidLootTriggerPolicy.isRetryable(Map.of()));
    }
}
