package com.hyunseo.hyunseorpg.crafting;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemData;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Canonical recipe registry. YAML recipes and enabled special-equipment recipes share this snapshot. */
public final class CraftingRecipeRegistry {
    private final ConfigService config;
    private final RPGItemService itemService;
    private Map<String, CraftingRecipeData> recipes = Map.of();
    private Map<String, String> specialRecipeIds = Map.of();
    private List<String> lastErrors = List.of();

    public CraftingRecipeRegistry(ConfigService config, RPGItemService itemService) {
        this.config = config;
        this.itemService = itemService;
    }

    public boolean load() {
        lastErrors = new ArrayList<>();
        try {
            Snapshot snapshot = parseSnapshot();
            if (snapshot.configuredEntries() > 0 && snapshot.recipes().isEmpty()) {
                config.getPlugin().getLogger().severe("Crafting registry has no valid recipes after configuration validation.");
                lastErrors = List.of("No valid recipes remain after configuration validation");
                return false;
            }
            recipes = snapshot.recipes();
            specialRecipeIds = snapshot.specialRecipeIds();
            if (snapshot.skippedEntries() > 0) {
                config.getPlugin().getLogger().warning(
                        "Crafting registry loaded with " + snapshot.skippedEntries() + " invalid recipe(s) skipped.");
            }
            return true;
        } catch (IllegalArgumentException exception) {
            lastErrors = List.of(exception.getMessage() == null ? "Invalid crafting configuration" : exception.getMessage());
            config.getPlugin().getLogger().warning("Crafting registry reload rejected: " + exception.getMessage());
            return false;
        }
    }

    public List<String> lastErrors() {
        return List.copyOf(lastErrors);
    }

    public List<CraftingRecipeData> getAll() {
        return recipes.values().stream().sorted(Comparator.comparing(CraftingRecipeData::id)).toList();
    }

    public List<CraftingRecipeData> getAll(CraftingCategory category) {
        String categoryId = category == null ? "" : category.configId();
        return getAll().stream().filter(recipe -> recipe.enabled()
                && recipe.categoryId().equalsIgnoreCase(categoryId)).toList();
    }

    public Optional<CraftingRecipeData> get(String rawId) {
        return Optional.ofNullable(recipes.get(normalize(rawId)));
    }

    /**
     * Returns the canonical runtime definition used by every crafting surface.
     * This is intentionally generic: diagnostics must not grow recipe-specific
     * branches as new YAML recipes are added.
     */
    public RecipeTrace trace(String rawId) {
        String id = normalize(rawId);
        CraftingRecipeData recipe = recipes.get(id);
        if (recipe == null) return new RecipeTrace(id, false, "", List.of(), "", 0);
        return new RecipeTrace(id, true, recipe.categoryId(), recipe.ingredients(),
                recipe.outputId(), recipe.outputAmount());
    }

    public Optional<String> specialRecipeId(String specialEquipmentId) {
        return Optional.ofNullable(specialRecipeIds.get(normalize(specialEquipmentId)));
    }

    public Optional<String> specialEquipmentIdForRecipe(String recipeId) {
        String normalized = normalize(recipeId);
        return specialRecipeIds.entrySet().stream().filter(entry -> entry.getValue().equals(normalized))
                .map(Map.Entry::getKey).findFirst();
    }

