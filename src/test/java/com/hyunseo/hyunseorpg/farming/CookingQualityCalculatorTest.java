package com.hyunseo.hyunseorpg.farming;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CookingQualityCalculatorTest {
    @Test
    void noQualityBearingCropDefaultsToNormal() {
        assertEquals(0, CookingQualityCalculator.floorAverage(List.of()));
    }

    @Test
    void singleCropUsesItsQualityScore() {
        assertEquals(4, CookingQualityCalculator.floorAverage(
                List.of(new CookingQualityCalculator.Contribution(4, 1))));
    }

    @Test
    void mixedTwoAndThreeCropInputsUseWeightedFloorAverage() {
        assertEquals(2, CookingQualityCalculator.floorAverage(List.of(
                new CookingQualityCalculator.Contribution(4, 1),
                new CookingQualityCalculator.Contribution(1, 2))));
        assertEquals(1, CookingQualityCalculator.floorAverage(List.of(
                new CookingQualityCalculator.Contribution(4, 1),
                new CookingQualityCalculator.Contribution(0, 1),
                new CookingQualityCalculator.Contribution(1, 1))));
    }

    @Test
    void consumedStackAmountIsTheOnlyAmountCounted() {
        assertEquals(1, CookingQualityCalculator.floorAverage(List.of(
                new CookingQualityCalculator.Contribution(4, 1),
                new CookingQualityCalculator.Contribution(0, 3))));
    }
}
