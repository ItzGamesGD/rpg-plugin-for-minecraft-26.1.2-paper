package com.hyunseo.hyunseorpg.item;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FarmingItemStackIdentityTest {
    @Test
    void canonicalIdentitySeparatesCropAndQuality() {
        assertEquals(new RPGItemService.FarmingIdentity("crop_corn", "corn", "normal"),
                RPGItemService.farmingIdentity("crop_corn").orElseThrow());
        assertEquals(new RPGItemService.FarmingIdentity("crop_corn_quality_basic", "corn", "basic"),
                RPGItemService.farmingIdentity("crop_corn_quality_basic").orElseThrow());
        assertEquals(new RPGItemService.FarmingIdentity("processed_onion_concentrate_normal", "onion", "normal"),
                RPGItemService.farmingIdentity("processed_onion_concentrate_normal").orElseThrow());
        assertEquals(new RPGItemService.FarmingIdentity("seed_corn", "corn", "none"),
                RPGItemService.farmingIdentity("seed_corn").orElseThrow());
    }

    @Test
    void sameCanonicalIdentityIsEqualButDifferentQualityIsNot() {
        RPGItemService.FarmingIdentity first = RPGItemService.farmingIdentity(
                "crop_corn_quality_basic").orElseThrow();
        RPGItemService.FarmingIdentity second = RPGItemService.farmingIdentity(
                "crop_corn_quality_basic").orElseThrow();
        RPGItemService.FarmingIdentity differentQuality = RPGItemService.farmingIdentity(
                "crop_corn_quality_supreme").orElseThrow();

        assertEquals(first, second);
        assertNotEquals(first, differentQuality);
    }

    @Test
    void stackableFarmingMetadataHasNoPerInstanceKeys() {
        Set<String> keys = RPGItemService.stackableFarmingMetadataKeys();

        assertEquals(Set.of("item_id", "farming_crop_id", "farming_quality", "farming_crop_data_version"), keys);
        assertTrue(keys.stream().noneMatch(key -> key.contains("instance")));
        assertTrue(keys.stream().noneMatch(key -> key.contains("session")));
    }
}
