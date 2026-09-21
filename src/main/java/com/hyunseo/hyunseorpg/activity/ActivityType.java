package com.hyunseo.hyunseorpg.activity;

import java.util.Locale;
import java.util.Optional;

/** Stable activity identifiers used by the shared vanilla activity reward loop. */
public enum ActivityType {
    MINING,
    LOGGING,
    HUNTING,
    HUSBANDRY,
    REPAIRING,
    FISHING;

    public static Optional<ActivityType> fromInput(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
