package com.hyunseo.hyunseorpg.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OutpostRaiderPolicyTest {
    @Test
    void shieldGuardDamageReductionStaysInTheConfiguredSaneRange() {
        assertEquals(0.4D, OutpostRaiderPolicy.guardDamageMultiplier(0.1D));
        assertEquals(0.5D, OutpostRaiderPolicy.guardDamageMultiplier(0.5D));
        assertEquals(0.6D, OutpostRaiderPolicy.guardDamageMultiplier(0.9D));
    }

    @Test
    void spikeEvokerBlocksOnlyVexSummonsAndMountedUnitIsExplicit() {
        assertTrue(OutpostRaiderPolicy.blocksEvokerSpell("SUMMON_VEX"));
        assertFalse(OutpostRaiderPolicy.blocksEvokerSpell("FANGS"));
        assertTrue(OutpostRaiderPolicy.isMountedHeavyUnit("ravager_rider"));
        assertFalse(OutpostRaiderPolicy.isMountedHeavyUnit("banner_raider"));
    }
}
