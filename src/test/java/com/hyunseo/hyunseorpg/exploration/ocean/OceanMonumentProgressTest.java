package com.hyunseo.hyunseorpg.exploration.ocean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OceanMonumentProgressTest {
    @Test
    void advancesUsingOnlyLogicalState() {
        OceanMonumentProgress progress = new OceanMonumentProgress();
        assertEquals(MonumentActionResult.ACCEPTED, progress.begin());
        assertEquals(MonumentActionResult.ACCEPTED, progress.defeatElderGuardian());
        assertEquals(MonumentActionResult.ACCEPTED, progress.defeatElderGuardian());
        assertEquals(MonumentActionResult.ACCEPTED, progress.defeatElderGuardian());
        assertEquals(MonumentPhase.CORE_EXPOSED, progress.phase());
        assertEquals(MonumentActionResult.ACCEPTED, progress.complete());
        assertEquals(MonumentPhase.COMPLETED, progress.phase());
    }

    @Test
    void rejectsActionsOutsideTheirPhase() {
        OceanMonumentProgress progress = new OceanMonumentProgress();
        assertEquals(MonumentActionResult.INVALID_PHASE, progress.defeatElderGuardian());
        assertEquals(MonumentActionResult.INVALID_PHASE, progress.complete());
    }
}
