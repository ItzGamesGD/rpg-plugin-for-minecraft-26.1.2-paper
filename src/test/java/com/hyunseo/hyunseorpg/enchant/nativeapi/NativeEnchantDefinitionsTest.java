package com.hyunseo.hyunseorpg.enchant.nativeapi;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class NativeEnchantDefinitionsTest {
    @Test
    void everyRegistryKeyIsUniqueAndValid() {
        var ids = new HashSet<String>();
        for (var definition : NativeEnchantDefinitions.ALL) {
            assertTrue(definition.id().matches("[a-z0-9_]+"), definition.id());
            assertTrue(ids.add(definition.id()), "duplicate registry key: " + definition.id());
        }
        assertEquals(NativeEnchantDefinitions.ALL.size(), NativeEnchantDefinitions.BY_ID.size());
    }

    @Test
    void everyDefinitionHasCompleteVanillaMetadata() {
        for (var definition : NativeEnchantDefinitions.ALL) {
            assertFalse(definition.displayName().isBlank(), definition.id());
            assertTrue(definition.maxLevel() >= 1, definition.id());
            assertTrue(definition.weight() >= 1 && definition.weight() <= 1024, definition.id());
            assertTrue(definition.minimumBaseCost() >= 1, definition.id());
            assertTrue(definition.maximumBaseCost() >= definition.minimumBaseCost(), definition.id());
            assertTrue(definition.anvilCost() >= 0, definition.id());
            assertTrue(definition.supportedItemTag().startsWith("enchantable/"), definition.id());
        }
    }

    @Test
    void knownLevelAndSupportedItemContractsAreStable() {
        assertEquals(4, NativeEnchantDefinitions.BY_ID.get("protection").maxLevel());
        assertEquals("enchantable/sword", NativeEnchantDefinitions.BY_ID.get("blade_chain").supportedItemTag());
        assertEquals("enchantable/bow", NativeEnchantDefinitions.BY_ID.get("laser_arrow").supportedItemTag());
        assertEquals("enchantable/foot_armor", NativeEnchantDefinitions.BY_ID.get("frost_walker").supportedItemTag());
    }
}
