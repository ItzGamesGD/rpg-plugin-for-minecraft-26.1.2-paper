package com.hyunseo.hyunseorpg.crafting;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.ui.KoreanDisplay;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Canonical persisted recipe placement, including configured future categories. */
public final class CraftingLayoutRegistry {
    public static final int CONTENT_SLOTS_PER_PAGE = 45;

    private final ConfigService config;
    private Map<String, Map<String, Integer>> layouts = Map.of();
    private List<String> lastErrors = List.of();

    public CraftingLayoutRegistry(ConfigService config) {
        this.config = config;
    }

    public boolean reload(CraftingRecipeRegistry recipes) {
        Validation validation = parse(recipes);
        validation.skippedEntries().forEach(entry -> config.getPlugin().getLogger().warning(
                entry.endsWith("(unknown recipe)")
                        ? "Skipped unknown crafting recipe: " + entry.substring(0, entry.length() - " (unknown recipe)".length())
                        : "Skipped invalid crafting layout entry: " + entry));
        if (!validation.errors().isEmpty()) {
            lastErrors = validation.errors();
            validation.errors().forEach(error -> config.getPlugin().getLogger().warning("Crafting layout reload rejected: " + error));
            return false;
        }
        layouts = validation.layouts();
        lastErrors = List.of();
        if (!validation.skippedEntries().isEmpty()) {
            config.getPlugin().getLogger().warning("Crafting layout loaded with "
                    + validation.skippedEntries().size() + " invalid entries skipped.");
        }
        return true;
    }

    public List<String> lastErrors() { return List.copyOf(lastErrors); }

    public String displayName(String rawCategory) {
        String category = normalize(rawCategory);
        String canonical = KoreanDisplay.craftingCategory(category);
        if (!canonical.isBlank()) return canonical;
        String configured = config.getCraftingString("crafting.menu-categories." + category + ".display-name", "");
        if (!configured.isBlank()) return configured;
        return category;
    }

    public Material icon(String rawCategory) {
        String value = config.getCraftingString("crafting.menu-categories." + normalize(rawCategory) + ".icon", "CHEST");
        Material material = Material.matchMaterial(value);
        return material == null || !material.isItem() ? Material.CHEST : material;
    }

    public int menuSlot(String rawCategory, int fallback) {
        int slot = config.getCraftingInt("crafting.menu-categories." + normalize(rawCategory) + ".slot", fallback);
        return slot < 0 ? fallback : slot;
    }

    public Set<String> categoryIds() {
        Set<String> result = new LinkedHashSet<>();
        result.addAll(layouts.keySet());
        result.addAll(config.getCraftingKeys("crafting.menu-categories"));
        result.addAll(config.getCraftingKeys("crafting.categories"));
        return Set.copyOf(result);
    }

    public Validation validate(CraftingCategory category, CraftingRecipeRegistry recipes,
                               Map<String, Integer> positions) {
        return validate(category == null ? "" : category.configId(), recipes, positions);
    }

    public Validation validate(String rawCategory, CraftingRecipeRegistry recipes,
                               Map<String, Integer> positions) {
        Map<String, Map<String, Integer>> candidate = copyLayouts();
        candidate.put(normalize(rawCategory), normalizePositions(positions));
        return validate(recipes, candidate);
    }

    public boolean save(CraftingRecipeRegistry recipes, CraftingCategory category,
                        Map<String, Integer> positions) {
        return save(recipes, category == null ? "" : category.configId(), positions);
    }

    public boolean save(CraftingRecipeRegistry recipes, String rawCategory,
                        Map<String, Integer> positions) {
        Validation validation = validate(rawCategory, recipes, positions);
        if (!validation.valid()) return false;
        if (!config.replaceCraftingLayout(validation.layouts())) return false;
        layouts = validation.layouts();
        return true;
    }

    public Map<String, Integer> positions(CraftingCategory category) {
        return positions(category == null ? "" : category.configId());
    }

    public Map<String, Integer> positions(String rawCategory) {
        return layouts.getOrDefault(normalize(rawCategory), Map.of());
    }

    public Map<Integer, String> pageEntries(CraftingCategory category, int page) {
        return pageEntries(category == null ? "" : category.configId(), page);
    }

