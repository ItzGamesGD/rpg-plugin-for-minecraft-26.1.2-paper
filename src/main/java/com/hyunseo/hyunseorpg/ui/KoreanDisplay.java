package com.hyunseo.hyunseorpg.ui;

import com.hyunseo.hyunseorpg.farming.CropQuality;
import com.hyunseo.hyunseorpg.farming.DeliveryProvider;
import com.hyunseo.hyunseorpg.farming.DeliveryStatus;
import com.hyunseo.hyunseorpg.farming.FarmingStage;
import com.hyunseo.hyunseorpg.item.RPGItemService;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Central fallback display text for stable internal IDs that must never leak to player UI. */
public final class KoreanDisplay {
    private static final Map<CropQuality, String> QUALITY = new EnumMap<>(CropQuality.class);

    static {
        QUALITY.put(CropQuality.NORMAL, "\uC77C\uBC18");
        QUALITY.put(CropQuality.BASIC, "\uCD08\uAE09");
        QUALITY.put(CropQuality.PROFICIENT, "\uC911\uAE09");
        QUALITY.put(CropQuality.ADVANCED, "\uACE0\uAE09");
        QUALITY.put(CropQuality.SUPREME, "\uCD5C\uACE0\uAE09");
    }

    private KoreanDisplay() { }

    public static String rarity(String raw) {
        return switch (normalize(raw)) {
            case "common" -> "\uC77C\uBC18";
            case "uncommon" -> "\uACE0\uAE09";
            case "rare" -> "\uD76C\uADC0";
            case "epic" -> "\uC601\uC6C5";
            case "legendary" -> "\uC804\uC124";
            case "mythic" -> "\uC2E0\uD654";
            default -> raw == null || raw.isBlank() ? "" : raw.trim();
        };
    }

    public static String farmingStage(String raw) {
        Optional<FarmingStage> stage = FarmingStage.fromInput(raw);
        return stage.map(FarmingStage::displayName).orElse(raw == null ? "" : raw.trim());
    }

    public static String quality(CropQuality quality) {
        return QUALITY.getOrDefault(quality == null ? CropQuality.NORMAL : quality, "\uC77C\uBC18");
    }

    public static String quality(String raw) {
        return CropQuality.fromId(raw).map(KoreanDisplay::quality).orElse(raw == null ? "" : raw.trim());
    }

    public static String provider(DeliveryProvider provider) {
        if (provider == null) return "";
        return provider.displayName();
    }

    public static String deliveryStatus(DeliveryStatus status) {
        if (status == null) return "";
        return status.displayName();
    }

    public static String craftingCategory(String raw) {
        return switch (normalize(raw)) {
            case "materials" -> "\uC7AC\uB8CC";
            case "equipment" -> "\uC7A5\uBE44";
            case "special" -> "\uD2B9\uC218 \uC7A5\uBE44";
            case "consumables" -> "\uC18C\uBAA8\uD488";
            case "farming" -> "\uB18D\uC0AC";
            case "cooking" -> "\uC694\uB9AC";
            default -> "";
        };
    }

    public static String crop(String raw) {
        return switch (normalize(raw).replace("crop_", "").replace("seed_", "")) {
            case "corn" -> "\uC625\uC218\uC218";
            case "onion" -> "\uC591\uD30C";
            case "chili" -> "\uACE0\uCD94";
            case "garlic" -> "\uB9C8\uB298";
            default -> raw == null ? "" : raw.trim();
        };
    }

    public static String itemFamily(String raw) {
        String value = normalize(raw);
        if (value.startsWith("crop_")) return crop(value.substring("crop_".length()));
        if (value.startsWith("processed_")) return processedCrop(value.substring("processed_".length())) + " \uAC00\uACF5\uD488";
        return raw == null ? "" : raw.trim();
    }

    public static String itemId(String raw, RPGItemService items) {
        String id = normalize(raw);
        Optional<String> canonical = canonicalItemName(id);
        if (canonical.isPresent()) return canonical.orElseThrow();
        if (items != null) {
            Optional<String> name = items.getData(id).map(data -> data.displayName());
            if (name.isPresent()) return name.orElseThrow();
        }
        if (id.startsWith("vanilla:")) return vanillaMaterial(id.substring("vanilla:".length()));
        if (id.startsWith("crop_") || id.startsWith("seed_")) return crop(id);
        if (id.startsWith("processed_")) return processedCrop(id.substring("processed_".length()));
        return raw == null ? "" : raw.trim();
    }

    /** Stable Korean names for farming IDs, applied before live registry text. */
    public static Optional<String> canonicalItemName(String raw) {
        String id = normalize(raw);
        return switch (id) {
            case "abundance_essence" -> Optional.of("\uD48D\uC694\uC758 \uC815\uC218");
            case "vitality_stat_token" -> Optional.of("\uC0DD\uBA85\uC758 \uC99D\uD45C");
            case "satiety_stat_token" -> Optional.of("\uD3EC\uB9CC\uC758 \uC99D\uD45C");
            case "abundance_stat_token" -> Optional.of("\uD48D\uC694\uC758 \uC99D\uD45C");
            default -> farmingItemName(id);
        };
    }

