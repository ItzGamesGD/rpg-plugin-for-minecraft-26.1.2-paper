package com.hyunseo.hyunseorpg.exploration.runtime;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExplorationRuntimeManagerTest {
    @Test
    void nextWaveRepeatsOnlyTheComponentForTheSelectedTier() {
        assertTrue(ExplorationRuntimeManager.matchesSelectedRaidWave("tier1", ExplorationComponentPhase.CHOICE_TIER_1));
        assertFalse(ExplorationRuntimeManager.matchesSelectedRaidWave("tier1", ExplorationComponentPhase.CHOICE_TIER_2));
        assertTrue(ExplorationRuntimeManager.matchesSelectedRaidWave("tier2", ExplorationComponentPhase.CHOICE_TIER_2));
        assertFalse(ExplorationRuntimeManager.matchesSelectedRaidWave("tier2", ExplorationComponentPhase.CHOICE_TIER_3));
        assertTrue(ExplorationRuntimeManager.matchesSelectedRaidWave("tier3", ExplorationComponentPhase.CHOICE_TIER_3));
        assertFalse(ExplorationRuntimeManager.matchesSelectedRaidWave("flee", ExplorationComponentPhase.CHOICE_TIER_1));
    }
}
