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
        assertTrue(PyramidLootTriggerPolicy.isRetryable(Map.of(
                "loot-taken", "true", "pyramid-room-prepared", "true",
                "pyramid-loot-trigger-status", "STARTED")));
        assertTrue(PyramidLootTriggerPolicy.isCommittedOrInFlight(Map.of(
                "loot-taken", "true", "pyramid-loot-trigger-status", "REVEALED")));
    }

    @Test
    void recoveryRequiredStateFailsClosed() {
        assertFalse(PyramidLootTriggerPolicy.isRetryable(Map.of(
                "loot-taken", "true", "pyramid-failure-state", "RECOVERY_REQUIRED")));
        assertFalse(PyramidLootTriggerPolicy.isRetryable(Map.of()));
    }

    @Test
    void preparedLegacyStartedStateDoesNotBlockRevealRetry() {
        assertTrue(PyramidLootTriggerPolicy.isRetryable(Map.of(
                "loot-taken", "true", "pyramid-room-prepared", "true",
                "pyramid-loot-trigger-status", "started")));
        assertFalse(PyramidLootTriggerPolicy.isRetryable(Map.of(
                "loot-taken", "true", "pyramid-loot-trigger-status", "revealed")));
    }

    @Test
    void livePreparedRecordCanRearmRevealAfterRuntimeRecreation() {
        Map<String, String> livePrepared = Map.of(
                "loot-taken", "true",
                "pyramid-room-prepared", "true",
                "pyramid-room-origin", "120,35,-40",
                "pyramid-room-radius", "4",
                "pyramid-room-height", "4",
                "pyramid-loot-trigger-status", "PREPARED");

        // This is the durable gate used before activate() rehydrates the
        // runtime and schedules pyramid_room_reveal again.
        assertTrue(PyramidLootTriggerPolicy.isRetryable(livePrepared));
        assertFalse(PyramidLootTriggerPolicy.isCommittedOrInFlight(livePrepared));
    }

    @Test
    void guardianCompletionDoesNotImplyUndergroundCompletion() {
        Map<String, String> guardianOnly = Map.of(
                "pyramid-guardian-started", "true",
                "pyramid-guardian-complete", "true",
                "pyramid-guardian-encounter-state", "complete");

        assertFalse(PyramidLootTriggerPolicy.isCommittedOrInFlight(guardianOnly));
        assertFalse(Boolean.parseBoolean(guardianOnly.getOrDefault("pyramid-underground-complete", "false")));
    }
}
