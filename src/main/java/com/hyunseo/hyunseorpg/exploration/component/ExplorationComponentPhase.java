package com.hyunseo.hyunseorpg.exploration.component;

import java.util.Locale;

public enum ExplorationComponentPhase {
    ACTIVATE,
    CLEAR,
    LOOT_EXIT,
    CHOICE_TIER_1,
    CHOICE_TIER_2,
    CHOICE_TIER_3;

    public static ExplorationComponentPhase parse(String raw, ExplorationComponentPhase fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
