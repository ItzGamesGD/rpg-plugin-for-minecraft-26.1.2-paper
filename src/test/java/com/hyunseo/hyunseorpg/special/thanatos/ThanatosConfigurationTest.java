package com.hyunseo.hyunseorpg.special.thanatos;

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

class ThanatosConfigurationTest {
    private static final Set<String> ABILITIES = Set.of(
            "mortal", "deaths-oppression", "death-sentence", "ultimatum");

    @Test
    void thanatosUsesCanonicalItemAndRegistryMetadata() {
        YamlConfiguration items = load("items.yml");
        YamlConfiguration equipment = load("special-equipment.yml");

        assertEquals("MACE", items.getString("items.thanatos_mace.material"));
        assertEquals("thanatos_mace",
                equipment.getString("special-equipment.items.thanatos_mace.item-id"));
        ConfigurationSection abilities = equipment.getConfigurationSection(
                "special-equipment.items.thanatos_mace.abilities");
        assertNotNull(abilities);
        for (String key : ABILITIES) {
            ConfigurationSection ability = abilities.getConfigurationSection(key);
            assertNotNull(ability, key);
            assertTrue(ability.isString("id") && !ability.getString("id", "").isBlank(), key + " id");
            assertTrue(ability.isString("display-name")
                    && !ability.getString("display-name", "").isBlank(), key + " display");
            assertTrue(ability.isString("trigger")
                    && !ability.getString("trigger", "").isBlank(), key + " trigger");
            assertTrue(ability.isString("description")
                    && !ability.getString("description", "").isBlank(), key + " description");
        }
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
