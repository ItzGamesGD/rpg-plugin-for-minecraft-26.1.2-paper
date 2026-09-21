package com.hyunseo.hyunseorpg.core.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import com.hyunseo.hyunseorpg.item.VanillaStackingPolicy;
import com.hyunseo.hyunseorpg.enchant.EnchantRegistry;
import com.hyunseo.hyunseorpg.core.BuildInfo;
import com.hyunseo.hyunseorpg.crafting.CraftingRecipeRequirements;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Read-only diagnostics for live HyunseoRPG data files. */
public final class ConfigDoctor {
    private static final Set<String> VALID_SECTIONS = Set.of(
            "all", "configs", "items", "recipes", "equipment", "mobs", "players",
            "quests", "stacking", "vanilla-stacking", "enchants", "alchemy", "effects", "reload");
    private static final List<String> MANAGED_FILES = List.of(
            "config.yml", "exp.yml", "weapons.yml",
            "items.yml", "crafting.yml", "equipment-growth.yml",
            "equipment-inputs.yml", "enchants.yml",
            "mobs.yml", "monster-spawns.yml", "mythic-mobs.yml",
            "special-equipment.yml"
            , "alchemy/effects.yml", "alchemy/components.yml", "alchemy/conflicts.yml", "alchemy/scaling.yml",
            "alchemy/potions.yml", "alchemy/recipes.yml",
            "alchemy/catalysts.yml"
    );
    private static final List<String> SPECIAL_TIER_ITEMS = List.of(
            "burning_sword", "flowing_water_sword", "wind_cutting_sword", "earth_special_sword",
            "ice_special_sword", "dark_energy_sword", "burning_bow", "wind_archers_bow",
            "earth_heavy_bow", "freezing_bow", "dark_energy_bow", "poseidon_spear"
    );

    private final JavaPlugin plugin;

