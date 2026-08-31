package com.hyunseo.hyunseorpg.special.water;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterTridentConfigurationTest {
    private static final Set<String> ABILITIES = Set.of(
            "pressure-thrust", "current-throw", "signature", "riptide");

    @Test void everyRuntimeAbilityUsesRegistryMetadataConvention() {
        YamlConfiguration yaml = load();
        ConfigurationSection abilities = yaml.getConfigurationSection(
                "special-equipment.items.poseidons_spear.abilities");
        assertNotNull(abilities);
        for (String key : ABILITIES) {
            ConfigurationSection ability = abilities.getConfigurationSection(key);
            assertNotNull(ability, key);
            assertTrue(ability.isString("id") && !ability.getString("id", "").isBlank(), key + " id");
            assertTrue(ability.isString("display-name") && !ability.getString("display-name", "").isBlank(), key + " display");
            assertTrue(ability.isString("trigger") && !ability.getString("trigger", "").isBlank(), key + " trigger");
            assertTrue(ability.isString("description") && !ability.getString("description", "").isBlank(), key + " description");
        }
        assertEquals(3, yaml.getInt(
                "special-equipment.items.poseidons_spear.abilities.signature.maximum-hits-per-trident"));
        assertTrue(!abilities.isConfigurationSection("defense"));
        assertTrue(!abilities.isConfigurationSection("large-skill"));
        assertTrue(!abilities.isConfigurationSection("eight-way"));
    }

    private YamlConfiguration load() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("special-equipment.yml")) {
            if (stream == null) throw new IllegalStateException("Missing special-equipment.yml");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
