package com.hyunseo.hyunseorpg.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Explicit, repeatable migrations for live plugin configuration and player files. */
public final class ConfigMigrationService {
    private static final int CURRENT_ENCHANT_CONFIG_VERSION = 5;
    private static final Set<String> LEGACY_PROFESSION_IDS = Set.of(
            "miner_blessing", "miner_grace", "warrior_blessing", "abyss_ore", "forest_essence",
            "abundance_crystal", "predator_fang", "precision_gear", "building_essence", "deepwater_scale",
            "miner_blessing_bundle", "miner_pickaxe", "miner_pickaxe_advanced");
    private static final Map<String, String> REMOVED_ENCHANT_BOOKS = Map.of(
            "spear_charge", "enchant_book_spear_charge",
            "spear_throw", "enchant_book_spear_throw",
            "lancer", "enchant_book_lancer",
            "paladins_blessing", "enchant_book_paladins_blessing",
            "holy_counter", "enchant_book_holy_counter",
            "homing_arrow", "enchant_book_homing_arrow",
            "hunters_mark", "enchant_book_hunters_mark");
    private static final Map<String, BookItemDefinition> REQUIRED_ENCHANT_BOOK_FALLBACKS = Map.of(
            "enchant_book_mining_bonus_drop", new BookItemDefinition("광맥 추가 채굴 인챈트 북", "EPIC", 22121,
                    List.of("광석 채굴 시 추가 보상을 얻을 수 있는 도구 인챈트입니다.")),
            "enchant_book_area_mining_pickaxe", new BookItemDefinition("광역 채굴 인챈트 북", "EPIC", 22122,
                    List.of("연결된 광물을 함께 채굴하는 도구 인챈트입니다.")),
            "enchant_book_chain_logging", new BookItemDefinition("연쇄 벌목 인챈트 북", "RARE", 22149,
                    List.of("도끼에 연쇄 벌목을 부여합니다."))
    );
    private static final List<String> MANAGED_FILES = List.of(
            "config.yml", "exp.yml", "weapons.yml",
            "items.yml", "equipment-growth.yml",
            "equipment-inputs.yml", "enchants.yml",
            "mobs.yml", "monster-spawns.yml", "mythic-mobs.yml", "hunting-grounds.yml",
            "worlds.yml", "special-equipment.yml"
    );
    private static final List<String> ALCHEMY_FILES = List.of(
            "alchemy/effects.yml", "alchemy/components.yml", "alchemy/conflicts.yml", "alchemy/scaling.yml",
            "alchemy/potions.yml", "alchemy/recipes.yml",
            "alchemy/catalysts.yml");
    private static final List<String> SPECIAL_TIER_ITEMS = List.of(
            "burning_sword", "flowing_water_sword", "wind_cutting_sword", "earth_special_sword",
            "ice_special_sword", "dark_energy_sword", "burning_bow", "wind_archers_bow",
            "earth_heavy_bow", "freezing_bow", "dark_energy_bow", "poseidon_spear"
    );
    private static final Set<String> RETIRED_SPECIAL_ITEM_IDS = Set.of(
            "fire_sword", "fire_bow", "water_sword", "wind_sword", "wind_bow",
            "earth_sword", "earth_bow", "earth_mace", "ice_sword", "ice_bow",
            "magic_sword", "magic_bow");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final List<String> DEPRECATED_PLAYER_PATHS = List.of(
            "selectedProfession", "profession", "professionLevel", "professionExp", "professionStats",
            "professionSkills", "professionCooldown", "job", "jobLevel", "jobExp",
            "currentHuntingGround", "huntingGroundSession", "huntingGroundKills",
            "currentDungeon", "dungeonClears", "dungeonProgress", "dungeonCooldown",
            "unlockedWorlds", "clearedWorlds", "worldUnlocks");

    private final JavaPlugin plugin;

    public ConfigMigrationService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public MigrationReport migrate(String target, boolean dryRun) {
        String normalized = normalize(target);
        pending.clear();
        List<String> lines = new ArrayList<>();
        List<File> changedFiles = new ArrayList<>();
        lines.add("[HyunseoRPG Migration]");
        lines.add("target: " + normalized);
        lines.add("mode: " + (dryRun ? "DRY-RUN" : "APPLY"));

        try {
            if (normalized.equals("configs") || normalized.equals("all")) {
                migrateLatestConfigs(lines, changedFiles);
            }
            if (normalized.equals("items")) {
                migrateEnchants(lines, changedFiles);
                migrateItems(lines, changedFiles);
                migrateLegacyItemReferences(lines, changedFiles);
            }
            if (normalized.equals("mobs")) {
                migrateMobDefinitions(lines, changedFiles);
            }
            if (normalized.equals("alchemy") || normalized.equals("all")) {
                migrateAlchemy(lines, changedFiles);
            }
            if (normalized.equals("players") || normalized.equals("all")) {
                migratePlayers(lines, changedFiles);
            }
            boolean archiveLegacy = normalized.equals("legacy") || normalized.equals("cleanup") || normalized.equals("all");
            if (archiveLegacy && !normalized.equals("all")) {
                migrateItems(lines, changedFiles);
            }
            if (!List.of("configs", "items", "mobs", "players", "alchemy", "legacy", "cleanup", "all").contains(normalized)) {
                lines.add("ERROR unknown migration target: " + normalized);
                return new MigrationReport(false, lines, null);
            }
            if (changedFiles.isEmpty() && !archiveLegacy) {
                lines.add("No changes required.");
                return new MigrationReport(true, lines, null);
            }
            if (dryRun) {
                if (archiveLegacy) describeLegacyArchive(lines);
                lines.add("No files were modified.");
                return new MigrationReport(true, lines, null);
            }
            File backupDirectory = null;
            if (!changedFiles.isEmpty()) {
                backupDirectory = backup(changedFiles);
                lines.add("backup: " + backupDirectory.getAbsolutePath());
                for (File file : changedFiles) savePending(file, lines);
                lines.add("Applied changes: " + changedFiles.size() + " file(s).");
            }
            if ((normalized.equals("configs") || normalized.equals("items") || normalized.equals("all"))
                    && !verifyRequiredEnchantBooks(lines)) {
                return new MigrationReport(false, lines, backupDirectory);
            }
            File legacyArchive = archiveLegacy ? archiveLegacyFiles(lines) : null;
            return new MigrationReport(true, lines, backupDirectory != null ? backupDirectory : legacyArchive);
        } catch (RuntimeException | IOException exception) {
            lines.add("ERROR migration failed: " + exception.getMessage());
            return new MigrationReport(false, lines, null);
        }
    }

