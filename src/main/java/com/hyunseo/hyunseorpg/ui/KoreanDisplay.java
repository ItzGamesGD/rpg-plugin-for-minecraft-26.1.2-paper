package com.hyunseo.hyunseorpg.ui;

import com.hyunseo.hyunseorpg.item.RPGItemService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Central fallback display text for stable internal IDs that must never leak to player UI. */
public final class KoreanDisplay {
    private KoreanDisplay() { }

    public static String rarity(String raw) {
        return switch (normalize(raw)) {
            case "common" -> "일반";
            case "uncommon" -> "고급";
            case "rare" -> "희귀";
            case "epic" -> "영웅";
            case "legendary" -> "전설";
            case "mythic" -> "신화";
            default -> raw == null || raw.isBlank() ? "" : raw.trim();
        };
    }

    public static String itemId(String raw, RPGItemService items) {
        String id = normalize(raw);
        if (items != null) {
            Optional<String> name = items.getData(id).map(data -> data.displayName());
            if (name.isPresent()) return name.orElseThrow();
        }
        if (id.startsWith("vanilla:")) return vanillaMaterial(id.substring("vanilla:".length()));
        return raw == null ? "" : raw.trim();
    }

    public static Optional<String> canonicalItemName(String raw) { return Optional.empty(); }
    public static Optional<List<String>> canonicalItemLore(String raw) { return Optional.empty(); }

    public static String tag(String raw) {
        return switch (normalize(raw)) {
            case "alchemy-catalyst" -> "양조 촉매";
            case "non-sellable" -> "판매 불가";
            default -> raw == null ? "" : raw.trim();
        };
    }

    private static String vanillaMaterial(String raw) {
        return switch (normalize(raw)) {
            case "bread" -> "빵";
            case "bowl" -> "그릇";
            case "egg" -> "달걀";
            case "fire_charge" -> "화염구";
            case "trident" -> "삼지창";
            case "amethyst_shard" -> "자수정 조각";
            case "prismarine_crystals" -> "프리즈마린 크리스탈";
            default -> raw == null ? "" : raw.trim();
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
