package com.hyunseo.hyunseorpg.special.flame;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlameAxeConfigurationTest {
    @Test void presentationAndExtendedChainValuesRemainExplicit() throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("special-equipment.yml")) {
            assertTrue(stream != null);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            String path = "special-equipment.items.flame_axe.abilities.spinning-throw.";
            assertEquals(2.1, yaml.getDouble(path + "display-scale"));
            assertEquals(1.75, yaml.getDouble(path + "contact-radius"));
            assertEquals(14.0, yaml.getDouble(path + "extended-search-radius"));
            assertTrue(yaml.getDouble(path + "extended-search-radius") > yaml.getDouble(path + "search-radius"));
        }
    }
}
