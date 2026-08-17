package com.hyunseo.hyunseorpg.special;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SpecialEquipmentRegistry {
    private final ConfigService config;
    private final Map<String, SpecialEquipmentData> entries = new LinkedHashMap<>();

    public SpecialEquipmentRegistry(ConfigService config) {
        this.config = config;
    }

    public void load() {
        entries.clear();
        ConfigurationSection root = config.getSpecialEquipmentSection("special-equipment.items");
        if (root == null) return;
        for (String rawId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(rawId);
            if (section == null) continue;
            String id = normalize(rawId);
            String itemId = normalize(section.getString("item-id", id));
            ConfigurationSection unlock = section.getConfigurationSection("unlock");
            ConfigurationSection crafting = section.getConfigurationSection("crafting");
            ConfigurationSection growth = section.getConfigurationSection("growth");
            boolean allowCustomEnchants = growth == null || growth.getBoolean("allow-custom-enchants", true);
            int customEnchantSlots = growth == null
                    ? (allowCustomEnchants ? 1 : 0)
                    : Math.max(0, growth.getInt("custom-enchant-slots", allowCustomEnchants ? 1 : 0));
            int grade = section.getInt("grade", 4);
            boolean ordinaryGrowthAllowed = grade < 4;
            entries.put(id, new SpecialEquipmentData(
                    id,
                    itemId,
                    section.getString("display.name", id),
                    normalize(section.getString("attribute.element", "PHYSICAL")),
                    normalize(section.getString("attribute.equipment-type", "UNKNOWN")),
                    section.getBoolean("enabled", true),
                    unlock == null ? 1 : unlock.getInt("required-rpg-level", 1),
                    normalizeDimensionList(unlock == null ? List.of() : unlock.getStringList("vanilla-progress.required-worlds")),
                    readLongMap(unlock == null ? null : unlock.getConfigurationSection("required-custom-mob-kills")),
                    readLongMap(unlock == null ? null : unlock.getConfigurationSection("required-boss-kills")),
                    readIntMap(unlock == null ? null : unlock.getConfigurationSection("required-items")),
                    normalize(unlock == null ? "" : unlock.getString("required-equipment.item-id", "")),
                    unlock == null ? 0 : unlock.getInt("required-equipment.minimum-upgrade-level", 0),
                    unlock == null ? 0 : unlock.getInt("required-equipment.minimum-promotion-stage", 0),
                    crafting != null && crafting.getBoolean("enabled", true),
                    readIntMap(crafting == null ? null : crafting.getConfigurationSection("inputs")),
                    crafting == null ? 1 : crafting.getInt("amount", 1),
                    grade,
                    section.getBoolean("final-gear-material-allowed", false),
                    false,
                    ordinaryGrowthAllowed && growth != null && growth.getBoolean("enhancement-enabled", false),
                    ordinaryGrowthAllowed && growth != null && growth.getBoolean("promotion-enabled", false),
                    growth == null || growth.getBoolean("allow-vanilla-enchants", false),
                    allowCustomEnchants,
                    customEnchantSlots,
                    growth != null && growth.getBoolean("unbreakable", true),
                    readAbilities(section.getConfigurationSection("abilities"), id)
            ));
        }
    }

    public Optional<SpecialEquipmentData> get(String id) {
        return Optional.ofNullable(entries.get(normalize(id)));
    }

    public List<SpecialEquipmentData> getAll() {
        return List.copyOf(entries.values());
    }

    public Optional<SpecialEquipmentAbilityDefinition> getAbility(String equipmentId, String abilityKey) {
        SpecialEquipmentData data = get(equipmentId).orElse(null);
        if (data == null) return Optional.empty();
        return Optional.ofNullable(data.abilities().get(normalize(abilityKey)));
    }

    private Map<String, Integer> readIntMap(ConfigurationSection section) {
        Map<String, Integer> values = new LinkedHashMap<>();
        if (section == null) return values;
        for (String key : section.getKeys(false)) values.put(normalize(key), Math.max(0, section.getInt(key, 0)));
        return values;
    }

    private Map<String, Long> readLongMap(ConfigurationSection section) {
        Map<String, Long> values = new LinkedHashMap<>();
        if (section == null) return values;
        for (String key : section.getKeys(false)) values.put(normalize(key), Math.max(0L, section.getLong(key, 0L)));
        return values;
    }

    private Map<String, SpecialEquipmentAbilityDefinition> readAbilities(ConfigurationSection section, String equipmentId) {
        Map<String, SpecialEquipmentAbilityDefinition> result = new LinkedHashMap<>();
        if (section == null) return result;
        for (String rawKey : section.getKeys(false)) {
            ConfigurationSection ability = section.getConfigurationSection(rawKey);
            if (ability == null || !ability.isSet("id")) continue;
            String key = normalize(rawKey);
            result.put(key, new SpecialEquipmentAbilityDefinition(
                    normalize(ability.getString("id", key)),
                    ability.getString("display-name", ability.getString("id", key)),
                    normalize(ability.getString("trigger", "")),
                    ability.getString("description", ""),
                    "special-equipment.items." + equipmentId + ".abilities." + rawKey
            ));
        }
        return result;
    }

    private List<String> normalizeList(List<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) if (value != null && !value.isBlank()) result.add(normalize(value));
        return result;
    }

    private List<String> normalizeDimensionList(List<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            switch (normalized) {
                case "WORLD", "OVERWORLD", "WORLD_OVERWORLD" -> normalized = "OVERWORLD";
                case "WORLD_NETHER", "NETHER" -> normalized = "NETHER";
                case "WORLD_THE_END", "THE_END", "END" -> normalized = "THE_END";
                default -> { }
            }
            if (!result.contains(normalized)) result.add(normalized);
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
