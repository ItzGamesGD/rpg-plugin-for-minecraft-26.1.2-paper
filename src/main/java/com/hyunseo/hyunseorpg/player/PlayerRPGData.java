package com.hyunseo.hyunseorpg.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class PlayerRPGData {
    private final UUID uuid;
    private int baseLevel;
    private long baseExp;
    private long minerHasteSeconds;
    private boolean firstWitherClear;
    private boolean firstEnderDragonClear;
    private int witherClearCount;
    private int enderDragonClearCount;
    private final Set<String> progressionFlags;
    private final Map<String, Integer> enhancementData;
    private LegacyQuestCompatibilityData legacyQuestCompatibilityData;
    private final Map<String, MonsterDiscoveryData> customMonsterDiscoveries;
    private final Map<String, Long> customMobKillCounts;
    private final Map<String, Long> bossKillCounts;
    private final Set<String> visitedWorlds;
    private final Set<String> unlockedWorlds;
    private final Set<String> clearedWorlds;
    private int alchemyDataVersion;
    private boolean alchemyDataMigrationRequired;

    public PlayerRPGData(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.baseLevel = 1;
        this.baseExp = 0L;
        this.minerHasteSeconds = 0L;
        this.firstWitherClear = false;
        this.firstEnderDragonClear = false;
        this.witherClearCount = 0;
        this.enderDragonClearCount = 0;
        this.progressionFlags = new HashSet<>();
        this.enhancementData = new HashMap<>();
        this.legacyQuestCompatibilityData = LegacyQuestCompatibilityData.empty();
        this.customMonsterDiscoveries = new HashMap<>();
        this.customMobKillCounts = new HashMap<>();
        this.bossKillCounts = new HashMap<>();
        this.visitedWorlds = new HashSet<>();
        this.unlockedWorlds = new HashSet<>();
        this.clearedWorlds = new HashSet<>();
        this.alchemyDataVersion = 1;
        this.alchemyDataMigrationRequired = true;
    }

    public UUID getUuid() {
        return uuid;
    }


    public int getBaseLevel() {
        return baseLevel;
    }

    public void setBaseLevel(int baseLevel) {
        this.baseLevel = requireAtLeast(baseLevel, 1, "baseLevel");
    }

    public long getBaseExp() {
        return baseExp;
    }

    public void setBaseExp(long baseExp) {
        this.baseExp = requireNonNegative(baseExp, "baseExp");
    }


    public long getMinerHasteSeconds() {
        return minerHasteSeconds;
    }

    public void setMinerHasteSeconds(long seconds) {
        this.minerHasteSeconds = requireNonNegative(seconds, "minerHasteSeconds");
    }

    public void addMinerHasteSeconds(long seconds) {
        if (seconds < 0L) {
            throw new IllegalArgumentException("seconds must be non-negative");
        }
        this.minerHasteSeconds = Math.addExact(this.minerHasteSeconds, seconds);
    }

    public boolean hasFirstWitherClear() { return firstWitherClear; }
    public void setFirstWitherClear(boolean value) { firstWitherClear = value; }
    public boolean hasFirstEnderDragonClear() { return firstEnderDragonClear; }
    public void setFirstEnderDragonClear(boolean value) { firstEnderDragonClear = value; }
    public int getWitherClearCount() { return witherClearCount; }
    public void setWitherClearCount(int value) { witherClearCount = requireNonNegative(value, "witherClearCount"); }
    public void incrementWitherClearCount() { witherClearCount = Math.addExact(witherClearCount, 1); }
    public int getEnderDragonClearCount() { return enderDragonClearCount; }
    public void setEnderDragonClearCount(int value) { enderDragonClearCount = requireNonNegative(value, "enderDragonClearCount"); }
    public void incrementEnderDragonClearCount() { enderDragonClearCount = Math.addExact(enderDragonClearCount, 1); }


    public boolean hasProgressionFlag(String flag) {
        return progressionFlags.contains(normalizeId(flag));
    }

    public void addProgressionFlag(String flag) {
        progressionFlags.add(normalizeId(flag));
    }

    public Set<String> getProgressionFlags() {
        return Collections.unmodifiableSet(progressionFlags);
    }

    public int getEnhancementLevel(String enhancementId) {
        return enhancementData.getOrDefault(normalizeId(enhancementId), 0);
    }

    public void setEnhancementLevel(String enhancementId, int level) {
        enhancementData.put(normalizeId(enhancementId), requireNonNegative(level, "enhancement level"));
    }

    public Map<String, Integer> getEnhancementData() {
        return Collections.unmodifiableMap(enhancementData);
    }

    void preserveLegacyQuestCompatibilityData(LegacyQuestCompatibilityData compatibilityData) {
        this.legacyQuestCompatibilityData = Objects.requireNonNull(compatibilityData, "compatibilityData");
    }

    LegacyQuestCompatibilityData legacyQuestCompatibilityData() {
        return legacyQuestCompatibilityData;
    }

    public MonsterDiscoveryData getCustomMonsterDiscovery(String mobId) {
        return customMonsterDiscoveries.getOrDefault(normalizeId(mobId), new MonsterDiscoveryData(0L, 0L));
    }

    public boolean hasDiscoveredCustomMonster(String mobId) {
        MonsterDiscoveryData discovery = getCustomMonsterDiscovery(mobId);
        return discovery.firstSeenAt() > 0L || discovery.firstKilledAt() > 0L
                || getCustomMobKillCount(mobId) > 0L;
    }

    public void markCustomMonsterSeen(String mobId, long timestamp) {
        String id = normalizeId(mobId);
        if (id.isBlank()) return;
        customMonsterDiscoveries.put(id, getCustomMonsterDiscovery(id).seen(timestamp));
    }

    public void markCustomMonsterKilled(String mobId, long timestamp) {
        String id = normalizeId(mobId);
        if (id.isBlank()) return;
        customMonsterDiscoveries.put(id, getCustomMonsterDiscovery(id).killed(timestamp));
    }

    public void setCustomMonsterDiscovery(String mobId, long firstSeenAt, long firstKilledAt) {
        String id = normalizeId(mobId);
        if (id.isBlank()) return;
        customMonsterDiscoveries.put(id, new MonsterDiscoveryData(firstSeenAt, firstKilledAt));
    }

    public Map<String, MonsterDiscoveryData> getCustomMonsterDiscoveries() {
        return Collections.unmodifiableMap(customMonsterDiscoveries);
    }

    public long getCustomMobKillCount(String mobId) {
        return customMobKillCounts.getOrDefault(normalizeId(mobId), 0L);
    }

    public void setCustomMobKillCount(String mobId, long amount) {
        customMobKillCounts.put(normalizeId(mobId), requireNonNegative(amount, "custom mob kill count"));
    }

    public void incrementCustomMobKillCount(String mobId) {
        String id = normalizeId(mobId);
        long current = customMobKillCounts.getOrDefault(id, 0L);
        customMobKillCounts.put(id, Long.MAX_VALUE - current <= 0L ? Long.MAX_VALUE : current + 1L);
    }

    public Map<String, Long> getCustomMobKillCounts() {
        return Collections.unmodifiableMap(customMobKillCounts);
    }

    public long getBossKillCount(String bossId) {
        return bossKillCounts.getOrDefault(normalizeId(bossId), 0L);
    }

    public void setBossKillCount(String bossId, long amount) {
        bossKillCounts.put(normalizeId(bossId), requireNonNegative(amount, "boss kill count"));
    }

    public void incrementBossKillCount(String bossId) {
        String id = normalizeId(bossId);
        long current = bossKillCounts.getOrDefault(id, 0L);
        bossKillCounts.put(id, Long.MAX_VALUE - current <= 0L ? Long.MAX_VALUE : current + 1L);
    }

    public Map<String, Long> getBossKillCounts() {
        return Collections.unmodifiableMap(bossKillCounts);
    }

    public boolean hasVisitedWorld(String worldName) {
        return visitedWorlds.contains(normalizeDimension(worldName));
    }

    public void addVisitedWorld(String worldName) {
        visitedWorlds.add(normalizeDimension(worldName));
    }

    public Set<String> getVisitedWorlds() {
        return Collections.unmodifiableSet(visitedWorlds);
    }

    public boolean hasUnlockedWorld(String worldName) {
        return unlockedWorlds.contains(normalizeId(worldName));
    }

    public void addUnlockedWorld(String worldName) {
        unlockedWorlds.add(normalizeId(worldName));
    }

    public Set<String> getUnlockedWorlds() {
        return Collections.unmodifiableSet(unlockedWorlds);
    }

    public boolean hasClearedWorld(String worldName) {
        return clearedWorlds.contains(normalizeId(worldName));
    }

    public void addClearedWorld(String worldName) {
        clearedWorlds.add(normalizeId(worldName));
    }

    public Set<String> getClearedWorlds() {
        return Collections.unmodifiableSet(clearedWorlds);
    }

    public int getAlchemyDataVersion() {
        return alchemyDataVersion;
    }

    public void setAlchemyDataVersion(int version) {
        alchemyDataVersion = requireAtLeast(version, 1, "alchemy data version");
    }

    public boolean isAlchemyDataMigrationRequired() {
        return alchemyDataMigrationRequired;
    }

    public void setAlchemyDataMigrationRequired(boolean required) {
        alchemyDataMigrationRequired = required;
    }

    private static String normalizeId(String value) {
        String normalized = Objects.requireNonNull(value, "value").trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("id must not be empty");
        }
        return normalized;
    }

    private static String normalizeDimension(String value) {
        String normalized = normalizeId(value).toUpperCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "WORLD", "OVERWORLD", "WORLD_OVERWORLD" -> "overworld";
            case "WORLD_NETHER", "NETHER" -> "nether";
            case "WORLD_THE_END", "THE_END", "END" -> "the_end";
            default -> normalized.toLowerCase(java.util.Locale.ROOT);
        };
    }

    private static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }

    private static double requireNonNegative(double value, String fieldName) {
        if (value < 0.0D) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }

    private static long requireNonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }

    private static long safeAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private static int requireAtLeast(int value, int minimum, String fieldName) {
        if (value < minimum) {
            throw new IllegalArgumentException(fieldName + " must be at least " + minimum);
        }
        return value;
    }
}
