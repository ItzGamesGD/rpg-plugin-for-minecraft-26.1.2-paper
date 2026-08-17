package com.hyunseo.hyunseorpg.farming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropGrowthCalculatorTest {
    @Test
    void catchesUpSeveralStagesAfterOfflineTime() {
        CropGrowthCalculator.GrowthResult result = CropGrowthCalculator.catchUp(
                0, 4, 35_000L, 10_000L, 10L);

        assertEquals(3, result.stage());
        assertEquals(40_000L, result.nextGrowthAt());
        assertTrue(result.changed());
    }

    @Test
    void clampsCatchUpAtMaximumAndStopsFutureGrowth() {
        CropGrowthCalculator.GrowthResult result = CropGrowthCalculator.catchUp(
                1, 4, 99_999L, 1_000L, 10L);

        assertEquals(4, result.stage());
        assertEquals(Long.MAX_VALUE, result.nextGrowthAt());
    }

    @Test
    void doesNotAdvanceBeforeDueTime() {
        CropGrowthCalculator.GrowthResult result = CropGrowthCalculator.catchUp(
                2, 4, 9_999L, 10_000L, 10L);

        assertEquals(2, result.stage());
        assertEquals(10_000L, result.nextGrowthAt());
    }

    @Test
    void invalidStageIsClampedWithoutOverflow() {
        CropGrowthCalculator.GrowthResult result = CropGrowthCalculator.catchUp(
                99, 4, 1L, 2L, 10L);

        assertEquals(4, result.stage());
        assertEquals(Long.MAX_VALUE, result.nextGrowthAt());
    }
}
