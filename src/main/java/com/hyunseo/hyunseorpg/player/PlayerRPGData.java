package com.hyunseo.hyunseorpg.player;

import com.hyunseo.hyunseorpg.classsystem.RPGClass;
import com.hyunseo.hyunseorpg.farming.FarmingStage;
import com.hyunseo.hyunseorpg.farming.FarmingDeliveryState;
import com.hyunseo.hyunseorpg.quest.AutoQuestData;
import com.hyunseo.hyunseorpg.quest.availability.MonsterDiscoveryData;
import com.hyunseo.hyunseorpg.stat.StatType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class PlayerRPGData {
    private final UUID uuid;
    private RPGClass selectedClass;
    private String selectedProfession;
    private int baseLevel;
    private long baseExp;
    private int statPoints;
    private int classLevel;
    private long classExp;
    private int skillPoints;
    private int classStatPoints;
    private double currentMana;
    private long minerHasteSeconds;
    private boolean firstWitherClear;
    private boolean firstEnderDragonClear;
    private int witherClearCount;
    private int enderDragonClearCount;
    private final EnumMap<StatType, Double> stats;
    private final EnumMap<StatType, Integer> statLevels;
    private final Map<String, Integer> skillStatLevels;
    private final Map<String, Integer> classStatLevels;
    private final Set<String> unlockedSkills;
    private final Map<String, Integer> skillLevels;
    private final Map<String, Integer> weaponProficiencyLevels;
    private final Map<String, Long> weaponProficiencyExp;
    private final Set<String> progressionFlags;
    private final Map<String, Integer> enhancementData;
    private final Map<String, String> questStates;
    private final Map<String, Integer> questProgress;
    private final Map<Integer, AutoQuestData> autoQuests;
    private long autoQuestCooldownUntil;
    private int completedAutoQuestCount;
    private int failedAutoQuestCount;
    private int abandonedAutoQuestCount;
    private final Map<String, MonsterDiscoveryData> customMonsterDiscoveries;
    private final Map<String, Long> customMobKillCounts;
    private final Map<String, Long> bossKillCounts;
    private final Set<String> visitedWorlds;
    private final Set<String> unlockedWorlds;
    private final Set<String> clearedWorlds;
    private int farmingDataVersion;
    private FarmingStage farmingStage;
    private long farmingTotalValidHarvests;
    private final Map<String, Long> farmingCropHarvests;
    private final Set<String> farmingUnlockedCrops;
    private final Map<String, Integer> farmingStatTokenUses;
    private long farmingAbundancePoints;
    private final Map<String, Long> farmingFavor;
    private final Map<String, FarmingDeliveryState> farmingDeliveries;
    private final Map<String, Integer> farmingDeliveryCompletedCounts;
    private boolean farmingDataMigrationRequired;
    private int alchemyDataVersion;
    private boolean alchemyDataMigrationRequired;

    public PlayerRPGData(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.baseLevel = 1;
        this.baseExp = 0L;
        this.statPoints = 0;
        this.classLevel = 1;
        this.classExp = 0L;
        this.skillPoints = 0;
        this.classStatPoints = 0;
        this.currentMana = 0.0D;
        this.minerHasteSeconds = 0L;
        this.firstWitherClear = false;
        this.firstEnderDragonClear = false;
        this.witherClearCount = 0;
        this.enderDragonClearCount = 0;
        this.stats = createDefaultStats();
        this.statLevels = createDefaultStatLevels();
        this.skillStatLevels = new HashMap<>();
        this.classStatLevels = new HashMap<>();
        this.unlockedSkills = new HashSet<>();
        this.skillLevels = new HashMap<>();
        this.weaponProficiencyLevels = new HashMap<>();
        this.weaponProficiencyExp = new HashMap<>();
        this.progressionFlags = new HashSet<>();
        this.enhancementData = new HashMap<>();
        this.questStates = new HashMap<>();
        this.questProgress = new HashMap<>();
        this.autoQuests = new HashMap<>();
        this.autoQuestCooldownUntil = 0L;
        this.completedAutoQuestCount = 0;
        this.failedAutoQuestCount = 0;
        this.abandonedAutoQuestCount = 0;
        this.customMonsterDiscoveries = new HashMap<>();
        this.customMobKillCounts = new HashMap<>();
        this.bossKillCounts = new HashMap<>();
        this.visitedWorlds = new HashSet<>();
        this.unlockedWorlds = new HashSet<>();
        this.clearedWorlds = new HashSet<>();
        this.farmingDataVersion = 3;
        this.farmingStage = FarmingStage.BASIC;
        this.farmingTotalValidHarvests = 0L;
        this.farmingCropHarvests = new HashMap<>();
        this.farmingUnlockedCrops = new HashSet<>();
        this.farmingUnlockedCrops.add("corn");
        this.farmingStatTokenUses = new HashMap<>();
        this.farmingAbundancePoints = 0L;
        this.farmingFavor = new HashMap<>();
        this.farmingDeliveries = new HashMap<>();
        this.farmingDeliveryCompletedCounts = new HashMap<>();
        this.farmingDataMigrationRequired = true;
        this.alchemyDataVersion = 1;
        this.alchemyDataMigrationRequired = true;
    }

    public UUID getUuid() {
        return uuid;
    }

    public RPGClass getSelectedClass() {
        return selectedClass;
    }

    public void setSelectedClass(RPGClass selectedClass) {
        this.selectedClass = selectedClass;
    }

    public String getSelectedProfession() {
        return selectedProfession;
    }

    public void setSelectedProfession(String selectedProfession) {
        this.selectedProfession = selectedProfession == null || selectedProfession.isBlank()
                ? null
                : normalizeId(selectedProfession);
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

    public int getStatPoints() {
        return statPoints;
    }

    public void setStatPoints(int statPoints) {
        this.statPoints = requireNonNegative(statPoints, "statPoints");
    }

    public int getClassLevel() {
        return classLevel;
    }

    public void setClassLevel(int classLevel) {
        this.classLevel = requireAtLeast(classLevel, 1, "classLevel");
    }

    public long getClassExp() {
        return classExp;
    }

    public void setClassExp(long classExp) {
        this.classExp = requireNonNegative(classExp, "classExp");
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = requireNonNegative(skillPoints, "skillPoints");
    }

    public int getClassStatPoints() {
        return classStatPoints;
    }

    public void setClassStatPoints(int classStatPoints) {
        this.classStatPoints = requireNonNegative(classStatPoints, "classStatPoints");
    }


    public double getCurrentMana() {
        return currentMana;
    }

    public void setCurrentMana(double currentMana) {
        if (currentMana < 0.0D) {
            throw new IllegalArgumentException("currentMana must be non-negative");
        }
        this.currentMana = currentMana;
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

    public double getStat(StatType statType) {
        return stats.getOrDefault(Objects.requireNonNull(statType, "statType"), 0.0D);
    }

    public void setStat(StatType statType, double value) {
        stats.put(Objects.requireNonNull(statType, "statType"), requireNonNegative(value, "stat value"));
    }

    public Map<StatType, Double> getStats() {
        return Collections.unmodifiableMap(stats);
    }

    public int getStatLevel(StatType statType) {
        return statLevels.getOrDefault(Objects.requireNonNull(statType, "statType"), 0);
    }

    public void setStatLevel(StatType statType, int level) {
        statLevels.put(Objects.requireNonNull(statType, "statType"), requireNonNegative(level, "stat level"));
    }

    public Map<StatType, Integer> getStatLevels() {
        return Collections.unmodifiableMap(statLevels);
    }

    public int getSkillStatLevel(String skillStatId) {
        return skillStatLevels.getOrDefault(normalizeId(skillStatId), 0);
    }

    public void setSkillStatLevel(String skillStatId, int level) {
        skillStatLevels.put(normalizeId(skillStatId), requireNonNegative(level, "skill stat level"));
    }

    public Map<String, Integer> getSkillStatLevels() {
        return Collections.unmodifiableMap(skillStatLevels);
    }

    public int getClassStatLevel(String classStatId) {
        return classStatLevels.getOrDefault(normalizeId(classStatId), 0);
    }

    public void setClassStatLevel(String classStatId, int level) {
        classStatLevels.put(normalizeId(classStatId), requireNonNegative(level, "class stat level"));
    }

    public Map<String, Integer> getClassStatLevels() {
        return Collections.unmodifiableMap(classStatLevels);
    }

    public boolean hasUnlockedSkill(String skillId) {
        return unlockedSkills.contains(normalizeId(skillId));
    }

    public void unlockSkill(String skillId) {
        unlockedSkills.add(normalizeId(skillId));
    }

    public Set<String> getUnlockedSkills() {
        return Collections.unmodifiableSet(unlockedSkills);
    }

    public int getSkillLevel(String skillId) {
        return skillLevels.getOrDefault(normalizeId(skillId), 0);
    }

    public void setSkillLevel(String skillId, int level) {
        skillLevels.put(normalizeId(skillId), requireNonNegative(level, "skill level"));
    }

    public Map<String, Integer> getSkillLevels() {
        return Collections.unmodifiableMap(skillLevels);
    }

    public int getWeaponProficiencyLevel(String weaponTypeId) {
        return weaponProficiencyLevels.getOrDefault(normalizeId(weaponTypeId), 1);
    }

    public void setWeaponProficiencyLevel(String weaponTypeId, int level) {
        weaponProficiencyLevels.put(normalizeId(weaponTypeId), requireAtLeast(level, 1, "weapon proficiency level"));
    }

    public Map<String, Integer> getWeaponProficiencyLevels() {
        return Collections.unmodifiableMap(weaponProficiencyLevels);
    }

    public long getWeaponProficiencyExp(String weaponTypeId) {
        return weaponProficiencyExp.getOrDefault(normalizeId(weaponTypeId), 0L);
    }

    public void setWeaponProficiencyExp(String weaponTypeId, long experience) {
        weaponProficiencyExp.put(normalizeId(weaponTypeId), requireNonNegative(experience, "weapon proficiency experience"));
    }

    public Map<String, Long> getWeaponProficiencyExpData() {
        return Collections.unmodifiableMap(weaponProficiencyExp);
    }

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

    public String getQuestState(String questId) {
        return questStates.getOrDefault(normalizeId(questId), "");
    }

    public void setQuestState(String questId, String state) {
        questStates.put(normalizeId(questId), Objects.requireNonNull(state, "state").trim().toUpperCase());
    }

    public boolean hasCompletedQuest(String questId) {
        return "COMPLETED".equalsIgnoreCase(getQuestState(questId));
    }

    public Map<String, String> getQuestStates() {
        return Collections.unmodifiableMap(questStates);
    }

    public int getQuestProgress(String questId, String objectiveId) {
        return questProgress.getOrDefault(questProgressKey(questId, objectiveId), 0);
    }

    public void setQuestProgress(String questId, String objectiveId, int amount) {
        questProgress.put(questProgressKey(questId, objectiveId), requireNonNegative(amount, "quest progress"));
    }

    public Map<String, Integer> getQuestProgressData() {
        return Collections.unmodifiableMap(questProgress);
    }

    public long getAutoQuestCooldownUntil() {
        return autoQuestCooldownUntil;
    }

    public void setAutoQuestCooldownUntil(long timestamp) {
        autoQuestCooldownUntil = Math.max(0L, timestamp);
    }

    public int getCompletedAutoQuestCount() {
        return completedAutoQuestCount;
    }

    public void setCompletedAutoQuestCount(int count) {
        completedAutoQuestCount = requireNonNegative(count, "completed auto quest count");
    }

    public void incrementCompletedAutoQuestCount() {
        if (completedAutoQuestCount < Integer.MAX_VALUE) completedAutoQuestCount++;
    }

    public int getFailedAutoQuestCount() {
        return failedAutoQuestCount;
    }

    public void setFailedAutoQuestCount(int count) {
        failedAutoQuestCount = requireNonNegative(count, "failed auto quest count");
    }

    public void incrementFailedAutoQuestCount() {
        if (failedAutoQuestCount < Integer.MAX_VALUE) failedAutoQuestCount++;
    }

    public int getAbandonedAutoQuestCount() {
        return abandonedAutoQuestCount;
    }

    public void setAbandonedAutoQuestCount(int count) {
        abandonedAutoQuestCount = requireNonNegative(count, "abandoned auto quest count");
    }

    public void incrementAbandonedAutoQuestCount() {
        if (abandonedAutoQuestCount < Integer.MAX_VALUE) abandonedAutoQuestCount++;
    }

    public AutoQuestData getAutoQuest(int slot) {
        return autoQuests.get(slot);
    }

    public void setAutoQuest(AutoQuestData quest) {
        if (quest != null) autoQuests.put(quest.slot(), quest);
    }

    public void removeAutoQuest(int slot) {
        autoQuests.remove(slot);
    }

    public Map<Integer, AutoQuestData> getAutoQuests() {
        return Collections.unmodifiableMap(autoQuests);
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

    public int getFarmingDataVersion() {
        return farmingDataVersion;
    }

    public void setFarmingDataVersion(int version) {
        farmingDataVersion = requireAtLeast(version, 1, "farming data version");
    }

    public FarmingStage getFarmingStage() {
        return farmingStage;
    }

    public void setFarmingStage(FarmingStage stage) {
        farmingStage = Objects.requireNonNull(stage, "stage");
    }

    public long getFarmingTotalValidHarvests() {
        return farmingTotalValidHarvests;
    }

    public void setFarmingTotalValidHarvests(long amount) {
        farmingTotalValidHarvests = requireNonNegative(amount, "farming total valid harvests");
    }

    public void addFarmingValidHarvest(String cropId, long amount) {
        long validAmount = requireNonNegative(amount, "farming harvest amount");
        if (validAmount == 0L) return;
        String id = normalizeId(cropId);
        farmingTotalValidHarvests = safeAdd(farmingTotalValidHarvests, validAmount);
        farmingCropHarvests.put(id, safeAdd(farmingCropHarvests.getOrDefault(id, 0L), validAmount));
    }

    public long getFarmingCropHarvestCount(String cropId) {
        return farmingCropHarvests.getOrDefault(normalizeId(cropId), 0L);
    }

    public void setFarmingCropHarvestCount(String cropId, long amount) {
        farmingCropHarvests.put(normalizeId(cropId), requireNonNegative(amount, "farming crop harvest count"));
    }

    public Map<String, Long> getFarmingCropHarvests() {
        return Collections.unmodifiableMap(farmingCropHarvests);
    }

    public void clearFarmingHarvestData() {
        farmingTotalValidHarvests = 0L;
        farmingCropHarvests.clear();
    }

    public boolean isFarmingCropUnlocked(String cropId) {
        return farmingUnlockedCrops.contains(normalizeId(cropId));
    }

    public void unlockFarmingCrop(String cropId) {
        farmingUnlockedCrops.add(normalizeId(cropId));
    }

    public void lockFarmingCrop(String cropId) {
        farmingUnlockedCrops.remove(normalizeId(cropId));
    }

    public void clearFarmingUnlockedCrops() {
        farmingUnlockedCrops.clear();
    }

    public Set<String> getFarmingUnlockedCrops() {
        return Collections.unmodifiableSet(farmingUnlockedCrops);
    }

    public int getFarmingStatTokenUses(String tokenId) {
        return farmingStatTokenUses.getOrDefault(normalizeId(tokenId), 0);
    }

    public void setFarmingStatTokenUses(String tokenId, int amount) {
        farmingStatTokenUses.put(normalizeId(tokenId), requireNonNegative(amount, "farming stat token uses"));
    }

    public Map<String, Integer> getFarmingStatTokenUses() {
        return Collections.unmodifiableMap(farmingStatTokenUses);
    }

    public void clearFarmingStatTokenUses() {
        farmingStatTokenUses.clear();
    }

    public long getFarmingAbundancePoints() {
        return farmingAbundancePoints;
    }

    public void setFarmingAbundancePoints(long amount) {
        farmingAbundancePoints = requireNonNegative(amount, "farming abundance points");
    }

    public void addFarmingAbundancePoints(long amount) {
        if (amount < 0L) throw new IllegalArgumentException("farming abundance points must be non-negative");
        farmingAbundancePoints = safeAdd(farmingAbundancePoints, amount);
    }

    public long getFarmingFavor(String providerId) {
        return farmingFavor.getOrDefault(normalizeId(providerId), 0L);
    }

    public void setFarmingFavor(String providerId, long amount) {
        farmingFavor.put(normalizeId(providerId), requireNonNegative(amount, "farming favor"));
    }

    public void addFarmingFavor(String providerId, long amount) {
        if (amount < 0L) throw new IllegalArgumentException("farming favor must be non-negative");
        String id = normalizeId(providerId);
        farmingFavor.put(id, safeAdd(farmingFavor.getOrDefault(id, 0L), amount));
    }

    public Map<String, Long> getFarmingFavor() {
        return Collections.unmodifiableMap(farmingFavor);
    }

    public void clearFarmingFavor() {
        farmingFavor.clear();
    }

    public FarmingDeliveryState getFarmingDelivery(String providerId) {
        return farmingDeliveries.get(normalizeId(providerId));
    }

    public void setFarmingDelivery(String providerId, FarmingDeliveryState state) {
        String id = normalizeId(providerId);
        if (state == null) farmingDeliveries.remove(id);
        else farmingDeliveries.put(id, state);
    }

    public void clearFarmingDelivery(String providerId) {
        farmingDeliveries.remove(normalizeId(providerId));
    }

    public Map<String, FarmingDeliveryState> getFarmingDeliveries() {
        return Collections.unmodifiableMap(farmingDeliveries);
    }

    public void clearFarmingDeliveries() {
        farmingDeliveries.clear();
    }

    public int getFarmingDeliveryCompletedCount(String providerId) {
        return farmingDeliveryCompletedCounts.getOrDefault(normalizeId(providerId), 0);
    }

    public void incrementFarmingDeliveryCompletedCount(String providerId) {
        String id = normalizeId(providerId);
        int current = getFarmingDeliveryCompletedCount(id);
        farmingDeliveryCompletedCounts.put(id, current == Integer.MAX_VALUE ? current : current + 1);
    }

    public void setFarmingDeliveryCompletedCount(String providerId, int amount) {
        farmingDeliveryCompletedCounts.put(normalizeId(providerId), requireNonNegative(amount,
                "farming delivery completed count"));
    }

    public Map<String, Integer> getFarmingDeliveryCompletedCounts() {
        return Collections.unmodifiableMap(farmingDeliveryCompletedCounts);
    }

    public void clearFarmingDeliveryCompletedCounts() {
        farmingDeliveryCompletedCounts.clear();
    }

    public boolean isFarmingDataMigrationRequired() {
        return farmingDataMigrationRequired;
    }

    public void setFarmingDataMigrationRequired(boolean required) {
        farmingDataMigrationRequired = required;
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

    private static String questProgressKey(String questId, String objectiveId) {
        return normalizeId(questId) + "." + normalizeId(objectiveId);
    }

    private static EnumMap<StatType, Double> createDefaultStats() {
        EnumMap<StatType, Double> defaultStats = new EnumMap<>(StatType.class);
        for (StatType statType : StatType.values()) {
            defaultStats.put(statType, 0.0D);
        }
        return defaultStats;
    }

    private static EnumMap<StatType, Integer> createDefaultStatLevels() {
        EnumMap<StatType, Integer> defaultStatLevels = new EnumMap<>(StatType.class);
        for (StatType statType : StatType.values()) {
            defaultStatLevels.put(statType, 0);
        }
        return defaultStatLevels;
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
