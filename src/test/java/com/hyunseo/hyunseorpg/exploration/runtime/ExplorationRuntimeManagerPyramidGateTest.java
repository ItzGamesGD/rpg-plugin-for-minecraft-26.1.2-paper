package com.hyunseo.hyunseorpg.exploration.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ExplorationRuntimeManagerPyramidGateTest {
    @Test
    void recoveryRequiredBlocksCentralPyramidCompletion() {
        assertFalse(ExplorationRuntimeManager.pyramidCompletionAllowed(
                "desert_pyramid", Map.of("pyramid-failure-state", "RECOVERY_REQUIRED")));
    }

    @Test
    void normalPyramidAndOtherStructuresRemainEligible() {
        assertTrue(ExplorationRuntimeManager.pyramidCompletionAllowed(
                "desert_pyramid", Map.of("pyramid-underground-complete", "true")));
        assertTrue(ExplorationRuntimeManager.pyramidCompletionAllowed(
                "pillager_outpost", Map.of("pyramid-failure-state", "RECOVERY_REQUIRED")));
    }
}
