package com.hyunseo.hyunseorpg.exploration.pyramid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PyramidPillarRecoveryPolicyTest {
    @Test
    void committedRoomRetriesUseOnlyPillarRestorePhase() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            PyramidPillarRecoveryPolicy.Decision decision = PyramidPillarRecoveryPolicy.afterFailure(attempt);
            assertTrue(decision.retry());
            assertFalse(decision.exhausted());
            assertEquals("pyramid_pillar_restore", decision.phase());
            assertTrue(decision.delayTicks() > 0L);
        }
    }

    @Test
    void recoveryStopsAfterBoundedAttemptsWithoutRevealFallback() {
        PyramidPillarRecoveryPolicy.Decision decision = PyramidPillarRecoveryPolicy.afterFailure(6);
        assertFalse(decision.retry());
        assertTrue(decision.exhausted());
        assertEquals(-1L, decision.delayTicks());
        assertEquals("pyramid_pillar_restore", decision.phase());
    }
}