    public Map<Integer, String> pageEntries(String rawCategory, int page) {
        int from = Math.max(0, page) * CONTENT_SLOTS_PER_PAGE;
        Map<Integer, String> result = new HashMap<>();
        positions(rawCategory).forEach((recipeId, position) -> {
            if (position >= from && position < from + CONTENT_SLOTS_PER_PAGE) result.put(position - from, recipeId);
        });
        return Map.copyOf(result);
    }

    /** Resolves a visible content slot from the canonical persisted layout. */
    public Optional<String> recipeAt(String rawCategory, int page, int slot) {
        if (slot < 0 || slot >= CONTENT_SLOTS_PER_PAGE) return Optional.empty();
        int absolute = Math.max(0, page) * CONTENT_SLOTS_PER_PAGE + slot;
        return positions(rawCategory).entrySet().stream()
                .filter(entry -> entry.getValue() == absolute)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public int pageCount(CraftingCategory category) {
        return pageCount(category == null ? "" : category.configId());
    }

    public int pageCount(String rawCategory) {
        int last = positions(rawCategory).values().stream().mapToInt(Integer::intValue).max().orElse(-1);
        return Math.max(1, last / CONTENT_SLOTS_PER_PAGE + 1);
    }

    public List<String> unplacedEnabledRecipes(CraftingRecipeRegistry recipes) {
        Set<String> placed = new HashSet<>();
        layouts.values().forEach(map -> placed.addAll(map.keySet()));
        return recipes.getAll().stream().filter(CraftingRecipeData::enabled)
                .map(CraftingRecipeData::id).filter(id -> !placed.contains(id)).sorted().toList();
    }

    private Validation parse(CraftingRecipeRegistry recipes) {
        Map<String, Map<String, Integer>> candidate = new LinkedHashMap<>();
        for (String category : categoryIdsFromConfig()) {
            Map<String, Integer> positions = readConfiguredPositions(category);
            if (positions.isEmpty()) {
                positions = legacyPositions(category, recipes);
            }
            candidate.put(category, positions);
        }
        return loadLenient(recipes, candidate);
    }

    private Set<String> categoryIdsFromConfig() {
        Set<String> result = new LinkedHashSet<>();
        ConfigurationSection layout = config.getCraftingSection("crafting.layout");
        if (layout != null) for (String key : layout.getKeys(false)) result.add(normalize(key));
        ConfigurationSection menu = config.getCraftingSection("crafting.menu-categories");
        if (menu != null) for (String key : menu.getKeys(false)) result.add(normalize(key));
        ConfigurationSection categories = config.getCraftingSection("crafting.categories");
        if (categories != null) for (String key : categories.getKeys(false)) result.add(normalize(key));
        return result;
    }

    private Map<String, Integer> readConfiguredPositions(String category) {
        Map<String, Integer> result = new LinkedHashMap<>();
        ConfigurationSection section = config.getCraftingSection("crafting.layout." + category);
        if (section == null) return result;
        for (String rawId : section.getKeys(false)) {
            Object rawPosition = section.get(rawId);
            result.put(normalize(rawId), rawPosition instanceof Number number
                    && number.doubleValue() == number.intValue() ? number.intValue() : -1);
        }
        return result;
    }

    private Map<String, Integer> legacyPositions(String rawCategory, CraftingRecipeRegistry recipes) {
        String category = normalize(rawCategory);
        List<String> ids = config.getCraftingStringList("crafting.categories." + category).stream()
                .map(this::normalize).filter(id -> !id.isBlank()).toList();
        if (ids.isEmpty()) ids = recipes.getAll().stream()
                .filter(recipe -> recipe.categoryId().equalsIgnoreCase(category))
                .map(CraftingRecipeData::id).toList();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int index = 0; index < ids.size(); index++) {
            CraftingRecipeData recipe = recipes.get(ids.get(index)).orElse(null);
            if (recipe != null && recipe.enabled() && recipe.categoryId().equalsIgnoreCase(category)) {
                result.put(recipe.id(), index);
            }
        }
        return result;
    }

