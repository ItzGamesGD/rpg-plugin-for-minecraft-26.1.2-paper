package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

import java.util.List;
import java.util.Locale;

/** Canonical configuration boundary for the point-only abundance essence recipe. */
public final class FarmingEssenceService {
    private final ConfigService config;

    public FarmingEssenceService(ConfigService config) {
        this.config = config;
    }

    public boolean enabled() {
        return config.getFarmingEssenceBoolean("enabled", false);
    }

    public String unlockStage() {
        return normalize(firstNonBlank(
                config.getFarmingEssenceString("unlock-stage", ""),
                config.getFarmingEssenceString("required-farming-stage", "expert")));
    }

    public long requiredAbundancePoints() {
        return Math.max(1L, config.getFarmingEssenceLong("required-abundance-points", 100L));
    }

    public String resultItemId() {
        return normalize(firstNonBlank(
                config.getFarmingEssenceString("result-item-id", ""),
                config.getFarmingEssenceString("item-id", "abundance_essence")));
    }

    /** Compatibility alias for existing point-recipe and item migration callers. */
    public String itemId() {
        return resultItemId();
    }

    public int resultCount() {
        int legacy = config.getFarmingEssenceInt("output-amount", 1);
        return Math.min(maxStack(), Math.max(1, config.getFarmingEssenceInt("result-count", legacy)));
    }

    public int maxStack() {
        return Math.min(64, Math.max(1, config.getFarmingEssenceInt("max-stack", 64)));
    }

    public boolean isTradable() {
        return config.getFarmingEssenceBoolean("tradable",
                config.getFarmingEssenceBoolean("tradeable", true));
    }

    public boolean isSellable() {
        // This is a policy boundary, not an operator-controlled shop switch.
        return false;
    }

    public boolean reverseConversionAllowed() {
        return false;
    }

    public List<String> usageTags() {
        return config.getFarmingEssenceStringList("usage-tags").stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public boolean hasUsageTag(String tag) {
        String normalized = normalize(tag);
        return !normalized.isBlank() && usageTags().contains(normalized);
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
