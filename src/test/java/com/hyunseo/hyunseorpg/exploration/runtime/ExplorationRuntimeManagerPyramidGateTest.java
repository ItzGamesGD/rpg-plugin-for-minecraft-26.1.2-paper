package com.hyunseo.hyunseorpg.exploration.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import com.hyunseo.hyunseorpg.exploration.model.StructureAnchor;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
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

    @Test
    void finalGateRequiresAuthoritativeActiveHealthyRecordAndBothModules() {
        assertFalse(ExplorationRuntimeManager.pyramidFinalCompletionAllowed(null));
        assertFalse(ExplorationRuntimeManager.pyramidFinalCompletionAllowed(pyramid(
                StructureEventState.ACTIVE, Map.of("pyramid-failure-state", "RECOVERY_REQUIRED",
                        "pyramid-guardian-complete", "true", "pyramid-underground-complete", "true"))));
        assertFalse(ExplorationRuntimeManager.pyramidFinalCompletionAllowed(pyramid(
                StructureEventState.ACTIVE, Map.of("pyramid-guardian-complete", "true"))));
        assertFalse(ExplorationRuntimeManager.pyramidFinalCompletionAllowed(pyramid(
                StructureEventState.CLEARED, completeMetadata())));
        assertFalse(ExplorationRuntimeManager.pyramidFinalCompletionAllowed(pyramid(
                StructureEventState.ABANDONED, completeMetadata())));
        assertTrue(ExplorationRuntimeManager.pyramidFinalCompletionAllowed(pyramid(
                StructureEventState.ACTIVE, completeMetadata())));
    }

    @Test
    void recoveryRequiredBlocksPendingAndActiveGuardianRestart() {
        for (String encounter : java.util.List.of("spawn_pending", "active")) {
            assertFalse(ExplorationRuntimeManager.pyramidGuardianRecoveryAllowed(pyramid(
                    StructureEventState.ACTIVE, Map.of("pyramid-guardian-encounter-state", encounter,
                            "pyramid-failure-state", "RECOVERY_REQUIRED"))));
        }
        assertTrue(ExplorationRuntimeManager.pyramidGuardianRecoveryAllowed(pyramid(
                StructureEventState.ACTIVE, Map.of("pyramid-guardian-encounter-state", "spawn_pending"))));
    }

    private static Map<String, String> completeMetadata() {
        return Map.of("pyramid-guardian-complete", "true", "pyramid-underground-complete", "true");
    }

    private static StructureRecord pyramid(StructureEventState state, Map<String, String> metadata) {
        UUID world = UUID.randomUUID();
        return new StructureRecord(UUID.randomUUID(), world, "desert_pyramid", "minecraft:desert_pyramid",
                new StructureAnchor(world, 0, 64, 0), new StructureBounds(-10, 58, -10, 10, 80, 10),
                true, "guardian_trial", state, metadata, false, Instant.now(), null, 1);
    }
}
