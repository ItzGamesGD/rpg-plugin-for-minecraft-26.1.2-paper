package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalystRuntimeBehaviorTest {
    @Test
    void productionCatalystsHaveExplicitRuntimeMappingsAndBounds() {
        YamlConfiguration yaml = load("alchemy/catalysts.yml");
        var section = yaml.getConfigurationSection("catalysts");
        assertNotNull(section);
        for (String id : section.getKeys(false)) {
            String path = "catalysts." + id;
            if (id.equals("slime")) assertFalse(yaml.getBoolean(path + ".enabled", true), id);
            else assertTrue(yaml.getBoolean(path + ".enabled", false), id);
            assertFalse(yaml.getString(path + ".vanilla-material", "").isBlank(), id);
            if ("SPECIAL".equalsIgnoreCase(yaml.getString(path + ".mode", ""))) {
                assertTrue(yaml.getString(path + ".kind", "").length() > 0, id);
                assertTrue(yaml.getInt(path + ".max-propagation-count", 1) > 0
                        || yaml.getDouble(path + ".max-distance", 1.0D) > 0.0D, id);
            }
        }
        assertTrue(yaml.getStringList("catalysts.fermented_spider_eye.allowed-potions")
                .contains("potion_vulnerability"));
        assertFalse(yaml.getString("catalysts.fermented_spider_eye.inversion-effect-ids.potion_vulnerability", "").isBlank());
    }

    private YamlConfiguration load(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
