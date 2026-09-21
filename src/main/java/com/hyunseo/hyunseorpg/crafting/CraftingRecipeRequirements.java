package com.hyunseo.hyunseorpg.crafting;

import org.bukkit.configuration.ConfigurationSection;
import java.util.Set;

/** Shared parsing helper for canonical item-input recipes. */
public final class CraftingRecipeRequirements {
    private CraftingRecipeRequirements() { }
    public static Set<String> inputKeys(ConfigurationSection inputs) {
        return inputs == null ? Set.of() : Set.copyOf(inputs.getKeys(false));
    }
}
