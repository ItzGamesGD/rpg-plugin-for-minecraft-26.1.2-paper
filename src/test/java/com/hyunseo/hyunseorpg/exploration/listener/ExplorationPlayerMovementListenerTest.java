package com.hyunseo.hyunseorpg.exploration.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExplorationPlayerMovementListenerTest {
    @Test
    void lookOnlyUpdateDoesNotCountAsPhysicalMovement() {
        assertFalse(ExplorationPlayerMovementListener.positionChanged(
                12.25D, 64.0D, -3.75D,
                12.25D, 64.0D, -3.75D));
    }

    @Test
    void anyExactXyzDeltaCountsAsPhysicalMovement() {
        assertTrue(ExplorationPlayerMovementListener.positionChanged(
                12.25D, 64.0D, -3.75D,
                12.25001D, 64.0D, -3.75D));
        assertTrue(ExplorationPlayerMovementListener.positionChanged(
                12.25D, 64.0D, -3.75D,
                12.25D, 64.0D, -3.5D));
    }
}
