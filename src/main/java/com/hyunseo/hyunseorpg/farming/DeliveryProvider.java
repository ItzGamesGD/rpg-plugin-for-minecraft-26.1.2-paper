package com.hyunseo.hyunseorpg.farming;

import java.util.Locale;
import java.util.Optional;

/** Providers are data-defined now; estate is reserved until a later stage. */
public enum DeliveryProvider {
    FARMER("farmer", "\uB18D\uBD80"),
    ALCHEMIST("alchemist", "\uC5F0\uAE08\uC220\uC0AC"),
    ESTATE_RESERVED("estate_reserved", "\uC601\uC9C0");

    private final String id;
    private final String displayName;

    DeliveryProvider(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<DeliveryProvider> fromInput(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (DeliveryProvider provider : values()) {
            if (provider.id.equals(value) || provider.displayName.equals(raw.trim())) return Optional.of(provider);
        }
        return Optional.empty();
    }
}