    private void migrateLatestConfigs(List<String> lines, List<File> changedFiles) {
        for (String fileName : MANAGED_FILES) {
            FileConfiguration configuration = loadLive(fileName);
            if (configuration == null) continue;
            if (!configuration.isSet("schema-version")) {
                configuration.set("schema-version", 1);
                mark(configuration, fileName, changedFiles, lines, "added schema-version: 1");
            }
        }
        migrateItems(lines, changedFiles);
        migrateEnchants(lines, changedFiles);
        migrateSpecialEquipment(lines, changedFiles);
        migrateEquipmentGrowth(lines, changedFiles);
        migrateVanillaStacking(lines, changedFiles);
        migrateMobAndPersistence(lines, changedFiles);
        migrateLegacyItemReferences(lines, changedFiles);
    }

    private void migrateAlchemy(List<String> lines, List<File> changedFiles) {
        for (String fileName : ALCHEMY_FILES) {
            FileConfiguration defaults = loadResource(fileName);
            if (defaults == null) {
                lines.add("ERROR " + fileName + ": bundled alchemy default is missing");
                continue;
            }
            FileConfiguration target = loadLive(fileName);
            if (target == null) {
                target = new YamlConfiguration();
                copyMissingRoot(target, defaults);
                mark(target, fileName, changedFiles, lines, "created missing alchemy config from bundled defaults");
                continue;
            }
            boolean changed = false;
            if (!target.isSet("schema-version") && defaults.isSet("schema-version")) {
                target.set("schema-version", defaults.get("schema-version"));
                changed = true;
                lines.add(fileName + ": added schema-version");
            }
            if (copyMissingTree(target, defaults, "effects")
                    || copyMissingTree(target, defaults, "components")
                    || copyMissingTree(target, defaults, "conflicts")
                    || copyMissingTree(target, defaults, "limits")
                    || copyMissingTree(target, defaults, "multiplier")) {
                changed = true;
            }
            if (fileName.equals("alchemy/potions.yml") && copyMissingTree(target, defaults, "potions")) changed = true;
            if (fileName.equals("alchemy/recipes.yml") && copyMissingTree(target, defaults, "recipes")) changed = true;
            if (fileName.equals("alchemy/catalysts.yml") && copyMissingTree(target, defaults, "catalysts")) changed = true;
            if (fileName.equals("alchemy/effects.yml")
                    && target.getInt("effects.effect_shock.baseline.tick-interval", 0) == 80) {
                target.set("effects.effect_shock.baseline.tick-interval", 320);
                changed = true;
                lines.add("alchemy/effects.yml: extended shock damage interval 80 -> 320 ticks");
            }
            if (changed) mark(target, fileName, changedFiles, lines, "alchemy migration staged");
        }
        activateProductionAlchemy(lines, changedFiles);
    }

    private void activateProductionAlchemy(List<String> lines, List<File> changedFiles) {
        List<String> effects = List.of("effect_vampire", "effect_berserk", "effect_corrosion",
                "effect_frostbite", "effect_shock", "effect_bleed", "effect_vulnerability", "effect_necrosis");
        List<String> potions = List.of("potion_vampire", "potion_berserk", "potion_corrosion",
                "potion_frostbite", "potion_shock", "potion_bleed", "potion_vulnerability", "potion_necrosis");
        migrateAlchemyItems(potions, lines, changedFiles);
        activateEntries("alchemy/effects.yml", "effects", effects, "enabled", true, lines, changedFiles);
        activateEntries("alchemy/effects.yml", "effects", effects, "status", "IMPLEMENTED_RUNTIME", lines, changedFiles);
        activateEntries("alchemy/potions.yml", "potions", potions, "enabled", true, lines, changedFiles);
        List<String> catalysts = List.of("redstone", "glowstone_dust", "gunpowder", "dragon_breath",
                "fermented_spider_eye", "sculk", "echo_shard", "slime", "wind_charge");
        activateEntries("alchemy/catalysts.yml", "catalysts", catalysts, "enabled", true, lines, changedFiles);
    }

    private void migrateAlchemyItems(List<String> potions, List<String> lines, List<File> changedFiles) {
        FileConfiguration target = loadLive("items.yml");
        FileConfiguration defaults = loadResource("items.yml");
        if (target == null || defaults == null) return;
        List<String> ids = new ArrayList<>(potions);
        ids.add("corrosion_essence");
        boolean changed = false;
        for (String id : ids) {
            String path = "items." + id;
            if (target.isSet(path) || !defaults.isSet(path)) continue;
            copyTree(target, defaults, path);
            changed = true;
            lines.add("items.yml: added missing alchemy production item " + id);
        }
        if (changed) mark(target, "items.yml", changedFiles, lines, "alchemy production item references staged");
    }

    private void activateEntries(String fileName, String root, List<String> ids, String field, Object value,
                                 List<String> lines, List<File> changedFiles) {
        FileConfiguration target = loadLive(fileName);
        if (target == null) return;
        boolean changed = false;
        if (ids.isEmpty()) {
            if (!target.isSet(field) || !target.getBoolean(field, false)) {
                target.set(field, value);
                changed = true;
            }
        } else {
            for (String id : ids) {
                String path = root + "." + id + "." + field;
                if (!target.isSet(path) || !String.valueOf(target.get(path)).equals(String.valueOf(value))) {
                    target.set(path, value);
                    changed = true;
                    lines.add(fileName + ": activated implemented production entry " + id);
                }
            }
        }
        if (changed) mark(target, fileName, changedFiles, lines, "alchemy production activation staged");
    }

