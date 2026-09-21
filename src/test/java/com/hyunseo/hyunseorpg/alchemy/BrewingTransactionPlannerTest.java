package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.alchemy.brewing.BrewingTransactionPlanner;
import com.hyunseo.hyunseorpg.alchemy.recipe.AlchemyRecipeDefinition;
import com.hyunseo.hyunseorpg.alchemy.recipe.AlchemyRecipeRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BrewingTransactionPlannerTest {
    private static final AlchemyRecipeDefinition RECIPE = new AlchemyRecipeDefinition(
            "test", "potion_base", "vanilla:redstone", "potion_result", true);

    @Test
    void registryMatchPlansCanonicalOutputWithoutChangingUnmatchedSlots() {
        BrewingTransactionPlanner<String> planner = new BrewingTransactionPlanner<>(registry(),
                (slot, recipe) -> Optional.of("factory:" + recipe.resultPotionId()));
        var plan = planner.plan(java.util.List.of("potion_base", "vanilla:water"),
                "vanilla:redstone", java.util.List.of("vanilla-result-0", "vanilla-result-1"));

        assertEquals(BrewingTransactionPlanner.Status.READY, plan.status());
        assertEquals(java.util.List.of("factory:potion_result", "vanilla-result-1"), plan.outputs());
    }

    @Test
    void unmatchedStateIsUntouchedAndInvalidFactoryIsAtomic() {
        BrewingTransactionPlanner<String> planner = new BrewingTransactionPlanner<>(registry(),
                (slot, recipe) -> Optional.empty());
        var unmatched = planner.plan(java.util.List.of("vanilla:water"), "vanilla:sugar",
                java.util.List.of("vanilla-result"));
        var invalid = planner.plan(java.util.List.of("potion_base"), "vanilla:redstone",
                java.util.List.of("original"));

        assertEquals(BrewingTransactionPlanner.Status.UNMATCHED, unmatched.status());
        assertEquals(java.util.List.of("vanilla-result"), unmatched.outputs());
        assertEquals(BrewingTransactionPlanner.Status.INVALID, invalid.status());
        assertEquals(java.util.List.of("original"), invalid.outputs());
    }

    private AlchemyRecipeRegistry registry() {
        return new AlchemyRecipeRegistry() {
            @Override public Optional<AlchemyRecipeDefinition> findByResult(String id) {
                return RECIPE.resultPotionId().equals(id) ? Optional.of(RECIPE) : Optional.empty();
            }
            @Override public Optional<AlchemyRecipeDefinition> findTransition(String base, String ingredient) {
                return RECIPE.baseInputId().equals(base) && RECIPE.ingredientId().equals(ingredient)
                        ? Optional.of(RECIPE) : Optional.empty();
            }
            @Override public Map<String, AlchemyRecipeDefinition> all() { return Map.of(RECIPE.id(), RECIPE); }
            @Override public boolean reload() { return true; }
        };
    }
}
