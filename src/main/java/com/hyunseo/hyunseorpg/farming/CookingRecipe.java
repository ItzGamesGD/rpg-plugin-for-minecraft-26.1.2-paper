package com.hyunseo.hyunseorpg.farming;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable cooking definition loaded from farming/cooking.yml. */
public record CookingRecipe(
        String id,
        boolean enabled,
        String categoryId,
        Map<String, Integer> ingredients,
        Map<CropQuality, String> outputItemIds,
        int outputAmount,
        boolean bulkCraftingEnabled,
        boolean sellable,
        String consumptionEffectId,
        List<String> questTags,
        List<String> territoryTags) {

    public CookingRecipe {
        id = normalize(id);
        categoryId = normalize(categoryId);
        ingredients = Map.copyOf(ingredients == null ? Map.of() : ingredients);
        outputItemIds = Map.copyOf(outputItemIds == null ? Map.of() : outputItemIds);
        outputAmount = Math.max(1, outputAmount);
        consumptionEffectId = consumptionEffectId == null ? "" : consumptionEffectId.trim();
        questTags = List.copyOf(questTags == null ? List.of() : questTags.stream().map(CookingRecipe::normalize).toList());
        territoryTags = List.copyOf(territoryTags == null ? List.of() : territoryTags.stream().map(CookingRecipe::normalize).toList());
    }

    public String outputFor(CropQuality quality) {
        return outputItemIds.getOrDefault(quality, outputItemIds.get(CropQuality.NORMAL));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
