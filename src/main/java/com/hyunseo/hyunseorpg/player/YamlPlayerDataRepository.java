package com.hyunseo.hyunseorpg.player;

import com.hyunseo.hyunseorpg.farming.FarmingStage;
import com.hyunseo.hyunseorpg.farming.CropQuality;
import com.hyunseo.hyunseorpg.farming.DeliveryStatus;
import com.hyunseo.hyunseorpg.farming.FarmingDeliveryState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
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
        data.preserveLegacyQuestCompatibilityData(LegacyQuestCompatibilityData.capture(yaml));
        readLongMap(yaml, "customMobKillCounts", data::setCustomMobKillCount);
        readCustomMonsterDiscoveries(yaml, data);
        readLongMap(yaml, "bossKillCounts", data::setBossKillCount);
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
        data.getCustomMobKillCounts().forEach((mobId, amount) -> yaml.set("customMobKillCounts." + mobId, amount));
        data.getCustomMonsterDiscoveries().forEach((mobId, discovery) -> {
            String root = "discovery.custom-monsters." + mobId;
            yaml.set(root + ".first-seen-at", discovery.firstSeenAt());
            yaml.set(root + ".first-killed-at", discovery.firstKilledAt());
        });
        data.getBossKillCounts().forEach((bossId, amount) -> yaml.set("bossKillCounts." + bossId, amount));
        data.legacyQuestCompatibilityData().writeTo(yaml);
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

}
