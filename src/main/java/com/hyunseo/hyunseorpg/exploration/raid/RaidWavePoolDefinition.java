package com.hyunseo.hyunseorpg.exploration.raid;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** YAML-defined, bounded initial encounter pool. It deliberately excludes combat summons. */
public record RaidWavePoolDefinition(String id, int totalMaxSpawns, int heavyMaxSpawns,
                                     List<RaidMobDefinition> mobs,
                                     Map<String, Integer> guaranteed) {
    public RaidWavePoolDefinition(String id, int totalMaxSpawns, int heavyMaxSpawns,
                                  List<RaidMobDefinition> mobs) {
        this(id, totalMaxSpawns, heavyMaxSpawns, mobs, Map.of());
    }

    public RaidWavePoolDefinition {
        id = normalize(id);
        if (totalMaxSpawns < 1) throw new IllegalArgumentException("totalMaxSpawns < 1");
        if (heavyMaxSpawns < 0 || heavyMaxSpawns > totalMaxSpawns) {
            throw new IllegalArgumentException("invalid heavyMaxSpawns");
        }
        mobs = List.copyOf(mobs == null ? List.of() : mobs);
        if (mobs.isEmpty()) throw new IllegalArgumentException("raid pool has no mobs");
        Set<String> ids = new LinkedHashSet<>();
        for (RaidMobDefinition mob : mobs) {
            if (!ids.add(mob.mobId())) throw new IllegalArgumentException("duplicate raid mob: " + mob.mobId());
        }
        Map<String, RaidMobDefinition> byId = new LinkedHashMap<>();
        for (RaidMobDefinition mob : mobs) byId.put(mob.mobId(), mob);
        Map<String, Integer> normalizedGuaranteed = new LinkedHashMap<>();
        int guaranteedCount = 0;
        int guaranteedHeavy = 0;
        if (guaranteed != null) {
            for (Map.Entry<String, Integer> entry : guaranteed.entrySet()) {
                String mobId = normalize(entry.getKey());
                int count = entry.getValue() == null ? 0 : entry.getValue();
                RaidMobDefinition mob = byId.get(mobId);
                if (mob == null) throw new IllegalArgumentException("guaranteed raid mob is not in pool: " + mobId);
                if (count < 1 || count > mob.maxSpawns()) {
                    throw new IllegalArgumentException("invalid guaranteed raid mob count: " + mobId);
                }
                normalizedGuaranteed.put(mobId, count);
                guaranteedCount += count;
                if (mob.heavy()) guaranteedHeavy += count;
            }
        }
        if (guaranteedCount > totalMaxSpawns || guaranteedHeavy > heavyMaxSpawns) {
            throw new IllegalArgumentException("guaranteed raid mobs exceed pool limits");
        }
        guaranteed = Map.copyOf(normalizedGuaranteed);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("blank raid pool id");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
