package com.hyunseo.hyunseorpg.farming;

import java.util.Locale;
import java.util.Optional;

/** Player farming progression stage. Promotion rules are intentionally out of scope for Stage 3. */
public enum FarmingStage {
    BASIC("\uAE30\uBCF8"),
    SKILLED("\uC219\uB828"),
    PROFICIENT("\uB2A5\uC219"),
    ADVANCED("\uC0C1\uAE09"),
    EXPERT("\uC804\uBB38");

    private final String displayName;

    FarmingStage(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean atLeast(FarmingStage required) {
        return required != null && ordinal() >= required.ordinal();
    }

    public static Optional<FarmingStage> fromInput(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String value = raw.trim();
        for (FarmingStage stage : values()) {
            if (stage.name().equalsIgnoreCase(value) || stage.displayName.equals(value)) {
                return Optional.of(stage);
            }
        }
        try {
            return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
