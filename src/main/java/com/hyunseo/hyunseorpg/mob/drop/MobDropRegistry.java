package com.hyunseo.hyunseorpg.mob.drop;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MobDropRegistry {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final Map<String, MobDropTable> tablesById = new LinkedHashMap<>();

    public MobDropRegistry(JavaPlugin plugin, ConfigService configService) {
        this.plugin = plugin;
        this.configService = configService;
    }

    public void load() {
        tablesById.clear();
        for (String tableId : configService.getMobsKeys("drop-tables")) {
            parseTable(tableId).ifPresent(table -> tablesById.put(table.tableId(), table));
        }
    }

    public Optional<MobDropTable> get(String tableId) {
        return Optional.ofNullable(tablesById.get(normalizeId(tableId)));
    }

    public List<MobDropTable> getAll() {
        return List.copyOf(tablesById.values());
    }

    private Optional<MobDropTable> parseTable(String rawTableId) {
        String tableId = normalizeId(rawTableId);
        ConfigurationSection section = configService.getMobsSection("drop-tables." + rawTableId);
        if (section == null) {
            return Optional.empty();
        }

        List<MobDropEntry> entries = new ArrayList<>();
        for (Map<?, ?> rawEntry : section.getMapList("entries")) {
            parseEntry(tableId, rawEntry).ifPresent(entries::add);
        }
        return Optional.of(new MobDropTable(tableId, entries));
    }

    private Optional<MobDropEntry> parseEntry(String tableId, Map<?, ?> rawEntry) {
        String itemId = normalizeId(stringValue(rawEntry, "item-id", ""));
        String materialName = stringValue(rawEntry, "material", "STONE")
                .toUpperCase(Locale.ROOT)
                .replace("-", "_");
        Material material = Material.matchMaterial(materialName);
        if (itemId.isBlank() || material == null || !material.isItem()) {
            plugin.getLogger().warning("Ignoring invalid drop entry in table " + tableId + ": " + rawEntry);
            return Optional.empty();
        }

        String displayName = stringValue(rawEntry, "display-name", itemId);
        double chance = parseDouble(rawEntry.get("chance"), 1.0D);
        int minAmount = parseInt(rawEntry.get("min-amount"), 1);
        int maxAmount = parseInt(rawEntry.get("max-amount"), minAmount);
        boolean bossOnly = Boolean.parseBoolean(stringValue(rawEntry, "boss-only", "false"));
        return Optional.of(new MobDropEntry(itemId, material, displayName, chance, minAmount, maxAmount, bossOnly));
    }

    private String stringValue(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int parseInt(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private double parseDouble(Object value, double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String normalizeId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
