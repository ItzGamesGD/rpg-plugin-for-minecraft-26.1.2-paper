package com.hyunseo.hyunseorpg.rpgtest.basic;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test void executableOrbitSelectionNeverRequestsShieldOrMoreFamilyStockThanExists() {
        Map<BasicWeaponPattern, List<Integer>> stock = new EnumMap<>(BasicWeaponPattern.class);
        stock.put(BasicWeaponPattern.MACE_MELEE, new ArrayList<>(List.of(1, 2)));
        stock.put(BasicWeaponPattern.MACE_DROP, new ArrayList<>(List.of(1, 2)));
        stock.put(BasicWeaponPattern.SPEAR_MELEE, new ArrayList<>(List.of(3, 4, 5)));
        stock.put(BasicWeaponPattern.SPEAR_LUNGE, new ArrayList<>(List.of(3, 4, 5)));
        stock.put(BasicWeaponPattern.TRIDENT_THROWER, new ArrayList<>(List.of(3, 4, 5)));
        stock.put(BasicWeaponPattern.AXE_MELEE, new ArrayList<>(List.of(6)));
        stock.put(BasicWeaponPattern.HOE_MELEE, new ArrayList<>(List.of(7)));
        stock.put(BasicWeaponPattern.SHIELD_ORBIT, new ArrayList<>(List.of(8)));
        for (int seed = 0; seed < 100; seed++) {
            Map<BasicWeaponPattern, List<Integer>> selected = selector().selectOrbitSlots(new Random(seed), 7, copy(stock));
            assertFalse(selected.containsKey(BasicWeaponPattern.SHIELD_ORBIT));
            List<Integer> all = selected.values().stream().flatMap(List::stream).toList();
            assertEquals(all.size(), all.stream().distinct().count());
            assertTrue(all.size() <= 7);
        }
    }

    private BasicWeaponPatternSelector selector() { return new BasicWeaponPatternSelector(); }
    private Map<BasicWeaponPattern, List<Integer>> copy(Map<BasicWeaponPattern, List<Integer>> source) {
        Map<BasicWeaponPattern, List<Integer>> copy = new EnumMap<>(BasicWeaponPattern.class);
        source.forEach((pattern, slots) -> copy.put(pattern, new ArrayList<>(slots)));
        return copy;
    }
}
