package com.hyunseo.hyunseorpg.farming;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FarmingStage5DataTest {
    @Test
    void progressionConfigKeepsPromotionConditionsSeparateFromHoePerformance() {
        YamlConfiguration progression = load("farming/progression.yml");

        assertTrue(progression.getBoolean("enabled"));
        assertFalse(progression.isSet("hoe"));
        assertEquals("onion", progression.getString("promotion.skilled.unlock-crop"));
        assertEquals("", progression.getString("promotion.expert.unlock-crop"));
    }

    @Test
    void enhancementDoesNotProvideQualityOrDropModifiers() {
        assertEquals(0.5D, com.hyunseo.hyunseorpg.equipment.HoeHarvestModifierService
                .durabilitySaveChance(0.7D, 0.5D), 0.000001D);
        assertEquals(0.0D, com.hyunseo.hyunseorpg.equipment.HoeHarvestModifierService
                .durabilitySaveChance(Double.NaN, 0.5D), 0.000001D);
    }

    @Test
    void farmingStagesRemainOrderedForPromotion() {
        assertEquals(0, FarmingStage.BASIC.ordinal());
        assertEquals(1, FarmingStage.SKILLED.ordinal());
        assertEquals(4, FarmingStage.EXPERT.ordinal());
    }

    private YamlConfiguration load(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
