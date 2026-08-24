package com.hyunseo.hyunseorpg.exploration.pyramid;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PyramidModuleProgressTest {

    @Test
    void combinedVariantRequiresEachIndependentModuleExactlyOnce() {
        PyramidModuleProgress progress = new PyramidModuleProgress(PyramidVariantModules.GUARDIAN_AND_PUZZLE);

        assertTrue(progress.complete(PyramidVariantModules.Module.GUARDIAN));
        assertFalse(progress.complete(PyramidVariantModules.Module.GUARDIAN));
        assertFalse(progress.structureComplete());

        assertTrue(progress.complete(PyramidVariantModules.Module.UNDERGROUND));
        assertTrue(progress.structureComplete());
    }

    @Test
    void unavailablePuzzleDoesNotRerollVariantAndOnlyPuzzleOnlyEndsWithoutReward() {
        PyramidModuleProgress combined = new PyramidModuleProgress(PyramidVariantModules.GUARDIAN_AND_PUZZLE);
        assertTrue(combined.markUnavailable(PyramidVariantModules.Module.UNDERGROUND));
        assertFalse(combined.structureComplete());
        assertFalse(combined.terminalWithoutReward());
        assertTrue(combined.complete(PyramidVariantModules.Module.GUARDIAN));
        assertTrue(combined.structureComplete());

        PyramidModuleProgress puzzleOnly = new PyramidModuleProgress(PyramidVariantModules.UNDERGROUND_PUZZLE_ONLY);
        assertTrue(puzzleOnly.markUnavailable(PyramidVariantModules.Module.UNDERGROUND));
        assertTrue(puzzleOnly.structureComplete());
        assertTrue(puzzleOnly.terminalWithoutReward());
    }
}
