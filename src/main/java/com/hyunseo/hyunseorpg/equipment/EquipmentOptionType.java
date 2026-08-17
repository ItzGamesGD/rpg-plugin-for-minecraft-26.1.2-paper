package com.hyunseo.hyunseorpg.equipment;

import java.util.Locale;
import java.util.Optional;

/** Option vocabulary shared by equipment, crafting, enhancement, and effects. */
public enum EquipmentOptionType {
    ATTACK,
    DEFENSE,
    LIFE_STEAL,
    BLEED,
    POISON,
    FREEZE,
    SHOCK,
    KNOCKBACK,
    PIERCE,
    REGENERATION,
    RESISTANCE,
    ATTACK_SPEED,
    MOVE_SPEED,
    STUN,
    BURN,
    EXPLOSION,
    MANA_REGEN,
    COOLDOWN_REDUCTION,
    LUCK,
    HARVEST_SPEED,
    MINING_SPEED,
    LOGGING_SPEED,
    EXPERIENCE_BONUS;

    public static Optional<EquipmentOptionType> fromInput(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(input.trim().toUpperCase(Locale.ROOT).replace('-', '_')));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
