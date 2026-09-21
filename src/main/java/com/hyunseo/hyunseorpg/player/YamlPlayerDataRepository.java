package com.hyunseo.hyunseorpg.player;

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

    private void readAlchemyData(YamlConfiguration yaml, PlayerRPGData data) {
        boolean hasAlchemySection = yaml.isConfigurationSection("alchemy");
        int storedVersion = yaml.getInt("alchemy.version", 1);
        data.setAlchemyDataVersion(Math.max(1, storedVersion));
        data.setAlchemyDataMigrationRequired(!hasAlchemySection || storedVersion < 1);
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
