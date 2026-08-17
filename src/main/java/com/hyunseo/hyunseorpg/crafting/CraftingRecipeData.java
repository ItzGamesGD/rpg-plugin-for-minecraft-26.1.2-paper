package com.hyunseo.hyunseorpg.crafting;

import java.util.List;
import java.util.Locale;

/** Immutable canonical recipe definition shared by every crafting surface. */
public record CraftingRecipeData(String id, boolean enabled, String categoryId,
                                 List<Ingredient> ingredients, String outputId, int outputAmount,
                                 String farmingType, String requiredFarmingStage,
                                 long requiredAbundancePoints) {
    public CraftingRecipeData {
        id = normalize(id);
        categoryId = normalize(categoryId);
        if (categoryId.isBlank()) categoryId = CraftingCategory.MATERIALS.configId();
        ingredients = List.copyOf(ingredients == null ? List.of() : ingredients);
        outputId = normalize(outputId);
        if (outputAmount < 1) throw new IllegalArgumentException("outputAmount must be positive");
        farmingType = normalize(farmingType);
        requiredFarmingStage = normalize(requiredFarmingStage);
        if (requiredAbundancePoints < 0L) {
            throw new IllegalArgumentException("requiredAbundancePoints must be non-negative");
        }
    }

    public CraftingRecipeData(String id, boolean enabled, String categoryId,
                              List<Ingredient> ingredients, String outputId, int outputAmount,
                              String farmingType, String requiredFarmingStage) {
        this(id, enabled, categoryId, ingredients, outputId, outputAmount,
                farmingType, requiredFarmingStage, 0L);
    }

    public CraftingRecipeData(String id, boolean enabled, String categoryId,
                              List<Ingredient> ingredients, String outputId, int outputAmount,
                              String farmingType) {
        this(id, enabled, categoryId, ingredients, outputId, outputAmount, farmingType, "");
    }

    public CraftingRecipeData(String id, boolean enabled, String categoryId,
                              List<Ingredient> ingredients, String outputId, int outputAmount) {
        this(id, enabled, categoryId, ingredients, outputId, outputAmount, "");
    }

    /** Compatibility constructor for existing integrations using the legacy core categories. */
    public CraftingRecipeData(String id, boolean enabled, CraftingCategory category,
                              List<Ingredient> ingredients, String outputId, int outputAmount) {
        this(id, enabled, category == null ? null : category.configId(), ingredients, outputId, outputAmount);
    }

    /** Compatibility constructor for legacy integrations. */
    public CraftingRecipeData(String id, List<Ingredient> ingredients, String outputId, int outputAmount) {
        this(id, true, CraftingCategory.MATERIALS.configId(), ingredients, outputId, outputAmount);
    }

    public record Ingredient(String id, int amount) {
        public Ingredient {
            id = normalize(id);
            if (amount < 1) throw new IllegalArgumentException("ingredient amount must be positive");
        }

        public boolean vanilla() {
            return id.startsWith("vanilla:");
        }

        public boolean tag() {
            return id.startsWith("tag:");
        }

        public String tagId() {
            return tag() ? id.substring("tag:".length()) : "";
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
