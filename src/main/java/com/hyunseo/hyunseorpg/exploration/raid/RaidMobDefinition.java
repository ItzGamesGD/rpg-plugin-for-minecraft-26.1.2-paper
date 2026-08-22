package com.hyunseo.hyunseorpg.exploration.raid;

import java.util.Locale;

/** One selectable unit in an outpost raid pool. A mounted unit still counts as one unit. */
public record RaidMobDefinition(String mobId, int maxSpawns, int weight, boolean heavy, int level) {
    public RaidMobDefinition {
        mobId = normalize(mobId);
        if (maxSpawns < 1) throw new IllegalArgumentException("maxSpawns < 1");
        if (weight < 1) throw new IllegalArgumentException("weight < 1");
        if (level < 1) throw new IllegalArgumentException("level < 1");
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("blank raid mob id");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
