package com.hyunseo.hyunseorpg.mythic;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.mob.drop.MobDropEntry;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MythicMobRegistry {
    private final ConfigService configService;
    private final java.util.Map<String, MythicMobData> dataById = new java.util.LinkedHashMap<>();

    public MythicMobRegistry(ConfigService configService) {
        this.configService = configService;
    }

    public void load() {
        dataById.clear();
        for (String rawId : configService.getMythicMobsKeys("mobs")) {
            parse(rawId).ifPresent(data -> dataById.put(data.mobId(), data));
        }
    }

    public Optional<MythicMobData> get(String mobId) {
        if (mobId == null) return Optional.empty();
        return Optional.ofNullable(dataById.get(normalize(mobId)));
    }

    public List<String> getMobIds() {
        return List.copyOf(dataById.keySet());
    }

    public Optional<ConfigurationSection> getSection(String mobId) {
        if (mobId == null || mobId.isBlank()) {
            return Optional.empty();
        }
        ConfigurationSection section = configService.getMythicMobsSection("mobs." + normalize(mobId));
        return Optional.ofNullable(section);
    }

    public List<String> getConfiguredBehaviorMobIds() {
        return dataById.keySet().stream()
                .filter(id -> getSection(id).map(section -> section.isString("base-type")).orElse(false))
                .toList();
    }

    private Optional<MythicMobData> parse(String rawId) {
        String id = normalize(rawId);
        String path = "mobs." + rawId + ".";
        ConfigurationSection section = configService.getMythicMobsSection("mobs." + rawId);
        if (section == null || !configService.getMythicMobsBoolean(path + "enabled", true)) {
            return Optional.empty();
        }

        List<MobDropEntry> drops = new ArrayList<>();
        for (Map<?, ?> rawDrop : section.getMapList("drops")) {
            String itemId = normalize(stringValue(rawDrop, "item-id", ""));
            if (itemId.isBlank()) continue;
            String materialName = stringValue(rawDrop, "material", "STONE")
                    .toUpperCase(Locale.ROOT).replace('-', '_');
            Material material = Material.matchMaterial(materialName);
            if (material == null || !material.isItem()) material = Material.STONE;
            drops.add(new MobDropEntry(
                    itemId,
                    material,
                    stringValue(rawDrop, "display-name", itemId),
                    doubleValue(rawDrop, "chance", 1.0D),
                    intValue(rawDrop, "min", 1),
                    intValue(rawDrop, "max", intValue(rawDrop, "min", 1)),
                    false
            ));
        }

        return Optional.of(new MythicMobData(
                id,
                configService.getMythicMobsString(path + "display-name", id),
                configService.getMythicMobsInt(path + "level", 1),
                configService.getMythicMobsString(path + "category", ""),
                configService.getMythicMobsLong(path + "exp-reward", 0L),
                configService.getMythicMobsLong(path + "class-exp-reward", 0L),
                configService.getMythicMobsBoolean(path + "boss", false),
                configService.getMythicMobsBoolean(path + "elite", false),
                drops
        ));
    }

    private String stringValue(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private int intValue(Map<?, ?> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private double doubleValue(Map<?, ?> map, String key, double fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) return number.doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