    private Validation validate(CraftingRecipeRegistry recipes, Map<String, Map<String, Integer>> raw) {
        List<String> errors = new ArrayList<>();
        Map<String, Map<String, Integer>> normalized = new LinkedHashMap<>();
        Set<String> seenRecipes = new HashSet<>();
        for (Map.Entry<String, Map<String, Integer>> categoryEntry : raw.entrySet()) {
            String category = normalize(categoryEntry.getKey());
            Map<String, Integer> entries = normalizePositions(categoryEntry.getValue());
            Set<Integer> slots = new HashSet<>();
            for (Map.Entry<String, Integer> entry : entries.entrySet()) {
                CraftingRecipeData recipe = recipes.get(entry.getKey()).orElse(null);
                if (recipe == null) errors.add(category + ": unknown recipe " + entry.getKey());
                else if (!recipe.enabled()) errors.add(category + ": disabled recipe " + entry.getKey());
                else if (isCoreCategory(category)
                        && !recipe.categoryId().equalsIgnoreCase(canonicalCategoryId(category))) {
                    errors.add(category + ": category mismatch for " + entry.getKey());
                }
                if (entry.getValue() < 0) errors.add(category + ": invalid position for " + entry.getKey());
                if (!slots.add(entry.getValue())) errors.add(category + ": duplicate content slot " + entry.getValue());
                if (!seenRecipes.add(entry.getKey())) errors.add("recipe is placed more than once: " + entry.getKey());
            }
            normalized.put(category, Map.copyOf(entries));
        }
        return new Validation(Map.copyOf(normalized), List.copyOf(errors), List.of());
    }

    private Validation loadLenient(CraftingRecipeRegistry recipes, Map<String, Map<String, Integer>> raw) {
        Map<String, Map<String, Integer>> normalized = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();
        Set<String> seenRecipes = new HashSet<>();
        for (Map.Entry<String, Map<String, Integer>> categoryEntry : raw.entrySet()) {
            String category = normalize(categoryEntry.getKey());
            Map<String, Integer> validEntries = new LinkedHashMap<>();
            Set<Integer> slots = new HashSet<>();
            for (Map.Entry<String, Integer> entry : normalizePositions(categoryEntry.getValue()).entrySet()) {
                String recipeId = entry.getKey();
                int position = entry.getValue();
                CraftingRecipeData recipe = recipes.get(recipeId).orElse(null);
                String location = "layout=" + category + ", slot=" + position + ", recipe=" + recipeId;
                if (recipe == null) { skipped.add(location + " (unknown recipe)"); continue; }
                if (!recipe.enabled()) { skipped.add(location + " (disabled recipe)"); continue; }
                if (isCoreCategory(category)
                        && !recipe.categoryId().equalsIgnoreCase(canonicalCategoryId(category))) {
                    skipped.add(location + " (category mismatch)"); continue;
                }
                if (position < 0) { skipped.add(location + " (invalid position)"); continue; }
                if (!slots.add(position)) { skipped.add(location + " (duplicate content slot)"); continue; }
                if (!seenRecipes.add(recipeId)) { skipped.add(location + " (recipe is placed more than once)"); continue; }
                validEntries.put(recipeId, position);
            }
            normalized.put(category, Map.copyOf(validEntries));
        }
        return new Validation(Map.copyOf(normalized), List.of(), List.copyOf(skipped));
    }

    private Map<String, Map<String, Integer>> copyLayouts() {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        layouts.forEach((id, entries) -> copy.put(id, new LinkedHashMap<>(entries)));
        return copy;
    }

    private boolean isCoreCategory(String category) {
        return CraftingCategory.fromConfig(category, null) != null;
    }

    private String canonicalCategoryId(String rawCategory) {
        CraftingCategory category = CraftingCategory.fromConfig(rawCategory, null);
        return category == null ? normalize(rawCategory) : category.configId();
    }

    private Map<String, Integer> normalizePositions(Map<String, Integer> source) {
        return source.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                entry -> normalize(entry.getKey()), entry -> entry.getValue() == null ? -1 : entry.getValue(),
                (left, right) -> left, LinkedHashMap::new));
    }

    private String normalize(String id) { return id == null ? "" : id.trim().toLowerCase(Locale.ROOT); }

    public record Validation(Map<String, Map<String, Integer>> layouts,
                             List<String> errors, List<String> skippedEntries) {
        public boolean valid() { return errors.isEmpty(); }
    }
}
