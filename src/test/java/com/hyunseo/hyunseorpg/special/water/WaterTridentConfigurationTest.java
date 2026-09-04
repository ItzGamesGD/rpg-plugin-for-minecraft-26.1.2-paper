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
import static org.junit.jupiter.api.Assertions.assertFalse;

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
        assertFalse(abilities.contains("defense"));
        assertFalse(abilities.contains("large-skill"));
        assertFalse(abilities.contains("eight-way"));
        String metadata = abilities.getValues(true).toString().toLowerCase();
        assertFalse(metadata.contains("orbit"));
        assertFalse(metadata.contains("shift_left"));
        assertFalse(metadata.contains("drop_key"));
        assertEquals(3, yaml.getInt(
                "special-equipment.items.poseidons_spear.abilities.signature.maximum-hits-per-trident"));
    }

    @Test void poseidonUsesCanonicalTridentAndKeepsVanillaEnchantPolicyClosed() {
        YamlConfiguration items = load("items.yml");
        YamlConfiguration equipment = load("special-equipment.yml");

        assertEquals("TRIDENT", items.getString("items.poseidon_spear.material"));
        assertEquals("poseidon_spear", equipment.getString("special-equipment.items.poseidons_spear.item-id"));
        assertFalse(equipment.getBoolean(
                "special-equipment.items.poseidons_spear.growth.allow-vanilla-enchants"));
    }

    private YamlConfiguration load() {
        return load("special-equipment.yml");
    }

    private YamlConfiguration load(String resource) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing " + resource);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
