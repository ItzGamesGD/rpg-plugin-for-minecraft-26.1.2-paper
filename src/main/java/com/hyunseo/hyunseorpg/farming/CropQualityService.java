package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/** Cached, single-roll quality resolver. Promotion changes probability density, not score. */
public final class CropQualityService {
    private final ConfigService config;
    private final RPGItemService items;
    private final Map<CropQuality, QualityDefinition> definitions = new EnumMap<>(CropQuality.class);
    private final Map<CropQuality, Double> baseDistribution = new EnumMap<>(CropQuality.class);
    private final Map<String, Map<CropQuality, Double>> cropDistributions = new LinkedHashMap<>();
    private final Map<String, Map<CropQuality, String>> itemIds = new LinkedHashMap<>();
    private boolean enabled;

    public CropQualityService(ConfigService config, RPGItemService items) {
        this.config = config;
        this.items = items;
    }

    public void load() {
        definitions.clear();
        baseDistribution.clear();
        cropDistributions.clear();
        itemIds.clear();
        enabled = config.getFarmingQualityBoolean("enabled", true);
        for (CropQuality quality : CropQuality.values()) {
            String path = "quality." + quality.id();
            double score = finite(config.getFarmingQualityDouble(path + ".quality-score",
                    config.getFarmingQualityDouble(path + ".minimum-score", 0.0D)), 0.0D);
            String displayName = config.getFarmingQualityString(path + ".display-name", quality.id());
            if (displayName.isBlank() || displayName.equalsIgnoreCase(quality.id())
                    || displayName.equalsIgnoreCase(quality.name())) {
                displayName = quality.displayName();
            }
            definitions.put(quality, new QualityDefinition(quality, score, displayName));
            baseDistribution.put(quality, safeNonNegative(config.getFarmingQualityDouble(
                    "base-distribution." + quality.id(), fallbackDistribution(quality))));
        }
        baseDistribution.putAll(normalizeDistribution(baseDistribution));

        for (String cropId : config.getFarmingQualityKeys("items")) {
            String normalizedCrop = normalize(cropId);
            Map<CropQuality, String> cropItems = new EnumMap<>(CropQuality.class);
            for (CropQuality quality : CropQuality.values()) {
                String itemId = normalize(config.getFarmingQualityString(
                        "items." + cropId + "." + quality.id(), ""));
                if (!itemId.isBlank()) cropItems.put(quality, itemId);
            }
            itemIds.put(normalizedCrop, Map.copyOf(cropItems));
            Map<CropQuality, Double> override = parseDistribution("crop-overrides." + cropId);
            if (!override.isEmpty()) cropDistributions.put(normalizedCrop, normalizeDistribution(override));
        }
        validateDistributions();
    }

    /** Compatibility overload: farming promotion has no quality modifier. */
    public QualityRoll roll(String cropId, double ignoredModifier, double ignoredExtension) {
        return roll(cropId);
    }

    public QualityRoll roll(String cropId) {
        return roll(cropId, 0.0D);
    }

    /** Applies the hoe promotion density shift once, immediately before the single roll. */
    public QualityRoll roll(String cropId, FarmingHoePromotionService.HoePassives passives) {
        double shift = passives == null ? 0.0D : passives.qualityDensityShift();
        return roll(cropId, shift);
    }

    public QualityRoll roll(String cropId, double qualityDensityShift) {
        Map<CropQuality, Double> distribution = effectiveDistribution(cropId, qualityDensityShift);
        if (!enabled) return normal(cropId, 0.0D);
        double total = distribution.values().stream().mapToDouble(Double::doubleValue).sum();
        if (!Double.isFinite(total) || total <= 0.0D) return normal(cropId, 0.0D);
        double selected = ThreadLocalRandom.current().nextDouble(total);
        double cursor = 0.0D;
        CropQuality quality = CropQuality.NORMAL;
        for (CropQuality candidate : CropQuality.values()) {
            cursor += distribution.getOrDefault(candidate, 0.0D);
            if (selected < cursor) {
                quality = candidate;
                break;
            }
        }
        return new QualityRoll(quality, selected, itemId(cropId, quality));
    }

