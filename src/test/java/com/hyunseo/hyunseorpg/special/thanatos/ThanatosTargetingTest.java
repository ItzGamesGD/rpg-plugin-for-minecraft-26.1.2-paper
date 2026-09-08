package com.hyunseo.hyunseorpg.special.thanatos;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThanatosTargetingTest {
    @Test
    void candidatesAreConstrainedToRangeAndForwardCone() {
        List<ThanatosTargeting.Candidate<String>> input = List.of(
                new ThanatosTargeting.Candidate<>("front", 0, 0, 5),
                new ThanatosTargeting.Candidate<>("behind", 0, 0, -2),
                new ThanatosTargeting.Candidate<>("far", 0, 0, 11),
                new ThanatosTargeting.Candidate<>("side", 5, 0, 0));
        assertEquals(List.of("front"), ThanatosTargeting.forward(input, 0, 0, 1, 10, 0.5));
    }

    @Test
    void invalidGeometryIsRejected() {
        List<ThanatosTargeting.Candidate<String>> input = List.of(
                new ThanatosTargeting.Candidate<>("nan", Double.NaN, 0, 1),
                new ThanatosTargeting.Candidate<>("infinite", 0, 0, Double.POSITIVE_INFINITY));
        assertTrue(ThanatosTargeting.forward(input, 0, 0, 1, 10, 0.5).isEmpty());
    }

    @Test
    void randomSelectionCanOnlyReturnValidCandidate() {
        List<String> valid = List.of("a", "b");
        RandomGenerator random = new Random(4);
        for (int i = 0; i < 100; i++) assertTrue(valid.contains(ThanatosTargeting.random(valid, random)));
        assertNull(ThanatosTargeting.random(List.of(), random));
    }
}
