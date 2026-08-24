package com.hyunseo.hyunseorpg.exploration.ocean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class OceanMonumentProgressTest {
    private static OceanMonumentProgress progress() {
        return new OceanMonumentProgress(
                Set.of("seal-east", "seal-north", "seal-west"),
                Set.of("monument-army", "elite-guardians"),
                true
        );
    }

    @Test
    void startsAtDiscoveryWithoutRewardEligibility() {
        OceanMonumentProgress progress = progress();

        assertEquals(MonumentPhase.DISCOVERY, progress.phase());
        assertEquals(3, progress.remainingSealCount());
        assertFalse(progress.rewardEligible());
    }

    @Test
    void sealsProgressExactlyOnceAndFinalSealNeedsWarningAcknowledgement() {
        OceanMonumentProgress progress = progress();
        progress.discover();

        assertTrue(progress.completeSeal("seal-east").applied());
        assertFalse(progress.completeSeal("seal-east").applied());
        assertEquals(MonumentActionResult.Reason.DUPLICATE,
                progress.completeSeal("seal-east").reason());
        assertEquals(1, progress.completedSealCount());

        assertTrue(progress.completeSeal("seal-north").applied());
        assertTrue(progress.finalSealWarningRequired());
        assertEquals(MonumentActionResult.Reason.FINAL_SEAL_WARNING_REQUIRED,
                progress.completeSeal("seal-west").reason());
        assertEquals(2, progress.completedSealCount());

        assertTrue(progress.acknowledgeFinalSealWarning().applied());
        assertEquals(MonumentPhase.FINAL_SEAL_READY, progress.phase());
        assertTrue(progress.completeSeal("seal-west").applied());
        assertEquals(MonumentPhase.TRANSITION_PENDING, progress.phase());
    }

    @Test
    void unknownSealAndPrematureEncounterDoNotCorruptState() {
        OceanMonumentProgress progress = progress();
        MonumentActionResult unknown = progress.completeSeal("not-a-seal");

        assertEquals(MonumentActionResult.Reason.UNKNOWN_ID, unknown.reason());
        assertEquals(MonumentPhase.DISCOVERY, progress.phase());
        assertEquals(MonumentActionResult.Reason.INVALID_PHASE, progress.beginEncounter().reason());
        assertEquals(MonumentPhase.DISCOVERY, progress.phase());
    }

    @Test
    void objectivesAreExactlyOnceAndBossNeedsAllOfThem() {
        OceanMonumentProgress progress = advanceToEncounter(progress());

        assertEquals(MonumentActionResult.Reason.INVALID_PHASE, progress.completeBoss().reason());
        assertTrue(progress.completeObjective("monument-army").applied());
        assertFalse(progress.completeObjective("monument-army").applied());
        assertEquals(1, progress.completedObjectiveCount());
        assertEquals(MonumentPhase.ENCOUNTER_ACTIVE, progress.phase());

        assertTrue(progress.completeObjective("elite-guardians").applied());
        assertEquals(MonumentPhase.BOSS_ELIGIBLE, progress.phase());
        assertTrue(progress.bossEligible());
        assertTrue(progress.completeBoss().applied());
        assertEquals(MonumentPhase.CLEAR_ELIGIBLE, progress.phase());
    }

    @Test
    void clearAndLogicalRewardClaimAreBothExactlyOnce() {
        OceanMonumentProgress progress = advanceToClearEligible(progress());

        assertTrue(progress.clear().applied());
        assertFalse(progress.clear().applied());
        assertTrue(progress.rewardEligible());
        assertTrue(progress.claimFinalReward().applied());
        assertFalse(progress.claimFinalReward().applied());
        assertEquals(MonumentActionResult.Reason.REWARD_ALREADY_CLAIMED,
                progress.claimFinalReward().reason());
        assertTrue(progress.rewardClaimed());
    }

    @Test
    void abandonLocksLaterProgressionWithoutMutatingCounts() {
        OceanMonumentProgress progress = progress();
        progress.discover();
        progress.completeSeal("seal-east");
        assertTrue(progress.abandon().applied());

        assertEquals(MonumentPhase.ABANDONED, progress.phase());
        assertEquals(1, progress.completedSealCount());
        assertEquals(MonumentActionResult.Reason.TERMINAL,
                progress.completeSeal("seal-north").reason());
        assertEquals(MonumentActionResult.Reason.TERMINAL,
                progress.acknowledgeFinalSealWarning().reason());
        assertEquals(MonumentActionResult.Reason.TERMINAL, progress.clear().reason());
    }

    @Test
    void definitionRequiresTheConfirmedThreeSealsAndNonBlankIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> new OceanMonumentProgress(
                Set.of("seal-one", "seal-two"),
                Set.of("army"),
                true
        ));
        assertThrows(IllegalArgumentException.class, () -> new OceanMonumentProgress(
                Set.of("seal-one", "seal-two", "seal-three"),
                Set.of(" "),
                true
        ));
    }

    private static OceanMonumentProgress advanceToEncounter(OceanMonumentProgress progress) {
        progress.discover();
        progress.completeSeal("seal-east");
        progress.completeSeal("seal-north");
        progress.acknowledgeFinalSealWarning();
        progress.completeSeal("seal-west");
        progress.beginEncounter();
        return progress;
    }

    private static OceanMonumentProgress advanceToClearEligible(OceanMonumentProgress progress) {
        progress = advanceToEncounter(progress);
        progress.completeObjective("monument-army");
        progress.completeObjective("elite-guardians");
        progress.completeBoss();
        return progress;
    }
}
