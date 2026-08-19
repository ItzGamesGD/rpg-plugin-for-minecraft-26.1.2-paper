package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
            assertTrue(yaml.getBoolean(path + ".enabled", false), id);
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
        assertEquals("SPLASH", yaml.getString("catalysts.sculk.delivery", ""));
        assertEquals("SPLASH", yaml.getString("catalysts.slime.delivery", ""));
        assertEquals("SPLASH", yaml.getString("catalysts.echo_shard.delivery", ""));
        assertEquals("SPLASH", yaml.getString("catalysts.wind_charge.delivery", ""));
    }

    @Test
    void specialCatalystMaterialsRemainCanonical() {
        YamlConfiguration yaml = load("alchemy/catalysts.yml");
        assertEquals("SCULK_CATALYST", yaml.getString("catalysts.sculk.vanilla-material", ""));
        assertEquals("SCULK", yaml.getString("catalysts.sculk.kind", ""));
        assertEquals("WIND_CHARGE", yaml.getString("catalysts.wind_charge.vanilla-material", ""));
        assertEquals("WIND_CHARGE", yaml.getString("catalysts.wind_charge.kind", ""));
    }

    private YamlConfiguration load(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
