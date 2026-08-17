package com.hyunseo.hyunseorpg.crafting;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingRecipeRequirementsTest {
    @Test
    void pointOnlyRecipeIsValidWithoutDummyIngredients() {
        assertTrue(CraftingRecipeRequirements.isValid(false, 100L));
        assertFalse(CraftingRecipeRequirements.isValid(false, 0L));
    }

    @Test
    void itemRecipeRemainsValidWithoutCurrency() {
        assertTrue(CraftingRecipeRequirements.isValid(true, 0L));
    }

    @Test
    void pointOnlyRecipeHasNoInputKeysAndDoesNotRequireDummyInputs() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("required-abundance-points", 100L);

        assertTrue(CraftingRecipeRequirements.inputKeys(config.getConfigurationSection("inputs")).isEmpty());

        config.set("inputs.crop_corn", 20);
        assertEquals(Set.of("crop_corn"),
                CraftingRecipeRequirements.inputKeys(config.getConfigurationSection("inputs")));
    }
}
