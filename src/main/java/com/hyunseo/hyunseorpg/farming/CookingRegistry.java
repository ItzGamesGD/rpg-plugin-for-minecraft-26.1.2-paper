package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.crafting.CraftingRecipeData;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Registry and runtime adapter for YAML-defined cooking recipes. */
public final class CookingRegistry {
    public static final int DATA_VERSION = 1;

    private final ConfigService config;
    private final RPGItemService items;
    private final CropQualityService cropQuality;
    private Map<String, CookingRecipe> recipes = Map.of();
    private Map<CropQuality, Integer> qualityScores = Map.of();
    private List<String> lastErrors = List.of();
    private boolean enabled;
    private boolean bulkCraftingEnabled;

    public CookingRegistry(ConfigService config, RPGItemService items, CropQualityService cropQuality) {
        this.config = config;
        this.items = items;
        this.cropQuality = cropQuality;
    }

    public boolean load() {
        Map<String, CookingRecipe> loaded = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        enabled = config.getFarmingCookingBoolean("enabled", true);
        bulkCraftingEnabled = config.getFarmingCookingBoolean("bulk-crafting-enabled", false);
        Map<CropQuality, Integer> loadedScores = new EnumMap<>(CropQuality.class);
        for (CropQuality quality : CropQuality.values()) {
            int fallback = quality.ordinal();
            int score = config.getFarmingCookingInt("quality-scores." + quality.id(), fallback);
            loadedScores.put(quality, Math.max(0, score));
        }

        ConfigurationSection section = config.getFarmingCookingSection("recipes");
        if (section == null) {
            lastErrors = List.of("farming/cooking.yml: missing recipes section");
            return false;
        }
        for (String rawId : section.getKeys(false)) {
            String id = normalize(rawId);
            try {
                CookingRecipe recipe = readRecipe(rawId, section.getConfigurationSection(rawId), loadedScores);
                if (recipe.enabled()) {
                    if (loaded.putIfAbsent(id, recipe) != null) {
                        throw new IllegalArgumentException("duplicate recipe id " + id);
                    }
                }
            } catch (IllegalArgumentException exception) {
                errors.add("recipe " + id + ": " + exception.getMessage());
            }
        }
        if (enabled && section.getKeys(false).size() > 0 && loaded.isEmpty()) {
            errors.add("no valid cooking recipes remain");
        }
        if (!errors.isEmpty()) {
            lastErrors = List.copyOf(errors);
            errors.forEach(error -> config.getPlugin().getLogger().warning("Cooking registry rejected: " + error));
            return false;
        }
        recipes = Map.copyOf(loaded);
        qualityScores = Map.copyOf(loadedScores);
        lastErrors = List.of();
        return true;
    }

    public List<String> lastErrors() { return List.copyOf(lastErrors); }

    public List<CookingRecipe> getAll() {
        return recipes.values().stream().sorted(Comparator.comparing(CookingRecipe::id)).toList();
    }

    public Optional<CookingRecipe> get(String rawId) {
        return Optional.ofNullable(recipes.get(normalize(rawId)));
    }

    public boolean isCookingRecipe(String rawId) {
        return recipes.containsKey(normalize(rawId));
    }

    public boolean bulkEnabled(String rawId) {
        CookingRecipe recipe = recipes.get(normalize(rawId));
        return recipe != null && bulkCraftingEnabled && recipe.bulkCraftingEnabled();
    }

    public List<CraftingRecipeData> asCraftingRecipes() {
        return getAll().stream().map(recipe -> new CraftingRecipeData(
                recipe.id(), recipe.enabled(), recipe.categoryId(),
                recipe.ingredients().entrySet().stream()
                        .map(entry -> new CraftingRecipeData.Ingredient(entry.getKey(), entry.getValue()))
                        .toList(),
                recipe.outputFor(CropQuality.NORMAL), recipe.outputAmount())).toList();
    }

    /** Returns a dynamic output only for cooking recipes; other crafting stays on its normal factory. */
    public Optional<ItemStack> createOutputIfCooking(Player player, CraftingRecipeData recipe) {
        if (player == null || recipe == null) return Optional.empty();
        CookingRecipe cooking = recipes.get(normalize(recipe.id()));
        if (cooking == null) return Optional.empty();
        return createOutput(cooking, player.getInventory().getStorageContents());
    }

    public Optional<ItemStack> createPreviewOutput(Player player, CraftingRecipeData recipe) {
        return createOutputIfCooking(player, recipe);
    }

    public Optional<ItemStack> createOutputForInputs(String rawRecipeId, ItemStack[] inputs) {
        CookingRecipe recipe = recipes.get(normalize(rawRecipeId));
        return recipe == null ? Optional.empty() : createOutput(recipe, inputs);
    }

    public Optional<CookingRecipe> recipe(String rawRecipeId) {
        return get(rawRecipeId);
    }

    public int countIngredient(ItemStack[] storage, String ingredient) {
        if (storage == null || ingredient == null || ingredient.isBlank()) return 0;
        int total = 0;
        for (ItemStack item : storage) {
            if (matchesIngredient(item, ingredient)) total += item.getAmount();
        }
        return total;
    }

    public boolean canCraft(CookingRecipe recipe, ItemStack[] storage) {
        if (recipe == null || storage == null) return false;
        return recipe.ingredients().entrySet().stream()
                .allMatch(entry -> countIngredient(storage, entry.getKey()) >= entry.getValue());
    }

