package com.hyunseo.hyunseorpg.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VanillaStackingPolicyTest {
    @Test
    void allowListIncludesPotionVehiclesAndSaddle() {
        assertEquals(VanillaStackingPolicy.Group.POTION,
                VanillaStackingPolicy.groupName("POTION"));
        assertEquals(VanillaStackingPolicy.Group.POTION,
                VanillaStackingPolicy.groupName("SPLASH_POTION"));
        assertEquals(VanillaStackingPolicy.Group.VEHICLES,
                VanillaStackingPolicy.groupName("OAK_BOAT"));
        assertEquals(VanillaStackingPolicy.Group.VEHICLES,
                VanillaStackingPolicy.groupName("OAK_CHEST_BOAT"));
        assertEquals(VanillaStackingPolicy.Group.VEHICLES,
                VanillaStackingPolicy.groupName("BAMBOO_RAFT"));
        assertEquals(VanillaStackingPolicy.Group.VEHICLES,
                VanillaStackingPolicy.groupName("MINECART"));
        assertEquals(VanillaStackingPolicy.Group.UTILITY,
                VanillaStackingPolicy.groupName("SADDLE"));
    }

    @Test
    void excludesRpgEquipmentAndAmbiguousItems() {
        assertEquals(VanillaStackingPolicy.Group.NONE,
                VanillaStackingPolicy.groupName("DIAMOND_SWORD"));
        assertEquals(VanillaStackingPolicy.Group.NONE,
                VanillaStackingPolicy.groupName("DIAMOND_PICKAXE"));
        assertEquals(VanillaStackingPolicy.Group.NONE,
                VanillaStackingPolicy.groupName("SHULKER_BOX"));
        assertEquals(VanillaStackingPolicy.Group.NONE,
                VanillaStackingPolicy.groupName("WRITTEN_BOOK"));
        assertTrue(VanillaStackingPolicy.groupName("TNT_MINECART")
                == VanillaStackingPolicy.Group.VEHICLES);
    }
}
