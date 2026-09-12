package com.hyunseo.hyunseorpg.rpgtest.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.util.random.RandomGenerator;

/**
 * Uniform, cost-free prototype selection. The cap counts attack-pattern AI actors only;
 * native projectiles emitted later by a trident thrower are not additional actors.
 */
public final class BasicWeaponPatternSelector {
    /**
     * Produces an executable selection from the actual orbit stock. Shield is presentation-only
     * and deliberately absent: selecting it as an attack would silently waste a cycle.
     */
    public Map<BasicWeaponPattern, List<Integer>> selectOrbitSlots(RandomGenerator random, int totalCap,
                                                                     Map<BasicWeaponPattern, List<Integer>> compatibleSlots) {
        if (totalCap < 0) throw new IllegalArgumentException("totalCap must be non-negative");
        Map<BasicWeaponPattern, List<Integer>> pools = new EnumMap<>(BasicWeaponPattern.class);
        compatibleSlots.forEach((pattern, slots) -> {
            if (pattern != BasicWeaponPattern.SHIELD_ORBIT && !slots.isEmpty()) pools.put(pattern, slots);
        });
        List<BasicWeaponPattern> patterns = new ArrayList<>(pools.keySet());
        Collections.shuffle(patterns, new java.util.Random(random.nextLong()));
        Map<BasicWeaponPattern, List<Integer>> result = new EnumMap<>(BasicWeaponPattern.class);
        int remaining = totalCap;
        for (BasicWeaponPattern pattern : patterns) {
            if (remaining == 0 || pools.get(pattern).isEmpty() || !random.nextBoolean()) continue;
            take(random, pattern, pools, result, 1 + random.nextInt(Math.min(remaining, pools.get(pattern).size())));
            remaining = totalCap - result.values().stream().mapToInt(List::size).sum();
        }
        if (result.isEmpty() && !patterns.isEmpty() && totalCap > 0) take(random, patterns.getFirst(), pools, result, 1);
        return result;
    }

    private void take(RandomGenerator random, BasicWeaponPattern pattern, Map<BasicWeaponPattern, List<Integer>> pools,
                      Map<BasicWeaponPattern, List<Integer>> result, int count) {
        List<Integer> pool = pools.get(pattern);
        for (int taken = 0; taken < count && !pool.isEmpty(); taken++) {
            Integer slot = pool.remove(random.nextInt(pool.size()));
            result.computeIfAbsent(pattern, ignored -> new ArrayList<>()).add(slot);
            for (Map.Entry<BasicWeaponPattern, List<Integer>> entry : pools.entrySet()) {
                if (family(entry.getKey()) == family(pattern)) entry.getValue().remove(slot);
            }
        }
    }

    private Family family(BasicWeaponPattern pattern) {
        return switch (pattern) {
            case MACE_MELEE, MACE_DROP -> Family.MACE;
            case SPEAR_MELEE, SPEAR_LUNGE, TRIDENT_THROWER -> Family.SPEAR;
            case AXE_MELEE -> Family.AXE;
            case HOE_MELEE -> Family.HOE;
            case SHIELD_ORBIT -> Family.SHIELD;
        };
    }
    private enum Family { MACE, SPEAR, AXE, HOE, SHIELD }

    public Map<BasicWeaponPattern, Integer> select(RandomGenerator random, int totalCap) {
        if (totalCap < 0) throw new IllegalArgumentException("totalCap must be non-negative");
        List<BasicWeaponPattern> patterns = new ArrayList<>(List.of(BasicWeaponPattern.values()));
        Collections.shuffle(patterns, new java.util.Random(random.nextLong()));
        Map<BasicWeaponPattern, Integer> result = new EnumMap<>(BasicWeaponPattern.class);
        int remaining = totalCap;
        for (BasicWeaponPattern pattern : patterns) {
            if (remaining == 0) break;
            if (random.nextBoolean()) {
                int count = 1 + random.nextInt(remaining);
                result.put(pattern, count);
                remaining -= count;
            }
        }
        if (result.isEmpty() && totalCap > 0) result.put(patterns.getFirst(), 1);
        return result;
    }
}
