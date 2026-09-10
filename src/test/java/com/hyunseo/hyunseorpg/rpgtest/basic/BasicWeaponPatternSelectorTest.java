package com.hyunseo.hyunseorpg.rpgtest.basic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicWeaponPatternSelectorTest {
    @Test void neverExceedsGlobalCapAndCanSelectManyTypes() {
        BasicWeaponPatternSelector selector = new BasicWeaponPatternSelector();
        boolean sawMoreThanFourTypes = false;
        for (int seed = 0; seed < 5_000; seed++) {
            var result = selector.select(new Random(seed), 14);
            assertFalse(result.isEmpty());
            assertTrue(result.values().stream().mapToInt(Integer::intValue).sum() <= 14);
            sawMoreThanFourTypes |= result.size() > 4;
        }
        assertTrue(sawMoreThanFourTypes, "selector must not impose a pattern-type limit");
    }
}
