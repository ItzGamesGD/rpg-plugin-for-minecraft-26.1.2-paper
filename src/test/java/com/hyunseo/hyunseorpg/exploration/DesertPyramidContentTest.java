package com.hyunseo.hyunseorpg.exploration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesertPyramidContentTest {
    @Test
    void bundledPyramidHasLiveGuardianTrialAndClearReward() {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("exploration/structures.yml")) {
            assertTrue(stream != null, "exploration/structures.yml is missing");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            assertTrue(yaml.getBoolean("enabled"));
            assertTrue(yaml.getBoolean("structures.desert_pyramid.enabled"));
            assertEquals(1.0D, yaml.getDouble("structures.desert_pyramid.selection-chance"));

            String root = "structures.desert_pyramid.variants.guardian_trial.components";
            List<Map<?, ?>> components = yaml.getMapList(root);
            assertEquals(2, components.size());
            assertEquals("pyramid_guardian", components.get(0).get("type"));
            assertEquals("custom:stone_armored_zombie", components.get(0).get("mob-id"));
            assertEquals(Boolean.TRUE, components.get(0).get("objective"));
            assertEquals("reward_drop", components.get(1).get("type"));
            assertEquals("clear", components.get(1).get("phase"));
            assertEquals("vanilla:GOLD_INGOT", components.get(1).get("reward-id"));
        } catch (Exception exception) {
            throw new AssertionError("Unable to read bundled Desert Pyramid config", exception);
        }
    }
}
