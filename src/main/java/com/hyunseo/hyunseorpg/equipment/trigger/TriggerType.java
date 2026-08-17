package com.hyunseo.hyunseorpg.equipment.trigger;

import java.util.Locale;

public enum TriggerType {
    INPUT,
    ATTACK_ATTEMPT,
    ATTACK_HIT,
    DAMAGED,
    KILL,
    GATHER_ATTEMPT,
    GATHER_SUCCESS,
    PROJECTILE_SHOOT,
    PROJECTILE_HIT,
    FISH_SUCCESS,
    ELYTRA_BOOST,
    LANDING;

    public static TriggerType parse(String raw) {
        return valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
