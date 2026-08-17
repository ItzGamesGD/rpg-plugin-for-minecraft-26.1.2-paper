package com.hyunseo.hyunseorpg.mob;

import java.util.Locale;

public enum MobAbilityType {
    SUMMON_MINIONS,
    ATTACK_PATTERN,
    DEBUFF,
    REVIVE,
    PHASE_CHANGE,
    AREA_DAMAGE,
    PROJECTILE_BURST,
    BUFF_SELF,
    TELEPORT,
    CUSTOM;

    public static MobAbilityType fromInput(String input) {
        if (input == null || input.isBlank()) {
            return CUSTOM;
        }

        String normalized = input.trim().toUpperCase(Locale.ROOT).replace("-", "_");
        for (MobAbilityType value : values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return CUSTOM;
    }
}
