package com.hyunseo.hyunseorpg.mob.variant;

import java.util.Locale;

public enum ZombieVariant {
    NORMAL,
    BOMB,
    LEAP;

    public static ZombieVariant fromStored(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return NORMAL;
        }
    }
}
