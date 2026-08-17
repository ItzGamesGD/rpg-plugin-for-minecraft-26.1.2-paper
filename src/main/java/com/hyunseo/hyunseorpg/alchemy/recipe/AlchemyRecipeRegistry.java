package com.hyunseo.hyunseorpg.alchemy.recipe;

import java.util.Map;
import java.util.Optional;
public interface AlchemyRecipeRegistry {
    Optional<AlchemyRecipeDefinition> findByResult(String potionId);
    Map<String, AlchemyRecipeDefinition> all();
    boolean reload();
}