    /** Canonical farming lore used when an older external item definition is loaded. */
    public static Optional<List<String>> canonicalItemLore(String raw) {
        String id = normalize(raw);
        if (id.equals("abundance_essence")) {
            return Optional.of(List.of("\uD6C4\uBC18 \uB18D\uC0AC \uD569\uC131 \uC7AC\uB8CC\uC785\uB2C8\uB2E4.", "\uC0C1\uC810\uC5D0\uC11C \uD310\uB9E4\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."));
        }
        if (id.endsWith("_stat_token")) {
            return Optional.of(List.of("\uACC4\uC815\uC5D0 \uADC0\uC18D\uB418\uB294 \uB18D\uC0AC \uC131\uC7A5 \uC99D\uD45C\uC785\uB2C8\uB2E4."));
        }
        if (id.startsWith("seed_")) {
            return Optional.of(List.of("\uB18D\uC0AC \uC791\uBB3C\uC744 \uC2EC\uB294 \uC528\uC557\uC785\uB2C8\uB2E4."));
        }
        if (id.startsWith("crop_")) {
            Optional<String> name = canonicalItemName(id);
            return name.map(value -> value.contains(" ")
                    ? List.of("\uC791\uBB3C: " + cropNameFromFarmingId(id), "\uD488\uC9C8: " + qualityFromFarmingId(id))
                    : List.of("\uC218\uD655\uD55C \uB18D\uC0AC \uC791\uBB3C\uC785\uB2C8\uB2E4."));
        }
        if (id.startsWith("processed_")) {
            return canonicalItemName(id).map(value -> List.of(
                    "\uB18D\uC0AC \uAC00\uACF5\uD488\uC785\uB2C8\uB2E4.",
                    "\uC791\uBB3C: " + cropNameFromFarmingId(id),
                    "\uD488\uC9C8: " + qualityFromFarmingId(id)));
        }
        return Optional.empty();
    }

    public static String tag(String raw) {
        return switch (normalize(raw)) {
            case "farming-supreme-material" -> "\uCD5C\uACE0\uAE09 \uB18D\uC0AC \uC7AC\uB8CC";
            case "farming-processed" -> "\uB18D\uC0AC \uAC00\uACF5\uD488";
            case "farming-cooked" -> "\uC694\uB9AC";
            case "alchemy-catalyst" -> "\uC591\uC870 \uCD09\uB9E4";
            case "non-sellable" -> "\uD310\uB9E4 \uBD88\uAC00";
            default -> raw == null ? "" : raw.trim();
        };
    }

    private static String processedCrop(String raw) {
        String value = normalize(raw);
        if (value.startsWith("corn")) return "\uC625\uC218\uC218 \uC804\uBD84";
        if (value.startsWith("onion")) return "\uC591\uD30C \uB18D\uCD95\uC561";
        if (value.startsWith("chili")) return "\uACE0\uCD94 \uCD94\uCD9C\uC561";
        if (value.startsWith("garlic")) return "\uB9C8\uB298 \uB18D\uCD95\uC561";
        return raw == null ? "" : raw.trim();
    }

    private static Optional<String> farmingItemName(String id) {
        if (id.startsWith("seed_")) return Optional.of(crop(id) + " \uC528\uC557");
        if (id.startsWith("crop_")) {
            String crop = cropNameFromFarmingId(id);
            String quality = qualityFromFarmingId(id);
            return quality.equals(quality(CropQuality.NORMAL)) ? Optional.of(crop) : Optional.of(quality + " " + crop);
        }
        if (id.startsWith("processed_")) {
            return Optional.of(processedCropNameFromFarmingId(id) + " - " + qualityFromFarmingId(id));
        }
        return Optional.empty();
    }

    private static String cropNameFromFarmingId(String id) {
        String value = normalize(id);
        if (value.startsWith("crop_")) value = value.substring("crop_".length());
        if (value.startsWith("processed_")) value = value.substring("processed_".length());
        int qualityMarker = value.indexOf("_quality_");
        if (qualityMarker >= 0) value = value.substring(0, qualityMarker);
        if (value.startsWith("corn")) return "\uC625\uC218\uC218";
        if (value.startsWith("onion")) return "\uC591\uD30C";
        if (value.startsWith("chili")) return "\uACE0\uCD94";
        if (value.startsWith("garlic")) return "\uB9C8\uB298";
        return value;
    }

    private static String processedCropNameFromFarmingId(String id) {
        return switch (cropNameFromFarmingId(id)) {
            case "\uC625\uC218\uC218" -> "\uC625\uC218\uC218 \uC804\uBD84";
            case "\uC591\uD30C" -> "\uC591\uD30C \uB18D\uCD95\uC561";
            case "\uACE0\uCD94" -> "\uACE0\uCD94 \uCD94\uCD9C\uC561";
            case "\uB9C8\uB298" -> "\uB9C8\uB298 \uB18D\uCD95\uC561";
            default -> processedCrop(id);
        };
    }

    private static String qualityFromFarmingId(String id) {
        String value = normalize(id);
        int marker = value.lastIndexOf("_quality_");
        if (marker >= 0) return quality(value.substring(marker + "_quality_".length()));
        if (value.startsWith("processed_")) {
            int separator = value.lastIndexOf('_');
            if (separator >= 0) return quality(value.substring(separator + 1));
        }
        return quality(CropQuality.NORMAL);
    }

    private static String vanillaMaterial(String raw) {
        return switch (normalize(raw)) {
            case "wheat" -> "\uBC00";
            case "wheat_seeds" -> "\uBC00 \uC528\uC557";
            case "bread" -> "\uBE75";
            case "bowl" -> "\uADF8\uB987";
            case "egg" -> "\uB2EC\uAC40";
            case "fire_charge" -> "\uD654\uC5FC\uAD6C";
            case "trident" -> "\uC0BC\uC9C0\uCC3D";
            case "amethyst_shard" -> "\uC790\uC218\uC815 \uC870\uAC01";
            case "prismarine_crystals" -> "\uD504\uB9AC\uC988\uB9C8\uB9B0 \uD06C\uB9AC\uC2A4\uD0C8";
            default -> raw == null ? "" : raw.trim();
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
