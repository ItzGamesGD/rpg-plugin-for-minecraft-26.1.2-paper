package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** U11 guard: playtests may observe balance, but cannot silently finalize it. */
class AlchemyU11BalanceContractTest {
    @Test
    void productionPotionRecipesRemainBalancePendingAfterRuntimeActivation() {
        YamlConfiguration yaml = load("alchemy/recipes.yml");
        ConfigurationSection recipes = yaml.getConfigurationSection("recipes");
        assertNotNull(recipes);

        for (String id : recipes.getKeys(false)) {
            if (id.equals("potion_test_speed")) continue;
            String path = "recipes." + id;
            assertTrue(yaml.getBoolean(path + ".enabled", false), id);
            assertEquals("BALANCE_PENDING", yaml.getString(path + ".balance"), id);
        }
    }

    @Test
    void productionEffectsAndCatalystsAreRuntimeEnabledWhileBalanceRemainsPending() {
        YamlConfiguration effects = load("alchemy/effects.yml");
        ConfigurationSection effectSection = effects.getConfigurationSection("effects");
        assertNotNull(effectSection);
        for (String id : effectSection.getKeys(false)) {
            if (id.equals("effect_test_speed")) continue;
            String path = "effects." + id;
            assertTrue(effects.getBoolean(path + ".enabled", false), id);
            assertEquals("BALANCE_PENDING", effects.getString(path + ".balance"), id);
        }

        YamlConfiguration catalysts = load("alchemy/catalysts.yml");
        ConfigurationSection catalystSection = catalysts.getConfigurationSection("catalysts");
        assertNotNull(catalystSection);
        for (String id : catalystSection.getKeys(false)) {
            String path = "catalysts." + id;
            assertTrue(catalysts.getBoolean(path + ".enabled", false), id);
        }
    }

    @Test
    void abundanceExchangeAndGuiRemainExplicitlyPending() {
        YamlConfiguration abundance = load("alchemy/abundance.yml");
        assertFalse(abundance.getBoolean("enabled", true));
        assertEquals("FARMING_BRIDGE_MAPPING_PENDING", abundance.getString("reason_disabled"));
        ConfigurationSection essences = abundance.getConfigurationSection("essences");
        assertNotNull(essences);
        for (String id : essences.getKeys(false)) {
            assertFalse(abundance.getBoolean("essences." + id + ".enabled", true), id);
            assertEquals("BALANCE_PENDING", abundance.getString("essences." + id + ".balance"), id);
        }

        YamlConfiguration gui = load("alchemy/gui.yml");
        assertTrue(gui.getBoolean("enabled", false));
        assertTrue(gui.getBoolean("gui.transaction.atomic-consume-and-give"));
        assertTrue(gui.getBoolean("gui.transaction.return-uncommitted-on-close"));
    }

    private YamlConfiguration load(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
