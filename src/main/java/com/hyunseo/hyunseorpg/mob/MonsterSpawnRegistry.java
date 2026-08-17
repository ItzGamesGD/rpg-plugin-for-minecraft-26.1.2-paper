package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MonsterSpawnRegistry {
    private final ConfigService configService;
    private final Map<String, MonsterSpawnData> definitions = new LinkedHashMap<>();

    public MonsterSpawnRegistry(ConfigService configService) {
        this.configService = configService;
    }

    public void load() {
        definitions.clear();
        for (String rawId : configService.getMonsterSpawnsKeys("spawns")) {
            String path = "spawns." + rawId;
            ConfigurationSection section = configService.getMonsterSpawnsSection(path);
            if (section == null) continue;
            EntityType baseMob = parseEntityType(section.getString("base-mob", ""));
            if (baseMob == null || !baseMob.isAlive()) {
                configService.getPlugin().getLogger().warning("Invalid base-mob in monster-spawns.yml: " + path);
                continue;
            }
            String id = normalize(rawId);
            ConfigurationSection spawn = section.getConfigurationSection("spawn");
            ConfigurationSection values = spawn == null ? section : spawn;
            String rawMode = values.getString("mode", "");
            boolean direct = values.getBoolean("direct-spawn", "ADDITIVE".equalsIgnoreCase(rawMode));
            boolean replace = values.getBoolean("replace-vanilla", !direct);
            String mode = rawMode.isBlank() ? (direct ? "ADDITIVE" : "REPLACEMENT") : rawMode;
            double replacementChance = values.getDouble("replacement-chance", values.getDouble("chance", 1.0D));
            double chance = values.getDouble("chance", direct ? 0.02D : replacementChance);
            List<String> worlds = values.getStringList("allowed-worlds");
            if (worlds.isEmpty()) worlds = values.getStringList("worlds");
            List<String> biomes = values.getStringList("biomes");
            definitions.put(id, new MonsterSpawnData(
                    id,
                    baseMob,
                    values.getDouble("spawn-weight", values.getDouble("weight", 1.0D)),
                    values.getInt("min-rpg-level", values.getInt("min-level", 1)),
                    values.getInt("max-rpg-level", values.getInt("max-level", 100)),
                    worlds,
                    biomes,
                    replace,
                    replacementChance,
                    direct,
                    values.getBoolean("enabled", values.getBoolean("spawn-enabled", true)),
                    mode,
                    chance,
                    values.getString("level-source", "NEAREST_PLAYER"),
                    values.getInt("min-light", 0),
                    values.getInt("max-light", 15),
                    values.getInt("min-y", -64),
                    values.getInt("max-y", 320),
                    values.getDouble("min-distance", values.getDouble("min-player-distance", 0.0D)),
                    values.getDouble("max-distance", values.getDouble("max-player-distance", -1.0D)),
                    values.getInt("max-nearby", 8),
                    values.getDouble("nearby-radius", 32.0D),
                    values.getInt("max-per-chunk", 0),
                    values.getInt("global-cap", -1),
                    values.getBoolean("require-solid-ground", values.getBoolean("requires-ground", true)),
                    values.getBoolean("require-open-space", true),
                    values.getBoolean("despawn-with-distance", true)
            ));
        }
        if (definitions.isEmpty()) {
            configService.getPlugin().getLogger().warning("No registered monster spawn definitions loaded from monster-spawns.yml.");
        } else {
            configService.getPlugin().getLogger().info(
                    "Loaded " + definitions.size() + " registered monster spawn definitions from monster-spawns.yml.");
        }
    }

    public List<MonsterSpawnData> getAll() {
        return List.copyOf(definitions.values());
    }

    public Optional<MonsterSpawnData> get(String monsterId) {
        return Optional.ofNullable(definitions.get(normalize(monsterId)));
    }

    public List<MonsterSpawnData> findForNatural(Location location, EntityType baseMob, int level) {
        if (location == null || location.getWorld() == null) return List.of();
        String biome = location.getBlock().getBiome().name();
        return definitions.values().stream()
                .filter(MonsterSpawnData::spawnEnabled)
                .filter(data -> data.isReplacement() && data.replaceVanilla() && data.baseMob() == baseMob)
                .filter(data -> data.acceptsLevel(level))
                .filter(data -> data.acceptsWorld(location.getWorld().getName()))
                .filter(data -> data.acceptsBiome(biome))
                .toList();
    }

    public List<MonsterSpawnData> findForDirect(Location location, int level) {
        if (location == null || location.getWorld() == null) return List.of();
        String biome = location.getBlock().getBiome().name();
        return definitions.values().stream()
                .filter(MonsterSpawnData::spawnEnabled)
                .filter(MonsterSpawnData::isAdditive)
                .filter(data -> data.acceptsLevel(level))
                .filter(data -> data.acceptsWorld(location.getWorld().getName()))
                .filter(data -> data.acceptsBiome(biome))
                .toList();
    }

    private EntityType parseEntityType(String raw) {
        if (raw == null) return null;
        try {
            return EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
