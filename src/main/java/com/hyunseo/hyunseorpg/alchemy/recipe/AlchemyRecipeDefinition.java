package com.hyunseo.hyunseorpg.alchemy.recipe;

import java.util.List;

public record AlchemyRecipeDefinition(String id, String resultPotionId, List<String> ingredientIds, boolean enabled) {
    public AlchemyRecipeDefinition { ingredientIds = List.copyOf(ingredientIds == null ? List.of() : ingredientIds); }
}