    private Snapshot parseSnapshot() {
        Map<String, CraftingRecipeData> loaded = new LinkedHashMap<>();
        int configuredEntries = 0;
        int skippedEntries = 0;
        for (String rawId : config.getCraftingKeys("crafting-recipes")) {
            configuredEntries++;
            String path = "crafting-recipes." + rawId;
            try {
                CraftingRecipeData recipe = readRecipe(rawId, path, null);
                if (!recipe.enabled()) continue;
                if (recipe.categoryId().equalsIgnoreCase("cooking")) {
                    config.getPlugin().getLogger().info(
                            "Ignoring retired cooking recipe from crafting.yml: " + recipe.id());
                    continue;
                }
                putUnique(loaded, recipe, path);
            } catch (IllegalArgumentException exception) {
                skippedEntries++;
                lastErrors.add("recipe " + normalize(rawId) + ": " + exception.getMessage());
                config.getPlugin().getLogger().warning(
                        "Skipped invalid crafting recipe: id=" + normalize(rawId) + ", reason=" + exception.getMessage());
            }
        }

        Map<String, String> specialIds = new LinkedHashMap<>();
        ConfigurationSection specialItems = config.getSpecialEquipmentSection("special-equipment.items");
        if (specialItems != null) {
            for (String rawSpecialId : specialItems.getKeys(false)) {
                ConfigurationSection section = specialItems.getConfigurationSection(rawSpecialId);
                if (section == null || !section.getBoolean("enabled", true)
                        || !section.getBoolean("crafting.enabled", true)) continue;
                configuredEntries++;
                String specialId = normalize(rawSpecialId);
                try {
                    CraftingRecipeData generated = readRecipe("special_" + specialId,
                            "special-equipment.items." + rawSpecialId + ".crafting", CraftingCategory.SPECIAL.configId());
                    String existingId = findEquivalentRecipe(loaded, generated);
                    if (existingId != null) {
                        specialIds.put(specialId, existingId);
                        continue;
                    }
                    putUnique(loaded, generated, "special-equipment.items." + rawSpecialId);
                    specialIds.put(specialId, generated.id());
                } catch (IllegalArgumentException exception) {
                    skippedEntries++;
                    config.getPlugin().getLogger().warning(
                            "Skipped invalid special-equipment recipe: id=" + specialId
                                    + ", reason=" + exception.getMessage());
                }
            }
        }

        return new Snapshot(Map.copyOf(loaded), Map.copyOf(specialIds), configuredEntries, skippedEntries);
    }

    private CraftingRecipeData readRecipe(String rawId, String path, String forcedCategory) {
        String id = normalize(rawId);
        String outputId;
        int outputAmount;
        ConfigurationSection inputs;
        boolean enabled;
        String category;
        if (forcedCategory == null) {
            outputId = normalize(config.getCraftingString(path + ".output.item-id", ""));
            outputAmount = positive(config.getCraftingSection(path + ".output") == null
                    ? null : config.getCraftingSection(path + ".output").get("amount"), path + ".output.amount");
            inputs = config.getCraftingSection(path + ".inputs");
            enabled = config.getCraftingBoolean(path + ".enabled", true);
            category = categoryFor(id, outputId, config.getCraftingString(path + ".category", ""));
        } else {
            outputId = normalize(config.getSpecialEquipmentString(path.substring(0, path.lastIndexOf(".crafting")) + ".item-id", ""));
            outputAmount = positive(config.getSpecialEquipmentSection(path) == null
                    ? null : config.getSpecialEquipmentSection(path).get("amount"), path + ".amount");
            inputs = config.getSpecialEquipmentSection(path + ".inputs");
            enabled = config.getSpecialEquipmentBoolean(path.substring(0, path.lastIndexOf(".crafting")) + ".enabled", true)
                    && config.getSpecialEquipmentBoolean(path + ".enabled", true);
            category = normalize(forcedCategory);
        }
        if (id.isBlank()) throw new IllegalArgumentException(path + ": blank recipe id");
        if (outputId.isBlank()) throw new IllegalArgumentException(path + ": missing output item-id");
        validateItem(outputId, path + ".output.item-id");
        List<String> inputKeys = CraftingRecipeRequirements.inputKeys(inputs).stream().toList();
        boolean hasInputs = !inputKeys.isEmpty();
        if (!hasInputs) throw new IllegalArgumentException(path + ": recipe has no inputs");
        List<CraftingRecipeData.Ingredient> ingredients = new ArrayList<>();
        for (String rawIngredient : inputKeys) {
            String ingredient = normalize(rawIngredient);
            if (ingredient.isBlank()) throw new IllegalArgumentException(path + ".inputs: blank item id");
            ingredients.add(new CraftingRecipeData.Ingredient(ingredient, positive(inputs.get(rawIngredient),
                    path + ".inputs." + rawIngredient)));
            validateItem(ingredient, path + ".inputs." + rawIngredient);
        }
        return new CraftingRecipeData(id, enabled, category, ingredients, outputId, outputAmount);
    }

