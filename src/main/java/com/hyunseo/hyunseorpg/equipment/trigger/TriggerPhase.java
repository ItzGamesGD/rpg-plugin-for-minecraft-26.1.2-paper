package com.hyunseo.hyunseorpg.equipment.trigger;

import java.util.Locale;

public enum TriggerPhase {
    PRE,
    CONFIRMED,
    POST;

    public static TriggerPhase parse(String raw) {
        return valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
