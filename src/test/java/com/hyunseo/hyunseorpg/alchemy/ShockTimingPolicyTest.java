package com.hyunseo.hyunseorpg.alchemy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShockTimingPolicyTest {
    @Test
    void firstPulseWaitsForTheConfiguredInterval() {
        assertEquals(180L, ShockTimingPolicy.firstPulseAt(100L, 80L));
        assertEquals(240L, ShockTimingPolicy.nextPulseAt(160L, 80L));
        assertFalse(100L >= ShockTimingPolicy.firstPulseAt(100L, 80L));
        assertTrue(180L >= ShockTimingPolicy.firstPulseAt(100L, 80L));
    }

    @Test
    void effectiveRootIsStrictlyShorterThanTheInterval() {
        assertEquals(20L, ShockTimingPolicy.effectiveRootDuration(20L, 80L));
        assertEquals(79L, ShockTimingPolicy.effectiveRootDuration(200L, 80L));
        assertTrue(ShockTimingPolicy.effectiveRootDuration(20L, 80L)
                < ShockTimingPolicy.interval(80L));
    }

    @Test
    void reusingAOneTickIntervalStillLeavesAPositiveShorterRoot() {
        assertEquals(2L, ShockTimingPolicy.interval(1L));
        assertEquals(1L, ShockTimingPolicy.effectiveRootDuration(20L, 1L));
    }
}
