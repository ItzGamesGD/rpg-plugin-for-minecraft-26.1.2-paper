package com.hyunseo.hyunseorpg.alchemy.recipe;

/** One vanilla-style brewing transition: base potion + ingredient -> potion. */
public record AlchemyRecipeDefinition(String id, String basePotionId, String ingredientId,
                                      String resultPotionId, boolean enabled) {
    public AlchemyRecipeDefinition {
        basePotionId = normalize(basePotionId);
        ingredientId = normalize(ingredientId);
        resultPotionId = normalize(resultPotionId);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
