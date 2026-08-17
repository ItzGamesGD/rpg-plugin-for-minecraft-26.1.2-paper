package com.hyunseo.hyunseorpg.farming;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookingStage8DataTest {
    private static final List<String> RECIPES = List.of("corn_soup", "garden_salad", "harvest_medley");
    private static final List<String> QUALITIES = List.of("normal", "basic", "proficient", "advanced", "supreme");

    @Test
    void cookingConfigHasSingleTwoAndThreeCropRecipes() {
        YamlConfiguration cooking = load("farming/cooking.yml");
        assertEquals(false, cooking.getBoolean("enabled"));
        assertTrue(cooking.getString("schema-version", "").equals("1"));
        // Cooking definitions are retained only as a non-destructive legacy
        // archive; active processing is defined in crafting.yml.
        for (String id : RECIPES) {
            assertTrue(cooking.isConfigurationSection("recipes." + id));
        }
        assertTrue(cooking.getConfigurationSection("recipes").getKeys(false).size() >= 1);
    }

    @Test
    void cookingOutputsAreCanonicalItemIds() {
        YamlConfiguration cooking = load("farming/cooking.yml");
        YamlConfiguration items = load("items.yml");
        for (String recipe : RECIPES) {
            for (String quality : QUALITIES) {
                String itemId = cooking.getString("recipes." + recipe + ".outputs." + quality, "");
                assertTrue(items.isConfigurationSection("items." + itemId), itemId);
                assertEquals("FARMING_COOKED", items.getString("items." + itemId + ".category"));
            }
        }
    }

    private YamlConfiguration load(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
