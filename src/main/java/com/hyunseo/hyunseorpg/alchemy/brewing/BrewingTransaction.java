package com.hyunseo.hyunseorpg.alchemy.brewing;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/** Pure, all-or-nothing planner used before a brewing inventory is mutated. */
public final class BrewingTransaction {
    private BrewingTransaction() { }

    public record Plan<T>(List<T> outputs, int ingredientConsumption) {
        public Plan { outputs = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(outputs)); }
    }

    public static <T> Optional<Plan<T>> plan(List<T> inputs, T ingredient,
                                             BiFunction<T, T, Optional<T>> transition) {
        if (ingredient == null || inputs == null || transition == null) return Optional.empty();
        java.util.ArrayList<T> outputs = new java.util.ArrayList<>(inputs.size());
        boolean matched = false;
        for (T input : inputs) {
            if (input == null) { outputs.add(null); continue; }
            Optional<T> output = transition.apply(input, ingredient);
            // A populated bottle that cannot complete makes the entire operation
            // ineligible. This prevents consuming one ingredient for partial output.
            if (output.isEmpty()) return Optional.empty();
            outputs.add(output.orElseThrow());
            matched = true;
        }
        return matched ? Optional.of(new Plan<>(outputs, 1)) : Optional.empty();
    }
}
