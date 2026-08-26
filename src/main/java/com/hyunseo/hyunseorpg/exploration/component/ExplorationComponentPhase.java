package com.hyunseo.hyunseorpg.exploration.component;

import java.util.Locale;

public enum ExplorationComponentPhase {
    ACTIVATE,
    CLEAR,
    LOOT_EXIT,
    PYRAMID_LOOT_TRIGGER,
    PYRAMID_GUARDIAN_SPAWN,
    PYRAMID_ROOM_REVEAL,
    PYRAMID_PUZZLE,
    CHOICE_TIER_1,
    CHOICE_TIER_2,
    CHOICE_TIER_3,
    NEXT_WAVE;

    public static ExplorationComponentPhase parse(String raw, ExplorationComponentPhase fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
