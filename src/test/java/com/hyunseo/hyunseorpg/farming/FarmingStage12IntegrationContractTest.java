package com.hyunseo.hyunseorpg.farming;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 12 regression contracts. These tests validate the current structure
 * and temporary inputs; they do not approve final economy values.
 */
class FarmingStage12IntegrationContractTest {
    private static final List<String> CROPS = List.of("corn", "onion", "chili", "garlic");
    private static final List<String> PROMOTION_TIERS = List.of("0", "1", "2", "3", "4", "5");

    @Test
    void activeQualityDistributionIsCompleteAndPromotionShiftPreservesTotal() {
        YamlConfiguration quality = load("farming/quality.yml");
        ConfigurationSection base = quality.getConfigurationSection("base-distribution");
        assertNotNull(base);
        Map<CropQuality, Double> baseDistribution = readDistribution(base);
        assertEquals(100.0D, sum(baseDistribution), 0.000001D);
        assertTrue(baseDistribution.values().stream().allMatch(value -> value >= 0.0D));

        YamlConfiguration promotion = load("farming/hoe_promotion.yml");
        ConfigurationSection tiers = promotion.getConfigurationSection("tiers");
        assertNotNull(tiers);
        double previousAdvanced = -1.0D;
        double previousSupreme = -1.0D;
        for (String tier : PROMOTION_TIERS) {
            String path = "tiers." + tier + ".0";
            double shift = promotion.getDouble(path + ".quality-density-shift", -1.0D);
            assertTrue(shift >= 0.0D, path);
            Map<CropQuality, Double> effective = shifted(baseDistribution, shift);
            assertEquals(100.0D, sum(effective), 0.000001D);
            assertTrue(effective.values().stream().allMatch(value -> value >= 0.0D));
            assertTrue(effective.get(CropQuality.ADVANCED) >= previousAdvanced);
            assertTrue(effective.get(CropQuality.SUPREME) >= previousSupreme);
            previousAdvanced = effective.get(CropQuality.ADVANCED);
            previousSupreme = effective.get(CropQuality.SUPREME);
        }
    }

    @Test
    void hoeEnhancementAndPromotionRemainSeparateByDesign() {
        YamlConfiguration enhancement = load("farming/hoe_enhancement.yml");
        YamlConfiguration promotion = load("farming/hoe_promotion.yml");
        assertTrue(enhancement.isSet("limits.maximum-quality-sale-bonus"));
        assertFalse(enhancement.isSet("levels.0.quality-density-shift"));
        assertFalse(enhancement.isSet("levels.0.abundance-point-multiplier"));
        assertTrue(promotion.isSet("tiers.0.0.quality-density-shift"));
        assertTrue(promotion.isSet("tiers.0.0.abundance-point-multiplier"));
        assertFalse(promotion.isSet("levels.0.quality-sale-bonus"));
    }

    @Test
    void pointAndEssenceContractsAreConnectedWithoutFinalizingBalance() {
        YamlConfiguration harvest = load("farming/harvest.yml");
        YamlConfiguration deliveries = load("farming/deliveries.yml");
        YamlConfiguration essence = load("farming/essence.yml");

        assertTrue(harvest.getLong("direct.abundance-points", 0L) > 0L);
        assertEquals(0L, deliveries.getLong("definitions.farmer_crops.preview-base-points", -1L));
        assertEquals(0L, deliveries.getLong("definitions.alchemist_processed.preview-base-points", -1L));
        assertTrue(essence.getLong("required-abundance-points", 0L) > 0L);
        assertFalse(essence.isSet("inputs"));
        assertEquals("expert", essence.getString("unlock-stage"));
    }

    @Test
    void growthInputsRemainWhileCurrencyShopIsRetired() {
        YamlConfiguration crops = load("farming/crops.yml");
        YamlConfiguration growth = load("farming/growth.yml");
        for (String crop : CROPS) {
            assertEquals(4, crops.getInt("crops." + crop + ".stages"));
            assertEquals(60L, growth.getLong("crops." + crop + ".seconds-per-stage"));
        }
        assertFalse(java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/shops.yml")));
        assertEquals(1200L, load("farming/deliveries.yml").getLong("refresh-seconds"));
        assertEquals(1200L, load("farming/deliveries.yml").getLong("time-limit-seconds"));
    }

    @Test
    void directPointEligibilityExcludesIndirectCauses() {
        assertEquals(1L, HarvestService.directHarvestPointAmount(
                1L, HarvestCause.DIRECT_PLAYER, true, true));
        assertEquals(2L, HarvestService.directHarvestPointAmount(
                1L, 1.25D, HarvestCause.DIRECT_PLAYER, true, true, 0.0D));
        assertEquals(1L, HarvestService.directHarvestPointAmount(
                1L, 1.25D, HarvestCause.DIRECT_PLAYER, true, true, 0.99D));
        assertEquals(0L, HarvestService.directHarvestPointAmount(
                1L, HarvestCause.WATER, true, true));
        assertEquals(0L, HarvestService.directHarvestPointAmount(
                1L, HarvestCause.PISTON, true, true));
        assertEquals(0L, HarvestService.directHarvestPointAmount(
                1L, HarvestCause.EXPLOSION, true, true));
    }

    private Map<CropQuality, Double> readDistribution(ConfigurationSection section) {
        Map<CropQuality, Double> result = new EnumMap<>(CropQuality.class);
        for (CropQuality quality : CropQuality.values()) {
            result.put(quality, section.getDouble(quality.id(), -1.0D));
            assertTrue(result.get(quality) >= 0.0D, quality.id());
        }
        return result;
    }

    private Map<CropQuality, Double> shifted(Map<CropQuality, Double> base, double shift) {
        Map<CropQuality, Double> result = new EnumMap<>(CropQuality.class);
        double total = 0.0D;
        for (CropQuality quality : CropQuality.values()) {
            double value = Math.max(0.0D, base.get(quality) + shift * quality.ordinal());
            result.put(quality, value);
            total += value;
        }
        for (CropQuality quality : CropQuality.values()) {
            result.put(quality, result.get(quality) * 100.0D / total);
        }
        return result;
    }

    private double sum(Map<CropQuality, Double> values) {
        return values.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private YamlConfiguration load(String name) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(stream, name);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            fail(exception);
            return new YamlConfiguration();
        }
    }
}
