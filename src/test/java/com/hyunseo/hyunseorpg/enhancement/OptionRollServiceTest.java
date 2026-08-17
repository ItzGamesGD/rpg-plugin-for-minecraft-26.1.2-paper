package com.hyunseo.hyunseorpg.enhancement;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OptionRollServiceTest {
    private static final OptionRollService.Settings SETTINGS = new OptionRollService.Settings(
            true, "weighted-halves", 3.0D, 1.0D, "uniform", true);

    @Test
    void rollsStayStepAlignedAndWithinRange() {
        for (int index = 0; index < 500; index++) {
            double value = OptionRollService.roll(0.5D, 1.5D, 0.1D, 0.5D, SETTINGS, new Random(index))
                    .orElseThrow();
            assertTrue(value >= 0.5D && value <= 1.5D);
            double steps = (value - 0.5D) / 0.1D;
            assertEquals(Math.rint(steps), steps, 0.000001D);
        }
    }

    @Test
    void rerollNeverReturnsBelowCurrentValueAndCanReachMaximum() {
        boolean maximumSeen = false;
        for (int index = 0; index < 500; index++) {
            double value = OptionRollService.roll(0.5D, 1.5D, 0.1D, 1.1D, SETTINGS, new Random(index))
                    .orElseThrow();
            assertTrue(value >= 1.1D && value <= 1.5D);
            maximumSeen |= Math.abs(value - 1.5D) < 0.000001D;
        }
        assertTrue(maximumSeen, "configured maximum must remain reachable");
    }

    @Test
    void fixedRangeReturnsOnlyTheFixedValue() {
        assertEquals(1.0D, OptionRollService.roll(1.0D, 1.0D, 1.0D, 1.0D, SETTINGS, new Random(1))
                .orElseThrow(), 0.000001D);
    }
}
