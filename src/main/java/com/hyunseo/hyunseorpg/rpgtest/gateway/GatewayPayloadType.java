package com.hyunseo.hyunseorpg.rpgtest.gateway;

import java.util.Locale;
import java.util.Optional;

public enum GatewayPayloadType {
    ARROW(true, false), TRIDENT(true, false), BLAZE_SMALL_FIREBALL(true, false),
    GHAST_FIREBALL(true, false), WITHER_SKULL(true, false), SHULKER_BULLET(true, false),
    WIND_CHARGE(true, false), END_CRYSTAL_BOMB(true, false), SONIC_BOOM(false, false),
    DRAGON_BREATH(false, true), FLAME_STREAM(false, true), BEAM(false, true);

    private final boolean reflectable;
    private final boolean sustained;

    GatewayPayloadType(boolean reflectable, boolean sustained) {
        this.reflectable = reflectable;
        this.sustained = sustained;
    }

    public boolean reflectable() { return reflectable; }
    public boolean sustained() { return sustained; }

    public static Optional<GatewayPayloadType> fromInput(String input) {
        try { return Optional.of(valueOf(input.toUpperCase(Locale.ROOT).replace('-', '_'))); }
        catch (IllegalArgumentException exception) { return Optional.empty(); }
    }
}
