package com.hyunseo.hyunseorpg.crafting;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that normal and farming processing recipes use the same canonical YAML shape. */
final class CraftingCanonicalRecipeStructureTest {
    private static final List<String> PROCESSING = List.of(
            "process_corn_normal", "process_corn_basic", "process_corn_proficient",
            "process_corn_advanced", "process_corn_supreme", "process_onion_normal",
            "process_onion_basic", "process_onion_proficient", "process_onion_advanced",
            "process_onion_supreme", "process_chili_normal", "process_chili_basic",
            "process_chili_proficient", "process_chili_advanced", "process_chili_supreme",
            "process_garlic_normal", "process_garlic_basic", "process_garlic_proficient",
            "process_garlic_advanced", "process_garlic_supreme");

    @Test
    void referenceRecipesUseTheSameCanonicalDefinitionShape() {
        YamlConfiguration crafting = load("crafting.yml");
        for (String recipeId : List.of("water_core", "process_onion_normal")) {
            assertCanonicalRecipe(crafting, recipeId);
        }

        assertEquals(shape(crafting, "water_core"), shape(crafting, "process_onion_normal"),
                "Both recipes must be represented by one canonical schema; only values may differ.");
    }

    @Test


    @Test
    void onionProcessingTraceMatchesCanonicalRecipeContract() {
        YamlConfiguration crafting = load("crafting.yml");
        assertEquals("materials", crafting.getString("crafting-recipes.process_onion_normal.category"));
        assertEquals("processed_onion_concentrate_normal",
                crafting.getString("crafting-recipes.process_onion_normal.output.item-id"));
        assertEquals(20, crafting.getInt("crafting-recipes.process_onion_normal.inputs.crop_onion"));
    }

    private void assertCanonicalRecipe(YamlConfiguration crafting, String recipeId) {
        String path = "crafting-recipes." + recipeId;
        assertTrue(crafting.isConfigurationSection(path), "Missing recipe: " + recipeId);
        assertTrue(crafting.isConfigurationSection(path + ".inputs"), "Missing inputs: " + recipeId);
        assertTrue(crafting.isSet(path + ".output.item-id"), "Missing output: " + recipeId);
        assertTrue(crafting.getInt(path + ".output.amount", 0) > 0, "Invalid output amount: " + recipeId);
    }

    private List<Boolean> shape(YamlConfiguration crafting, String recipeId) {
        String path = "crafting-recipes." + recipeId;
        return List.of(crafting.isConfigurationSection(path + ".inputs"),
                crafting.isSet(path + ".output.item-id"),
                crafting.isSet(path + ".output.amount"));
    }

    private YamlConfiguration load(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
