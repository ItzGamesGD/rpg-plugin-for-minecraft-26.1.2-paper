package com.hyunseo.hyunseorpg.farming;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FarmingStage4DataTest {
    @Test
    void harvestDefaultsKeepDirectAndIndirectPoliciesSeparate() {
        YamlConfiguration harvest = load("farming/harvest.yml");

        assertTrue(harvest.getBoolean("enabled"));
        assertEquals(1, harvest.getInt("direct.crop-amount"));
        assertEquals(1, harvest.getInt("direct.seed-amount"));
        assertTrue(harvest.getDouble("indirect.crop-drop-chance") > 0.0D);
        assertTrue(harvest.getDouble("indirect.crop-drop-chance") < 1.0D);
        assertEquals(1, harvest.getInt("indirect.crop-amount"));
    }

    @Test
    void harvestCausesKeepDirectAndIndirectProcessingDistinct() {
        assertNotEquals(HarvestCause.DIRECT_PLAYER, HarvestCause.WATER);
        assertNotEquals(HarvestCause.PISTON, HarvestCause.EXPLOSION);
        assertNotEquals(HarvestCause.SOIL_LOSS, HarvestCause.OTHER_NON_PLAYER);
    }

    @Test
    void invalidIndirectChanceFallsBackInsteadOfBecomingGuaranteed() {
        assertEquals(0.10D, HarvestService.safeIndirectCropDropChance(10.0D));
        assertEquals(0.10D, HarvestService.safeIndirectCropDropChance(-0.1D));
        assertEquals(0.10D, HarvestService.safeIndirectCropDropChance(Double.NaN));
        assertEquals(0.10D, HarvestService.safeIndirectCropDropChance(Double.POSITIVE_INFINITY));
        assertEquals(0.10D, HarvestService.safeIndirectCropDropChance(0.10D));
        assertEquals(1.0D, HarvestService.safeIndirectCropDropChance(1.0D));
    }

    @Test
    void vanillaDropSuppressionIncludesSupportingSoil() {
        UUID world = UUID.randomUUID();
        CropPosition crop = new CropPosition(world, 10, 65, -3);

        Set<CropPosition> positions = HarvestService.vanillaDropSuppressionPositions(crop, false);

        assertTrue(positions.contains(crop));
        assertTrue(positions.contains(new CropPosition(world, 10, 64, -3)));
    }

    @Test
    void twoBlockVanillaDropSuppressionIncludesUpperAndSupportingSoil() {
        UUID world = UUID.randomUUID();
        CropPosition crop = new CropPosition(world, 10, 65, -3);

        Set<CropPosition> positions = HarvestService.vanillaDropSuppressionPositions(crop, true);

        assertTrue(positions.contains(crop));
        assertTrue(positions.contains(new CropPosition(world, 10, 64, -3)));
        assertTrue(positions.contains(new CropPosition(world, 10, 66, -3)));
        assertEquals(3, positions.size());
    }

    private YamlConfiguration load(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