    /** Restores missing bundled positions without moving or rewriting operator placements. */
    private void migrateMobAndPersistence(List<String> lines, List<File> changedFiles) {
        migrateMobDefinitions(lines, changedFiles);

        FileConfiguration spawns = loadLive("monster-spawns.yml");
        FileConfiguration spawnDefaults = loadResource("monster-spawns.yml");
        if (spawns != null && spawnDefaults != null) {
            boolean changed = false;
            for (String path : List.of("settings.nearby-player-radius", "settings.level-calculation",
                    "spawns.explosive_breeze", "spawns.riptide_drowned")) {
                if (!spawns.isSet(path) && spawnDefaults.isSet(path)) {
                    copyTree(spawns, spawnDefaults, path);
                    changed = true;
                }
            }
            if (changed) {
                lines.add("monster-spawns.yml: added nearby-level spawn settings");
                mark(spawns, "monster-spawns.yml", changedFiles, lines, "nearby spawn migration staged");
            }
        }

        FileConfiguration config = loadLive("config.yml");
        FileConfiguration configDefaults = loadResource("config.yml");
        if (config != null && configDefaults != null && !config.isConfigurationSection("persistence")
                && configDefaults.isConfigurationSection("persistence")) {
            copyTree(config, configDefaults, "persistence");
            lines.add("config.yml: added dirty player persistence settings");
            mark(config, "config.yml", changedFiles, lines, "persistence migration staged");
        }
    }

    /** Adds only missing official mob definitions and preserves all operator values. */
    private void migrateMobDefinitions(List<String> lines, List<File> changedFiles) {
        FileConfiguration mobs = loadLive("mobs.yml");
        FileConfiguration defaults = loadResource("mobs.yml");
        if (mobs == null || defaults == null) return;

        boolean changed = false;
        if (!mobs.isConfigurationSection("elemental-fragments")
                && defaults.isConfigurationSection("elemental-fragments")) {
            copyTree(mobs, defaults, "elemental-fragments");
            lines.add("mobs.yml: added explicit elemental fragment registry");
            changed = true;
        }
        if (changed) mark(mobs, "mobs.yml", changedFiles, lines, "custom mob migration staged");
    }

    private void migrateVanillaStacking(List<String> lines, List<File> changedFiles) {
        String fileName = "config.yml";
        FileConfiguration target = loadLive(fileName);
        FileConfiguration defaults = loadResource(fileName);
        if (target == null || defaults == null || !defaults.isConfigurationSection("vanilla-stacking")) return;
        if (!target.isConfigurationSection("vanilla-stacking")) {
            copyTree(target, defaults, "vanilla-stacking");
            mark(target, fileName, changedFiles, lines, "added vanilla-stacking settings");
        }
    }

    private void describeLegacyArchive(List<String> lines) {
        for (String fileName : List.of("enhancements.yml", "dungeons.yml", "professions.yml",
                "hunting-grounds.yml", "worlds.yml", "world-unlocks.yml")) {
            File file = new File(plugin.getDataFolder(), fileName);
            if (file.isFile()) lines.add("would archive legacy file: " + fileName);
        }
        File legacyActivityDb = new File(plugin.getDataFolder(), "placed-job-blocks.db");
        File activeActivityDb = new File(plugin.getDataFolder(), "activity-blocks.db");
        if (legacyActivityDb.isFile() && activeActivityDb.isFile()) {
            lines.add("would archive legacy file: placed-job-blocks.db");
        }
    }

