package com.hyunseo.hyunseorpg.player;

import com.hyunseo.hyunseorpg.farming.FarmingStage;
import com.hyunseo.hyunseorpg.farming.CropQuality;
import com.hyunseo.hyunseorpg.farming.DeliveryStatus;
import com.hyunseo.hyunseorpg.farming.FarmingDeliveryState;
import com.hyunseo.hyunseorpg.quest.AutoQuestData;
import com.hyunseo.hyunseorpg.quest.AutoQuestObjectiveData;
import com.hyunseo.hyunseorpg.quest.AutoQuestStatus;
import com.hyunseo.hyunseorpg.quest.AutoQuestType;
import com.hyunseo.hyunseorpg.quest.availability.MonsterDiscoveryData;
import com.hyunseo.hyunseorpg.quest.availability.QuestTargetSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public final class YamlPlayerDataRepository implements PlayerDataRepository {
    private final JavaPlugin plugin;
    private final File playersDirectory;

    public YamlPlayerDataRepository(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.playersDirectory = new File(plugin.getDataFolder(), "players");
    }

    @Override
    public PlayerRPGData load(UUID uuid) throws IOException {
        Objects.requireNonNull(uuid, "uuid");
        File playerFile = getPlayerFile(uuid);
        PlayerRPGData data = new PlayerRPGData(uuid);

        if (!playerFile.exists()) {
            return data;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerFile);
        String storedUuid = yaml.getString("uuid");
        if (storedUuid != null && !storedUuid.equals(uuid.toString())) {
            throw new IOException("Player data UUID mismatch in " + playerFile.getName());
        }

        // Older/manual test files may use 0 as an uninitialized level. PlayerRPGData
        // treats level 1 as the minimum valid RPG level, so migrate such values here.
        int storedBaseLevel = yaml.getInt("baseLevel", data.getBaseLevel());
        data.setBaseLevel(Math.max(1, storedBaseLevel));
        data.setBaseExp(yaml.getLong("baseExp", data.getBaseExp()));
        data.setMinerHasteSeconds(yaml.getLong("minerHasteSeconds", data.getMinerHasteSeconds()));
        data.setFirstWitherClear(yaml.getBoolean("bossProgress.firstWitherClear", false));
        data.setFirstEnderDragonClear(yaml.getBoolean("bossProgress.firstEnderDragonClear", false));
        data.setWitherClearCount(yaml.getInt("bossProgress.witherClearCount", 0));
        data.setEnderDragonClearCount(yaml.getInt("bossProgress.enderDragonClearCount", 0));
        readStringSet(yaml, "progressionFlags", data::addProgressionFlag);
        readStringSet(yaml, "visitedWorlds", value -> {
            String dimension = canonicalDimension(value);
            if (dimension != null) data.addVisitedWorld(dimension);
        });
        readStringSet(yaml, "unlockedWorlds", data::addUnlockedWorld);
        readStringSet(yaml, "clearedWorlds", data::addClearedWorld);
        readIntegerMap(yaml, "enhancementData", data::setEnhancementLevel);
        readStringMap(yaml, "questStates", data::setQuestState);
        readLongMap(yaml, "customMobKillCounts", data::setCustomMobKillCount);
        readCustomMonsterDiscoveries(yaml, data);
        readLongMap(yaml, "bossKillCounts", data::setBossKillCount);
        readIntegerMap(yaml, "questProgress", (key, value) -> {
            String[] parts = key.split("\\.", 2);
            if (parts.length == 2) {
                data.setQuestProgress(parts[0], parts[1], value);
            }
        });
        data.setCompletedAutoQuestCount(yaml.getInt("quests.history.completed", 0));
        data.setFailedAutoQuestCount(yaml.getInt("quests.history.failed", 0));
        data.setAbandonedAutoQuestCount(yaml.getInt("quests.history.abandoned", 0));
        readAutoQuests(yaml, data);
        readFarmingData(yaml, data);
        readAlchemyData(yaml, data);
        return data;
    }

    @Override
    public void save(PlayerRPGData data) throws IOException {
        Objects.requireNonNull(data, "data");
        ensurePlayersDirectory();

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("schema-version", 4);
        yaml.set("uuid", data.getUuid().toString());
        yaml.set("baseLevel", data.getBaseLevel());
        yaml.set("baseExp", data.getBaseExp());
        yaml.set("minerHasteSeconds", data.getMinerHasteSeconds());
        yaml.set("bossProgress.firstWitherClear", data.hasFirstWitherClear());
        yaml.set("bossProgress.firstEnderDragonClear", data.hasFirstEnderDragonClear());
        yaml.set("bossProgress.witherClearCount", data.getWitherClearCount());
        yaml.set("bossProgress.enderDragonClearCount", data.getEnderDragonClearCount());

        yaml.set("progressionFlags", data.getProgressionFlags().stream().sorted().toList());
        yaml.set("visitedWorlds", data.getVisitedWorlds().stream()
                .filter(this::isCanonicalDimension)
                .sorted().toList());
        data.getEnhancementData().forEach((enhancementId, level) -> yaml.set("enhancementData." + enhancementId, level));
        data.getQuestStates().forEach((questId, state) -> yaml.set("questStates." + questId, state));
        data.getCustomMobKillCounts().forEach((mobId, amount) -> yaml.set("customMobKillCounts." + mobId, amount));
        data.getCustomMonsterDiscoveries().forEach((mobId, discovery) -> {
            String root = "discovery.custom-monsters." + mobId;
            yaml.set(root + ".first-seen-at", discovery.firstSeenAt());
            yaml.set(root + ".first-killed-at", discovery.firstKilledAt());
        });
        data.getBossKillCounts().forEach((bossId, amount) -> yaml.set("bossKillCounts." + bossId, amount));
        data.getQuestProgressData().forEach((progressId, amount) -> yaml.set("questProgress." + progressId, amount));
        yaml.set("quests.cooldown-until", data.getAutoQuestCooldownUntil());
        yaml.set("quests.history.completed", data.getCompletedAutoQuestCount());
        yaml.set("quests.history.failed", data.getFailedAutoQuestCount());
        yaml.set("quests.history.abandoned", data.getAbandonedAutoQuestCount());
        data.getAutoQuests().forEach((slot, quest) -> {
            String root = "quests.active.slot-" + slot;
            yaml.set(root + ".id", quest.id());
            yaml.set(root + ".type", quest.type().name());
            yaml.set(root + ".target-source", quest.targetSource().name());
            yaml.set(root + ".target", quest.target());
            yaml.set(root + ".display-name", quest.displayName());
            yaml.set(root + ".amount", quest.amount());
            yaml.set(root + ".progress", quest.progress());
            yaml.set(root + ".created-at", quest.createdAt());
            yaml.set(root + ".expires-at", quest.expiresAt());
            yaml.set(root + ".reward-exp", quest.rewardExp());
            yaml.set(root + ".difficulty", quest.difficulty());
            yaml.set(root + ".status", quest.status().name());
            int objectiveIndex = 1;
            for (AutoQuestObjectiveData objective : quest.objectives()) {
                String objectiveRoot = root + ".objectives.objective-" + objectiveIndex++;
                yaml.set(objectiveRoot + ".target-source", objective.source().name());
                yaml.set(objectiveRoot + ".target", objective.target());
                yaml.set(objectiveRoot + ".display-name", objective.displayName());
                yaml.set(objectiveRoot + ".amount", objective.amount());
                yaml.set(objectiveRoot + ".progress", objective.progress());
                yaml.set(objectiveRoot + ".difficulty", objective.difficulty());
            }
        });
        yaml.set("farming.version", data.getFarmingDataVersion());
        yaml.set("farming.stage", data.getFarmingStage().name());
        yaml.set("farming.total-valid-harvests", data.getFarmingTotalValidHarvests());
        yaml.set("farming.abundance-points", data.getFarmingAbundancePoints());
        data.getFarmingCropHarvests().forEach((cropId, amount) ->
                yaml.set("farming.crop-harvests." + cropId, amount));
        yaml.set("farming.unlocked-crops", data.getFarmingUnlockedCrops().stream().sorted().toList());
        data.getFarmingStatTokenUses().forEach((tokenId, amount) ->
                yaml.set("farming.stat-token-uses." + tokenId, amount));
        data.getFarmingDeliveryCompletedCounts().forEach((provider, count) ->
                yaml.set("farming.deliveries." + provider + ".completed-count", count));
        data.getFarmingDeliveries().forEach((provider, delivery) -> {
            String root = "farming.deliveries." + provider;
            yaml.set(root + ".active-delivery-id", delivery.deliveryId());
            yaml.set(root + ".definition-id", delivery.definitionId());
            yaml.set(root + ".item-family", delivery.itemFamily());
            yaml.set(root + ".required-amount", delivery.requiredAmount());
            yaml.set(root + ".minimum-quality", delivery.minimumQuality().id());
            yaml.set(root + ".created-at", delivery.createdAt());
            yaml.set(root + ".expires-at", delivery.expiresAt());
            yaml.set(root + ".status", delivery.status().name());
            yaml.set(root + ".status-at", delivery.statusAt());
        });
        yaml.set("alchemy.version", data.getAlchemyDataVersion());
        File target = getPlayerFile(data.getUuid());
        File temp = new File(target.getParentFile(), target.getName() + ".tmp");
        yaml.save(temp);
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private boolean isCanonicalDimension(String value) {
        return canonicalDimension(value) != null;
    }

    private String canonicalDimension(String value) {
        if (value == null) return null;
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "WORLD", "OVERWORLD", "WORLD_OVERWORLD" -> "OVERWORLD";
            case "WORLD_NETHER", "NETHER" -> "NETHER";
            case "WORLD_THE_END", "THE_END", "END" -> "THE_END";
            default -> null;
        };
    }

    @Override
    public boolean exists(UUID uuid) {
        return getPlayerFile(Objects.requireNonNull(uuid, "uuid")).exists();
    }




    private void readStringSet(YamlConfiguration yaml, String path, StringValueConsumer consumer) {
        for (String value : yaml.getStringList(path)) {
            if (value != null && !value.isBlank()) {
                consumer.accept(value);
            }
        }
    }

    private void readIntegerMap(YamlConfiguration yaml, String path, StringIntegerConsumer consumer) {
        ConfigurationSection section = yaml.getConfigurationSection(path);
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            consumer.accept(key, section.getInt(key, 0));
        }
    }

    private void readLongMap(YamlConfiguration yaml, String path, StringLongConsumer consumer) {
        ConfigurationSection section = yaml.getConfigurationSection(path);
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            consumer.accept(key, section.getLong(key, 0L));
        }
    }


    private void readStringMap(YamlConfiguration yaml, String path, StringStringConsumer consumer) {
        ConfigurationSection section = yaml.getConfigurationSection(path);
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            consumer.accept(key, section.getString(key, ""));
        }
    }

    private void readAutoQuests(YamlConfiguration yaml, PlayerRPGData data) {
        data.setAutoQuestCooldownUntil(yaml.getLong("quests.cooldown-until", 0L));
        ConfigurationSection active = yaml.getConfigurationSection("quests.active");
        if (active == null) return;
        for (String slotKey : active.getKeys(false)) {
            ConfigurationSection section = active.getConfigurationSection(slotKey);
            if (section == null || !slotKey.toLowerCase(Locale.ROOT).startsWith("slot-")) continue;
            int slot;
            try {
                slot = Integer.parseInt(slotKey.substring(5));
                AutoQuestType type = AutoQuestType.valueOf(section.getString("type", "HUNT").toUpperCase(Locale.ROOT));
                QuestTargetSource source = parseTargetSource(section.getString("target-source", ""), type);
                AutoQuestStatus status = parseStatus(section.getString("status", ""), section.getInt("progress", 0), section.getInt("amount", 1));
                List<AutoQuestObjectiveData> objectives = readAutoQuestObjectives(section, type);
                if (objectives.isEmpty()) {
                    data.setAutoQuest(new AutoQuestData(slot,
                            section.getString("id", "auto-" + slot), type, source,
                            section.getString("target", ""), section.getString("display-name", ""),
                            section.getInt("amount", 1), section.getInt("progress", 0),
                            section.getLong("created-at", 0L), section.getLong("expires-at", 0L),
                            0L, section.getLong("reward-exp", 0L),
                            section.getInt("difficulty", 1), status));
                } else {
                    data.setAutoQuest(new AutoQuestData(slot, section.getString("id", "auto-" + slot), type,
                            objectives, section.getString("display-name", ""), section.getLong("created-at", 0L),
                            section.getLong("expires-at", 0L), 0L,
                            section.getLong("reward-exp", 0L), status));
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid auto quest slot in " + getPlayerFile(data.getUuid()).getName() + ": " + slotKey);
            }
        }
    }

    private QuestTargetSource parseTargetSource(String raw, AutoQuestType type) {
        try {
            return QuestTargetSource.valueOf(raw == null ? "" : raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return type == AutoQuestType.ITEM_DELIVERY
                    ? QuestTargetSource.HYUNSEORPG_CUSTOM_ITEM : QuestTargetSource.HYUNSEORPG_CUSTOM_MOB;
        }
    }

    private List<AutoQuestObjectiveData> readAutoQuestObjectives(ConfigurationSection quest, AutoQuestType type) {
        ConfigurationSection section = quest.getConfigurationSection("objectives");
        if (section == null) return List.of();
        List<AutoQuestObjectiveData> objectives = new java.util.ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection objective = section.getConfigurationSection(key);
            if (objective == null) continue;
            QuestTargetSource source = parseTargetSource(objective.getString("target-source", ""), type);
            objectives.add(new AutoQuestObjectiveData(source, objective.getString("target", ""),
                    objective.getString("display-name", ""), objective.getInt("amount", 1),
                    objective.getInt("progress", 0), objective.getInt("difficulty", 1)));
        }
        return List.copyOf(objectives);
    }

    private AutoQuestStatus parseStatus(String raw, int progress, int amount) {
        try {
            return AutoQuestStatus.valueOf(raw == null ? "" : raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return progress >= Math.max(1, amount) ? AutoQuestStatus.READY_TO_COMPLETE : AutoQuestStatus.ACTIVE;
        }
    }

    private void readCustomMonsterDiscoveries(YamlConfiguration yaml, PlayerRPGData data) {
        ConfigurationSection discoveries = yaml.getConfigurationSection("discovery.custom-monsters");
        if (discoveries == null) return;
        for (String mobId : discoveries.getKeys(false)) {
            ConfigurationSection section = discoveries.getConfigurationSection(mobId);
            if (section == null) continue;
            data.setCustomMonsterDiscovery(mobId,
                    section.getLong("first-seen-at", 0L),
                    section.getLong("first-killed-at", 0L));
        }
    }

    private void readFarmingData(YamlConfiguration yaml, PlayerRPGData data) {
        boolean hasFarmingSection = yaml.isConfigurationSection("farming");
        int storedVersion = yaml.getInt("farming.version", 1);
        // Keep the on-disk version in memory. Explicit farming migration owns
        // version upgrades; loading an old profile must not silently promote it.
        data.setFarmingDataVersion(Math.max(1, storedVersion));
        String rawStage = yaml.getString("farming.stage");
        if (rawStage != null) {
            FarmingStage.fromInput(rawStage).ifPresentOrElse(
                    data::setFarmingStage,
                    () -> plugin.getLogger().warning("Ignoring unknown farming stage in player data: " + rawStage));
        }
        data.setFarmingTotalValidHarvests(yaml.getLong("farming.total-valid-harvests", 0L));
        data.setFarmingAbundancePoints(Math.max(0L, yaml.getLong("farming.abundance-points", 0L)));
        readLongMap(yaml, "farming.favor",
                (provider, amount) -> data.setFarmingFavor(provider, Math.max(0L, amount)));
        readLongMap(yaml, "farming.crop-harvests", data::setFarmingCropHarvestCount);
        if (yaml.get("farming.unlocked-crops") != null) {
            data.clearFarmingUnlockedCrops();
            readStringSet(yaml, "farming.unlocked-crops", data::unlockFarmingCrop);
        }
        readIntegerMap(yaml, "farming.stat-token-uses", data::setFarmingStatTokenUses);
        readFarmingDeliveries(yaml, data);
        data.setFarmingDataMigrationRequired(!hasFarmingSection || storedVersion < 3);
    }

    private void readAlchemyData(YamlConfiguration yaml, PlayerRPGData data) {
        boolean hasAlchemySection = yaml.isConfigurationSection("alchemy");
        int storedVersion = yaml.getInt("alchemy.version", 1);
        data.setAlchemyDataVersion(Math.max(1, storedVersion));
        data.setAlchemyDataMigrationRequired(!hasAlchemySection || storedVersion < 1);
    }

    private void readFarmingDeliveries(YamlConfiguration yaml, PlayerRPGData data) {
        ConfigurationSection deliveries = yaml.getConfigurationSection("farming.deliveries");
        if (deliveries == null) return;
        for (String provider : deliveries.getKeys(false)) {
            String root = "farming.deliveries." + provider;
            data.setFarmingDeliveryCompletedCount(provider,
                    Math.max(0, yaml.getInt(root + ".completed-count", 0)));
            String deliveryId = yaml.getString(root + ".active-delivery-id", "");
            if (deliveryId.isBlank()) continue;
            try {
                CropQuality quality = CropQuality.fromId(yaml.getString(root + ".minimum-quality", "normal"))
                        .orElse(CropQuality.NORMAL);
                DeliveryStatus status;
                try {
                    status = DeliveryStatus.valueOf(yaml.getString(root + ".status", "ACTIVE").toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    status = DeliveryStatus.ACTIVE;
                }
                data.setFarmingDelivery(provider, new FarmingDeliveryState(
                        deliveryId,
                        yaml.getString(root + ".definition-id", provider),
                        yaml.getString(root + ".item-family", "crop_corn"),
                        Math.max(1, yaml.getInt(root + ".required-amount", 1)),
                        quality,
                        Math.max(0L, yaml.getLong(root + ".created-at", 0L)),
                        Math.max(0L, yaml.getLong(root + ".expires-at", 0L)),
                        status,
                        Math.max(0L, yaml.getLong(root + ".status-at", 0L))));
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Ignoring invalid farming delivery state for provider " + provider + ": " + exception.getMessage());
            }
        }
    }

    private void ensurePlayersDirectory() throws IOException {
        if (playersDirectory.exists()) {
            if (!playersDirectory.isDirectory()) {
                throw new IOException("Player data path is not a directory: " + playersDirectory);
            }
            return;
        }

        if (!playersDirectory.mkdirs()) {
            throw new IOException("Failed to create player data directory: " + playersDirectory);
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(playersDirectory, uuid + ".yml");
    }

    @FunctionalInterface
    private interface StringValueConsumer {
        void accept(String value);
    }

    @FunctionalInterface
    private interface StringIntegerConsumer {
        void accept(String key, int value);
    }

    @FunctionalInterface
    private interface StringLongConsumer {
        void accept(String key, long value);
    }

    @FunctionalInterface
    private interface StringStringConsumer {
        void accept(String key, String value);
    }
}
