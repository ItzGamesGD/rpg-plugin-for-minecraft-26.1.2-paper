package com.hyunseo.hyunseorpg.equipment.trigger;

import java.util.Locale;

public enum SourceScope {
    TRIGGERING_ITEM,
    MAIN_HAND,
    OFF_HAND,
    HEAD,
    CHEST,
    LEGS,
    FEET,
    ARMOR,
    HELD_ITEMS,
    ALL_EQUIPPED;

    public static SourceScope parse(String raw) {
        return valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
