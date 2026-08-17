package com.hyunseo.hyunseorpg.equipment.trigger;

import java.util.Locale;

public enum DuplicatePolicy {
    HIGHEST_LEVEL,
    FIRST,
    ALL;

    public static DuplicatePolicy parse(String raw) {
        return valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