    public QualityRoll normal(String cropId, double score) {
        return new QualityRoll(CropQuality.NORMAL, score, itemId(cropId, CropQuality.NORMAL));
    }

    public Map<CropQuality, Double> distribution(String cropId) {
        Map<CropQuality, Double> crop = cropDistributions.get(normalize(cropId));
        return crop == null ? Map.copyOf(baseDistribution) : crop;
    }

    public Map<CropQuality, Double> effectiveDistribution(String cropId, double qualityDensityShift) {
        Map<CropQuality, Double> base = distribution(cropId);
        if (!Double.isFinite(qualityDensityShift) || Math.abs(qualityDensityShift) < 0.000001D) {
            return base;
        }
        Map<CropQuality, Double> shifted = new EnumMap<>(CropQuality.class);
        for (CropQuality quality : CropQuality.values()) {
            double value = base.getOrDefault(quality, 0.0D) + qualityDensityShift * quality.ordinal();
            shifted.put(quality, safeNonNegative(value));
        }
        Map<CropQuality, Double> normalized = normalizeDistribution(shifted);
        return normalized.isEmpty() ? base : normalized;
    }

    public Optional<String> itemId(String cropId, CropQuality quality) {
        Map<CropQuality, String> mapping = itemIds.get(normalize(cropId));
        if (mapping == null) return Optional.empty();
        String itemId = mapping.get(quality);
        return itemId == null || items.getData(itemId).isEmpty() ? Optional.empty() : Optional.of(itemId);
    }

    /** Resolves quality from the canonical quality-item mapping, never from lore text. */
    public Optional<CropQuality> qualityOfItem(String rawItemId) {
        String itemId = normalize(rawItemId);
        if (itemId.isBlank()) return Optional.empty();
        Optional<CropQuality> configured = itemIds.values().stream()
                .flatMap(mapping -> mapping.entrySet().stream())
                .filter(entry -> entry.getValue().equals(itemId))
                .map(Map.Entry::getKey)
                .findFirst();
        if (configured.isPresent()) return configured;
        return qualityFromCanonicalId(itemId);
    }

    /** Resolves the crop family for a canonical quality-bearing item ID. */
    public Optional<String> cropOfItem(String rawItemId) {
        String itemId = normalize(rawItemId);
        if (itemId.isBlank()) return Optional.empty();
        Optional<String> configured = itemIds.entrySet().stream()
                .filter(entry -> entry.getValue().containsValue(itemId))
                .map(Map.Entry::getKey)
                .findFirst();
        if (configured.isPresent()) return configured;
        return cropFromCanonicalId(itemId);
    }

    /**
     * Compatibility fallback for an older external quality.yml. Canonical crop
     * IDs remain the source of identity; the fallback only recognizes the
     * stable crop_<id>[_quality_<quality>] naming form.
     */
    private Optional<CropQuality> qualityFromCanonicalId(String itemId) {
        int marker = itemId.lastIndexOf("_quality_");
        if (marker < 0 && itemId.startsWith("processed_")) {
            int suffix = itemId.lastIndexOf('_');
            return suffix < 0 ? Optional.empty() : CropQuality.fromId(itemId.substring(suffix + 1));
        }
        if (!itemId.startsWith("crop_") || marker < 0) return Optional.empty();
        return CropQuality.fromId(itemId.substring(marker + "_quality_".length()));
    }

    private Optional<String> cropFromCanonicalId(String itemId) {
        if (!itemId.startsWith("crop_") && !itemId.startsWith("processed_")) return Optional.empty();
        String crop = itemId.startsWith("crop_")
                ? itemId.substring("crop_".length())
                : itemId.substring("processed_".length());
        int marker = crop.lastIndexOf("_quality_");
        if (marker >= 0) crop = crop.substring(0, marker);
        if (itemId.startsWith("processed_") && crop.indexOf('_') >= 0) {
            crop = crop.substring(0, crop.indexOf('_'));
        }
        return crop.isBlank() ? Optional.empty() : Optional.of(crop);
    }

