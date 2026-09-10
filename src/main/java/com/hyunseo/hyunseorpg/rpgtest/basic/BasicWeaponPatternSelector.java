package com.hyunseo.hyunseorpg.rpgtest.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/** Uniform, cost-free prototype selection. Only the global entity cap constrains a cycle. */
public final class BasicWeaponPatternSelector {
    public Map<BasicWeaponPattern, Integer> select(RandomGenerator random, int totalCap) {
        if (totalCap < 0) throw new IllegalArgumentException("totalCap must be non-negative");
        List<BasicWeaponPattern> patterns = new ArrayList<>(List.of(BasicWeaponPattern.values()));
        Collections.shuffle(patterns, new java.util.Random(random.nextLong()));
        Map<BasicWeaponPattern, Integer> result = new EnumMap<>(BasicWeaponPattern.class);
        int remaining = totalCap;
        for (BasicWeaponPattern pattern : patterns) {
            if (remaining == 0) break;
            if (random.nextBoolean()) {
                int count = 1 + random.nextInt(Math.min(3, remaining));
                result.put(pattern, count);
                remaining -= count;
            }
        }
        if (result.isEmpty() && totalCap > 0) result.put(patterns.getFirst(), 1);
        return result;
    }
}
