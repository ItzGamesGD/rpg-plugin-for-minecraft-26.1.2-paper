package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FarmingStage3DataTest {
    @Test
    void newProfileStartsAtBasicWithOnlyCornUnlocked() {
        PlayerRPGData data = new PlayerRPGData(UUID.randomUUID());

        assertEquals(3, data.getFarmingDataVersion());
        assertEquals(FarmingStage.BASIC, data.getFarmingStage());
        assertTrue(data.isFarmingCropUnlocked("corn"));
        assertFalse(data.isFarmingCropUnlocked("onion"));
        assertFalse(data.isFarmingCropUnlocked("chili"));
        assertFalse(data.isFarmingCropUnlocked("garlic"));
    }

    @Test
    void profileDataIsPlayerBoundAndHarvestCountsAreIndependent() {
        PlayerRPGData first = new PlayerRPGData(UUID.randomUUID());
        PlayerRPGData second = new PlayerRPGData(UUID.randomUUID());

        first.unlockFarmingCrop("onion");
        first.setFarmingStage(FarmingStage.SKILLED);
        first.addFarmingValidHarvest("corn", 3L);

        assertTrue(first.isFarmingCropUnlocked("onion"));
        assertEquals(FarmingStage.SKILLED, first.getFarmingStage());
        assertEquals(3L, first.getFarmingTotalValidHarvests());
        assertEquals(3L, first.getFarmingCropHarvestCount("corn"));
        assertFalse(second.isFarmingCropUnlocked("onion"));
        assertEquals(0L, second.getFarmingTotalValidHarvests());
    }

    @Test
    void lockAndTokenDataAreSafeAndNormalized() {
        PlayerRPGData data = new PlayerRPGData(UUID.randomUUID());

        data.unlockFarmingCrop(" Onion ");
        data.setFarmingStatTokenUses("Quality", 2);
        data.lockFarmingCrop("onion");

        assertFalse(data.isFarmingCropUnlocked("onion"));
        assertEquals(2, data.getFarmingStatTokenUses("quality"));
        assertTrue(FarmingStage.fromInput("숙련").isPresent());
        assertTrue(FarmingStage.fromInput("skilled").isPresent());
        assertTrue(FarmingStage.fromInput("unknown").isEmpty());
    }
}
