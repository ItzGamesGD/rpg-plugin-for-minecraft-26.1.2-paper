package com.hyunseo.hyunseorpg.alchemy.recipe;

import java.util.Locale;

/** One visible Brewing Stand transition: bottle input + ingredient -> canonical potion. */
public record AlchemyRecipeDefinition(String id, String baseInputId, String ingredientId,
                                      String resultPotionId, boolean enabled) {
    public AlchemyRecipeDefinition {
        id = normalize(id);
        baseInputId = normalize(baseInputId);
        ingredientId = normalize(ingredientId);
        resultPotionId = normalize(resultPotionId);
        if (id.isBlank() || baseInputId.isBlank() || ingredientId.isBlank() || resultPotionId.isBlank()) {
            throw new IllegalArgumentException("Brewing transitions require id, base input, ingredient, and result");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
