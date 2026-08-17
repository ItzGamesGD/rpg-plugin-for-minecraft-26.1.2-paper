package com.hyunseo.hyunseorpg.crafting;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Set;

/** Shared validity predicate for item-input and player-currency recipes. */
public final class CraftingRecipeRequirements {
    private CraftingRecipeRequirements() {
    }

    public static boolean isValid(boolean hasItemInputs, long requiredAbundancePoints) {
        return hasItemInputs || requiredAbundancePoints > 0L;
    }

    public static String invalidReason(boolean hasItemInputs, long requiredAbundancePoints) {
        return isValid(hasItemInputs, requiredAbundancePoints)
                ? ""
                : "no inputs or player currency requirement";
    }

    /** Returns recipe input keys without requiring an inputs section to exist. */
    public static Set<String> inputKeys(ConfigurationSection inputs) {
        return inputs == null ? Set.of() : inputs.getKeys(false);
    }
}
