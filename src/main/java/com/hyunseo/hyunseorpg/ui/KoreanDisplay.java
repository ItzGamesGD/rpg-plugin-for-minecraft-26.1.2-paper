package com.hyunseo.hyunseorpg.ui;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Stable presentation helpers for active item and recipe systems. */
public final class KoreanDisplay {
    private KoreanDisplay() { }

    public static String rarity(String raw) {
        if (raw == null) return "";
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "COMMON" -> "일반"; case "UNCOMMON" -> "고급"; case "RARE" -> "희귀";
            case "EPIC" -> "영웅"; case "LEGENDARY" -> "전설"; default -> raw.trim();
        };
    }

    public static String itemId(String raw, RPGItemService items) {
        String id = normalize(raw);
        if (id.startsWith("vanilla:")) return id.substring("vanilla:".length()).toLowerCase(Locale.ROOT);
        return items == null ? id : items.getData(id).map(data -> data.displayName()).orElse(id);
    }

    public static Optional<String> canonicalItemName(String raw) { return Optional.empty(); }
    public static Optional<List<String>> canonicalItemLore(String raw) { return Optional.empty(); }

    public static String tag(String raw) {
        String id = normalize(raw);
        return switch (id) {
            case "alchemy-material" -> "연금술 재료";
            case "alchemy-catalyst" -> "연금술 촉매";
            case "non-sellable" -> "판매 불가";
            default -> id;
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
