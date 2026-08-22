package com.hyunseo.hyunseorpg.exploration.raid;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** YAML-defined, bounded initial encounter pool. It deliberately excludes combat summons. */
public record RaidWavePoolDefinition(String id, int totalMaxSpawns, int heavyMaxSpawns,
                                     List<RaidMobDefinition> mobs) {
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
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("blank raid pool id");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
