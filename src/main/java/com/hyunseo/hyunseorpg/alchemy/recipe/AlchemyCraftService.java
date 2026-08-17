package com.hyunseo.hyunseorpg.alchemy.recipe;

public interface AlchemyCraftService<S> {
    CraftResult craft(S session);
    enum CraftResult { SUCCESS, SESSION_LOCKED, RECIPE_NOT_FOUND, MATERIALS_INVALID, OUTPUT_WOULD_OVERFLOW, TRANSACTION_FAILED }
}