    /** Consumes one recipe from a caller-owned copy after canCraft has passed. */
    public void consumeOne(CookingRecipe recipe, ItemStack[] storage) {
        if (!canCraft(recipe, storage)) throw new IllegalStateException("Cooking input changed");
        for (Map.Entry<String, Integer> ingredient : recipe.ingredients().entrySet()) {
            int remaining = ingredient.getValue();
            for (int slot = 0; slot < storage.length && remaining > 0; slot++) {
                ItemStack item = storage[slot];
                if (!matchesIngredient(item, ingredient.getKey())) continue;
                int used = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - used);
                if (item.getAmount() <= 0) storage[slot] = null;
                remaining -= used;
            }
            if (remaining > 0) throw new IllegalStateException("Cooking input plan changed");
        }
    }

    public Optional<CropQuality> resolveQuality(ItemStack[] storage, CookingRecipe recipe) {
        if (storage == null || recipe == null) return Optional.of(CropQuality.NORMAL);
        List<CookingQualityCalculator.Contribution> contributions = new ArrayList<>();
        for (Map.Entry<String, Integer> ingredient : recipe.ingredients().entrySet()) {
            if (ingredient.getKey().startsWith("vanilla:")) continue;
            int remaining = ingredient.getValue();
            for (ItemStack item : storage) {
                if (remaining <= 0) break;
                if (!matchesIngredient(item, ingredient.getKey())) continue;
                String itemId = items.getItemId(item).orElse("");
                CropQuality quality = cropQuality.qualityOfItem(itemId).orElse(null);
                if (quality == null) continue;
                int used = Math.min(remaining, item.getAmount());
                contributions.add(new CookingQualityCalculator.Contribution(
                        qualityScores.getOrDefault(quality, quality.ordinal()), used));
                remaining -= used;
            }
        }
        int average = CookingQualityCalculator.floorAverage(contributions);
        return Optional.of(qualityForScore(average));
    }

    private Optional<ItemStack> createOutput(CookingRecipe recipe, ItemStack[] storage) {
        CropQuality quality = resolveQuality(storage, recipe).orElse(CropQuality.NORMAL);
        String itemId = recipe.outputFor(quality);
        if (itemId == null || itemId.isBlank()) return Optional.empty();
        return items.create(itemId, recipe.outputAmount());
    }

    private CookingRecipe readRecipe(String rawId, ConfigurationSection section,
                                     Map<CropQuality, Integer> scores) {
        String path = "farming/cooking.yml recipes." + rawId;
        if (section == null) throw new IllegalArgumentException(path + ": missing section");
        String id = normalize(rawId);
        String category = normalize(section.getString("category", "cooking"));
        ConfigurationSection ingredientsSection = section.getConfigurationSection("ingredients");
        if (ingredientsSection == null || ingredientsSection.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException(path + ": missing ingredients");
        }
        Map<String, Integer> ingredients = new LinkedHashMap<>();
        for (String rawIngredient : ingredientsSection.getKeys(false)) {
            String ingredient = normalize(rawIngredient);
            int amount = positive(ingredientsSection.get(rawIngredient), path + ".ingredients." + rawIngredient);
            validateItem(ingredient, path + ".ingredients." + rawIngredient);
            ingredients.put(ingredient, amount);
        }
        ConfigurationSection outputsSection = section.getConfigurationSection("outputs");
        if (outputsSection == null) throw new IllegalArgumentException(path + ": missing outputs");
        Map<CropQuality, String> outputs = new EnumMap<>(CropQuality.class);
        for (CropQuality quality : CropQuality.values()) {
            String output = normalize(outputsSection.getString(quality.id(), ""));
            if (output.isBlank()) throw new IllegalArgumentException(path + ".outputs." + quality.id() + ": missing item id");
            validateItem(output, path + ".outputs." + quality.id());
            outputs.put(quality, output);
        }
        return new CookingRecipe(id, section.getBoolean("enabled", true), category, ingredients, outputs,
                positive(section.get("output-amount", 1), path + ".output-amount"),
                section.getBoolean("bulk-crafting-enabled", false), section.getBoolean("sellable", false),
                section.getString("consumption-effect-id", ""), section.getStringList("quest-tags"),
                section.getStringList("territory-tags"));
    }

    private void validateItem(String id, String path) {
        if (id.startsWith("vanilla:")) {
            if (Material.matchMaterial(id.substring("vanilla:".length()).toUpperCase(Locale.ROOT)) == null) {
                throw new IllegalArgumentException(path + ": unknown vanilla material " + id);
            }
            return;
        }
        if (items.create(id, 1).isEmpty()) throw new IllegalArgumentException(path + ": unknown item " + id);
    }

    public boolean matchesIngredient(ItemStack item, String ingredient) {
        if (item == null || item.getType().isAir()) return false;
        if (!ingredient.startsWith("vanilla:")) {
            if (items.isItem(item, ingredient)) return true;
            if (ingredient.contains("_quality_")) return false;
            String actualId = items.getItemId(item).orElse("");
            return cropQuality.cropOfItem(ingredient).isPresent()
                    && cropQuality.cropOfItem(actualId).equals(cropQuality.cropOfItem(ingredient));
        }
        if (items.getItemId(item).isPresent()) return false;
        Material material = Material.matchMaterial(ingredient.substring("vanilla:".length()).toUpperCase(Locale.ROOT));
        return material != null && item.getType() == material;
    }

    private CropQuality qualityForScore(int score) {
        return qualityScores.entrySet().stream()
                .filter(entry -> entry.getValue() <= score)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(CropQuality.NORMAL);
    }

    private int positive(Object value, String path) {
        if (value instanceof Number number && number.doubleValue() >= 1D && number.doubleValue() == number.intValue()) {
            return number.intValue();
        }
        if (value instanceof String text && text.matches("[1-9][0-9]*")) return Integer.parseInt(text);
        throw new IllegalArgumentException(path + ": expected positive integer");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
