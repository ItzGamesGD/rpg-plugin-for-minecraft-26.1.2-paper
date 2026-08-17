package com.hyunseo.hyunseorpg.farming;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the approved Stage 7 data boundaries without starting Bukkit. */
class FarmingStage7DesignTest {
    private static final List<String> CROPS = List.of("corn", "onion", "chili", "garlic");
    private static final List<String> QUALITIES = List.of("normal", "basic", "proficient", "advanced", "supreme");

    @Test
    void processingUsesTheCanonicalCraftingSourceForEveryQuality() {
        YamlConfiguration crafting = load("crafting.yml");
        for (String crop : CROPS) {
            for (String quality : QUALITIES) {
                String id = "process_" + crop + "_" + quality;
                assertTrue(crafting.isConfigurationSection("crafting-recipes." + id), id);
                assertEquals("materials", crafting.getString("crafting-recipes." + id + ".category"));
                assertEquals(1, crafting.getInt("crafting-recipes." + id + ".output.amount"));
                assertTrue(crafting.isSet("crafting.layout.materials." + id), id);
                assertTrue(crafting.getString("crafting-recipes." + id + ".output.item-id", "")
                        .startsWith("processed_"));
            }
        }
        assertFalse(crafting.isConfigurationSection("crafting.menu-categories.cooking"));
        assertFalse(crafting.isConfigurationSection("crafting.layout.cooking"));
    }

    @Test
    void retiredCookingIsNotAnActiveRegistryConfiguration() {
        YamlConfiguration cooking = load("farming/cooking.yml");
        assertFalse(cooking.getBoolean("enabled", true));
        assertEquals("1", cooking.getString("schema-version"));
    }

    @Test
    void hoeGrowthAxesRemainSeparate() {
        YamlConfiguration enhancement = load("farming/hoe_enhancement.yml");
        YamlConfiguration promotion = load("farming/hoe_promotion.yml");
        assertTrue(enhancement.isSet("levels.0.durability-save-chance"));
        assertTrue(enhancement.isSet("levels.0.quality-sale-bonus"));
        assertFalse(enhancement.isSet("levels.0.quality-density-shift"));
        assertTrue(promotion.isSet("tiers.0.0.quality-density-shift"));
        assertTrue(promotion.isSet("tiers.0.0.abundance-point-multiplier"));
        assertTrue(promotion.isSet("tiers.0.0.rare-seed-chance"));
    }

    private YamlConfiguration load(String resource) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull(stream, resource);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
