package com.hyunseo.hyunseorpg.exploration.persistence;

import com.hyunseo.hyunseorpg.exploration.model.StructureAnchor;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Atomic YAML storage, isolated behind StructureStorage for later SQLite/region migration. */
public final class YamlStructureStorage implements StructureStorage {
    private static final int SCHEMA_VERSION = 1;
    private final JavaPlugin plugin;
    private final File root;

    public YamlStructureStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.root = new File(plugin.getDataFolder(), "exploration/structures");
    }

    @Override
    public List<StructureRecord> loadWorld(UUID worldId) throws IOException {
        File file = file(worldId);
        if (!file.isFile()) return List.of();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getInt("schema-version", 0) != SCHEMA_VERSION) {
            plugin.getLogger().warning("Unsupported exploration world schema: " + file.getName());
            return List.of();
        }
        if (!worldId.toString().equalsIgnoreCase(yaml.getString("world-uuid", ""))) {
            plugin.getLogger().warning("Exploration world UUID mismatch: " + file.getName());
            return List.of();
        }
        ConfigurationSection records = yaml.getConfigurationSection("records");
        if (records == null) return List.of();
        List<StructureRecord> result = new ArrayList<>();
        for (String rawId : records.getKeys(false)) {
            ConfigurationSection section = records.getConfigurationSection(rawId);
            if (section == null) continue;
            try {
                result.add(read(UUID.fromString(rawId), worldId, section));
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Skipping corrupt exploration record " + rawId + ": " + exception.getMessage());
            }
        }
        return List.copyOf(result);
    }

    @Override
    public void saveWorld(UUID worldId, List<StructureRecord> records) throws IOException {
        if (!root.exists() && !root.mkdirs()) throw new IOException("Cannot create " + root);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("schema-version", SCHEMA_VERSION);
        yaml.set("world-uuid", worldId.toString());
        for (StructureRecord record : records) {
            if (!worldId.equals(record.worldId())) continue;
            String base = "records." + record.structureId();
            yaml.set(base + ".structure-type", record.structureType());
            yaml.set(base + ".minecraft-key", record.minecraftKey());
            yaml.set(base + ".anchor.x", record.anchor().x());
            yaml.set(base + ".anchor.y", record.anchor().y());
            yaml.set(base + ".anchor.z", record.anchor().z());
            yaml.set(base + ".bounds.min-x", record.bounds().minX());
            yaml.set(base + ".bounds.min-y", record.bounds().minY());
            yaml.set(base + ".bounds.min-z", record.bounds().minZ());
            yaml.set(base + ".bounds.max-x", record.bounds().maxX());
            yaml.set(base + ".bounds.max-y", record.bounds().maxY());
            yaml.set(base + ".bounds.max-z", record.bounds().maxZ());
            yaml.set(base + ".rpg-selected", record.rpgSelected());
            yaml.set(base + ".variant-id", record.variantId());
            yaml.set(base + ".state", record.state().name());
            yaml.set(base + ".activation-metadata", record.activationMetadata());
            yaml.set(base + ".reward-claimed", record.rewardClaimed());
            yaml.set(base + ".created-at", record.createdAt().toEpochMilli());
            yaml.set(base + ".completed-at", record.completedAt() == null ? null : record.completedAt().toEpochMilli());
            yaml.set(base + ".data-version", record.dataVersion());
        }
        File target = file(worldId);
        Path temp = Files.createTempFile(root.toPath(), worldId + "-", ".tmp");
        try {
            yaml.save(temp.toFile());
            try {
                Files.move(temp, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private StructureRecord read(UUID id, UUID worldId, ConfigurationSection section) {
        StructureAnchor anchor = new StructureAnchor(worldId,
                section.getDouble("anchor.x"), section.getDouble("anchor.y"), section.getDouble("anchor.z"));
        StructureBounds bounds = new StructureBounds(
                section.getInt("bounds.min-x"), section.getInt("bounds.min-y"), section.getInt("bounds.min-z"),
                section.getInt("bounds.max-x"), section.getInt("bounds.max-y"), section.getInt("bounds.max-z"));
        Map<String, String> metadata = new LinkedHashMap<>();
        ConfigurationSection meta = section.getConfigurationSection("activation-metadata");
        if (meta != null) meta.getKeys(false).forEach(key -> metadata.put(key, meta.getString(key, "")));
        long completedMillis = section.getLong("completed-at", -1L);
        return new StructureRecord(
                id, worldId,
                section.getString("structure-type", "unknown"),
                section.getString("minecraft-key", "minecraft:unknown"),
                anchor, bounds,
                section.getBoolean("rpg-selected", false),
                section.getString("variant-id", ""),
                StructureEventState.valueOf(section.getString("state", "VANILLA")),
                metadata,
                section.getBoolean("reward-claimed", false),
                Instant.ofEpochMilli(section.getLong("created-at", 0L)),
                completedMillis < 0L ? null : Instant.ofEpochMilli(completedMillis),
                section.getInt("data-version", StructureRecord.CURRENT_DATA_VERSION));
    }

    private File file(UUID worldId) { return new File(root, worldId + ".yml"); }
}
