package com.hyunseo.hyunseorpg.alchemy.brewing;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class BrewingTransactionTest {
    @Test void matchingTransitionsPlanOutputsAndOneIngredient() {
        var plan = BrewingTransaction.plan(List.of("base", "base"), "ingredient",
                (base, ingredient) -> base.equals("base") && ingredient.equals("ingredient")
                        ? Optional.of("canonical-potion") : Optional.empty()).orElseThrow();
        assertEquals(List.of("canonical-potion", "canonical-potion"), plan.outputs());
        assertEquals(1, plan.ingredientConsumption());
    }

    @Test void unmatchedStateProducesNoMutationPlan() {
        assertTrue(BrewingTransaction.plan(List.of("vanilla"), "custom",
                (base, ingredient) -> Optional.empty()).isEmpty());
    }

    @Test void failedOutputCreationCannotProducePartialPlan() {
        assertTrue(BrewingTransaction.plan(List.of("base", "invalid"), "ingredient",
                (base, ingredient) -> base.equals("base") ? Optional.of("result") : Optional.empty()).isEmpty());
    }
}
