package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Static contract checks for the runtime production loop; live Paper tests remain separate. */
class AlchemyProductionCompletionATest {
    private static final List<String> EFFECTS = List.of(
            "effect_vampire", "effect_berserk", "effect_corrosion", "effect_frostbite",
            "effect_shock", "effect_bleed", "effect_vulnerability", "effect_necrosis");
    private static final List<String> POTIONS = List.of(
            "potion_vampire", "potion_berserk", "potion_corrosion", "potion_frostbite",
            "potion_shock", "potion_bleed", "potion_vulnerability", "potion_necrosis");

    @Test
    void productionDefinitionsHaveRuntimeContracts() {
        YamlConfiguration effects = load("alchemy/effects.yml");
        for (String id : EFFECTS) {
            String path = "effects." + id;
            assertTrue(effects.getBoolean(path + ".enabled"));
            assertTrue(effects.getInt(path + ".duration-ticks") > 0);
            assertFalse(effects.getString(path + ".handler-id", "").isBlank());
            assertEquals("BALANCE_PENDING", effects.getString(path + ".balance"));
        }

        YamlConfiguration potions = load("alchemy/potions.yml");
        for (String id : POTIONS) {
            String path = "potions." + id;
            assertTrue(potions.getBoolean(path + ".enabled"));
            assertEquals(id, potions.getString(path + ".output-item-id"));
            String effectId = potions.getString(path + ".effect-id", "");
            assertEquals("effect_" + id.substring("potion_".length()), effectId);
        }
    }

    @Test
    void canonicalCraftingRecipesUseRealIngredientsAndOutputs() {
        YamlConfiguration crafting = load("crafting.yml");
        ConfigurationSection recipes = crafting.getConfigurationSection("crafting-recipes");
        assertNotNull(recipes);
        assertNotNull(recipes.getConfigurationSection("corrosion_essence"));
        assertEquals("alchemy_essence", recipes.getString("corrosion_essence.farming-type"));
        assertEquals("corrosion_essence", recipes.getString("corrosion_essence.output.item-id"));
        for (String id : POTIONS) {
            assertEquals("alchemy_potion", recipes.getString(id + ".farming-type"), id);
            assertEquals(id, recipes.getString(id + ".output.item-id"), id);
            ConfigurationSection inputs = recipes.getConfigurationSection(id + ".inputs");
            assertNotNull(inputs, id);
            for (String ingredient : inputs.getKeys(false)) {
                assertFalse(ingredient.contains("tomato"), ingredient);
                assertFalse(ingredient.equalsIgnoreCase("chili_powder"), ingredient);
            }
        }
    }

    private YamlConfiguration load(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