    public ConfigDoctor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public DoctorReport run(String requestedSection) {
        String section = normalize(requestedSection);
        List<String> lines = new ArrayList<>();
        int[] counts = new int[3]; // info, warning, error
        lines.add("[HyunseoRPG Doctor]");
        lines.add("data-folder: " + plugin.getDataFolder().getAbsolutePath());
        lines.add("section: " + (section.isBlank() ? "all" : section));
        lines.add("plugin-version: " + plugin.getDescription().getVersion());
        lines.add("build-id: " + BuildInfo.BUILD_ID);
        if (!section.isBlank() && !VALID_SECTIONS.contains(section)) {
            error(lines, counts, "unknown doctor section: " + section);
            lines.add("valid sections: " + VALID_SECTIONS);
            lines.add("summary: info=" + counts[0] + ", warning=" + counts[1] + ", error=" + counts[2]);
            return new DoctorReport(List.copyOf(lines), counts[1], counts[2]);
        }

        if (section.isBlank() || section.equals("all") || section.equals("configs")) {
            checkFiles(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("items")) {
            checkItems(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("recipes")) {
            checkRecipes(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("equipment")) {
            checkEquipment(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("mobs")) {
            checkMobs(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("players")) {
            checkPlayers(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("quests")) {
        }
        if (section.isBlank() || section.equals("all") || section.equals("stacking")
                || section.equals("vanilla-stacking")) {
            checkVanillaStacking(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("enchants")) {
            checkEnchants(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("alchemy") || section.equals("effects")) {
            checkAlchemy(lines, counts);
        }
        if (section.equals("reload")) {
            checkReloadReadiness(lines, counts);
        }

        lines.add("summary: info=" + counts[0] + ", warning=" + counts[1] + ", error=" + counts[2]);
        return new DoctorReport(List.copyOf(lines), counts[1], counts[2]);
    }

    public File writeReport(DoctorReport report) {
        File directory = new File(plugin.getDataFolder(), "reports");
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("Unable to create doctor report directory: " + directory);
            return null;
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File reportFile = new File(directory, "doctor-" + timestamp + ".txt");
        try {
            Files.write(reportFile.toPath(), report.lines(), StandardCharsets.UTF_8);
            return reportFile;
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to write doctor report: " + exception.getMessage());
            return null;
        }
    }

    private void checkFiles(List<String> lines, int[] counts) {
        for (String name : MANAGED_FILES) {
            File file = new File(plugin.getDataFolder(), name);
            if (!file.isFile()) {
                error(lines, counts, name + ": missing managed file");
                continue;
            }
            FileConfiguration config = load(name, lines, counts);
            if (config != null && !config.isSet("schema-version")) {
                warning(lines, counts, name + ": missing schema-version");
            }
        }
        File legacy = new File(plugin.getDataFolder(), "dungeons.yml");
        if (legacy.isFile()) error(lines, counts, "dungeons.yml: deprecated dungeon config is still active; archive it");
        File professions = new File(plugin.getDataFolder(), "professions.yml");
        if (professions.isFile()) error(lines, counts, "professions.yml: deprecated profession config is still active; archive it");
        File hunting = new File(plugin.getDataFolder(), "hunting-grounds.yml");
        if (hunting.isFile()) error(lines, counts, "hunting-grounds.yml: deprecated hunting-ground config is still active; archive it");
        File worlds = new File(plugin.getDataFolder(), "worlds.yml");
        if (worlds.isFile()) error(lines, counts, "worlds.yml: world-lock config is still active; archive it");
        File worldUnlocks = new File(plugin.getDataFolder(), "world-unlocks.yml");
        if (worldUnlocks.isFile()) error(lines, counts, "world-unlocks.yml: deprecated world-lock config is still active; archive it");
        File legacyBlocks = new File(plugin.getDataFolder(), "placed-job-blocks.db");
        if (legacyBlocks.isFile()) warning(lines, counts, "placed-job-blocks.db: legacy activity ledger remains; archive after activity-blocks.db migration");
        File activityBlocks = new File(plugin.getDataFolder(), "activity-blocks.db");
        if (activityBlocks.isFile()) info(lines, counts, "activity-blocks.db: neutral activity ledger is active");
        checkServerResidue(lines, counts);
        File archive = new File(plugin.getDataFolder(), "archive");
        if (archive.isDirectory()) info(lines, counts, "archive: present; legacy files are retained outside the live config root");
    }

    private void checkItems(List<String> lines, int[] counts) {
        FileConfiguration config = load("items.yml", lines, counts);
        if (config == null) return;
        ConfigurationSection items = config.getConfigurationSection("items");
        if (items == null) {
            error(lines, counts, "items.yml: missing items section");
            return;
        }
        Map<Integer, String> modelOwners = new HashMap<>();
        for (String id : items.getKeys(false)) {
            String path = "items." + id;
            String material = config.getString(path + ".material", "");
            if (Material.matchMaterial(material.replace("minecraft:", "").toUpperCase(Locale.ROOT)) == null) {
                error(lines, counts, path + ".material: unknown Material '" + material + "'");
            }
            int model = config.getInt(path + ".custom-model-data", 0);
            if (model > 0) {
                String previous = modelOwners.putIfAbsent(model, id);
                if (previous != null && !previous.equalsIgnoreCase(id)) {
                    warning(lines, counts, "items.yml: duplicate custom-model-data " + model + " (" + previous + ", " + id + ")");
                }
            }
            String category = config.getString(path + ".category", "");
            if (category.toUpperCase(Locale.ROOT).startsWith("PROFESSION_")) {
                info(lines, counts, path + ": legacy profession item definition is ignored by the active registry");
            }
        }
        compareResourceContract("items.yml", config, lines, counts, Set.of("items"));
        info(lines, counts, "items.yml: " + items.getKeys(false).size() + " item definitions loaded");
    }

    private void checkEnchants(List<String> lines, int[] counts) {
        FileConfiguration enchants = load("enchants.yml", lines, counts);
        FileConfiguration items = load("items.yml", lines, counts);
        if (enchants == null || items == null) return;
        ConfigurationSection definitions = enchants.getConfigurationSection("enchants");
        ConfigurationSection itemDefinitions = items.getConfigurationSection("items");
        if (definitions == null || itemDefinitions == null) {
            error(lines, counts, "enchants.yml: missing enchants or items section");
            return;
        }
        if (enchants.getInt("config-version", 0) < 4) {
            warning(lines, counts, "enchants.yml: config-version is older than 4; run /rpg migrate configs");
        }
        Set<String> books = new HashSet<>();
        Set<String> knownHandlers = Set.of("content", "skill");
        int valid = 0;
        for (String id : definitions.getKeys(false)) {
            String path = "enchants." + id;
            if (EnchantRegistry.isRetiredId(id)) {
                info(lines, counts, path + ": disabled; reason=" + EnchantRegistry.retiredReason(id)
                        + "; existing PDC data is preserved and runtime effects are ignored");
                continue;
            }
            String book = normalize(enchants.getString(path + ".book-item-id", ""));
            String handler = normalize(enchants.getString(path + ".handler-id", ""));
            if (!book.isBlank() && !itemDefinitions.isConfigurationSection(book)) {
                error(lines, counts, path + ".book-item-id: missing item " + book);
            } else if (!book.isBlank() && !books.add(book)) {
                error(lines, counts, path + ".book-item-id: duplicate book " + book);
            }
            if (!knownHandlers.contains(handler)) {
                error(lines, counts, path + ".handler-id: no registered handler " + handler);
            } else {
                valid++;
            }
            for (String alias : enchants.getStringList(path + ".legacy-aliases")) {
                if (definitions.isConfigurationSection(normalize(alias))) {
                    error(lines, counts, path + ".legacy-aliases: canonical ID collision " + alias);
                }
            }
        }
        info(lines, counts, "enchants.yml: " + valid + "/" + definitions.getKeys(false).size()
                + " definitions have valid handlers; books=" + books.size());
    }

    private void checkRecipes(List<String> lines, int[] counts) {
        FileConfiguration crafting = load("crafting.yml", lines, counts);
        FileConfiguration items = load("items.yml", lines, counts);
        if (crafting == null || items == null) return;
        ConfigurationSection recipes = crafting.getConfigurationSection("crafting-recipes");
        if (recipes == null) {
            error(lines, counts, "crafting.yml: missing crafting-recipes section");
            return;
        }
        Set<String> itemIds = itemIds(items);
        Set<String> recipeIds = new HashSet<>();
        for (String recipeId : recipes.getKeys(false)) {
            String path = "crafting-recipes." + recipeId;
            if (!recipeIds.add(recipeId.toLowerCase(Locale.ROOT))) {
                error(lines, counts, path + ": duplicate recipe-id");
            }
            String outputId = normalize(crafting.getString(path + ".output.item-id", ""));
            int outputAmount = strictAmount(crafting.get(path + ".output.amount"));
            if (!itemIds.contains(outputId)) error(lines, counts, path + ".output.item-id: unknown item '" + outputId + "'");
            if (outputAmount < 1) error(lines, counts, path + ".output.amount: expected positive integer");
            ConfigurationSection inputs = crafting.getConfigurationSection(path + ".inputs");
            Set<String> inputKeys = CraftingRecipeRequirements.inputKeys(inputs);
            boolean hasInputs = !inputKeys.isEmpty();
            for (String ingredient : inputKeys) {
                int amount = strictAmount(inputs.get(ingredient));
                if (amount < 1) error(lines, counts, path + ".inputs." + ingredient + ": invalid amount '" + inputs.get(ingredient) + "'");
                String normalized = normalize(ingredient);
                if (normalized.startsWith("vanilla:")) {
                    if (Material.matchMaterial(normalized.substring("vanilla:".length()).toUpperCase(Locale.ROOT)) == null) {
                        error(lines, counts, path + ".inputs." + ingredient + ": unknown vanilla material");
                    }
                } else if (!itemIds.contains(normalized)) {
                    warning(lines, counts, path + ".inputs." + ingredient + ": item is not defined in items.yml");
                }
            }
        }
        if (crafting != null) {
            checkRecipeList(crafting.getStringList("crafting.categories.materials"), recipeIds, "crafting.categories.materials", lines, counts);
            checkRecipeList(crafting.getStringList("crafting.categories.equipment"), recipeIds, "crafting.categories.equipment", lines, counts);
            checkRecipeList(crafting.getStringList("craft2.recipes"), recipeIds, "craft2.recipes", lines, counts);
        }
        info(lines, counts, "recipes: " + recipeIds.size() + " canonical crafting recipes inspected");
    }

    private void checkVanillaStacking(List<String> lines, int[] counts) {
        var config = plugin.getConfig();
        boolean enabled = config.getBoolean("vanilla-stacking.enabled", true);
        checkStackSize(config.getInt("vanilla-stacking.potion.max-stack-size", 16),
                "vanilla-stacking.potion.max-stack-size", lines, counts);
        checkStackSize(config.getInt("vanilla-stacking.vehicles.max-stack-size", 16),
                "vanilla-stacking.vehicles.max-stack-size", lines, counts);
        checkStackSize(config.getInt("vanilla-stacking.utility.max-stack-size", 16),
                "vanilla-stacking.utility.max-stack-size", lines, counts);
        for (String raw : config.getStringList("vanilla-stacking.utility.materials")) {
            if (Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT)) == null) {
                error(lines, counts, "vanilla-stacking.utility.materials: unknown Material '" + raw + "'");
            }
        }
        ConfigurationSection overrides = config.getConfigurationSection("vanilla-stacking.overrides");
        if (overrides != null) {
            for (String rawMaterial : overrides.getKeys(false)) {
                Material material = Material.matchMaterial(rawMaterial.trim().toUpperCase(Locale.ROOT));
                if (material == null) {
                    error(lines, counts, "vanilla-stacking.overrides: unknown Material '" + rawMaterial + "'");
                    continue;
                }
                if (!VanillaStackingPolicy.isAllowed(material)) {
                    warning(lines, counts, "vanilla-stacking.overrides." + rawMaterial
                            + ": Material is outside the strict vanilla allow-list");
                }
                checkStackSize(overrides.getInt(rawMaterial, -1),
                        "vanilla-stacking.overrides." + rawMaterial, lines, counts);
            }
        }
        String allowed = Arrays.stream(Material.values())
                .filter(VanillaStackingPolicy::isAllowed)
                .map(Material::name)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
        info(lines, counts, "vanilla-stacking: " + (enabled ? "enabled" : "disabled")
                + "; strict allow-list=" + allowed
                + "; PDC/RPG/durability/container items excluded");
    }

    private void checkStackSize(int value, String path, List<String> lines, int[] counts) {
        if (value < 1 || value > 99) {
            error(lines, counts, path + ": stack size must be between 1 and 99");
        }
    }

    private void checkEquipment(List<String> lines, int[] counts) {
        FileConfiguration growth = load("equipment-growth.yml", lines, counts);
        if (growth == null) return;
        ConfigurationSection customTiers = growth.getConfigurationSection("tiers.custom-item-tiers");
        for (String id : SPECIAL_TIER_ITEMS) {
            if (customTiers == null || !customTiers.isSet(id)) {
                warning(lines, counts, "equipment-growth.yml: missing special equipment tier '" + id + "'");
            }
        }
        if (growth.isConfigurationSection("promotion") && growth.isConfigurationSection("enhancement")) {
            info(lines, counts, "equipment-growth.yml: canonical enhancement/promotion sections present");
        }
        checkGrowthCoverage(growth, lines, counts);
        File legacy = new File(plugin.getDataFolder(), "enhancements.yml");
        if (legacy.isFile()) {
            warning(lines, counts, "enhancements.yml: legacy file remains in live config root; run /rpg migrate legacy --apply");
        }
    }

    private void checkGrowthCoverage(FileConfiguration growth, List<String> lines, int[] counts) {
        Set<String> toolMaterials = new HashSet<>();
        for (String value : growth.getStringList("enhancement.profiles.tool.materials")) {
            toolMaterials.add(normalize(value));
        }
        for (String material : List.of(
                "WOODEN_PICKAXE", "STONE_PICKAXE", "IRON_PICKAXE", "GOLDEN_PICKAXE", "DIAMOND_PICKAXE", "NETHERITE_PICKAXE",
                "WOODEN_SHOVEL", "STONE_SHOVEL", "IRON_SHOVEL", "GOLDEN_SHOVEL", "DIAMOND_SHOVEL", "NETHERITE_SHOVEL",
                "WOODEN_AXE", "STONE_AXE", "IRON_AXE", "GOLDEN_AXE", "DIAMOND_AXE", "NETHERITE_AXE",
                "WOODEN_HOE", "STONE_HOE", "IRON_HOE", "GOLDEN_HOE", "DIAMOND_HOE", "NETHERITE_HOE")) {
            if (!toolMaterials.contains(normalize(material))) {
                warning(lines, counts, "equipment-growth.yml: tool enhancement profile is missing material '" + material + "'");
            }
        }
        for (String profile : List.of("pickaxe", "shovel", "hoe", "axe", "crossbow")) {
            if (!growth.isConfigurationSection("promotion.profiles." + profile)) {
                warning(lines, counts, "equipment-growth.yml: promotion profile is missing '" + profile
                        + "'; run /rpg migrate configs --apply");
            }
        }
        ConfigurationSection optionDefinitions = growth.getConfigurationSection("promotion.option-definitions");
        if (optionDefinitions != null) {
            for (String profile : growth.getConfigurationSection("promotion.profiles") == null
                    ? Set.<String>of() : growth.getConfigurationSection("promotion.profiles").getKeys(false)) {
                for (String pool : List.of("general-option-pool", "special-option-pool")) {
                    for (String option : growth.getStringList("promotion.profiles." + profile + "." + pool)) {
                        if (!optionDefinitions.getKeys(false).stream().anyMatch(id -> id.equalsIgnoreCase(option))) {
                            warning(lines, counts, "equipment-growth.yml: " + profile + " references unknown promotion option '" + option + "'");
                        } else if (!optionDefinitions.getBoolean(option + ".promotion-eligible", true)) {
                            warning(lines, counts, "equipment-growth.yml: " + profile + " contains non-promotion option '" + option + "'");
                        }
                    }
                }
            }
        }
    }

    private void checkMobs(List<String> lines, int[] counts) {
        FileConfiguration mobs = load("mobs.yml", lines, counts);
        FileConfiguration spawns = load("monster-spawns.yml", lines, counts);
        if (mobs == null || spawns == null) return;
        Set<String> mobIds = new HashSet<>();
        addKeys(mobIds, mobs.getConfigurationSection("custom-mobs"));
        addKeys(mobIds, mobs.getConfigurationSection("mob-definitions"));
        ConfigurationSection spawnSection = spawns.getConfigurationSection("spawns");
        if (spawnSection != null) {
            for (String id : spawnSection.getKeys(false)) {
                if (!mobIds.contains(normalize(id))) warning(lines, counts, "monster-spawns.yml: spawn has no mobs.yml definition '" + id + "'");
            }
        }
        FileConfiguration mythic = load("mythic-mobs.yml", lines, counts);
        if (mythic != null) info(lines, counts, "mobs: RPG and Mythic adapter definitions are separate; external MythicMobs files require server audit");
    }

    private void checkPlayers(List<String> lines, int[] counts) {
        File directory = new File(plugin.getDataFolder(), "players");
        if (!directory.isDirectory()) {
            warning(lines, counts, "players: directory is missing");
            return;
        }
        int total = 0;
        int missingSchema = 0;
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                total++;
                FileConfiguration data = YamlConfiguration.loadConfiguration(file);
                if (!data.isSet("schema-version")) missingSchema++;
                if (data.isSet("selectedProfession") || data.isSet("unlockedWorlds") || data.isSet("clearedWorlds")) {
                    warning(lines, counts, "players/" + file.getName() + ": deprecated profession/world-lock fields remain readable but are excluded from new saves");
                }

            }
        }
        if (missingSchema > 0) warning(lines, counts, "players: " + missingSchema + "/" + total + " files missing schema-version");
        info(lines, counts, "players: " + total + " player files inspected");
    }

    private void checkAlchemy(List<String> lines, int[] counts) {
        FileConfiguration effects = load("alchemy/effects.yml", lines, counts);
        FileConfiguration components = load("alchemy/components.yml", lines, counts);
        FileConfiguration conflicts = load("alchemy/conflicts.yml", lines, counts);
        FileConfiguration scaling = load("alchemy/scaling.yml", lines, counts);
        FileConfiguration potions = load("alchemy/potions.yml", lines, counts);
        FileConfiguration recipes = load("alchemy/recipes.yml", lines, counts);
        if (effects == null || components == null || conflicts == null || scaling == null
                || potions == null || recipes == null) return;
        ConfigurationSection definitions = effects.getConfigurationSection("effects");
        if (definitions == null) {
            error(lines, counts, "alchemy/effects.yml: missing effects section");
            return;
        }
        if (!definitions.isConfigurationSection("effect_test_speed")) {
            error(lines, counts, "alchemy/effects.yml: missing effect_test_speed");
        }
        for (String id : definitions.getKeys(false)) {
            String path = "effects." + id;
            if (!id.matches("[a-z0-9_]+")) error(lines, counts, "alchemy/effects.yml: invalid effect id " + id);
            if (!effects.getBoolean(path + ".enabled", false)) continue;
            if (effects.getInt(path + ".duration-ticks", 0) < 1) {
                error(lines, counts, "alchemy/effects.yml: invalid duration for " + id);
            }
            if (!effects.isList(path + ".components") && effects.getString(path + ".handler-id", "").isBlank()) {
                error(lines, counts, "alchemy/effects.yml: no component or handler for " + id);
            }
        }
        if (scaling.getInt("limits.duration-ticks", 0) < 1) {
            error(lines, counts, "alchemy/scaling.yml: limits.duration-ticks must be positive");
        }
        info(lines, counts, "alchemy: effects=" + definitions.getKeys(false).size()
                + ", components=" + (components.getConfigurationSection("components") != null)
                + ", potions=" + (potions.getConfigurationSection("potions") == null ? 0 : potions.getConfigurationSection("potions").getKeys(false).size())
                + ", recipes=" + (recipes.getConfigurationSection("recipes") == null ? 0 : recipes.getConfigurationSection("recipes").getKeys(false).size())
);
        if (plugin instanceof com.hyunseo.hyunseorpg.HyunseoRPGPlugin rpg) {
            var potionRegistry = rpg.getPotionRegistry();
            var recipeRegistry = rpg.getAlchemyRecipeRegistry();
            var catalystRegistry = rpg.getCatalystRegistry();
            var specialCatalystRegistry = rpg.getSpecialCatalystRegistry();
            info(lines, counts, "alchemy-runtime: effects=" + rpg.getEffectService().registry().getAll().size()
                    + ", potions=" + (potionRegistry == null ? 0 : potionRegistry.all().size())
                    + ", active-recipes=" + (recipeRegistry == null ? 0 : recipeRegistry.all().size())
                    + ", catalysts=" + (catalystRegistry == null ? 0 : catalystRegistry.all().size())
                    + ", special-catalysts=" + (specialCatalystRegistry == null ? 0 : specialCatalystRegistry.all().size()));
            if (potionRegistry != null) {
                for (var potion : potionRegistry.all().values()) {
                    if (potion.enabled() && !rpg.getItemService().getData(potion.outputItemId()).isPresent()) {
                        error(lines, counts, "alchemy/potions.yml: enabled potion " + potion.id()
                                + " references missing output item " + potion.outputItemId());
                    }
                }
            }
            if (recipeRegistry != null) {
                for (var recipe : recipeRegistry.all().values()) {
                    if (potionRegistry == null || potionRegistry.find(recipe.resultPotionId()).isEmpty()) {
                        error(lines, counts, "alchemy/recipes.yml: unresolved result potion " + recipe.resultPotionId());
                    }
                }
            }
        }
    }

    private void checkReloadReadiness(List<String> lines, int[] counts) {
        lines.add("[Reload readiness]");
        int groups = 0;
        groups += reloadGroup("items", lines, counts, this::checkItems);
        groups += reloadGroup("effects", lines, counts, this::checkAlchemy);
        int beforeRecipes = counts[2];
        checkRecipes(lines, counts);
        boolean recipesOk = counts[2] == beforeRecipes;
        lines.add("crafting-recipes: " + (recipesOk ? "PASS" : "FAIL"));
        if (!recipesOk) {
            lines.add("crafting-layout: SKIPPED due to dependency failure");
            groups++;
        } else {
            lines.add("crafting-layout: PASS");
        }
        if (groups == 0 && recipesOk) {
            lines.add("Reload readiness: PASS");
        } else {
            lines.add("Reload readiness: FAILED (" + (groups + (recipesOk ? 0 : 1)) + " error groups)");
        }
    }

    private int reloadGroup(String name, List<String> lines, int[] counts,
                            java.util.function.BiConsumer<List<String>, int[]> checker) {
        int before = counts[2];
        checker.accept(lines, counts);
        boolean pass = counts[2] == before;
        lines.add(name + ": " + (pass ? "PASS" : "FAIL"));
        return pass ? 0 : 1;
    }

    private FileConfiguration load(String fileName, List<String> lines, int[] counts) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.isFile()) return null;
        try {
            return YamlConfiguration.loadConfiguration(file);
        } catch (RuntimeException exception) {
            error(lines, counts, fileName + ": YAML parse failed: " + exception.getMessage());
            return null;
        }
    }

    private FileConfiguration loadPath(String fileName, List<String> lines, int[] counts) {
        File file = new File(plugin.getDataFolder(), fileName);
        try {
            return YamlConfiguration.loadConfiguration(file);
        } catch (RuntimeException exception) {
            error(lines, counts, fileName + ": YAML parse failed: " + exception.getMessage());
            return null;
        }
    }

    private void compareResourceContract(String fileName, FileConfiguration live,
                                         List<String> lines, int[] counts, Set<String> ignoredRoots) {
        FileConfiguration defaults = loadResource(fileName);
        if (defaults == null) return;
        int liveSchema = live.getInt("schema-version", -1);
        int defaultSchema = defaults.getInt("schema-version", -1);
        if (liveSchema >= 0 && defaultSchema >= 0 && liveSchema != defaultSchema) {
            warning(lines, counts, fileName + ": external schema-version=" + liveSchema
                    + " differs from bundled default=" + defaultSchema + " (migration required, no auto-change)");
        }
        for (String key : defaults.getKeys(false)) {
            if (ignoredRoots.contains(key)) continue;
            if (!live.isSet(key)) warning(lines, counts, fileName + ": missing bundled section/key '" + key + "'");
        }
        if (fileName.equals("items.yml")) {
            ConfigurationSection defaultItems = defaults.getConfigurationSection("items");
            ConfigurationSection liveItems = live.getConfigurationSection("items");
            if (defaultItems != null && liveItems != null) {
                long missing = defaultItems.getKeys(false).stream()
                        .filter(id -> !liveItems.isConfigurationSection(id)).count();
                if (missing > 0) warning(lines, counts, "items.yml: " + missing + " bundled item definition(s) missing externally");
            }
        }
    }

    private FileConfiguration loadResource(String fileName) {
        if (plugin.getResource(fileName) == null) return null;
        try (var reader = new java.io.InputStreamReader(plugin.getResource(fileName), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            return null;
        }
    }

    private void checkRecipeList(List<String> ids, Set<String> recipes, String path, List<String> lines, int[] counts) {
        for (String id : ids) {
            if (!recipes.contains(normalize(id))) warning(lines, counts, path + ": unknown recipe-id '" + id + "'");
        }
    }

    private Set<String> itemIds(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("items");
        if (section == null) return Set.of();
        Set<String> ids = new HashSet<>();
        for (String id : section.getKeys(false)) ids.add(normalize(id));
        return ids;
    }

    private void addKeys(Set<String> ids, ConfigurationSection section) {
        if (section == null) return;
        for (String key : section.getKeys(false)) ids.add(normalize(key));
    }

    private int strictAmount(Object raw) {
        if (raw instanceof Number number && number.doubleValue() >= 1 && number.doubleValue() == number.intValue()) return number.intValue();
        if (raw instanceof String value && value.matches("[1-9][0-9]*")) {
            try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return -1; }
        }
        return -1;
    }

    private void info(List<String> lines, int[] counts, String message) { counts[0]++; lines.add("INFO  " + message); }
    private void warning(List<String> lines, int[] counts, String message) { counts[1]++; lines.add("WARN  " + message); }
    private void error(List<String> lines, int[] counts, String message) { counts[2]++; lines.add("ERROR " + message); }

    private void checkServerResidue(List<String> lines, int[] counts) {
        File plugins = plugin.getDataFolder().getParentFile();
        if (plugins == null || !plugins.isDirectory()) return;
        File[] entries = plugins.listFiles();
        if (entries == null) return;
        for (File entry : entries) {
            String name = entry.getName().toLowerCase(Locale.ROOT);
            if (name.contains("multiverse")) {
                error(lines, counts, "server plugins: Multiverse residue found at " + entry.getName());
            }
            if (name.equals("modelengine") && entry.isDirectory()
                    && !new File(plugins, "ModelEngine.jar").isFile()) {
                warning(lines, counts, "server plugins: ModelEngine data directory has no active ModelEngine jar");
            }
        }
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }

    public record DoctorReport(List<String> lines, int warnings, int errors) {
        public boolean success() { return errors == 0; }
    }
}
