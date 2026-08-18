package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.alchemy.potion.PotionDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AlchemyU10ContractTest {
    @Test void productionAlchemyDefinitionsAreEnabledOnlyAfterRuntimeRegistration() {
        YamlConfiguration potions = load("alchemy/potions.yml");
        for (String id : potions.getConfigurationSection("potions").getKeys(false)) {
            if (id.equals("potion_test_speed")) assertFalse(potions.getBoolean("potions." + id + ".enabled", true), id);
            else assertTrue(potions.getBoolean("potions." + id + ".enabled", false), id);
        }
        YamlConfiguration catalysts = load("alchemy/catalysts.yml");
        for (String id : catalysts.getConfigurationSection("catalysts").getKeys(false)) {
            assertTrue(catalysts.getBoolean("catalysts." + id + ".enabled", false), id);
        }
    }

    @Test void potionOutputItemIsExplicitAndDefinitionKeepsCompatibilityConstructor() {
        PotionDefinition definition = new PotionDefinition("potion_test_speed", "테스트", PotionDefinition.Delivery.DRINK,
                "effect_test_speed", false);
        assertEquals("potion_test_speed", definition.outputItemId());
        YamlConfiguration potions = load("alchemy/potions.yml");
        for (String id : potions.getConfigurationSection("potions").getKeys(false)) {
            if (potions.getBoolean("potions." + id + ".enabled", false)) {
                assertFalse(potions.getString("potions." + id + ".output-item-id", "").isBlank(), id);
            }
        }
    }

    @Test void vanillaBypassPolicyTargetsCustomPotionNotEveryBrewingStand() {
        YamlConfiguration gui = load("alchemy/gui.yml");
        assertTrue(gui.getBoolean("enabled", false));
        assertTrue(load("alchemy/catalysts.yml").getConfigurationSection("catalysts").getKeys(false).size() > 0);
    }

    private YamlConfiguration load(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
