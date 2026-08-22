package com.hyunseo.hyunseorpg.exploration.raid;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Pure bounded weighted selection used by the runtime component and unit tests. */
public final class RaidWavePlanner {
    public List<RaidMobDefinition> plan(RaidWavePoolDefinition pool, Random random) {
        if (pool == null) return List.of();
        Random source = random == null ? new Random() : random;
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<RaidMobDefinition> result = new ArrayList<>();
        int heavy = 0;
        while (result.size() < pool.totalMaxSpawns()) {
            int currentHeavy = heavy;
            List<RaidMobDefinition> candidates = pool.mobs().stream()
                    .filter(mob -> counts.getOrDefault(mob.mobId(), 0) < mob.maxSpawns())
                    .filter(mob -> !mob.heavy() || currentHeavy < pool.heavyMaxSpawns())
                    .toList();
            if (candidates.isEmpty()) break;
            RaidMobDefinition selected = select(candidates, source);
            result.add(selected);
            counts.merge(selected.mobId(), 1, Integer::sum);
            if (selected.heavy()) heavy++;
        }
        return List.copyOf(result);
    }

    private RaidMobDefinition select(List<RaidMobDefinition> candidates, Random random) {
        int totalWeight = candidates.stream().mapToInt(RaidMobDefinition::weight).sum();
        int roll = random.nextInt(totalWeight);
        for (RaidMobDefinition candidate : candidates) {
            roll -= candidate.weight();
            if (roll < 0) return candidate;
        }
        return candidates.get(candidates.size() - 1);
    }
}
