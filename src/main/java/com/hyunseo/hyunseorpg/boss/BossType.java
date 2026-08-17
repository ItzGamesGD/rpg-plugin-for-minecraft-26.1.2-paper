package com.hyunseo.hyunseorpg.boss;

import org.bukkit.entity.EntityType;

import java.util.Locale;
import java.util.Optional;

public enum BossType {
    WITHER(EntityType.WITHER, "wither"),
    ENDER_DRAGON(EntityType.ENDER_DRAGON, "ender-dragon");

    private final EntityType entityType;
    private final String configId;

    BossType(EntityType entityType, String configId) {
        this.entityType = entityType;
        this.configId = configId;
    }

    public EntityType entityType() { return entityType; }
    public String configId() { return configId; }

    public static Optional<BossType> from(EntityType type) {
        for (BossType value : values()) if (value.entityType == type) return Optional.of(value);
        return Optional.empty();
    }

    public static Optional<BossType> fromInput(String input) {
        if (input == null) return Optional.empty();
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (BossType value : values()) {
            if (value.configId.equals(normalized) || value.name().equalsIgnoreCase(normalized)
                    || (value == WITHER && normalized.equals("wither"))
                    || (value == ENDER_DRAGON && normalized.equals("dragon"))) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
