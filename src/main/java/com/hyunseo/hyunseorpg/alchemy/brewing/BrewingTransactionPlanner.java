package com.hyunseo.hyunseorpg.alchemy.brewing;

import com.hyunseo.hyunseorpg.alchemy.recipe.AlchemyRecipeDefinition;
import com.hyunseo.hyunseorpg.alchemy.recipe.AlchemyRecipeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/** Pure all-or-nothing planner used before a Brewing Stand inventory is mutated. */
public final class BrewingTransactionPlanner<T> {
    private final AlchemyRecipeRegistry recipes;
    private final BiFunction<Integer, AlchemyRecipeDefinition, Optional<T>> outputFactory;

    public BrewingTransactionPlanner(AlchemyRecipeRegistry recipes,
                                     BiFunction<Integer, AlchemyRecipeDefinition, Optional<T>> outputFactory) {
        this.recipes = recipes;
        this.outputFactory = outputFactory;
    }

    public Plan<T> plan(List<String> baseInputIds, String ingredientId, List<T> vanillaResults) {
        if (baseInputIds.size() != vanillaResults.size()) throw new IllegalArgumentException("slot count mismatch");
        List<T> outputs = new ArrayList<>(vanillaResults);
        boolean matched = false;
        for (int slot = 0; slot < baseInputIds.size(); slot++) {
            AlchemyRecipeDefinition recipe = recipes.findTransition(baseInputIds.get(slot), ingredientId).orElse(null);
            if (recipe == null) continue;
            matched = true;
            Optional<T> output = outputFactory.apply(slot, recipe);
            if (output.isEmpty()) return new Plan<>(Status.INVALID, List.copyOf(vanillaResults));
            outputs.set(slot, output.orElseThrow());
        }
        return matched ? new Plan<>(Status.READY, List.copyOf(outputs))
                : new Plan<>(Status.UNMATCHED, List.copyOf(vanillaResults));
    }

    public enum Status { UNMATCHED, READY, INVALID }
    public record Plan<T>(Status status, List<T> outputs) { }
}
