package com.hyunseo.hyunseorpg.farming;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/** YAML implementation kept behind CropStorage for future SQLite/region migration. */
public final class YamlChunkCropStorage implements CropStorage {
    private final JavaPlugin plugin;
    private final File root;

    public YamlChunkCropStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.root = new File(plugin.getDataFolder(), "farming/crops");
    }

    @Override
    public CropChunkSnapshot load(UUID worldId, int chunkX, int chunkZ) throws IOException {
        File file = file(worldId, chunkX, chunkZ);
        if (!file.isFile()) return new CropChunkSnapshot(worldId, chunkX, chunkZ, List.of());

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getInt("schema-version", 0) != 1) {
            plugin.getLogger().warning("Ignoring unsupported farming crop chunk schema: " + file.getName());
            return new CropChunkSnapshot(worldId, chunkX, chunkZ, List.of());
        }
        if (!worldId.toString().equalsIgnoreCase(yaml.getString("world-uuid", ""))
                || yaml.getInt("chunk-x", Integer.MIN_VALUE) != chunkX
                || yaml.getInt("chunk-z", Integer.MIN_VALUE) != chunkZ) {
            plugin.getLogger().warning("Ignoring mismatched farming crop chunk: " + file.getName());
            return new CropChunkSnapshot(worldId, chunkX, chunkZ, List.of());
        }

        List<CropInstance> crops = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("crops");
        if (section == null) return new CropChunkSnapshot(worldId, chunkX, chunkZ, List.of());
        for (String key : section.getKeys(false)) {
            ConfigurationSection crop = section.getConfigurationSection(key);
            if (crop == null) continue;
            try {
                int x = crop.getInt("x", Integer.MIN_VALUE);
                int y = crop.getInt("y", Integer.MIN_VALUE);
                int z = crop.getInt("z", Integer.MIN_VALUE);
                if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE
                        || (x >> 4) != chunkX || (z >> 4) != chunkZ) {
                    plugin.getLogger().warning("Skipping invalid crop coordinate in " + file.getName());
                    continue;
                }
                crops.add(new CropInstance(
                        crop.getString("crop-id", ""),
                        new CropPosition(worldId, x, y, z),
                        crop.getInt("stage", 0),
                        crop.getLong("planted-at", 0L),
                        crop.getLong("next-growth-at", Long.MAX_VALUE),
                        crop.getInt("data-version", 1)));
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING,
                        "Skipping corrupted crop entry " + key + " in " + file.getName(), exception);
            }
        }
        return new CropChunkSnapshot(worldId, chunkX, chunkZ, crops);
    }

    @Override
    public void save(CropChunkSnapshot snapshot) throws IOException {
        File file = file(snapshot.worldId(), snapshot.chunkX(), snapshot.chunkZ());
        if (snapshot.crops().isEmpty()) {
            Files.deleteIfExists(file.toPath());
            return;
        }
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create farming crop storage directory: " + parent);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("schema-version", 1);
        yaml.set("world-uuid", snapshot.worldId().toString());
        yaml.set("chunk-x", snapshot.chunkX());
        yaml.set("chunk-z", snapshot.chunkZ());
        int index = 0;
        for (CropInstance crop : snapshot.crops()) {
            String path = "crops." + index++;
            yaml.set(path + ".crop-id", crop.cropId());
            yaml.set(path + ".x", crop.position().x());
            yaml.set(path + ".y", crop.position().y());
            yaml.set(path + ".z", crop.position().z());
            yaml.set(path + ".stage", crop.stage());
            yaml.set(path + ".planted-at", crop.plantedAt());
            yaml.set(path + ".next-growth-at", crop.nextGrowthAt());
            yaml.set(path + ".data-version", crop.dataVersion());
        }
        Path target = file.toPath();
        Path temporary = target.resolveSibling(file.getName() + ".tmp");
        yaml.save(temporary.toFile());
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private File file(UUID worldId, int chunkX, int chunkZ) {
        return new File(new File(root, worldId.toString()), chunkX + "_" + chunkZ + ".yml");
    }
}
