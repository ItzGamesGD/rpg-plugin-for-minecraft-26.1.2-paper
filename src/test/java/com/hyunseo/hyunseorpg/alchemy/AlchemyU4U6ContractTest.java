package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.alchemy.potion.PotionDefinition;
import com.hyunseo.hyunseorpg.alchemy.recipe.AlchemyRecipeDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlchemyU4U6ContractTest {
    @Test
    void abundanceContractIsDisabledUntilRuntimeFarmingBridgeIsExplicitlyEnabled() {
        YamlConfiguration yaml = load("alchemy/abundance.yml");
        assertFalse(yaml.getBoolean("enabled", true));
        assertEquals("FARMING_BRIDGE_MAPPING_PENDING", yaml.getString("reason_disabled"));
        assertFalse(yaml.getBoolean("activities.indirect_block_break", true));
        assertFalse(yaml.getBoolean("activities.automation", true));
    }

    @Test
    void potionDefinitionCarriesEffectAndDeliveryWithoutEmbeddingEffectLogic() {
        PotionDefinition definition = new PotionDefinition("potion_test_speed", "테스트 속도 물약",
                PotionDefinition.Delivery.DRINK, "effect_test_speed", false);
        assertEquals("effect_test_speed", definition.effectId());
        assertFalse(definition.enabled());
    }

    @Test
    void recipeContractKeepsIngredientIdsDataDriven() {
        AlchemyRecipeDefinition recipe = new AlchemyRecipeDefinition("potion_test_speed", "potion_test_speed",
                List.of("abundance_essence"), false);
        assertEquals(List.of("abundance_essence"), recipe.ingredientIds());
        assertFalse(recipe.enabled());
        assertTrue(load("alchemy/recipes.yml").isConfigurationSection("recipes"));
    }

    @Test
    void firstProductionLoopIdsExistAndVanillaIngredientsAreExplicit() {
        YamlConfiguration items = load("items.yml");
        var itemDefinitions = items.getConfigurationSection("items");
        assertNotNull(itemDefinitions);
        for (String id : List.of("corrosion_essence",
                "potion_vampire", "potion_berserk", "potion_corrosion", "potion_frostbite",
                "potion_shock", "potion_bleed", "potion_vulnerability", "potion_necrosis")) {
            assertTrue(itemDefinitions.isConfigurationSection(id), "missing item id " + id);
        }

        YamlConfiguration recipes = load("alchemy/recipes.yml");
        assertFalse(itemDefinitions.isConfigurationSection("tomato_concentrate"));
        assertFalse(itemDefinitions.isConfigurationSection("chili_powder"));
        assertEquals("processed_garlic_concentrate_normal",
                recipes.getStringList("recipes.potion_vampire.ingredients").get(2));
        assertEquals("processed_chili_extract_normal",
                recipes.getStringList("recipes.potion_berserk.ingredients").get(2));
        assertEquals("processed_chili_extract_normal",
                recipes.getStringList("recipes.potion_bleed.ingredients").get(2));
        for (String id : List.of("potion_vampire", "potion_berserk", "potion_corrosion", "potion_frostbite",
                "potion_shock", "potion_bleed", "potion_vulnerability", "potion_necrosis")) {
            assertEquals(id, recipes.getString("recipes." + id + ".result-potion-id"));
            assertTrue(recipes.getStringList("recipes." + id + ".ingredients").stream()
                    .filter(value -> value.startsWith("vanilla:"))
                    .allMatch(value -> !value.substring("vanilla:".length()).isBlank()));
        }
    }

    private YamlConfiguration load(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
