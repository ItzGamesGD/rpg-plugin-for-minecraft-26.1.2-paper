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
            assertEquals(4.0D, yaml.getDouble("structures.desert_pyramid.entry-boundary-padding"));
            assertEquals(1.0D, yaml.getDouble("structures.desert_pyramid.selection-chance"));

            String root = "structures.desert_pyramid.variants.guardian_trial.components";
            List<Map<?, ?>> components = yaml.getMapList(root);
            assertEquals(7, components.size());
            assertEquals("pyramid_room", components.get(0).get("type"));
            assertEquals("pyramid_loot_trigger", components.get(0).get("phase"));
            assertEquals(4, components.get(0).get("room-radius"));
            assertEquals(140, components.get(0).get("reveal-delay-ticks"));
            assertEquals("pyramid_room_reveal", components.get(1).get("type"));
            assertEquals("pyramid_room_reveal", components.get(1).get("phase"));
            assertEquals("pyramid_repel", components.get(2).get("type"));
            assertEquals("pyramid_entry", components.get(2).get("phase"));
            assertEquals("choice_prompt", components.get(3).get("type"));
            assertEquals("pyramid_quiz", components.get(3).get("phase"));
            assertEquals(List.of("answer_a", "answer_b", "answer_c"), components.get(3).get("choices"));
            assertEquals("pyramid_guardian", components.get(4).get("type"));
            assertEquals("pyramid_guardian_spawn", components.get(4).get("phase"));
            assertEquals("custom:stone_armored_zombie", components.get(4).get("mob-id"));
            assertEquals(Boolean.TRUE, components.get(4).get("objective"));
            assertEquals("pyramid_push_pillars", components.get(5).get("type"));
            assertEquals("pyramid_pillar_restore", components.get(5).get("phase"));
            List<?> pillars = (List<?>) components.get(5).get("pillars");
            assertEquals(4, pillars.size());
            assertEquals("-3,-3", ((Map<?, ?>) pillars.get(0)).get("initial"));
            assertEquals("-1,-1", ((Map<?, ?>) pillars.get(0)).get("target"));
            assertEquals("3,-3", ((Map<?, ?>) pillars.get(1)).get("initial"));
            assertEquals("1,-1", ((Map<?, ?>) pillars.get(1)).get("target"));
            assertEquals("-3,3", ((Map<?, ?>) pillars.get(2)).get("initial"));
            assertEquals("-1,1", ((Map<?, ?>) pillars.get(2)).get("target"));
            assertEquals("3,3", ((Map<?, ?>) pillars.get(3)).get("initial"));
            assertEquals("1,1", ((Map<?, ?>) pillars.get(3)).get("target"));
            assertEquals("reward_drop", components.get(6).get("type"));
            assertEquals("clear", components.get(6).get("phase"));
            assertEquals("entry_actor", components.get(6).get("recipient"));
            assertEquals("vanilla:GOLD_INGOT", components.get(6).get("reward-id"));
        } catch (Exception exception) {
            throw new AssertionError("Unable to read bundled Desert Pyramid config", exception);
        }
    }
}
