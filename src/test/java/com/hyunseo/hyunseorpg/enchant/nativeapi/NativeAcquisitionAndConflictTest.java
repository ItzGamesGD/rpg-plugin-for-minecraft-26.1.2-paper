package com.hyunseo.hyunseorpg.enchant.nativeapi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NativeAcquisitionAndConflictTest {
    @Test
    void everyDefinitionHasExplicitPolicyAndMatchingMinecraftTagResources() throws Exception {
        Set<String> table = entries("in_enchanting_table");
        Set<String> trade = entries("tradeable");
        Set<String> loot = entries("on_random_loot");
        Set<String> treasure = entries("treasure");
        Set<String> nonTreasure = entries("non_treasure");
        for (var definition : NativeEnchantDefinitions.ALL) {
            assertNotNull(definition.acquisition(), definition.id());
            String key = "hyunseorpg:" + definition.id();
            assertEquals(definition.acquisition().enchantingTable(), table.contains(key), key + " table");
            assertEquals(definition.acquisition().tradeable(), trade.contains(key), key + " trade");
            assertEquals(definition.acquisition().randomLoot(), loot.contains(key), key + " loot");
            assertEquals(definition.acquisition().treasurePolicy() == AcquisitionPolicy.TreasurePolicy.TREASURE,
                    treasure.contains(key), key + " treasure");
            assertEquals(definition.acquisition().treasurePolicy() == AcquisitionPolicy.TreasurePolicy.NON_TREASURE,
                    nonTreasure.contains(key), key + " non_treasure");
            assertNotEquals(treasure.contains(key), nonTreasure.contains(key), key + " treasure partition");
        }
    }

    @Test
    void nativeExclusiveSetCoversCoApplicableInputConflictOnly() throws Exception {
        assertTrue(NativeEnchantDefinitions.conflicts("wind_arrow", "fire_arrow_rain"));
        assertTrue(entries("hyunseorpg", "exclusive_set/bow_shift_left")
                .containsAll(Set.of("hyunseorpg:wind_arrow", "hyunseorpg:fire_arrow_rain")));
        assertFalse(NativeEnchantDefinitions.conflicts("blade_chain", "light_greatsword"));
        assertFalse(NativeEnchantDefinitions.conflicts("laser_arrow", "wind_arrow"));
    }

    private Set<String> entries(String name) throws IOException {
        return DatapackJsonTestSupport.tagValues("minecraft", name);
    }

    private Set<String> entries(String namespace, String name) throws IOException {
        return DatapackJsonTestSupport.tagValues(namespace, name);
    }
}