    private File archiveLegacyFiles(List<String> lines) throws IOException {
        File dataFolder = plugin.getDataFolder();
        File canonical = new File(dataFolder, "equipment-growth.yml");
        FileConfiguration growth = canonical.isFile() ? YamlConfiguration.loadConfiguration(canonical) : null;
        if (growth == null || !growth.isConfigurationSection("enhancement")) {
            throw new IOException("Cannot archive legacy growth files before equipment-growth.yml is complete");
        }

        List<File> legacyFiles = new ArrayList<>();
        for (String fileName : List.of("enhancements.yml", "dungeons.yml", "professions.yml",
                "hunting-grounds.yml", "worlds.yml", "world-unlocks.yml")) {
            File file = new File(dataFolder, fileName);
            if (file.isFile()) legacyFiles.add(file);
        }
        File legacyActivityDb = new File(dataFolder, "placed-job-blocks.db");
        File activeActivityDb = new File(dataFolder, "activity-blocks.db");
        if (legacyActivityDb.isFile() && activeActivityDb.isFile()) {
            legacyFiles.add(legacyActivityDb);
            lines.add("placed-job-blocks.db: active activity ledger exists; safe to archive");
        }
        if (legacyFiles.isEmpty()) {
            lines.add("No legacy live-root files found.");
            return null;
        }

        File archive = new File(dataFolder, "archive/legacy/" + LocalDateTime.now().format(STAMP));
        if (!archive.mkdirs() && !archive.isDirectory()) throw new IOException("Unable to create legacy archive " + archive);
        for (File file : legacyFiles) {
            Files.move(file.toPath(), new File(archive, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            lines.add("archived legacy file: " + file.getName());
        }
        lines.add("legacy archive: " + archive.getAbsolutePath());
        return archive;
    }

    private void migrateItems(List<String> lines, List<File> changedFiles) {
        String fileName = "items.yml";
        FileConfiguration target = loadLive(fileName);
        FileConfiguration defaults = loadResource(fileName);
        if (target == null || defaults == null) return;
        boolean changed = false;
        ConfigurationSection defaultItems = defaults.getConfigurationSection("items");
        if (defaultItems != null) {
            for (String id : defaultItems.getKeys(false)) {
                String normalizedId = normalize(id);
                if (LEGACY_PROFESSION_IDS.contains(normalizedId)) continue;
                String path = "items." + id;
                // Preserve any operator-owned definition, including malformed ones; Doctor reports it.
                if (target.isSet(path)) continue;
                copyTree(target, defaults, path);
                lines.add("items.yml: added missing item " + id);
                changed = true;
            }
        }
        changed |= migrateRequiredEnchantBooks(target, defaults, loadLive("enchants.yml"),
                loadResource("enchants.yml"), lines);
        String earthHeartMaterial = target.getString("items.earth_heart.material", "");
        if (earthHeartMaterial.equalsIgnoreCase("NETHER_QUARTZ")) {
            target.set("items.earth_heart.material", "QUARTZ");
            lines.add("items.yml: corrected items.earth_heart.material from NETHER_QUARTZ to QUARTZ");
            changed = true;
        }
        for (String bookId : REMOVED_ENCHANT_BOOKS.values()) {
            String path = "items." + bookId;
            if (!target.isSet(path)) continue;
            target.set(path, null);
            lines.add("items.yml: removed retired enchant book " + bookId);
            changed = true;
        }
        Set<String> legacyItems = collectLegacyProfessionItemIds(target);
        for (String id : legacyItems) {
            String path = "items." + id;
            if (!target.isSet(path)) continue;
            target.set(path, null);
            lines.add("items.yml: removed legacy profession item " + id);
            changed = true;
        }
        if (!target.isSet("schema-version")) {
            target.set("schema-version", 1);
            changed = true;
        }
        if (changed) mark(target, fileName, changedFiles, lines, "latest item migration staged");
    }

    private void migrateLegacyItemReferences(List<String> lines, List<File> changedFiles) {
        Map<String, String> aliases = Map.of();
        for (String fileName : List.of("mythic-mobs.yml", "mobs.yml",
                "equipment-growth.yml", "enchants.yml")) {
            FileConfiguration target = loadLive(fileName);
            if (target == null) continue;
            int replacements = replaceLegacyValues(target, aliases);
            if (replacements > 0) {
                lines.add(fileName + ": replaced legacy item references " + replacements + " time(s)");
                mark(target, fileName, changedFiles, lines, "legacy item IDs normalized");
            }
        }
    }

    private int replaceLegacyValues(ConfigurationSection section, Map<String, String> aliases) {
        int count = 0;
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            String replacement = value instanceof String string
                    ? aliases.get(normalize(string)) : null;
            if (replacement != null) {
                section.set(key, replacement);
                count++;
            } else if (value instanceof ConfigurationSection child) {
                count += replaceLegacyValues(child, aliases);
            } else if (value instanceof List<?> list) {
                List<Object> normalized = new ArrayList<>(list);
                boolean changed = false;
                for (int index = 0; index < normalized.size(); index++) {
                    Object entry = normalized.get(index);
                    if (!(entry instanceof String string)) continue;
                    String mapped = aliases.get(normalize(string));
                    if (mapped == null) continue;
                    normalized.set(index, mapped);
                    changed = true;
                    count++;
                }
                if (changed) section.set(key, normalized);
            }
        }
        return count;
    }

    /**
     * The enchant registry is the source of truth for book references. This
     * second pass prevents a partially migrated items.yml from leaving valid
     * enchantments unusable merely because their book was added after the
     * original item catalogue pass.
     */
    private static boolean migrateRequiredEnchantBooks(FileConfiguration target, FileConfiguration itemDefaults,
                                                FileConfiguration liveEnchants, FileConfiguration enchantDefaults,
                                                List<String> lines) {
        if (itemDefaults == null || enchantDefaults == null) return false;
        ConfigurationSection liveDefinitions = liveEnchants == null
                ? null : liveEnchants.getConfigurationSection("enchants");
        ConfigurationSection defaultDefinitions = enchantDefaults.getConfigurationSection("enchants");
        if (liveDefinitions == null && defaultDefinitions == null) return false;
        Set<String> enchantIds = new HashSet<>();
        if (defaultDefinitions != null) enchantIds.addAll(defaultDefinitions.getKeys(false));
        if (liveDefinitions != null) enchantIds.addAll(liveDefinitions.getKeys(false));
        boolean changed = false;
        for (String enchantId : enchantIds) {
            String bookId = liveEnchants != null
                    ? liveEnchants.getString("enchants." + enchantId + ".book-item-id", "")
                    : "";
            if (bookId == null || bookId.isBlank()) {
                bookId = enchantDefaults.getString("enchants." + enchantId + ".book-item-id", "");
            }
            if (bookId == null || bookId.isBlank()) continue;
            String path = "items." + bookId;
            // Never overwrite an external definition. A malformed existing section is a Doctor error,
            // while a truly absent definition is safe to recover from embedded defaults.
            if (target.isSet(path)) continue;
            if (!itemDefaults.isConfigurationSection(path)) {
                BookItemDefinition fallback = REQUIRED_ENCHANT_BOOK_FALLBACKS.get(bookId);
                if (fallback == null) {
                    lines.add("ERROR items.yml: enchant book " + bookId
                            + " is referenced but missing from embedded items.yml");
                    continue;
                }
                target.set(path + ".material", "ENCHANTED_BOOK");
                target.set(path + ".display-name", fallback.displayName());
                target.set(path + ".category", "ENCHANTMENT");
                target.set(path + ".rarity", fallback.rarity());
                target.set(path + ".custom-model-data", fallback.customModelData());
                target.set(path + ".lore", fallback.lore());
                lines.add("items.yml: added required enchant book fallback " + bookId);
                changed = true;
                continue;
            }
            copyTree(target, itemDefaults, path);
            lines.add("items.yml: added required enchant book " + bookId);
            changed = true;
        }
        return changed;
    }

    private boolean verifyRequiredEnchantBooks(List<String> lines) {
        FileConfiguration items = loadLive("items.yml");
        FileConfiguration enchants = loadLive("enchants.yml");
        if (items == null || enchants == null) return true;
        ConfigurationSection definitions = enchants.getConfigurationSection("enchants");
        ConfigurationSection itemDefinitions = items.getConfigurationSection("items");
        if (definitions == null || itemDefinitions == null) {
            lines.add("ERROR post-migration enchant book verification: missing items or enchants section");
            return false;
        }
        List<String> missing = new ArrayList<>();
        for (String id : definitions.getKeys(false)) {
            String book = normalize(enchants.getString("enchants." + id + ".book-item-id", ""));
            if (!book.isBlank() && !itemDefinitions.isConfigurationSection(book)) missing.add(book);
        }
        if (!missing.isEmpty()) {
            lines.add("ERROR post-migration enchant book verification: missing " + String.join(", ", missing));
            return false;
        }
        lines.add("Verified enchant books after migration: " +
                definitions.getKeys(false).stream()
                        .map(id -> enchants.getString("enchants." + id + ".book-item-id", ""))
                        .filter(id -> id != null && !id.isBlank()).distinct().count());
        return true;
    }

    private void migrateEnchants(List<String> lines, List<File> changedFiles) {
        String fileName = "enchants.yml";
        FileConfiguration target = loadLive(fileName);
        FileConfiguration defaults = loadResource(fileName);
        if (target == null || defaults == null) return;
        boolean changed = migrateCanonicalEnchantStructure(target, defaults, lines)
                | removeRetiredEnchantDefinitions(target, lines)
                | migrateLegacyEnchantDefaults(target, lines)
                | migrateEnchantLore(target, defaults, lines)
                | copyMissingTree(target, defaults, "enchants")
                | copyMissingTree(target, defaults, "enchant-lore");
        if (target.isSet("enchant-slots")) {
            target.set("enchant-slots", null);
            changed = true;
        }
        if (changed) {
            lines.add("enchants.yml: reconciled native enchant definitions");
            mark(target, fileName, changedFiles, lines, "enchant content migration staged");
        }
    }

    /**
     * Converts known structural aliases before the registry is loaded. Values
     * outside the broken identity/input fields are retained wherever possible.
     */
    private boolean migrateCanonicalEnchantStructure(FileConfiguration target, FileConfiguration defaults,
                                                     List<String> lines) {
        boolean changed = false;
        changed |= migrateDefinitionAlias(target, defaults, "blade_throw", "blade_chain",
                List.of("handler-id", "book-item-id", "executor-id", "weapon-type", "source-scope",
                        "triggers", "input", "legacy-aliases"), lines);
        changed |= migrateDefinitionAlias(target, defaults, "area_mining_pickaxe", "area_excavation",
                List.of("handler-id", "book-item-id", "executor-id", "equipment-category", "material-patterns",
                        "source-scope", "triggers", "input", "legacy-aliases"), lines);
        if (target.isConfigurationSection("enchants.durability_save_pickaxe")) {
            target.set("enchants.durability_save_pickaxe", null);
            lines.add("enchants.yml: retired vanilla duplicate durability_save_pickaxe");
            changed = true;
        }
        for (String retired : List.of("protection", "fire_protection", "blast_protection",
                "projectile_protection", "thorns", "respiration", "aqua_affinity", "swift_sneak",
                "depth_strider", "soul_speed", "frost_walker", "unbreaking")) {
            if (target.isConfigurationSection("enchants." + retired)) {
                target.set("enchants." + retired, null);
                target.set("enchant-lore." + retired, null);
                lines.add("enchants.yml: retired vanilla duplicate " + retired);
                changed = true;
            }
        }

        if (target.isConfigurationSection("enchants.mining_bonus_drop")
                && !"content".equalsIgnoreCase(target.getString("enchants.mining_bonus_drop.handler-id", ""))) {
            target.set("enchants.mining_bonus_drop.handler-id", "content");
            lines.add("enchants.yml: corrected mining_bonus_drop handler-id -> content");
            changed = true;
        }
        if (target.getInt("config-version", 0) < CURRENT_ENCHANT_CONFIG_VERSION) {
            target.set("config-version", CURRENT_ENCHANT_CONFIG_VERSION);
            lines.add("enchants.yml: config-version -> " + CURRENT_ENCHANT_CONFIG_VERSION);
            changed = true;
        }
        return changed;
    }

    private boolean migrateDefinitionAlias(FileConfiguration target, FileConfiguration defaults,
                                           String legacyId, String canonicalId, List<String> structuralKeys,
                                           List<String> lines) {
        String legacyPath = "enchants." + legacyId;
        String canonicalPath = "enchants." + canonicalId;
        boolean changed = false;
        if (!target.isConfigurationSection(canonicalPath)
                && target.isConfigurationSection(legacyPath)
                && defaults.isConfigurationSection(canonicalPath)) {
            copyTree(target, defaults, canonicalPath);
            for (String key : structuralKeys) {
                if (target.isSet(legacyPath + "." + key)) {
                    target.set(canonicalPath + "." + key, target.get(legacyPath + "." + key));
                }
            }
            lines.add("enchants.yml: migrated " + legacyId + " -> " + canonicalId);
            changed = true;
        }
        if (target.isConfigurationSection(canonicalPath)) {
            if (defaults.isConfigurationSection(canonicalPath)) {
                for (String key : structuralKeys) {
                    Object canonicalValue = defaults.get(canonicalPath + "." + key);
                    if (canonicalValue != null && !canonicalValue.equals(target.get(canonicalPath + "." + key))) {
                        target.set(canonicalPath + "." + key, canonicalValue);
                        changed = true;
                    }
                }
            }
            if (!target.getStringList(canonicalPath + ".legacy-aliases").stream()
                    .anyMatch(value -> legacyId.equalsIgnoreCase(value))) {
                List<String> aliases = new ArrayList<>(target.getStringList(canonicalPath + ".legacy-aliases"));
                aliases.add(legacyId);
                target.set(canonicalPath + ".legacy-aliases", aliases);
                changed = true;
            }
            if (target.isConfigurationSection(legacyPath)) {
                target.set(legacyPath, null);
                lines.add("enchants.yml: removed duplicate legacy definition " + legacyId);
                changed = true;
            }
        }
        return changed;
    }

    /** Removes only enchant content explicitly retired from the canonical catalogue. */
    private boolean removeRetiredEnchantDefinitions(FileConfiguration target, List<String> lines) {
        boolean changed = false;
        for (String enchantId : REMOVED_ENCHANT_BOOKS.keySet()) {
            String definitionPath = "enchants." + enchantId;
            if (target.isSet(definitionPath)) {
                target.set(definitionPath, null);
                lines.add("enchants.yml: removed retired enchant " + enchantId);
                changed = true;
            }
            String lorePath = "enchant-lore." + enchantId;
            if (target.isSet(lorePath)) {
                target.set(lorePath, null);
                lines.add("enchants.yml: removed retired enchant lore " + enchantId);
                changed = true;
            }
        }
        return changed;
    }

    /** Changes only the exact former defaults; custom input layouts are retained untouched. */
    private boolean migrateLegacyEnchantDefaults(FileConfiguration target, List<String> lines) {
        boolean changed = false;
        changed |= replaceSingleInputIfLegacy(target, "light_greatsword", "OFFHAND_QUICK", "SHIFT_RIGHT_CLICK", lines);
        changed |= replaceSingleInputIfLegacy(target, "light_greatsword", "DROP_KEY", "SHIFT_RIGHT_CLICK", lines);
        changed |= replaceSingleInputIfLegacy(target, "wind_arrow", "SHIFT_RIGHT_CLICK", "OFFHAND_QUICK", lines);
        changed |= replaceSingleInputIfLegacy(target, "wind_arrow", "SHIFT_LEFT_CLICK", "OFFHAND_QUICK", lines);
        if (hasTriggerType(target, "precision_flight", "GLIDE_TICK")) {
            target.set("enchants.precision_flight.triggers", List.of(
                    Map.of("type", "ELYTRA_BOOST", "phase", "CONFIRMED")));
            lines.add("enchants.yml: migrated precision_flight GLIDE_TICK -> ELYTRA_BOOST");
            changed = true;
        }
        if (target.isSet("enchants.wind_arrow.settings.required-charge-ticks")
                && !target.isSet("enchants.wind_arrow.settings.minimum-force")) {
            target.set("enchants.wind_arrow.settings.minimum-force", 0.95D);
            target.set("enchants.wind_arrow.settings.maximum-charge-ticks", 20);
            target.set("enchants.wind_arrow.settings.required-charge-ticks", null);
            lines.add("enchants.yml: migrated wind_arrow charge timing to bow force threshold");
            changed = true;
        }
        if (target.isSet("enchants.precision_flight.settings.rocket-consume-interval-ticks")
                && !target.isSet("enchants.precision_flight.settings.firework-consume-interval-ticks")) {
            target.set("enchants.precision_flight.settings.firework-consume-interval-ticks",
                    target.getInt("enchants.precision_flight.settings.rocket-consume-interval-ticks", 200));
            target.set("enchants.precision_flight.settings.rocket-consume-interval-ticks", null);
            lines.add("enchants.yml: migrated precision_flight rocket interval to firework interval");
            changed = true;
        }
        if (!hasOnlyInput(target, "blade_chain", "OFFHAND_QUICK")) {
            target.set("enchants.blade_chain.triggers", List.of(
                    Map.of("type", "INPUT", "input", "OFFHAND_QUICK", "phase", "CONFIRMED")));
            lines.add("enchants.yml: consolidated blade_chain to one F input");
            changed = true;
        }
        ConfigurationSection speeds = target.getConfigurationSection("enchants.precision_flight.settings.hotbar-speed");
        if (speeds != null && speeds.isSet("0")) {
            List<Object> values = new ArrayList<>();
            for (int index = 0; index <= 8; index++) values.add(speeds.get(String.valueOf(index)));
            for (int index = 0; index <= 8; index++) target.set("enchants.precision_flight.settings.hotbar-speed." + (index + 1), values.get(index));
            target.set("enchants.precision_flight.settings.hotbar-speed.0", null);
            lines.add("enchants.yml: migrated precision_flight hotbar speeds to slots 1-9");
            changed = true;
        }
        return changed;
    }

    /** Replaces only the known broken generated-lore revision with the current canonical Korean text. */
    private boolean migrateEnchantLore(FileConfiguration target, FileConfiguration defaults, List<String> lines) {
        int currentRevision = target.getInt("enchant-lore-version", 0);
        int requiredRevision = defaults.getInt("enchant-lore-version", 1);
        if (currentRevision >= requiredRevision) return false;
        target.set("enchant-lore", null);
        copyTree(target, defaults, "enchant-lore");
        target.set("enchant-lore-version", requiredRevision);
        lines.add("enchants.yml: replaced legacy generated enchant lore with revision " + requiredRevision);
        return true;
    }

    private boolean replaceSingleInputIfLegacy(FileConfiguration target, String enchantId, String oldInput,
                                                String newInput, List<String> lines) {
        if (!hasOnlyInput(target, enchantId, oldInput)) return false;
        target.set("enchants." + enchantId + ".triggers", List.of(Map.of(
                "type", "INPUT", "input", newInput, "phase", "CONFIRMED")));
        lines.add("enchants.yml: migrated " + enchantId + " input " + oldInput + " -> " + newInput);
        return true;
    }

    private boolean hasOnlyInput(FileConfiguration target, String enchantId, String input) {
        List<Map<?, ?>> triggers = target.getMapList("enchants." + enchantId + ".triggers");
        return triggers.size() == 1 && input.equalsIgnoreCase(String.valueOf(triggers.getFirst().get("input")));
    }

    private boolean hasTriggerType(FileConfiguration target, String enchantId, String type) {
        for (Map<?, ?> binding : target.getMapList("enchants." + enchantId + ".triggers")) {
            Object value = binding.get("type");
            if (value != null && type.equalsIgnoreCase(String.valueOf(value))) return true;
        }
        return false;
    }

    private boolean hasOnlyTriggerType(FileConfiguration target, String enchantId, String type) {
        List<Map<?, ?>> triggers = target.getMapList("enchants." + enchantId + ".triggers");
        return triggers.size() == 1 && type.equalsIgnoreCase(String.valueOf(triggers.getFirst().get("type")));
    }

    private void migrateSpecialEquipment(List<String> lines, List<File> changedFiles) {
        String fileName = "special-equipment.yml";
        FileConfiguration target = loadLive(fileName);
        FileConfiguration defaults = loadResource(fileName);
        if (target == null || defaults == null) return;
        boolean changed = copyMissingTree(target, defaults, "special-equipment");
        changed |= normalizeSpecialGrowthPolicy(target, lines);
        if (changed) {
            lines.add("special-equipment.yml: added missing special equipment settings");
            mark(target, fileName, changedFiles, lines, "special equipment migration staged");
        }
    }

    private boolean normalizeSpecialGrowthPolicy(FileConfiguration target, List<String> lines) {
        ConfigurationSection items = target.getConfigurationSection("special-equipment.items");
        if (items == null) return false;
        boolean changed = false;
        for (String id : items.getKeys(false)) {
            String root = "special-equipment.items." + id;
            ConfigurationSection growth = target.getConfigurationSection(root + ".growth");
            if (growth == null) {
                growth = target.createSection(root + ".growth");
                changed = true;
            }
            if (growth.isSet("enhancement-enabled")) {
                growth.set("enhancement-enabled", null);
                changed = true;
            }
            if (growth.isSet("promotion-enabled")) {
                growth.set("promotion-enabled", null);
                changed = true;
            }
            if (!Boolean.TRUE.equals(growth.get("unbreakable"))) {
                growth.set("unbreakable", true);
                changed = true;
            }
            if (growth.isSet("allow-upgrade")) {
                growth.set("allow-upgrade", null);
                changed = true;
            }
            if (growth.isSet("allow-promotion")) {
                growth.set("allow-promotion", null);
                changed = true;
            }
            for (String retired : List.of("promotion", "future-promotion", "grade")) {
                String path = root + "." + retired;
                if (target.isSet(path)) {
                    target.set(path, null);
                    changed = true;
                }
            }
            if (growth.isSet("custom-enchant-slots")) {
                growth.set("custom-enchant-slots", null);
                changed = true;
            }
        }
        return changed;
    }

    private void migrateEquipmentGrowth(List<String> lines, List<File> changedFiles) {
        String fileName = "equipment-growth.yml";
        FileConfiguration target = loadLive(fileName);
        FileConfiguration defaults = loadResource(fileName);
        if (target == null || defaults == null) return;
        ConfigurationSection tiers = target.getConfigurationSection("tiers.custom-item-tiers");
        if (tiers == null) {
            target.createSection("tiers.custom-item-tiers");
            tiers = target.getConfigurationSection("tiers.custom-item-tiers");
        }
        boolean changed = false;
        for (String id : SPECIAL_TIER_ITEMS) {
            if (tiers != null && !tiers.isSet(id)) {
                Object value = defaults.get("tiers.custom-item-tiers." + id, "custom_legendary");
                tiers.set(id, value);
                lines.add("equipment-growth.yml: added tier mapping " + id + " -> " + value);
                changed = true;
            }
        }
        changed |= migrateGrowthProfile(target, defaults, "enhancement.profiles.tool", lines,
                "added tool enhancement profile");
        changed |= migrateGrowthProfile(target, defaults, "enhancement.axe-combat", lines,
                "added axe combat enhancement profile");
        changed |= mergeMissingListValues(target, defaults, "enhancement.profiles.tool.materials", lines,
                "added missing tool enhancement materials");
        if (target.isSet("promotion") || target.isSet("option-roll")) {
            target.set("promotion", null);
            target.set("option-roll", null);
            lines.add("equipment-growth.yml: removed retired equipment promotion configuration");
            changed = true;
        }
        changed |= migrateStageTwoAnvilEnhancement(target, defaults, lines);
        if (target.getInt("schema-version", 0) < 2) {
            target.set("schema-version", 2);
            changed = true;
        }
        if (changed) mark(target, fileName, changedFiles, lines, "equipment growth profile migration staged");
    }

    private boolean migrateGrowthProfile(FileConfiguration target, FileConfiguration defaults, String path,
                                          List<String> lines, String description) {
        if (!defaults.isConfigurationSection(path)) return false;
        if (!copyMissingTree(target, defaults, path)) return false;
        lines.add("equipment-growth.yml: " + description);
        return true;
    }

    private boolean mergeMissingListValues(FileConfiguration target, FileConfiguration defaults, String path,
                                           List<String> lines, String description) {
        List<String> source = defaults.getStringList(path);
        if (source.isEmpty()) return false;
        List<String> existing = new ArrayList<>(target.getStringList(path));
        boolean changed = false;
        for (String value : source) {
            if (existing.stream().noneMatch(current -> current.equalsIgnoreCase(value))) {
                existing.add(value);
                changed = true;
            }
        }
        if (!changed) return false;
        target.set(path, existing);
        lines.add("equipment-growth.yml: " + description);
        return true;
    }



    private boolean migrateStageTwoAnvilEnhancement(FileConfiguration target, FileConfiguration defaults,
                                                     List<String> lines) {
        boolean changed = false;
        for (String path : List.of("enhancement.caps", "enhancement.elemental-item-ids",
                "enhancement.xp-level-cost")) {
            if (!target.isSet(path) && defaults.isSet(path)) {
                if (defaults.isConfigurationSection(path)) copyTree(target, defaults, path);
                else target.set(path, defaults.get(path));
                changed = true;
            }
        }
        for (String legacy : List.of("enhancement.start-chance", "enhancement.final-chance",
                "enhancement.fail-bonus", "enhancement.maximum-chance", "enhancement.initial-cost",
                "enhancement.cost-curve")) {
            if (target.isSet(legacy)) {
                target.set(legacy, null);
                changed = true;
            }
        }
        if (changed) lines.add("equipment-growth.yml: migrated enhancement to vanilla anvil XP-level policy");
        return changed;
    }



    /** Applies the approved economy policy without replacing unrelated operator settings. */
    private boolean activeEnchantBook(String itemId) {
        return switch (normalize(itemId)) {
            case "enchant_book_blade_throw", "enchant_book_light_greatsword", "enchant_book_laser_arrow",
                    "enchant_book_axe_heavy_strike", "enchant_book_titans_wrath",
                    "enchant_book_skill_protection", "enchant_book_rolling_landing",
                    "enchant_book_wind_arrow", "enchant_book_fire_arrow_rain", "enchant_book_crossbow_barrage",
                    "enchant_book_treasure_finder", "enchant_book_multi_catch", "enchant_book_elytra_launch",
                    "enchant_book_precision_flight",
                    "enchant_book_mining_bonus_drop", "enchant_book_area_mining_pickaxe",
                    "enchant_book_auto_replant", "enchant_book_auto_smelt", "enchant_book_chain_logging",
                    "enchant_book_explosive_mace" -> true;
            default -> false;
        };
    }

    private boolean setIfDifferent(FileConfiguration configuration, String path, Object value) {
        Object current = configuration.get(path);
        if (value == null ? current == null : value.equals(current)) return false;
        configuration.set(path, value);
        return true;
    }

    private Set<String> collectLegacyProfessionItemIds(FileConfiguration configuration) {
        Set<String> result = new HashSet<>(LEGACY_PROFESSION_IDS);
        if (configuration == null) return result;
        ConfigurationSection items = configuration.getConfigurationSection("items");
        if (items == null) return result;
        for (String rawId : items.getKeys(false)) {
            ConfigurationSection definition = items.getConfigurationSection(rawId);
            if (isLegacyProfessionItem(definition, rawId)) result.add(normalize(rawId));
        }
        return result;
    }

    private static boolean isLegacyProfessionItem(ConfigurationSection definition, String rawId) {
        String id = normalize(rawId);
        if (LEGACY_PROFESSION_IDS.contains(id)) return true;
        if (REQUIRED_ENCHANT_BOOK_FALLBACKS.containsKey(id) || id.startsWith("enchant_book_")) return false;
        if (definition == null) return false;

        String category = normalize(definition.getString("category", ""));
        // Current content must never be classified by a display-name fragment.
        if (category.equals("enchantment") || category.equals("equipment") || category.equals("material")) {
            return false;
        }
        if (category.equals("profession") || category.equals("job")
                || category.startsWith("profession_") || category.startsWith("job_")) return true;
        for (String key : List.of("profession-bonus", "job-bonus", "required-profession", "profession-level",
                "job-level", "extra-drop-chance")) {
            if (definition.isSet(key)) return true;
        }
        return false;
    }

    private void migratePlayers(List<String> lines, List<File> changedFiles) {
        File directory = new File(plugin.getDataFolder(), "players");
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(file);
            boolean changed = false;
            if (!data.isSet("schema-version")) {
                data.set("schema-version", 2);
                lines.add("players/" + file.getName() + ": added schema-version: 2");
                changed = true;
            } else if (data.getInt("schema-version", 1) < 2) {
                data.set("schema-version", 2);
                lines.add("players/" + file.getName() + ": upgraded schema-version to 2");
                changed = true;
            }
            if (data.getInt("baseLevel", 1) < 1) {
                data.set("baseLevel", 1);
                lines.add("players/" + file.getName() + ": normalized baseLevel to 1");
                changed = true;
            }
            if (data.getInt("classLevel", 1) < 1) {
                data.set("classLevel", 1);
                lines.add("players/" + file.getName() + ": normalized classLevel to 1");
                changed = true;
            }
            for (String path : DEPRECATED_PLAYER_PATHS) {
                if (data.isSet(path)) {
                    data.set(path, null);
                    lines.add("players/" + file.getName() + ": removed deprecated field " + path + " (backup retained)");
                    changed = true;
                }
            }
            if (changed) {
                changedFiles.add(file);
                pending.put(file, data);
            }
        }
    }

