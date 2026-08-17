package com.hyunseo.hyunseorpg.farming;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FarmingStage6DataTest {
    @Test
    void qualityConfigContainsOneOrderedRollAndAllCropMappings() {
        YamlConfiguration quality = load("farming/quality.yml");

        assertTrue(quality.getBoolean("enabled"));
        assertEquals(0.0D, quality.getDouble("roll.random-minimum"), 0.000001D);
        assertEquals(100.0D, quality.getDouble("roll.random-maximum"), 0.000001D);
        double previous = -1.0D;
        for (CropQuality value : CropQuality.values()) {
            double threshold = quality.getDouble("quality." + value.id() + ".minimum-score", -1.0D);
            assertTrue(threshold >= previous);
            previous = threshold;
            for (String crop : new String[]{"corn", "onion", "chili", "garlic"}) {
                assertFalse(quality.getString("items." + crop + "." + value.id(), "").isBlank());
            }
        }
    }

    @Test
    void qualityIdsRemainStableAndSingleValued() {
        assertEquals("normal", CropQuality.NORMAL.id());
        assertEquals("supreme", CropQuality.SUPREME.id());
        assertEquals(5, CropQuality.values().length);
        assertTrue(CropQuality.fromId("advanced").isPresent());
        assertTrue(CropQuality.fromId("missing").isEmpty());
    }

    @Test
    void farmingPromotionDoesNotOwnDropBalance() {
        YamlConfiguration progression = load("farming/progression.yml");
        assertFalse(progression.isSet("hoe.drop-multiplier"));
        assertFalse(progression.isSet("hoe.quality-score"));
    }

    private YamlConfiguration load(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
