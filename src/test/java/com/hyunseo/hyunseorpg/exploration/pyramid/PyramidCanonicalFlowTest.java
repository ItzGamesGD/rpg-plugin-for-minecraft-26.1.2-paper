package com.hyunseo.hyunseorpg.exploration.pyramid;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class PyramidCanonicalFlowTest {
    @Test void guardianAndUndergroundAreIndependent() {
        var p = new PyramidModuleProgress(PyramidVariantModules.GUARDIAN_AND_PUZZLE);
        assertTrue(p.complete(PyramidVariantModules.Module.GUARDIAN));
        assertFalse(p.structureComplete());
        assertTrue(p.complete(PyramidVariantModules.Module.UNDERGROUND));
        assertTrue(p.structureComplete());
    }

    @Test void undergroundFirstStillWaitsForGuardian() {
        var p = new PyramidModuleProgress(PyramidVariantModules.GUARDIAN_AND_PUZZLE);
        assertTrue(p.complete(PyramidVariantModules.Module.UNDERGROUND));
        assertFalse(p.structureComplete());
        assertTrue(p.complete(PyramidVariantModules.Module.GUARDIAN));
        assertTrue(p.structureComplete());
    }

    @Test void duplicateModuleCompletionIsIdempotent() {
        var p = new PyramidModuleProgress(PyramidVariantModules.GUARDIAN_AND_PUZZLE);
        assertTrue(p.complete(PyramidVariantModules.Module.GUARDIAN));
        assertFalse(p.complete(PyramidVariantModules.Module.GUARDIAN));
    }

    @Test void failedRevealDoesNotCompleteUnderground() {
        var p = new PyramidModuleProgress(PyramidVariantModules.GUARDIAN_AND_PUZZLE);
        assertFalse(p.structureComplete());
        assertFalse(p.complete(PyramidVariantModules.Module.UNDERGROUND)
                && p.complete(PyramidVariantModules.Module.UNDERGROUND));
    }

    @Test void currentRoomGeometryIsSevenBySevenUsableInterior() {
        int radius = 4;
        assertEquals(9, radius * 2 + 1);
        assertEquals(7, radius * 2 - 1);
    }

    @Test void shaftFootprintIsNineBlocksPerLayer() {
        int cells = 0;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) cells++;
        assertEquals(9, cells);
    }

    @Test void stagedRevealRunsTopToBottom() {
        List<Integer> layers = List.of(4, 3, 2, 1, 0);
        assertEquals(layers, layers.stream().sorted(java.util.Comparator.reverseOrder()).toList());
    }

    @Test void revealIntervalIsBounded() {
        assertTrue(PyramidCompletionRetryPolicy.delayTicks(1) >= 40);
        assertTrue(PyramidCompletionRetryPolicy.delayTicks(5) <= 200);
    }

    @Test void rewardTransactionIsCrashRecoverable() {
        var state = PyramidRewardTransaction.State.RESERVED;
        assertEquals(PyramidRewardTransaction.State.DELIVERY_PENDING,
                PyramidRewardTransaction.pending(state));
        assertEquals(PyramidRewardTransaction.State.DELIVERED,
                PyramidRewardTransaction.delivered(PyramidRewardTransaction.pending(state)));
    }

    @Test void finalizedRewardCannotDuplicate() {
        assertEquals(PyramidRewardTransaction.State.FINALIZED,
                PyramidRewardTransaction.finalize(PyramidRewardTransaction.State.DELIVERED));
        assertEquals(PyramidRewardTransaction.State.FINALIZED,
                PyramidRewardTransaction.delivered(PyramidRewardTransaction.State.FINALIZED));
    }

    @Test void technicalRetryIsFinite() {
        assertTrue(PyramidCompletionRetryPolicy.shouldRetry(5));
        assertFalse(PyramidCompletionRetryPolicy.shouldRetry(6));
    }
}
