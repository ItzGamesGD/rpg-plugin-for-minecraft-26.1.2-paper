package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FarmingStage8DeliveryTest {
    @Test
    void deliveryDefaultsDefineProvidersAndTwentyMinuteRefresh() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("farming/deliveries.yml")) {
            assertNotNull(input);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            assertEquals(1200L, yaml.getLong("refresh-seconds"));
            assertEquals(1200L, yaml.getLong("time-limit-seconds"));
            assertEquals("farmer", yaml.getString("definitions.farmer_crops.provider"));
            assertEquals("alchemist", yaml.getString("definitions.alchemist_processed.provider"));
            assertTrue(yaml.isConfigurationSection("definitions.estate_reserved"));
            assertTrue(yaml.getStringList("definitions.farmer_crops.item-families").contains("crop_corn"));
        } catch (Exception exception) {
            fail(exception);
        }
    }

    @Test
    void activeLifetimeUsesTimeLimitInsteadOfRefreshCooldown() {
        assertEquals(1_201_000L, DeliveryGenerator.addSeconds(1_000L, 1_200L));
        assertEquals(Long.MAX_VALUE, DeliveryGenerator.addSeconds(
                Long.MAX_VALUE - 500L, 1L));
    }

    @Test
    void deliveryStateIsPlayerBoundAndCompletedCountIsIndependent() {
        PlayerRPGData first = new PlayerRPGData(UUID.randomUUID());
        PlayerRPGData second = new PlayerRPGData(UUID.randomUUID());
        FarmingDeliveryState state = FarmingDeliveryState.active(
                "delivery-1", "farmer_crops", "crop_corn", 2,
                CropQuality.NORMAL, 1000L, 2000L);

        first.setFarmingDelivery("farmer", state);
        first.incrementFarmingDeliveryCompletedCount("farmer");

        assertEquals(state, first.getFarmingDelivery("farmer"));
        assertEquals(1, first.getFarmingDeliveryCompletedCount("farmer"));
        assertNull(second.getFarmingDelivery("farmer"));
        assertEquals(0, second.getFarmingDeliveryCompletedCount("farmer"));
    }

    @Test
    void qualityAggregationUsesActualSubmittedDistribution() {
        EnumMap<CropQuality, Integer> counts = new EnumMap<>(CropQuality.class);
        counts.put(CropQuality.NORMAL, 2);
        counts.put(CropQuality.SUPREME, 1);
        DeliveryQualitySummary summary = new DeliveryQualitySummary(counts, (0.0D + 2.0D * 0.0D + 1.0D * 95.0D) / 3.0D, 3);

        assertEquals(3, summary.totalAmount());
        assertEquals(2, summary.counts().get(CropQuality.NORMAL));
        assertEquals(1, summary.counts().get(CropQuality.SUPREME));
        assertEquals(95.0D / 3.0D, summary.averageScore());
    }

    @Test
    void completedDeliveryKeepsARefreshWindowAndLegacyStateDoesNotDeadlock() {
        DeliveryDefinition definition = new DeliveryDefinition(
                "farmer_crops", DeliveryProvider.FARMER, "Farmer", List.of("crop_corn"),
                1, 1, 0L, 1.0D);
        DeliverySession waiting = new DeliverySession(
                "delivery-1", definition,
                new DeliveryRequirement("crop_corn", 1, CropQuality.NORMAL),
                1000L, 2000L, DeliveryStatus.COMPLETED, 11_000L);

        assertEquals(10L, waiting.remainingSeconds(1000L));
        assertEquals(0L, waiting.remainingSeconds(11_000L));

        FarmingDeliveryState legacy = new FarmingDeliveryState(
                "delivery-legacy", "farmer_crops", "crop_corn", 1,
                CropQuality.NORMAL, 1000L, 2000L, DeliveryStatus.COMPLETED);
        assertEquals(0L, legacy.statusAt());
    }

    @Test
    void deliveryDefinitionWeightProvidesConfigurableTypeSelection() {
        DeliveryDefinition disabled = new DeliveryDefinition(
                "disabled", DeliveryProvider.FARMER, "Disabled", List.of("crop_corn"),
                1, 1, 0L, 1.0D, 0);
        DeliveryDefinition selected = new DeliveryDefinition(
                "selected", DeliveryProvider.FARMER, "Selected", List.of("crop_onion"),
                2, 4, 0L, 1.0D, 1);

        assertEquals("selected", DeliveryGenerator.weightedDefinition(List.of(disabled, selected)).id());
        assertEquals(2, selected.amountMin());
        assertEquals(4, selected.amountMax());
    }
}