    private String categoryFor(String recipeId, String outputId, String explicit) {
        String configured = normalize(explicit);
        if (!configured.isBlank()) return canonicalCategoryId(configured);

        // Legacy category lists are read only as a compatibility fallback. New
        // definitions declare their category directly on the physical recipe.
        for (String category : configuredCategoryIds()) {
            if (config.getCraftingStringList("crafting.categories." + category).stream()
                    .anyMatch(value -> value.equalsIgnoreCase(recipeId))) return category;
        }

        RPGItemData item = itemService.getData(outputId).orElse(null);
        if (item == null) return CraftingCategory.MATERIALS.configId();
        String itemCategory = item.category().toUpperCase(Locale.ROOT);
        if (itemCategory.contains("SPECIAL")) return CraftingCategory.SPECIAL.configId();
        if (itemCategory.contains("CONSUMABLE") || itemCategory.contains("BUFF")) {
            return CraftingCategory.CONSUMABLES.configId();
        }
        if (itemCategory.contains("EQUIPMENT") || itemCategory.contains("WEAPON") || itemCategory.contains("ARMOR")) {
            return CraftingCategory.EQUIPMENT.configId();
        }
        return CraftingCategory.MATERIALS.configId();
    }

    private List<String> configuredCategoryIds() {
        return java.util.Arrays.stream(CraftingCategory.values())
                .map(CraftingCategory::configId)
                .toList();
    }

    private String canonicalCategoryId(String rawCategory) {
        CraftingCategory alias = CraftingCategory.fromConfig(rawCategory, null);
        return alias == null ? normalize(rawCategory) : alias.configId();
    }

    private void validateItem(String id, String path) {
        if (id.startsWith("tag:")) {
            String tag = id.substring("tag:".length());
            if (tag.isBlank()) {
                throw new IllegalArgumentException(path + ": blank item tag");
            }
            if (itemService.getAllData().stream().noneMatch(item -> item.tags().contains(tag))) {
                throw new IllegalArgumentException(path + ": unknown item tag " + tag);
            }
            return;
        }
        if (id.startsWith("vanilla:")) {
            if (Material.matchMaterial(id.substring("vanilla:".length()).toUpperCase(Locale.ROOT)) == null) {
                throw new IllegalArgumentException(path + ": unknown vanilla material " + id);
            }
            return;
        }
        if (itemService.create(id, 1).isEmpty()) throw new IllegalArgumentException(path + ": unknown item " + id);
    }

    private void putUnique(Map<String, CraftingRecipeData> target, CraftingRecipeData recipe, String path) {
        if (target.putIfAbsent(recipe.id(), recipe) != null) {
            throw new IllegalArgumentException(path + ": duplicate recipe id " + recipe.id());
        }
    }

    private String findEquivalentRecipe(Map<String, CraftingRecipeData> recipes, CraftingRecipeData target) {
        return recipes.values().stream().filter(existing -> existing.outputId().equals(target.outputId())
                && existing.outputAmount() == target.outputAmount() && existing.ingredients().equals(target.ingredients()))
                .map(CraftingRecipeData::id).findFirst().orElse(null);
    }

    private int positive(Object value, String path) {
        if (value instanceof Number number && number.doubleValue() >= 1D && number.doubleValue() == number.intValue()) {
            return number.intValue();
        }
        if (value instanceof String text && text.matches("[1-9][0-9]*")) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) { }
        }
        throw new IllegalArgumentException(path + ": expected positive integer");
    }

    private long nonNegativeLong(Object value, String path) {
        if (value == null) return 0L;
        if (value instanceof Number number && number.doubleValue() >= 0D
                && number.doubleValue() == number.longValue()) {
            return number.longValue();
        }
        if (value instanceof String text && text.matches("[0-9]+")) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) { }
        }
        throw new IllegalArgumentException(path + ": expected non-negative long");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record Snapshot(Map<String, CraftingRecipeData> recipes,
                            Map<String, String> specialRecipeIds,
                            int configuredEntries,
                            int skippedEntries) { }

    public record RecipeTrace(String id, boolean registered, String categoryId,
                              List<CraftingRecipeData.Ingredient> ingredients,
                              String outputId, int outputAmount) { }
}
