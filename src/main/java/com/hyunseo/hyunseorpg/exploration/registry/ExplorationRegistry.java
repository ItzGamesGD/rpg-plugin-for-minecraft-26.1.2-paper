package com.hyunseo.hyunseorpg.exploration.registry;

import com.hyunseo.hyunseorpg.exploration.raid.RaidMobDefinition;
import com.hyunseo.hyunseorpg.exploration.raid.RaidWavePoolDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/** Exploration-local YAML registry. It deliberately does not require ConfigService. */
public final class ExplorationRegistry {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, ExplorationStructureDefinition> byId = new LinkedHashMap<>();
    private final Map<String, ExplorationStructureDefinition> byMinecraftKey = new LinkedHashMap<>();
    private final Map<String, RaidWavePoolDefinition> raidPools = new LinkedHashMap<>();
    private boolean enabled;
    private long heartbeatTicks = 10L;

    public ExplorationRegistry(JavaPlugin plugin) {
        this(plugin, new File(plugin.getDataFolder(), "exploration/structures.yml"));
    }

    public ExplorationRegistry(JavaPlugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    public boolean load() {
        byId.clear();
        byMinecraftKey.clear();
        raidPools.clear();
        ensureFile();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        enabled = yaml.getBoolean("enabled", false);
        heartbeatTicks = Math.max(5L, yaml.getLong("runtime.heartbeat-ticks", 10L));
        boolean valid = parseRaidPools(yaml.getConfigurationSection("raid-pools"));
        ConfigurationSection structures = yaml.getConfigurationSection("structures");
        if (structures == null) {
            plugin.getLogger().warning("Exploration registry has no structures section; module remains inert.");
            return !enabled;
        }
        for (String rawId : structures.getKeys(false)) {
            ConfigurationSection section = structures.getConfigurationSection(rawId);
            if (section == null) continue;
            try {
                ExplorationStructureDefinition definition = parse(rawId, section);
                if (byId.putIfAbsent(definition.id(), definition) != null) {
                    throw new IllegalArgumentException("duplicate structure id");
                }
                if (byMinecraftKey.putIfAbsent(definition.minecraftKey(), definition) != null) {
                    throw new IllegalArgumentException("duplicate minecraft-key: " + definition.minecraftKey());
                }
            } catch (RuntimeException exception) {
                valid = false;
                plugin.getLogger().log(Level.WARNING, "Skipping invalid exploration structure: " + rawId, exception);
            }
        }
        if (!enabled && byId.values().stream().anyMatch(ExplorationStructureDefinition::enabled)) {
            plugin.getLogger().warning(
                    "Exploration root enabled=false while one or more structures are enabled; "
                            + "the module will remain inactive until the root flag is enabled.");
        }
        if (enabled && byId.isEmpty()) {
            plugin.getLogger().warning("Exploration enabled but no valid structures loaded; disabling safely.");
            enabled = false;
            return false;
        }
        plugin.getLogger().info("Exploration registry loaded: " + byId.size() + " definition(s), enabled=" + enabled);
        return valid || !enabled;
    }

    public boolean isEnabled() { return enabled; }
    public long heartbeatTicks() { return heartbeatTicks; }
    public Optional<ExplorationStructureDefinition> get(String id) { return Optional.ofNullable(byId.get(normalize(id))); }
    public Optional<ExplorationStructureDefinition> byMinecraftKey(String key) { return Optional.ofNullable(byMinecraftKey.get(normalize(key))); }
    public List<ExplorationStructureDefinition> all() { return List.copyOf(byId.values()); }
    public Set<String> minecraftKeys() { return Set.copyOf(byMinecraftKey.keySet()); }
    public Optional<RaidWavePoolDefinition> raidPool(String id) { return Optional.ofNullable(raidPools.get(normalize(id))); }
    public List<RaidWavePoolDefinition> raidPools() { return List.copyOf(raidPools.values()); }

    private boolean parseRaidPools(ConfigurationSection pools) {
        if (pools == null) return true;
        boolean valid = true;
        for (String rawId : pools.getKeys(false)) {
            ConfigurationSection pool = pools.getConfigurationSection(rawId);
            if (pool == null) continue;
            try {
                List<RaidMobDefinition> entries = new ArrayList<>();
                ConfigurationSection mobs = pool.getConfigurationSection("mobs");
                if (mobs != null) {
                    for (String rawMobId : mobs.getKeys(false)) {
                        ConfigurationSection mob = mobs.getConfigurationSection(rawMobId);
                        if (mob == null) continue;
                        entries.add(new RaidMobDefinition(rawMobId,
                                mob.getInt("max", 1), mob.getInt("weight", 1),
                                mob.getBoolean("heavy", false), Math.max(1, mob.getInt("level", 1))));
                    }
                }
                Map<String, Integer> guaranteed = new LinkedHashMap<>();
                ConfigurationSection guaranteedSection = pool.getConfigurationSection("guaranteed");
                if (guaranteedSection != null) {
                    for (String mobId : guaranteedSection.getKeys(false)) {
                        guaranteed.put(mobId, guaranteedSection.getInt(mobId, 0));
                    }
                }
                RaidWavePoolDefinition definition = new RaidWavePoolDefinition(rawId,
                        pool.getInt("total-max-spawns", 1), pool.getInt("heavy-max-spawns", 0), entries, guaranteed);
                if (raidPools.putIfAbsent(definition.id(), definition) != null) {
                    throw new IllegalArgumentException("duplicate raid pool id");
                }
            } catch (RuntimeException exception) {
                valid = false;
                plugin.getLogger().log(Level.WARNING, "Skipping invalid exploration raid pool: " + rawId, exception);
            }
        }
        return valid;
    }

    private ExplorationStructureDefinition parse(String id, ConfigurationSection section) {
        String minecraftKey = section.getString("minecraft-key", "");
        boolean structureEnabled = section.getBoolean("enabled", false);
        double chance = section.getDouble("selection-chance", 0.0D);
        double trigger = section.getDouble("trigger-radius", 32.0D);
        double lootTrigger = section.getDouble("loot-trigger-radius", trigger);
        long lootGrace = section.getLong("loot-trigger-grace-ticks",
                section.getLong("abandon-grace-ticks", 100L));
        double combatAbandon = section.getDouble("combat-abandon-radius",
                section.getDouble("abandon-radius", 64.0D));
        long combatGrace = section.getLong("combat-abandon-grace-ticks",
                section.getLong("abandon-grace-ticks", 100L));
        List<StructureVariantDefinition> variants = new ArrayList<>();
        ConfigurationSection variantSection = section.getConfigurationSection("variants");
        if (variantSection != null) {
            for (String rawVariant : variantSection.getKeys(false)) {
                ConfigurationSection variant = variantSection.getConfigurationSection(rawVariant);
                if (variant == null) continue;
                variants.add(new StructureVariantDefinition(
                        rawVariant,
                        Math.max(0.0D, variant.getDouble("weight", 1.0D)),
                        variant.getBoolean("enabled", true),
                        parseComponents(variant.getMapList("components"))));
            }
        }
        double entryPadding = Math.max(0.0D, section.getDouble("entry-boundary-padding", 4.0D));
        return new ExplorationStructureDefinition(id, minecraftKey, structureEnabled, chance, trigger,
                lootTrigger, lootGrace, combatAbandon, combatGrace, variants, entryPadding);
    }

    private List<ExplorationComponentSpec> parseComponents(List<Map<?, ?>> rawList) {
        List<ExplorationComponentSpec> result = new ArrayList<>();
        for (Map<?, ?> raw : rawList) {
            Object type = raw.get("type");
            if (type == null) continue;
            Map<String, Object> options = new LinkedHashMap<>();
            raw.forEach((key, value) -> {
                if (key != null && !"type".equalsIgnoreCase(String.valueOf(key))) options.put(String.valueOf(key), value);
            });
            result.add(new ExplorationComponentSpec(String.valueOf(type), options));
        }
        return result;
    }

    private void ensureFile() {
        if (file.isFile()) return;
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Unable to create exploration config directory: " + parent);
            return;
        }
        try {
            plugin.saveResource("exploration/structures.yml", false);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("exploration/structures.yml resource is not installed yet; using empty registry.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
