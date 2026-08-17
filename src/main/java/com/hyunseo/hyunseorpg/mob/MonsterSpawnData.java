package com.hyunseo.hyunseorpg.mob;

import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Locale;

/** One YAML-defined replacement or additive spawn rule. */
public record MonsterSpawnData(
        String monsterId,
        EntityType baseMob,
        double spawnWeight,
        int minLevel,
        int maxLevel,
        List<String> allowedWorlds,
        List<String> biomes,
        boolean replaceVanilla,
        double replacementChance,
        boolean directSpawn,
        boolean spawnEnabled,
        String mode,
        double chance,
        String levelSource,
        int minLight,
        int maxLight,
        int minY,
        int maxY,
        double minDistance,
        double maxDistance,
        int maxNearby,
        double nearbyRadius,
        int maxPerChunk,
        int globalCap,
        boolean requireSolidGround,
        boolean requireOpenSpace,
        boolean despawnWithDistance
) {
    public MonsterSpawnData(
            String monsterId,
            EntityType baseMob,
            double spawnWeight,
            int minLevel,
            int maxLevel,
            List<String> allowedWorlds,
            List<String> biomes,
            boolean replaceVanilla,
            double replacementChance,
            boolean directSpawn,
            boolean spawnEnabled
    ) {
        this(monsterId, baseMob, spawnWeight, minLevel, maxLevel, allowedWorlds, biomes,
                replaceVanilla, replacementChance, directSpawn, spawnEnabled,
                directSpawn ? "ADDITIVE" : "REPLACEMENT", replacementChance, "NEAREST_PLAYER",
                0, 15, -64, 320, 0.0D, -1.0D, 8, 32.0D, 0, -1,
                true, true, true);
    }

    public MonsterSpawnData {
        monsterId = monsterId == null ? "" : monsterId.trim().toLowerCase(Locale.ROOT);
        spawnWeight = Math.max(0.0D, spawnWeight);
        minLevel = Math.max(1, minLevel);
        maxLevel = Math.max(minLevel, maxLevel);
        allowedWorlds = List.copyOf(allowedWorlds == null ? List.of() : allowedWorlds);
        biomes = List.copyOf(biomes == null ? List.of() : biomes);
        replacementChance = clampChance(replacementChance);
        chance = clampChance(chance);
        mode = mode == null || mode.isBlank() ? (directSpawn ? "ADDITIVE" : "REPLACEMENT")
                : mode.trim().toUpperCase(Locale.ROOT);
        levelSource = levelSource == null || levelSource.isBlank() ? "NEAREST_PLAYER"
                : levelSource.trim().toUpperCase(Locale.ROOT);
        minLight = Math.max(0, minLight);
        maxLight = Math.max(minLight, maxLight);
        maxNearby = Math.max(1, maxNearby);
        nearbyRadius = Math.max(1.0D, nearbyRadius);
        maxPerChunk = Math.max(0, maxPerChunk);
        globalCap = Math.max(-1, globalCap);
    }

    public boolean isReplacement() {
        return "REPLACEMENT".equals(mode);
    }

    public boolean isAdditive() {
        return "ADDITIVE".equals(mode);
    }

    public double spawnChance() {
        return "REPLACEMENT".equals(mode) ? replacementChance : chance;
    }

    public boolean acceptsLevel(int level) {
        return level >= minLevel && level <= maxLevel;
    }

    public boolean acceptsWorld(String worldName) {
        return allowedWorlds.isEmpty()
                || allowedWorlds.stream().anyMatch(value -> value.equalsIgnoreCase(worldName));
    }

    public boolean acceptsBiome(String biomeName) {
        return biomes.isEmpty()
                || biomes.stream().anyMatch(value -> value.equalsIgnoreCase(biomeName));
    }

    private static double clampChance(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
