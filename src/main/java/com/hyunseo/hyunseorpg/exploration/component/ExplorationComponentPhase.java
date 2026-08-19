package com.hyunseo.hyunseorpg.exploration.component;

import java.util.Locale;

public enum ExplorationComponentPhase {
    ACTIVATE,
    CLEAR;

    public static ExplorationComponentPhase parse(String raw, ExplorationComponentPhase fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