    private final Map<File, FileConfiguration> pending = new java.util.LinkedHashMap<>();

    private FileConfiguration loadLive(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        FileConfiguration staged = pending.get(file);
        if (staged != null) return staged;
        return file.isFile() ? YamlConfiguration.loadConfiguration(file) : null;
    }

    private FileConfiguration loadResource(String fileName) {
        if (plugin.getResource(fileName) == null) return null;
        try (var reader = new java.io.InputStreamReader(plugin.getResource(fileName), java.nio.charset.StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read resource " + fileName, exception);
        }
    }

    private static void copyTree(FileConfiguration target, FileConfiguration defaults, String root) {
        ConfigurationSection source = defaults.getConfigurationSection(root);
        if (source == null) return;
        copySection(target, source, root);
    }

    private boolean copyMissingTree(FileConfiguration target, FileConfiguration defaults, String root) {
        ConfigurationSection source = defaults.getConfigurationSection(root);
        return source != null && copyMissingSection(target, source, root);
    }

    private boolean copyMissingRoot(FileConfiguration target, FileConfiguration defaults) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            ConfigurationSection nested = defaults.getConfigurationSection(key);
            if (nested != null) changed |= copyMissingSection(target, nested, key);
            else if (!target.isSet(key)) {
                target.set(key, defaults.get(key));
                changed = true;
            }
        }
        return changed;
    }

    private boolean copyMissingSection(FileConfiguration target, ConfigurationSection source, String path) {
        boolean changed = false;
        for (String key : source.getKeys(false)) {
            String child = path + "." + key;
            ConfigurationSection nested = source.getConfigurationSection(key);
            if (nested != null) {
                changed |= copyMissingSection(target, nested, child);
            } else if (!target.isSet(child)) {
                target.set(child, source.get(key));
                changed = true;
            }
        }
        return changed;
    }

    private static void copySection(FileConfiguration target, ConfigurationSection source, String path) {
        for (String key : source.getKeys(false)) {
            String child = path + "." + key;
            if (source.isConfigurationSection(key)) {
                copySection(target, source.getConfigurationSection(key), child);
            } else if (!target.isSet(child)) {
                target.set(child, source.get(key));
            }
        }
    }

    private void mark(FileConfiguration configuration, String fileName, List<File> changedFiles, List<String> lines, String description) {
        File file = new File(plugin.getDataFolder(), fileName);
        pending.put(file, configuration);
        if (!changedFiles.contains(file)) changedFiles.add(file);
        lines.add(fileName + ": " + description);
    }

    private File backup(List<File> files) throws IOException {
        File root = new File(plugin.getDataFolder(), "archive/migrations/" + LocalDateTime.now().format(STAMP));
        if (!root.mkdirs() && !root.isDirectory()) throw new IOException("Unable to create backup directory " + root);
        for (File file : files) {
            if (!file.isFile()) continue;
            Path relative = plugin.getDataFolder().toPath().relativize(file.toPath());
            File copy = new File(root, relative.toString());
            File parent = copy.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.copy(file.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return root;
    }

    private void savePending(File file, List<String> lines) throws IOException {
        FileConfiguration configuration = pending.get(file);
        if (configuration == null) return;
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        File temporary = new File(file.getParentFile(), file.getName() + ".migration.tmp");
        configuration.save(temporary);
        try {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        lines.add("saved: " + file.getName());
    }

    private double parseDouble(String value) {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException ignored) { return Double.NaN; }
    }

    private static String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }

    public record MigrationReport(boolean success, List<String> lines, File backupDirectory) { }

    private record BookItemDefinition(String displayName, String rarity, int customModelData, List<String> lore) { }
}
