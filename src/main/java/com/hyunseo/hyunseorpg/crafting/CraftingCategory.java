package com.hyunseo.hyunseorpg.crafting;

import java.util.Locale;

/** Player-facing categories used by both recipe data and persisted menu layouts. */
public enum CraftingCategory {
    MATERIALS("materials"),
    EQUIPMENT("equipment"),
    SPECIAL("special"),
    CONSUMABLES("consumables");

    private final String configId;

    CraftingCategory(String configId) {
        this.configId = configId;
    }

    public String configId() {
        return configId;
    }

    public static CraftingCategory fromConfig(String value, CraftingCategory fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "MATERIAL", "MATERIALS", "SYSTEM_MATERIAL" -> MATERIALS;
            case "EQUIPMENT", "WEAPON", "WEAPONS", "TOOLS", "TOOL" -> EQUIPMENT;
            case "SPECIAL", "SPECIAL_EQUIPMENT" -> SPECIAL;
            case "CONSUMABLE", "CONSUMABLES" -> CONSUMABLES;
            default -> fallback;
        };
    }
}
