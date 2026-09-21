package com.hyunseo.hyunseorpg.crafting;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Set;

/** Null-safe access to configured crafting inputs. */
public final class CraftingRecipeRequirements {
    private CraftingRecipeRequirements() {
    }

    /** Returns recipe input keys without requiring an inputs section to exist. */
    public static Set<String> inputKeys(ConfigurationSection inputs) {
        return inputs == null ? Set.of() : inputs.getKeys(false);
    }
}
