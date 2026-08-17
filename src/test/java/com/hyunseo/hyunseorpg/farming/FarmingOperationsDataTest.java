package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.BuildInfo;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FarmingOperationsDataTest {
    @Test
    void stageAndUnlockAreIndependentAtDataLayer() {
        PlayerRPGData data = new PlayerRPGData(java.util.UUID.randomUUID());
        data.setFarmingStage(FarmingStage.EXPERT);
        data.lockFarmingCrop("corn");
        assertEquals(FarmingStage.EXPERT, data.getFarmingStage());
        assertFalse(data.isFarmingCropUnlocked("corn"));
        data.unlockFarmingCrop("onion");
        assertTrue(data.isFarmingCropUnlocked("onion"));
    }

    @Test
    void managedFarmingDefaultsContainCanonicalFilesAndCrops() throws Exception {
        for (String file : List.of("crops.yml", "growth.yml", "harvest.yml", "progression.yml", "quality.yml")) {
            assertNotNull(getClass().getClassLoader().getResource("farming/" + file));
        }
        try (var stream = getClass().getClassLoader().getResourceAsStream("farming/crops.yml")) {
            assertNotNull(stream);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (String crop : List.of("corn", "onion", "chili", "garlic")) {
                assertTrue(config.isConfigurationSection("crops." + crop));
            }
        }
    }

    @Test
    void buildIdentityIsAvailableForStaleJarChecks() {
        assertTrue(BuildInfo.BUILD_ID.matches("2026-08-08_\\d{3}-[a-z0-9-]+"));
    }
}
