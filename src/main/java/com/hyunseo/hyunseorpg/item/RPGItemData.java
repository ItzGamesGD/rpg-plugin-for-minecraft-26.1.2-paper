package com.hyunseo.hyunseorpg.item;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

public record RPGItemData(
        String itemId,
        String displayName,
        Material material,
        String category,
        String rarity,
        int customModelData,
        List<String> lore,
        String useEffect,
        int useDurationSeconds,
        int useAmplifier,
        boolean consumeOnUse,
        List<String> tags
) {
    public RPGItemData {
        lore = lore == null ? List.of() : List.copyOf(lore);
        tags = tags == null ? List.of() : tags.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    /**
     * Profession items remain readable for old inventories, but are no longer
     * part of the active acquisition path.
     */
    public boolean legacyProfessionItem() {
        return category != null
                && category.toUpperCase(Locale.ROOT).startsWith("PROFESSION_");
    }
}
