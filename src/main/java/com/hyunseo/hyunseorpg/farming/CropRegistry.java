package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/** Loads the safe, minimal Stage 1 crop definitions. */
public final class CropRegistry {
    private final ConfigService config;
    private final RPGItemService itemService;
    private final Map<String, CropDefinition> definitions = new LinkedHashMap<>();
    private java.util.List<String> lastErrors = java.util.List.of();
    private boolean enabled;

    public CropRegistry(ConfigService config, RPGItemService itemService) {
        this.config = config;
        this.itemService = itemService;
    }

    public boolean load() {
        Map<String, CropDefinition> previous = new LinkedHashMap<>(definitions);
        boolean previousEnabled = enabled;
        lastErrors = new java.util.ArrayList<>();
        definitions.clear();
        enabled = config.getFarmingCropsBoolean("enabled", true)
                && config.getFarmingGrowthBoolean("enabled", true);
        if (!enabled) return true;
        ConfigurationSection crops = config.getFarmingCropsSection("crops");
        if (crops == null) {
            disable("farming/crops.yml is missing crops");
            restore(previous, previousEnabled);
            return false;
        }
        ConfigurationSection growth = config.getFarmingGrowthSection("crops");
        for (String rawId : crops.getKeys(false)) {
            ConfigurationSection section = crops.getConfigurationSection(rawId);
            if (section == null) continue;
            String id = normalize(rawId);
            if (!section.getBoolean("enabled", true)) continue;
            try {
                Material displayBlock = Material.matchMaterial(section.getString("display-block", "WHEAT"));
                if (displayBlock == null || !displayBlock.isBlock()) {
                    throw new IllegalArgumentException("invalid display-block");
                }
                Set<Material> soil = section.getStringList("soil").stream()
                        .map(value -> Material.matchMaterial(value == null ? "" : value))
                        .filter(material -> material != null && material.isBlock())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
                if (soil.isEmpty()) throw new IllegalArgumentException("soil must contain a valid block");
                long growthSeconds = growth == null ? 60L
                        : Math.max(1L, growth.getLong(id + ".seconds-per-stage", 60L));
                CropDefinition definition = new CropDefinition(
                        id,
                        section.getString("seed-item-id", ""),
                        section.getString("crop-item-id", ""),
                        displayBlock,
                        section.getInt("stages", 4),
                        section.getBoolean("two-block", false),
                        soil,
                        growthSeconds,
                        section.getInt("data-version", 1),
                        true);
                if (itemService.getData(definition.seedItemId()).isEmpty()
                        || itemService.getData(definition.cropItemId()).isEmpty()) {
                    throw new IllegalArgumentException("seed or crop item is not registered");
                }
                if (definitions.putIfAbsent(id, definition) != null) {
                    throw new IllegalArgumentException("duplicate crop id");
                }
            } catch (RuntimeException exception) {
                config.getPlugin().getLogger().log(Level.WARNING,
                        "Skipping invalid farming crop definition " + rawId, exception);
            }
        }
        if (definitions.isEmpty()) {
            disable("farming/crops.yml has no valid crop definitions");
            restore(previous, previousEnabled);
            return false;
        }
        config.getPlugin().getLogger().info("Farming crop registry loaded: " + definitions.size() + " crop(s).");
        return true;
    }

    public boolean isEnabled() { return enabled; }

    public java.util.Optional<CropDefinition> get(String id) {
        return java.util.Optional.ofNullable(definitions.get(normalize(id)));
    }

    public java.util.Optional<CropDefinition> findBySeed(String itemId) {
        String normalized = normalize(itemId);
        return definitions.values().stream()
                .filter(definition -> definition.seedItemId().equals(normalized))
                .findFirst();
    }

    public java.util.List<CropDefinition> getAll() {
        return java.util.List.copyOf(definitions.values());
    }

    public java.util.List<String> lastErrors() {
        return java.util.List.copyOf(lastErrors);
    }

    private void restore(Map<String, CropDefinition> previous, boolean previousEnabled) {
        definitions.clear();
        definitions.putAll(previous);
        enabled = previousEnabled;
    }

    private void disable(String reason) {
        enabled = false;
        if (lastErrors instanceof java.util.ArrayList<?> rawErrors) {
            @SuppressWarnings("unchecked") java.util.ArrayList<String> errors = (java.util.ArrayList<String>) rawErrors;
            errors.add(reason);
        }
        config.getPlugin().getLogger().warning("Farming module disabled safely: " + reason);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