    public boolean enabled() { return enabled; }

    /** Canonical quality score used by delivery previews and later reward stages. */
    public double qualityScore(CropQuality quality) {
        if (quality == null) return 0.0D;
        QualityDefinition definition = definitions.get(quality);
        return definition == null ? 0.0D : definition.minimumScore();
    }

    public boolean distributionsValid() {
        return isValidDistribution(baseDistribution)
                && cropDistributions.values().stream().allMatch(this::isValidDistribution);
    }

    public Map<CropQuality, Long> simulate(String cropId, int trials, double qualityDensityShift, double ignoredExtension) {
        int count = Math.max(0, trials);
        Map<CropQuality, Long> result = new EnumMap<>(CropQuality.class);
        for (CropQuality quality : CropQuality.values()) result.put(quality, 0L);
        Map<CropQuality, Double> distribution = effectiveDistribution(cropId, qualityDensityShift);
        double total = distribution.values().stream().mapToDouble(Double::doubleValue).sum();
        if (!Double.isFinite(total) || total <= 0.0D) return Map.copyOf(result);
        for (int i = 0; i < count; i++) {
            double selected = ThreadLocalRandom.current().nextDouble(total);
            double cursor = 0.0D;
            for (CropQuality quality : CropQuality.values()) {
                cursor += distribution.getOrDefault(quality, 0.0D);
                if (selected < cursor) {
                    result.put(quality, result.get(quality) + 1L);
                    break;
                }
            }
        }
        return Map.copyOf(result);
    }

    private Map<CropQuality, Double> parseDistribution(String path) {
        if (config.getFarmingQualitySection(path) == null) return Map.of();
        Map<CropQuality, Double> result = new EnumMap<>(CropQuality.class);
        for (CropQuality quality : CropQuality.values()) {
            result.put(quality, safeNonNegative(config.getFarmingQualityDouble(
                    path + "." + quality.id(), 0.0D)));
        }
        return result;
    }

    private Map<CropQuality, Double> normalizeDistribution(Map<CropQuality, Double> source) {
        double total = source.values().stream().mapToDouble(Double::doubleValue).sum();
        if (!Double.isFinite(total) || total <= 0.0D) return Map.of();
        Map<CropQuality, Double> result = new EnumMap<>(CropQuality.class);
        source.forEach((quality, value) -> result.put(quality, value * 100.0D / total));
        return Map.copyOf(result);
    }

    private boolean isValidDistribution(Map<CropQuality, Double> distribution) {
        if (distribution == null || distribution.isEmpty()) return false;
        double total = distribution.values().stream().mapToDouble(Double::doubleValue).sum();
        return Double.isFinite(total) && Math.abs(total - 100.0D) < 0.000001D
                && distribution.values().stream().allMatch(value -> Double.isFinite(value) && value >= 0.0D);
    }

    private void validateDistributions() {
        if (!isValidDistribution(baseDistribution)) {
            config.getPlugin().getLogger().warning("Farming quality base-distribution is invalid; safe fallback was used.");
        }
        cropDistributions.forEach((crop, distribution) -> {
            if (!isValidDistribution(distribution)) {
                config.getPlugin().getLogger().warning("Farming quality distribution is invalid for crop " + crop);
            }
        });
    }

    private double fallbackDistribution(CropQuality quality) {
        return switch (quality) {
            case NORMAL -> 55.0D;
            case BASIC -> 25.0D;
            case PROFICIENT -> 12.0D;
            case ADVANCED -> 6.0D;
            case SUPREME -> 2.0D;
        };
    }

    private double safeNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record QualityDefinition(CropQuality quality, double minimumScore, String displayName) { }

    public record QualityRoll(CropQuality quality, double score, Optional<String> itemId) {
        public QualityRoll {
            itemId = itemId == null ? Optional.empty() : itemId;
        }
    }

}
