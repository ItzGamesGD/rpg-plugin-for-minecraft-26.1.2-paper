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
            assertEquals(6, components.size());
            assertEquals("pyramid_room", components.get(0).get("type"));
            assertEquals("pyramid_loot_trigger", components.get(0).get("phase"));
            assertEquals(3, components.get(0).get("room-radius"));
            assertEquals("pyramid_repel", components.get(1).get("type"));
            assertEquals("pyramid_loot_trigger", components.get(1).get("phase"));
            assertEquals("sequence_delay", components.get(2).get("type"));
            assertEquals("pyramid_guardian_spawn", components.get(2).get("next-phase"));
            assertEquals("pyramid_guardian", components.get(3).get("type"));
            assertEquals("pyramid_guardian_spawn", components.get(3).get("phase"));
            assertEquals("custom:stone_armored_zombie", components.get(3).get("mob-id"));
            assertEquals(Boolean.TRUE, components.get(3).get("objective"));
            assertEquals("pyramid_push_pillars", components.get(4).get("type"));
            assertEquals("pyramid_puzzle", components.get(4).get("phase"));
            assertEquals(4, ((List<?>) components.get(4).get("pillars")).size());
            assertEquals("reward_drop", components.get(5).get("type"));
            assertEquals("clear", components.get(5).get("phase"));
            assertEquals("vanilla:GOLD_INGOT", components.get(5).get("reward-id"));
        } catch (Exception exception) {
            throw new AssertionError("Unable to read bundled Desert Pyramid config", exception);
        }
    }
}
