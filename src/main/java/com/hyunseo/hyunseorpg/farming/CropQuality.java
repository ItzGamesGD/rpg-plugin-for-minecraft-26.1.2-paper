package com.hyunseo.hyunseorpg.farming;

import java.util.Locale;
import java.util.Optional;

/** Stable quality IDs used by farming crop items. */
public enum CropQuality {
    NORMAL("normal", "\uC77C\uBC18"),
    BASIC("basic", "\uCD08\uAE09"),
    PROFICIENT("proficient", "\uC911\uAE09"),
    ADVANCED("advanced", "\uACE0\uAE09"),
    SUPREME("supreme", "\uCD5C\uACE0\uAE09");

    private final String id;
    private final String displayName;

    CropQuality(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<CropQuality> fromId(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String value = raw.trim().toLowerCase(Locale.ROOT);
        for (CropQuality quality : values()) {
            if (quality.id.equals(value) || quality.displayName.equals(raw.trim())) return Optional.of(quality);
        }
        return Optional.empty();
    }
}
