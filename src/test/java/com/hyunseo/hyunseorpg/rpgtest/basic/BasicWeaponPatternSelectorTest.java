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

    @Test void aSinglePatternMayConsumeMoreThanThreeActorSlots() {
        BasicWeaponPatternSelector selector = new BasicWeaponPatternSelector();
        boolean found = false;
        for (int seed = 0; seed < 20_000 && !found; seed++) {
            var result = selector.select(new Random(seed), 14);
            found = result.values().stream().anyMatch(count -> count > 3);
        }
        assertTrue(found, "there must be no hidden generic per-pattern cap of three");
    }
}
