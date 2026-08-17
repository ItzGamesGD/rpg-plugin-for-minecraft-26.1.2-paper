package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FarmingStage9RewardTest {
    @Test
    void abundanceAndFavorAreNumericPlayerDataAndProviderBound() {
        PlayerRPGData data = new PlayerRPGData(UUID.randomUUID());
        assertEquals(0L, data.getFarmingAbundancePoints());
        assertEquals(0L, data.getFarmingFavor("farmer"));

        data.addFarmingAbundancePoints(12L);
        data.addFarmingFavor("farmer", 3L);

        assertEquals(12L, data.getFarmingAbundancePoints());
        assertEquals(3L, data.getFarmingFavor("farmer"));
        assertEquals(0L, data.getFarmingFavor("alchemist"));
    }

    @Test
    void onlySuccessfulDirectMatureHarvestCanProduceHarvestPoints() {
        assertEquals(7L, HarvestService.directHarvestPointAmount(
                7L, HarvestCause.DIRECT_PLAYER, true, true));
        assertEquals(0L, HarvestService.directHarvestPointAmount(
                7L, HarvestCause.DIRECT_PLAYER, false, true));
        assertEquals(0L, HarvestService.directHarvestPointAmount(
                7L, HarvestCause.WATER, true, true));
        assertEquals(0L, HarvestService.directHarvestPointAmount(
                7L, HarvestCause.PISTON, true, true));
        assertEquals(14L, HarvestService.directHarvestPointAmount(
                7L, 2.0D, HarvestCause.DIRECT_PLAYER, true, true));
        assertEquals(0L, HarvestService.directHarvestPointAmount(
                7L, Double.NaN, HarvestCause.DIRECT_PLAYER, true, true));
        assertEquals(0L, HarvestService.directHarvestPointAmount(
                -1L, HarvestCause.DIRECT_PLAYER, true, true));
    }

    @Test
    void previewExposesAllStage9FormulaTermsWithoutMutatingPlayerData() {
        EnumMap<CropQuality, Integer> counts = new EnumMap<>(CropQuality.class);
        counts.put(CropQuality.NORMAL, 1);
        DeliveryQualitySummary quality = new DeliveryQualitySummary(counts, 0.0D, 1);
        DeliveryPreview preview = new DeliveryPreview(quality, 10L, 1.0D, 1.2D, 1.1D,
                4L, 1.3D, 17L, 2L, 10L, "upper-middle");

        assertEquals(10L, preview.basePoints());
        assertEquals(1.0D, preview.deliveryMultiplier());
        assertEquals(1.2D, preview.qualityMultiplier());
        assertEquals(1.1D, preview.hoeMultiplier());
        assertEquals(4L, preview.currentFavor());
        assertEquals(1.3D, preview.favorMultiplier());
        assertEquals(17L, preview.finalPoints());
        assertEquals(2L, preview.favorIncrease());
        assertEquals("upper-middle", preview.qualityBand());
    }

    @Test
    void stage9DefaultsAreNeutralAndConfiguredSeparately() {
        try (InputStream deliveries = getClass().getClassLoader()
                .getResourceAsStream("farming/deliveries.yml");
             InputStream favor = getClass().getClassLoader()
                     .getResourceAsStream("farming/favor.yml")) {
            assertNotNull(deliveries);
            assertNotNull(favor);
            YamlConfiguration deliveryYaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(deliveries, StandardCharsets.UTF_8));
            YamlConfiguration favorYaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(favor, StandardCharsets.UTF_8));
            assertEquals(1.0D, deliveryYaml.getDouble("reward.quality-multipliers.normal"));
            assertFalse(favorYaml.getBoolean("enabled"));
            assertEquals(0L, favorYaml.getLong("providers.farmer.max-favor"));
            assertEquals(1.0D, favorYaml.getDouble("providers.alchemist.base-multiplier"));
        } catch (Exception exception) {
            fail(exception);
        }
    }
}
