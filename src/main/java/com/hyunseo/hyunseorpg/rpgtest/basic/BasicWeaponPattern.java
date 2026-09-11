package com.hyunseo.hyunseorpg.rpgtest.basic;

import java.util.Locale;
import java.util.Optional;

public enum BasicWeaponPattern {
    MACE_MELEE, MACE_DROP, SPEAR_MELEE, SPEAR_LUNGE, TRIDENT_THROWER, AXE_MELEE, HOE_MELEE, SHIELD_ORBIT;

    public static Optional<BasicWeaponPattern> fromInput(String input) {
        try { return Optional.of(valueOf(input.toUpperCase(Locale.ROOT).replace('-', '_'))); }
        catch (IllegalArgumentException exception) { return Optional.empty(); }
    }
}
