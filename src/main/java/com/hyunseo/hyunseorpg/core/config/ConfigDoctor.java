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
            "all", "configs", "items", "recipes", "shops", "equipment", "mobs", "players",
            "progression", "quests", "stacking", "vanilla-stacking", "enchants", "menu", "menus", "farming", "alchemy", "effects", "reload");
    private static final List<String> FARMING_FILES = List.of(
            "crops.yml", "growth.yml", "harvest.yml", "progression.yml", "quality.yml", "processing.yml",
            "hoe_enhancement.yml", "hoe_promotion.yml", "deliveries.yml", "favor.yml",
            "essence.yml", "stat_tokens.yml");
    private static final List<String> MANAGED_FILES = List.of(
            "config.yml", "stats.yml", "exp.yml", "classes.yml", "skills.yml", "weapons.yml",
            "items.yml", "shops.yml", "crafting.yml", "equipment-growth.yml",
            "equipment-options.yml", "equipment-inputs.yml", "enchants.yml",
            "mobs.yml", "monster-spawns.yml", "mythic-mobs.yml", "bosses.yml",
            "progression-loop.yml", "quests.yml", "special-equipment.yml", "equipment-support.yml"
            , "alchemy/effects.yml", "alchemy/components.yml", "alchemy/conflicts.yml", "alchemy/scaling.yml",
            "alchemy/abundance.yml", "alchemy/potions.yml", "alchemy/recipes.yml",
            "alchemy/catalysts.yml", "alchemy/gui.yml"
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
        if (section.isBlank() || section.equals("all") || section.equals("shops")) {
            checkShops(lines, counts);
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
        if (section.isBlank() || section.equals("all") || section.equals("progression")) {
            checkProgression(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("quests")) {
            checkQuests(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("stacking")
                || section.equals("vanilla-stacking")) {
            checkVanillaStacking(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("enchants")) {
            checkEnchants(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("menu")
                || section.equals("menus")) {
            checkMenu(lines, counts);
        }
        if (section.isBlank() || section.equals("all") || section.equals("farming")) {
            checkFarming(lines, counts);
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
        for (String id : List.of("magic_stone_fragment", "wither_core", "wither_fragment", "dragon_core", "dragon_fragment")) {
            if (!items.isConfigurationSection(id)) warning(lines, counts, "items.yml: missing latest item '" + id + "'");
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

    private void checkMenu(List<String> lines, int[] counts) {
        String expected = "제작";
        info(lines, counts, "[ConfigDoctor/Menu] main-menu.crafting.display-name=" + expected
                + " source=RPGMenuService.openMain fallback-used=false");
        info(lines, counts, "[ConfigDoctor/Menu] crafting-gui.title="
                + com.hyunseo.hyunseorpg.crafting.CraftingGuiService.MAIN_TITLE
                + " source=CraftingGuiService.openMain fallback-used=false");
        if (!expected.equals(com.hyunseo.hyunseorpg.ui.RPGMenuService.MAIN_MENU_CRAFTING_LABEL)
                || !expected.equals(com.hyunseo.hyunseorpg.crafting.CraftingGuiService.MAIN_TITLE)) {
            error(lines, counts, "[ConfigDoctor/Menu] crafting display name is not canonical: " + expected);
        }
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
            long requiredAbundancePoints = crafting.getLong(path + ".required-abundance-points", 0L);
            if (requiredAbundancePoints < 0L) {
                error(lines, counts, path + ".required-abundance-points: must not be negative");
            }
            if (!CraftingRecipeRequirements.isValid(hasInputs, requiredAbundancePoints)) {
                error(lines, counts, path + ".inputs: "
                        + CraftingRecipeRequirements.invalidReason(hasInputs, requiredAbundancePoints));
                continue;
            }
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
            checkCraftingLayout(crafting, recipeIds, lines, counts);
        }
        info(lines, counts, "recipes: " + recipeIds.size() + " canonical crafting recipes inspected");
    }

    private void checkCraftingLayout(FileConfiguration crafting, Set<String> recipeIds,
                                     List<String> lines, int[] counts) {
        ConfigurationSection layout = crafting.getConfigurationSection("crafting.layout");
        if (layout == null) {
            warning(lines, counts, "crafting.yml: canonical crafting.layout is missing; run /rpg migrate configs");
            return;
        }
        FileConfiguration special = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "special-equipment.yml"));
        Set<String> known = new HashSet<>(recipeIds);
        ConfigurationSection specialItems = special.getConfigurationSection("special-equipment.items");
        if (specialItems != null) {
            for (String id : specialItems.getKeys(false)) {
                if (special.getBoolean("special-equipment.items." + id + ".enabled", true)
                        && special.getBoolean("special-equipment.items." + id + ".crafting.enabled", true)) {
                    known.add("special_" + normalize(id));
                }
            }
        }
        Set<String> placed = new HashSet<>();
        for (String category : layout.getKeys(false)) {
            ConfigurationSection entries = layout.getConfigurationSection(category);
            if (entries == null) {
                error(lines, counts, "crafting.layout." + category + ": expected recipe positions");
                continue;
            }
            Set<Integer> slots = new HashSet<>();
            for (String recipeId : entries.getKeys(false)) {
                String path = "crafting.layout." + category + "." + recipeId;
                String normalized = normalize(recipeId);
                if (!known.contains(normalized)) error(lines, counts, path + ": unknown recipe");
                if (!placed.add(normalized)) error(lines, counts, path + ": recipe is placed more than once");
                Object rawPosition = entries.get(recipeId);
                if (!(rawPosition instanceof Number number) || number.doubleValue() != number.intValue()
                        || number.intValue() < 0) {
                    error(lines, counts, path + ": position must be a non-negative integer");
                } else if (!slots.add(number.intValue())) {
                    error(lines, counts, path + ": duplicate content slot " + number.intValue());
                }
            }
        }
        for (String recipeId : recipeIds) {
            if (!placed.contains(recipeId)) warning(lines, counts, "crafting.layout: active recipe is not placed '" + recipeId + "'");
        }
    }

    private void checkQuests(List<String> lines, int[] counts) {
        FileConfiguration quests = load("quests.yml", lines, counts);
        if (quests == null) return;
        ConfigurationSection auto = quests.getConfigurationSection("auto");
        if (auto == null) {
            warning(lines, counts, "quests.yml: automatic quest section is missing");
            return;
        }
        int maxActive = quests.getInt("auto.max-active", 0);
        if (maxActive < 1) error(lines, counts, "quests.yml:auto.max-active must be at least 1");
        if (quests.getLong("auto.time-limit-seconds", 0L) <= 0L) error(lines, counts, "quests.yml:auto.time-limit-seconds must be positive");
        int huntWeight = quests.getInt("auto.types.hunt.weight", -1);
        int itemWeight = quests.getInt("auto.types.item-delivery.weight", -1);
        if (huntWeight < 0 || itemWeight < 0 || huntWeight + itemWeight <= 0) {
            error(lines, counts, "quests.yml:auto.types weights must contain a positive total");
        }
        checkQuestRange(quests, "auto.types.hunt.amount-per-target", lines, counts);
        checkQuestRange(quests, "auto.types.item-delivery.amount", lines, counts);
        checkQuestRange(quests, "auto.types.hunt.target-count", lines, counts);
        for (String key : List.of("after-accept-seconds", "after-complete-seconds", "after-fail-seconds", "after-abandon-seconds")) {
            if (quests.getLong("auto.cooldown." + key, -1L) < 0L) error(lines, counts, "quests.yml:auto.cooldown." + key + " is negative");
        }
        for (String raw : quests.getStringList("auto.eligibility.vanilla-mobs")) {
            try { EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException exception) { error(lines, counts, "quests.yml:auto.eligibility.vanilla-mobs has unknown entity " + raw); }
        }
        for (String raw : quests.getStringList("auto.eligibility.vanilla-items")) {
            if (Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT)) == null) {
                error(lines, counts, "quests.yml:auto.eligibility.vanilla-items has unknown material " + raw);
            }
        }
        FileConfiguration items = load("items.yml", lines, counts);
        Set<String> itemIds = items == null ? Set.of() : itemIds(items);
        for (String id : quests.getStringList("auto.eligibility.items.custom-item-ids")) {
            if (!itemIds.contains(normalize(id))) error(lines, counts, "quests.yml:auto custom item candidate is missing: " + id);
        }
        info(lines, counts, "quests.yml: automatic quest candidate policy inspected");
    }

    private void checkQuestRange(FileConfiguration config, String path, List<String> lines, int[] counts) {
        int min = config.getInt(path + ".min", -1);
        int max = config.getInt(path + ".max", -1);
        if (min < 1 || max < min) error(lines, counts, "quests.yml:" + path + " has invalid min/max values");
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

    private void checkShops(List<String> lines, int[] counts) {
        FileConfiguration shops = load("shops.yml", lines, counts);
        FileConfiguration items = load("items.yml", lines, counts);
        if (shops == null) return;
        ConfigurationSection shopSection = shops.getConfigurationSection("shops");
        if (shopSection == null) {
            error(lines, counts, "shops.yml: missing shops section");
            return;
        }
        Set<String> itemIds = items == null ? Set.of() : itemIds(items);
        int productCount = 0;
        for (String shopId : shopSection.getKeys(false)) {
            ConfigurationSection products = shops.getConfigurationSection("shops." + shopId + ".items");
            if (products == null) {
                warning(lines, counts, "shops." + shopId + ": missing items section");
                continue;
            }
            Map<Integer, String> orders = new HashMap<>();
            for (String productId : products.getKeys(false)) {
                productCount++;
                String root = "shops." + shopId + ".items." + productId;
                Object rawAmount = shops.get(root + ".amount");
                if (!shops.isSet(root + ".amount")) {
                    ItemStack legacy = shops.getItemStack(root + ".item.serialized");
                    warning(lines, counts, root + ".amount: missing; runtime fallback uses serialized.count"
                            + (legacy == null ? " or 1" : "=" + Math.max(1, legacy.getAmount())));
                } else if (strictAmount(rawAmount) < 1) {
                    error(lines, counts, root + ".amount: expected positive integer, got '" + rawAmount + "'");
                } else if (shops.getLong(root + ".amount", 1L) > Integer.MAX_VALUE) {
                    error(lines, counts, root + ".amount: exceeds integer range");
                }

                long buyPrice = shops.getLong(root + ".buy-price", 0L);
                long sellPrice = shops.getLong(root + ".sell-price", 0L);
                boolean purchasable = shops.getBoolean(root + ".purchasable", false);
                boolean sellable = shops.getBoolean(root + ".sellable", false);
                if (buyPrice < 0L) error(lines, counts, root + ".buy-price: negative value");
                if (sellPrice < 0L) error(lines, counts, root + ".sell-price: negative value");
                if (purchasable && buyPrice <= 0L) error(lines, counts, root + ".buy-price: must be positive when purchasable");
                if (sellable && sellPrice <= 0L) error(lines, counts, root + ".sell-price: must be positive when sellable");

                int order = shops.getInt(root + ".order", productCount - 1);
                String previous = orders.putIfAbsent(order, productId);
                if (previous != null) error(lines, counts, root + ".order: duplicate slot with " + previous);

                ItemStack serialized = shops.getItemStack(root + ".item.serialized");
                if (serialized != null && !serialized.getType().isAir() && serialized.getAmount() != 1) {
                    warning(lines, counts, root + ".item.serialized.count=" + serialized.getAmount()
                            + "; canonical value is amount and serialized count should be 1");
                }

                String type = normalize(shops.getString(root + ".item.type", "vanilla"));
                if (type.equals("custom")) {
                    String customId = normalize(shops.getString(root + ".item.id", ""));
                    if (customId.isBlank() || !itemIds.contains(customId)) {
                        error(lines, counts, root + ".item.id: custom item is not defined in items.yml ('" + customId + "')");
                    }
                } else {
                    String material = shops.getString(root + ".item.material", "");
                    if (material.isBlank() && (serialized == null || serialized.getType().isAir())) {
                        error(lines, counts, root + ".item: missing vanilla material or serialized template");
                    } else if (!material.isBlank()
                            && Material.matchMaterial(material.replace("minecraft:", "").toUpperCase(Locale.ROOT)) == null
                            && (serialized == null || serialized.getType().isAir())) {
                        error(lines, counts, root + ".item.material: unknown Material '" + material + "'");
                    }
                }

                String matchMode = normalize(shops.getString(root + ".match-mode", "MATERIAL"));
                if (!Set.of("material", "custom_id", "exact").contains(matchMode)) {
                    error(lines, counts, root + ".match-mode: unknown mode '" + matchMode + "'");
                }
                String currency = normalize(shops.getString(root + ".currency-item-id", ""));
                if (currency.equals("coin")) {
                    info(lines, counts, root + ".currency-item-id: legacy CoinService alias; migration normalizes it to blank");
                } else if (!currency.isBlank() && !itemIds.contains(currency)) {
                    error(lines, counts, root + ".currency-item-id: item is not defined in items.yml ('" + currency + "')");
                }
            }
        }
        info(lines, counts, "shops: " + productCount + " products inspected");
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
        checkEquipmentSupport(lines, counts);
        File legacy = new File(plugin.getDataFolder(), "enhancements.yml");
        if (legacy.isFile()) {
            warning(lines, counts, "enhancements.yml: legacy file remains in live config root; run /rpg migrate legacy --apply");
        }
    }

    private void checkEquipmentSupport(List<String> lines, int[] counts) {
        FileConfiguration support = load("equipment-support.yml", lines, counts);
        if (support == null) return;
        ConfigurationSection future = support.getConfigurationSection("future-features");
        if (future == null) return;
        for (String id : future.getKeys(false)) {
            boolean enabled = support.getBoolean("future-features." + id + ".enabled", false);
            boolean implemented = support.getBoolean("future-features." + id + ".implemented", false);
            if (enabled && !implemented) warning(lines, counts,
                    "equipment-support.yml: future feature '" + id + "' is enabled but not implemented");
        }
        info(lines, counts, "equipment-support.yml: enchant extraction support settings inspected");
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
        int legacy = 0;
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                total++;
                FileConfiguration data = YamlConfiguration.loadConfiguration(file);
                if (!data.isSet("schema-version")) missingSchema++;
                if (data.isSet("weaponProficiencyLevels") || data.isSet("weaponProficiencyExp")) legacy++;
                if (data.isSet("selectedProfession") || data.isSet("unlockedWorlds") || data.isSet("clearedWorlds")) {
                    warning(lines, counts, "players/" + file.getName() + ": deprecated profession/world-lock fields remain readable but are excluded from new saves");
                }
                if (data.isConfigurationSection("farming")) {
                    if (!data.isSet("farming.version")) warning(lines, counts, "players/" + file.getName() + ": farming.version is missing");
                    if (!data.isString("farming.stage")) warning(lines, counts, "players/" + file.getName() + ": farming.stage is missing or not a string");
                    if (data.get("farming.unlocked-crops") != null && !(data.get("farming.unlocked-crops") instanceof List<?>)) {
                        error(lines, counts, "players/" + file.getName() + ": farming.unlocked-crops must be a list");
                    }
                }
            }
        }
        if (missingSchema > 0) warning(lines, counts, "players: " + missingSchema + "/" + total + " files missing schema-version");
        if (legacy > 0) warning(lines, counts, "players: " + legacy + "/" + total + " files contain legacy flat proficiency fields");
        info(lines, counts, "players: " + total + " player files inspected");
    }

    private void checkFarming(List<String> lines, int[] counts) {
        lines.add("[ConfigDoctor/Farming]");
        Map<String, FileConfiguration> configs = new HashMap<>();
        for (String fileName : FARMING_FILES) {
            String path = "farming/" + fileName;
            File file = new File(plugin.getDataFolder(), path);
            if (!file.isFile()) {
                error(lines, counts, path + ": missing managed farming file");
                continue;
            }
            FileConfiguration config = loadPath(path, lines, counts);
            if (config != null) {
                configs.put(fileName, config);
                if (!config.isSet("schema-version")) warning(lines, counts, path + ": missing schema-version");
                compareResourceContract(path, config, lines, counts, Set.of("schema-version"));
            }
        }
        FileConfiguration crops = configs.get("crops.yml");
        Set<String> cropIds = new HashSet<>();
        if (crops == null || !crops.isConfigurationSection("crops")) {
            error(lines, counts, "farming/crops.yml: missing crops section");
        } else {
            ConfigurationSection section = crops.getConfigurationSection("crops");
            addKeys(cropIds, section);
            for (String crop : List.of("corn", "onion", "chili", "garlic")) {
                if (!cropIds.contains(crop)) error(lines, counts, "farming/crops.yml: missing canonical crop '" + crop + "'");
            }
            FileConfiguration items = load("items.yml", lines, counts);
            Set<String> itemIds = items == null ? Set.of() : itemIds(items);
            for (String crop : cropIds) {
                String seed = normalize(crops.getString("crops." + crop + ".seed-item-id", ""));
                String result = normalize(crops.getString("crops." + crop + ".crop-item-id", ""));
                if (seed.isBlank() || !itemIds.contains(seed)) error(lines, counts, "farming/crops.yml: missing seed item-id '" + seed + "' for " + crop);
                if (result.isBlank() || !itemIds.contains(result)) error(lines, counts, "farming/crops.yml: missing crop item-id '" + result + "' for " + crop);
            }
        }
        FileConfiguration growth = configs.get("growth.yml");
        if (growth == null || !growth.isConfigurationSection("crops")) error(lines, counts, "farming/growth.yml: missing crops growth section");
        FileConfiguration processing = configs.get("processing.yml");
        if (processing == null || !"crafting.yml".equalsIgnoreCase(processing.getString("authoritative-source", ""))) {
            error(lines, counts, "farming/processing.yml: authoritative-source must be crafting.yml");
        }
        FileConfiguration farmingCrafting = load("crafting.yml", lines, counts);
        ConfigurationSection craftingRecipes = farmingCrafting == null
                ? null : farmingCrafting.getConfigurationSection("crafting-recipes");
        if (craftingRecipes != null) {
            for (String recipe : craftingRecipes.getKeys(false)) {
                String type = normalize(farmingCrafting.getString("crafting-recipes." + recipe + ".farming-type", ""));
                if (!type.equals("processing")) continue;
                ConfigurationSection inputs = farmingCrafting.getConfigurationSection("crafting-recipes." + recipe + ".inputs");
                if (inputs == null || inputs.getKeys(false).size() != 1
                        || inputs.getInt(inputs.getKeys(false).iterator().next(), 0) != 20) {
                    error(lines, counts, "crafting.yml: processing recipe must use a 20:1 input ratio: " + recipe);
                }
            }
        }
        FileConfiguration farmingShops = load("shops.yml", lines, counts);
        if (farmingShops == null || !farmingShops.isConfigurationSection("shops.farming.items")) {
            error(lines, counts, "shops.yml: farming seed shop is missing");
        } else {
            for (String crop : List.of("corn", "onion", "chili", "garlic")) {
                String path = "shops.farming.items.seed_" + crop;
                if (!farmingShops.isConfigurationSection(path)
                        || !farmingShops.getBoolean(path + ".purchasable", false)
                        || !crop.equalsIgnoreCase(farmingShops.getString(path + ".required-farming-crop", ""))) {
                    error(lines, counts, "shops.yml: missing gated seed product seed_" + crop);
                }
            }
        }
        FileConfiguration harvest = configs.get("harvest.yml");
        if (harvest == null || harvest.getLong("direct.abundance-points", 0L) < 1L) {
            error(lines, counts, "farming/harvest.yml: direct.abundance-points must be positive; run migrate farming --apply");
        }
        FileConfiguration progression = configs.get("progression.yml");
        if (progression == null || !progression.isConfigurationSection("promotion")) {
            error(lines, counts, "farming/progression.yml: missing promotion section");
        } else {
            ConfigurationSection promotion = progression.getConfigurationSection("promotion");
            for (String stage : promotion.getKeys(false)) {
                String unlock = normalize(progression.getString("promotion." + stage + ".unlock-crop", ""));
                if (!unlock.isBlank() && !cropIds.contains(unlock)) {
                    error(lines, counts, "farming/progression.yml: promotion " + stage + " unlocks unknown crop '" + unlock + "'");
                }
            }
        }
        FileConfiguration quality = configs.get("quality.yml");
        if (quality == null || !quality.isConfigurationSection("quality")) {
            warning(lines, counts, "farming/quality.yml: quality section is missing");
        } else {
            FileConfiguration items = load("items.yml", lines, counts);
            Set<String> itemIds = items == null ? Set.of() : itemIds(items);
            ConfigurationSection mappings = quality.getConfigurationSection("items");
            if (mappings != null) for (String crop : mappings.getKeys(false)) {
                ConfigurationSection levels = mappings.getConfigurationSection(crop);
                if (levels == null) continue;
                for (String level : levels.getKeys(false)) {
                    String itemId = normalize(levels.getString(level, ""));
                    if (!itemId.isBlank() && !itemIds.contains(itemId)) {
                        error(lines, counts, "farming/quality.yml: missing quality item-id '" + itemId + "'");
                    }
                }
            }
            checkQualityDistribution(quality, lines, counts);
        }
        FileConfiguration hoeEnhancement = configs.get("hoe_enhancement.yml");
        if (hoeEnhancement == null || !hoeEnhancement.isConfigurationSection("levels")) {
            error(lines, counts, "farming/hoe_enhancement.yml: missing levels section");
        } else {
            for (String level : hoeEnhancement.getConfigurationSection("levels").getKeys(false)) {
                if (!hoeEnhancement.isSet("levels." + level + ".durability-save-chance")) {
                    error(lines, counts, "farming/hoe_enhancement.yml: missing durability-save-chance at " + level);
                }
                if (!hoeEnhancement.isSet("levels." + level + ".quality-sale-bonus")) {
                    error(lines, counts, "farming/hoe_enhancement.yml: missing quality-sale-bonus at " + level);
                }
                for (String forbidden : List.of("quality-density-shift", "quality-score",
                        "abundance-point-multiplier", "rare-seed-chance")) {
                    if (hoeEnhancement.isSet("levels." + level + "." + forbidden)) {
                        warning(lines, counts, "farming/hoe_enhancement.yml: legacy enhancement key requires migration: " + forbidden);
                    }
                }
            }
            if (!hoeEnhancement.isSet("limits.maximum-quality-sale-bonus")) {
                error(lines, counts, "farming/hoe_enhancement.yml: missing limits.maximum-quality-sale-bonus");
            }
        }
        FileConfiguration hoePromotion = configs.get("hoe_promotion.yml");
        if (hoePromotion == null) {
            error(lines, counts, "farming/hoe_promotion.yml: file is missing");
        } else {
            if (!hoePromotion.isConfigurationSection("tiers")) {
                warning(lines, counts, "farming/hoe_promotion.yml: missing bundled section/key 'tiers'");
            }
            for (String forbidden : List.of("farming-stage-mapping", "levels")) {
                if (hoePromotion.isSet(forbidden)) {
                    warning(lines, counts, "farming/hoe_promotion.yml: legacy numerical promotion section is ignored: " + forbidden);
                }
            }
            ConfigurationSection hoeTiers = hoePromotion.getConfigurationSection("tiers");
            if (hoeTiers != null) {
                for (String tier : hoeTiers.getKeys(false)) {
                    ConfigurationSection stars = hoeTiers.getConfigurationSection(tier);
                    if (stars == null) continue;
                    for (String star : stars.getKeys(false)) {
                        String path = "tiers." + tier + "." + star;
                        for (String required : List.of("quality-density-shift",
                                "abundance-point-multiplier", "rare-seed-chance")) {
                            if (!hoePromotion.isSet(path + "." + required)) {
                                error(lines, counts, "farming/hoe_promotion.yml: missing " + path + "." + required);
                            }
                        }
                    }
                }
            }
            info(lines, counts, "farming/hoe_promotion.yml: item-local fixed tier/star passives; player stage remains profile data");
        }
        FileConfiguration shops = load("shops.yml", lines, counts);
        if (shops == null || !shops.isConfigurationSection("shops.farming")) {
            warning(lines, counts, "shops.yml: farming shop section is missing");
        }
        if (shops != null) compareResourceContract("shops.yml", shops, lines, counts, Set.of("shops"));
        FileConfiguration deliveries = configs.get("deliveries.yml");
        if (deliveries == null || !deliveries.isConfigurationSection("definitions")) {
            error(lines, counts, "farming/deliveries.yml: missing definitions section");
        } else {
            if (deliveries.getLong("refresh-seconds", 0L) <= 0L) {
                error(lines, counts, "farming/deliveries.yml: refresh-seconds must be positive");
            }
            if (deliveries.getLong("time-limit-seconds", 0L) <= 0L) {
                error(lines, counts, "farming/deliveries.yml: time-limit-seconds must be positive");
            }
            if (!deliveries.isConfigurationSection("reward")) {
                warning(lines, counts, "farming/deliveries.yml: missing bundled section/key 'reward'");
            }
            for (String definition : deliveries.getConfigurationSection("definitions").getKeys(false)) {
                String provider = normalize(deliveries.getString("definitions." + definition + ".provider", ""));
                if (!Set.of("farmer", "alchemist", "estate_reserved").contains(provider)) {
                    error(lines, counts, "farming/deliveries.yml: unknown provider '" + provider + "'");
                }
                if (!provider.equals("estate_reserved")
                        && deliveries.getStringList("definitions." + definition + ".item-families").isEmpty()) {
                    error(lines, counts, "farming/deliveries.yml: active definition has no item-families: " + definition);
                }
            }
            info(lines, counts, "farming/deliveries.yml: delivery definitions inspected");
        }
        FileConfiguration essence = configs.get("essence.yml");
        if (essence == null || !essence.getBoolean("enabled", false)) {
            warning(lines, counts, "farming/essence.yml: essence crafting is disabled or missing");
        } else {
            boolean legacyEssenceKeys = essence.isSet("item-id")
                    || essence.isSet("required-farming-stage")
                    || essence.isSet("output-amount")
                    || essence.isSet("tradeable");
            if (legacyEssenceKeys) {
                warning(lines, counts, "farming/essence.yml: legacy essence keys remain; run migrate farming --apply");
            }
            String essenceItem = normalize(firstNonBlank(
                    essence.getString("result-item-id", ""),
                    essence.getString("item-id", "")));
            String requiredStage = normalize(firstNonBlank(
                    essence.getString("unlock-stage", ""),
                    essence.getString("required-farming-stage", "")));
            long requiredPoints = essence.getLong("required-abundance-points", 0L);
            int resultCount = Math.max(1, essence.getInt("result-count",
                    essence.getInt("output-amount", 1)));
            int maxStack = Math.min(64, Math.max(1, essence.getInt("max-stack", 64)));
            FileConfiguration items = load("items.yml", lines, counts);
            Set<String> itemIds = items == null ? Set.of() : itemIds(items);
            if (essenceItem.isBlank() || !itemIds.contains(essenceItem)) {
                error(lines, counts, "farming/essence.yml: missing result-item-id '" + essenceItem + "'");
            }
            if (!requiredStage.equals("expert")) {
                error(lines, counts, "farming/essence.yml: unlock-stage must be expert");
            }
            if (requiredPoints < 1L) {
                error(lines, counts, "farming/essence.yml: required-abundance-points must be positive");
            }
            if (resultCount < 1 || resultCount > maxStack) {
                error(lines, counts, "farming/essence.yml: result-count must be between 1 and max-stack");
            }
            Set<String> usageTags = new HashSet<>();
            for (String tag : essence.getStringList("usage-tags")) usageTags.add(normalize(tag));
            Set<String> requiredUsageTags = Set.of(
                    "alchemy", "stat-token", "elemental-equipment", "endgame-equipment");
            if (!usageTags.containsAll(requiredUsageTags)) {
                error(lines, counts, "farming/essence.yml: usage-tags must include " + requiredUsageTags);
            }
            if (essence.getBoolean("sellable", false)) {
                error(lines, counts, "farming/essence.yml: abundance essence must not be sellable");
            }
            if (essence.getBoolean("reverse-conversion", false)) {
                error(lines, counts, "farming/essence.yml: abundance essence reverse conversion must be disabled");
            }
            if (essence.isSet("material-tag") || essence.isSet("required-material-amount")) {
                error(lines, counts, "farming/essence.yml: legacy supreme-material requirement remains; run migrate farming --apply");
            }
            FileConfiguration crafting = load("crafting.yml", lines, counts);
            String recipePath = "crafting-recipes.abundance_essence";
            if (crafting == null || !crafting.isConfigurationSection(recipePath)) {
                error(lines, counts, "crafting.yml: missing abundance_essence recipe");
            } else {
                long recipePoints = crafting.getLong(recipePath + ".required-abundance-points", 0L);
                if (recipePoints != requiredPoints) {
                    error(lines, counts, "crafting.yml: abundance_essence point requirement does not match farming/essence.yml");
                }
                String recipeItem = normalize(crafting.getString(recipePath + ".output.item-id", ""));
                int recipeCount = Math.max(1, crafting.getInt(recipePath + ".output.amount", 1));
                String recipeStage = normalize(crafting.getString(recipePath + ".required-farming-stage", ""));
                if (!recipeItem.equals(essenceItem) || recipeCount != resultCount
                        || !recipeStage.equals(requiredStage)) {
                    error(lines, counts, "crafting.yml: abundance_essence output does not match farming/essence.yml");
                }
                ConfigurationSection inputs = crafting.getConfigurationSection(recipePath + ".inputs");
                if (!CraftingRecipeRequirements.inputKeys(inputs).isEmpty()) {
                    error(lines, counts, "crafting.yml: abundance_essence must not require item inputs");
                }
            }
        }
        FileConfiguration statTokens = configs.get("stat_tokens.yml");
        if (statTokens == null || !statTokens.isConfigurationSection("tokens")) {
            error(lines, counts, "farming/stat_tokens.yml: missing tokens section");
        } else {
            FileConfiguration items = load("items.yml", lines, counts);
            Set<String> itemIds = items == null ? Set.of() : itemIds(items);
            for (String token : statTokens.getConfigurationSection("tokens").getKeys(false)) {
                String itemId = normalize(statTokens.getString("tokens." + token + ".item-id", ""));
                int maxUses = statTokens.getInt("tokens." + token + ".maximum-uses", 0);
                if (itemId.isBlank() || !itemIds.contains(itemId)) {
                    error(lines, counts, "farming/stat_tokens.yml: missing token item-id '" + itemId + "'");
                }
                if (maxUses < 1) error(lines, counts, "farming/stat_tokens.yml: invalid maximum-uses for " + token);
            }
        }
        info(lines, counts, "farming: managed files=" + FARMING_FILES.size() + ", crops=" + cropIds);
        if (new File(plugin.getDataFolder(), "farming/cooking.yml").isFile()) {
            warning(lines, counts, "farming/cooking.yml: legacy/inactive file is present; Prompt 11 does not load or migrate it");
        }
        checkFarmingPlayerData(lines, counts);
    }

    private void checkQualityDistribution(FileConfiguration quality, List<String> lines, int[] counts) {
        ConfigurationSection distribution = quality.getConfigurationSection("base-distribution");
        if (distribution == null) {
            error(lines, counts, "farming/quality.yml: missing base-distribution");
            return;
        }
        double total = 0.0D;
        for (String key : distribution.getKeys(false)) {
            double value = distribution.getDouble(key, Double.NaN);
            if (!Double.isFinite(value) || value < 0.0D) {
                error(lines, counts, "farming/quality.yml: invalid or negative probability at " + key);
            }
            total += Double.isFinite(value) ? value : 0.0D;
        }
        if (Math.abs(total - 100.0D) > 0.000001D) {
            error(lines, counts, "farming/quality.yml: base-distribution total must be 100 (actual=" + total + ")");
        }
    }

    private void checkAlchemy(List<String> lines, int[] counts) {
        FileConfiguration effects = load("alchemy/effects.yml", lines, counts);
        FileConfiguration components = load("alchemy/components.yml", lines, counts);
        FileConfiguration conflicts = load("alchemy/conflicts.yml", lines, counts);
        FileConfiguration scaling = load("alchemy/scaling.yml", lines, counts);
        FileConfiguration abundance = load("alchemy/abundance.yml", lines, counts);
        FileConfiguration potions = load("alchemy/potions.yml", lines, counts);
        FileConfiguration recipes = load("alchemy/recipes.yml", lines, counts);
        if (effects == null || components == null || conflicts == null || scaling == null
                || abundance == null || potions == null || recipes == null) return;
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
                + ", abundance=" + abundance.getBoolean("enabled", false));
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

    private void checkFarmingPlayerData(List<String> lines, int[] counts) {
        File directory = new File(plugin.getDataFolder(), "players");
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(file);
            if (!data.isConfigurationSection("farming")) {
                warning(lines, counts, "players/" + file.getName() + ": farming profile missing; migration required");
                continue;
            }
            int version = data.getInt("farming.version", 1);
            if (version < 3) warning(lines, counts, "players/" + file.getName() + ": farming.version=" + version + " (migration required)");
            long points = data.getLong("farming.abundance-points", 0L);
            if (points < 0L) error(lines, counts, "players/" + file.getName() + ": abundance-points is negative");
            ConfigurationSection favor = data.getConfigurationSection("farming.favor");
            if (favor != null) {
                for (String provider : favor.getKeys(false)) {
                    long value = favor.getLong(provider, 0L);
                    if (value < 0L) error(lines, counts, "players/" + file.getName() + ": negative favor for " + provider);
                    FileConfiguration favorConfig = load("farming/favor.yml", lines, counts);
                    long max = favorConfig == null ? Long.MAX_VALUE
                            : favorConfig.getLong("providers." + provider + ".max-favor", Long.MAX_VALUE);
                    if (value > max) warning(lines, counts, "players/" + file.getName() + ": favor exceeds configured cap for " + provider);
                }
            }
            ConfigurationSection deliveries = data.getConfigurationSection("farming.deliveries");
            if (deliveries != null) {
                for (String provider : deliveries.getKeys(false)) {
                    String id = data.getString("farming.deliveries." + provider + ".active-delivery-id", "");
                    String status = normalize(data.getString("farming.deliveries." + provider + ".status", "active"));
                    long expires = data.getLong("farming.deliveries." + provider + ".expires-at", 0L);
                    if (!id.isBlank() && !Set.of("active", "completed", "expired").contains(status)) {
                        error(lines, counts, "players/" + file.getName() + ": invalid delivery status for " + provider);
                    }
                    if (status.equals("active") && !id.isBlank() && expires <= 0L) {
                        warning(lines, counts, "players/" + file.getName() + ": active delivery has invalid expires-at for " + provider);
                    }
                }
            }
        }
    }

    private void checkReloadReadiness(List<String> lines, int[] counts) {
        lines.add("[Reload readiness]");
        int groups = 0;
        groups += reloadGroup("items", lines, counts, this::checkItems);
        groups += reloadGroup("progression-loop", lines, counts, this::checkProgression);
        groups += reloadGroup("farming", lines, counts, this::checkFarming);
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

    private void checkProgression(List<String> lines, int[] counts) {
        FileConfiguration progression = load("progression-loop.yml", lines, counts);
        if (progression == null) return;
        ConfigurationSection stages = progression.getConfigurationSection("stages");
        if (stages == null) {
            error(lines, counts, "progression-loop.yml: missing stages section");
            return;
        }
        if (stages.isConfigurationSection("dungeon")) error(lines, counts, "progression-loop.yml: deprecated dungeon stage remains");
        if (stages.isConfigurationSection("hunting_ground")) error(lines, counts, "progression-loop.yml: deprecated hunting-ground stage remains");
        if (!stages.isConfigurationSection("boss")) warning(lines, counts, "progression-loop.yml: boss stage is missing");
        if (!stages.isConfigurationSection("crafting")) error(lines, counts, "progression-loop.yml: crafting stage is missing");
        Set<String> stageRecipes = new HashSet<>();
        for (String stage : stages.getKeys(false)) {
            stageRecipes.addAll(progression.getStringList("stages." + stage + ".recipes")
                    .stream().map(this::normalize).toList());
        }
        FileConfiguration crafting = load("crafting.yml", lines, counts);
        ConfigurationSection recipes = crafting == null ? null : crafting.getConfigurationSection("crafting-recipes");
        FileConfiguration items = load("items.yml", lines, counts);
        for (String recipeId : stageRecipes) {
            if (recipes == null || recipes.getKeys(false).stream().noneMatch(id -> id.equalsIgnoreCase(recipeId))) {
                error(lines, counts, "progression-loop.yml: unknown recipe '" + recipeId + "'");
            }
        }
        Set<String> progressionItems = items == null ? Set.of() : itemIds(items);
        for (String stage : stages.getKeys(false)) {
            checkProgressionItemMaps(progression, "stages." + stage + ".inputs", progressionItems, lines, counts);
            checkProgressionItemMaps(progression, "stages." + stage + ".outputs", progressionItems, lines, counts);
        }
        ConfigurationSection sources = progression.getConfigurationSection("sources");
        if (sources != null) for (String source : sources.getKeys(false)) {
            checkProgressionItemMaps(progression, "sources." + source + ".outputs", progressionItems, lines, counts);
        }
        String fragment = normalize(progression.getString("access-materials.fragment-item-id", ""));
        if (!fragment.isBlank() && !progressionItems.contains(fragment)) {
            error(lines, counts, "progression-loop.yml: missing item reference '" + fragment + "'");
        }
        String magicFragment = normalize(progression.getString("magic-stone-fragments.item-id", ""));
        if (!magicFragment.isBlank() && !progressionItems.contains(magicFragment)) {
            error(lines, counts, "progression-loop.yml: missing item reference '" + magicFragment + "'");
        }
        for (String stage : stages.getKeys(false)) {
            for (String next : stages.getStringList(stage + ".next")) {
                if (next.equalsIgnoreCase("dungeon") || next.equalsIgnoreCase("hunting_ground")) {
                    error(lines, counts, "progression-loop.yml: deprecated stage reference " + stage + ".next -> " + next);
                }
                if (!stages.isConfigurationSection(next)) warning(lines, counts, "progression-loop.yml: " + stage + ".next references missing stage '" + next + "'");
            }
        }
        if (!progression.isConfigurationSection("magic-stone-fragments")) warning(lines, counts, "progression-loop.yml: magic-stone-fragments section is missing");
    }

    private void checkProgressionItemMaps(FileConfiguration progression, String path, Set<String> itemIds,
                                          List<String> lines, int[] counts) {
        for (Map<?, ?> entry : progression.getMapList(path)) {
            String type = normalize(String.valueOf(entry.containsKey("type") ? entry.get("type") : ""));
            if (!type.equals("item")) continue;
            String id = normalize(String.valueOf(entry.containsKey("id") ? entry.get("id") : ""));
            if (!id.isBlank() && !itemIds.contains(id)) {
                error(lines, counts, "progression-loop.yml: missing item reference '" + id + "' at " + path);
            }
        }
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
