package com.hyunseo.hyunseorpg.exploration.ocean;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OceanMonumentProgressTest {
    private static OceanMonumentProgress progress() {
        return new OceanMonumentProgress(Set.of("north", "east", "west"), Set.of("drain", "guardians"));
    }

    @Test
    void requiresThreeKnownSealsAndFinalSealAcknowledgement() {
        assertThrows(IllegalArgumentException.class,
                () -> new OceanMonumentProgress(Set.of("north", "east"), Set.of("drain")));
        OceanMonumentProgress progress = progress();
        assertEquals(MonumentActionResult.APPLIED, progress.discover());
        assertEquals(MonumentActionResult.UNKNOWN_ID, progress.breakSeal("south"));
        assertEquals(MonumentActionResult.APPLIED, progress.breakSeal("north"));
        assertEquals(MonumentActionResult.DUPLICATE, progress.breakSeal("north"));
        assertEquals(MonumentActionResult.APPLIED, progress.breakSeal("east"));
        assertEquals(MonumentActionResult.ACKNOWLEDGEMENT_REQUIRED, progress.breakSeal("west"));
        assertEquals(MonumentActionResult.APPLIED, progress.acknowledgeFinalSealWarning());
        assertEquals(MonumentActionResult.APPLIED, progress.breakSeal("west"));
        assertEquals(MonumentPhase.TRANSITION, progress.phase());
    }

    @Test
    void objectivesGateBossAndBossGatesClear() {
        OceanMonumentProgress progress = progress();
        progress.discover();
        progress.breakSeal("north");
        progress.breakSeal("east");
        progress.acknowledgeFinalSealWarning();
        progress.breakSeal("west");
        assertEquals(MonumentActionResult.APPLIED, progress.beginEncounter());
        assertEquals(MonumentActionResult.NOT_ELIGIBLE, progress.defeatBoss());
        assertEquals(MonumentActionResult.UNKNOWN_ID, progress.completeObjective("treasure"));
        assertEquals(MonumentActionResult.APPLIED, progress.completeObjective("drain"));
        assertEquals(MonumentActionResult.DUPLICATE, progress.completeObjective("drain"));
        assertEquals(MonumentActionResult.APPLIED, progress.completeObjective("guardians"));
        assertTrue(progress.bossEligible());
        assertEquals(MonumentActionResult.APPLIED, progress.defeatBoss());
        assertTrue(progress.clearEligible());
        assertEquals(MonumentActionResult.APPLIED, progress.clear());
        assertEquals(MonumentActionResult.APPLIED, progress.claimFinalReward());
        assertEquals(MonumentActionResult.DUPLICATE, progress.claimFinalReward());
    }

    @Test
    void abandonmentIsTerminal() {
        OceanMonumentProgress progress = progress();
        assertEquals(MonumentActionResult.APPLIED, progress.abandon());
        assertEquals(MonumentPhase.ABANDONED, progress.phase());
        assertEquals(MonumentActionResult.TERMINAL, progress.discover());
        assertEquals(MonumentActionResult.TERMINAL, progress.breakSeal("north"));
        assertEquals(MonumentActionResult.TERMINAL, progress.claimFinalReward());
    }
}
