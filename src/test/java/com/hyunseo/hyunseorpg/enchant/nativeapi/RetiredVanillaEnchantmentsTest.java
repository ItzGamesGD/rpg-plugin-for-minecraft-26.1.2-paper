package com.hyunseo.hyunseorpg.enchant.nativeapi;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RetiredVanillaEnchantmentsTest {
    @Test
    void everyDuplicateMapsToItsVanillaKeyAndIsAbsentFromGameFacingSources() throws Exception {
        assertEquals(List.of("protection", "fire_protection", "blast_protection", "projectile_protection",
                "thorns", "respiration", "aqua_affinity", "swift_sneak", "depth_strider", "soul_speed",
                "frost_walker", "unbreaking").stream().sorted().toList(),
                RetiredVanillaEnchantments.IDS.stream().sorted().toList());
        for (var mapping : RetiredVanillaEnchantments.VANILLA_TARGETS.entrySet()) {
            String oldId = mapping.getKey();
            String vanillaId = mapping.getValue();
            assertEquals(oldId, vanillaId);
            assertFalse(NativeEnchantDefinitions.BY_ID.containsKey(oldId));
        }
        String yaml = Files.readString(Path.of("src/main/resources/enchants.yml"));
        String items = Files.readString(Path.of("src/main/resources/items.yml"));
        for (String id : RetiredVanillaEnchantments.IDS) {
            assertFalse(yaml.contains("  " + id + ":"), id);
            String book = id.equals("unbreaking") ? "enchant_book_durability_save_pickaxe" : "enchant_book_" + id;
            assertFalse(items.contains(book), book);
            for (String tag : List.of("in_enchanting_table", "tradeable", "on_random_loot", "treasure", "non_treasure")) {
                assertFalse(DatapackJsonTestSupport.tagValues("minecraft", tag).contains("hyunseorpg:" + id), id + " in " + tag);
            }
        }
    }

    @Test
    void migrationMergesByMaximumAndClampsWithoutAddingLevels() {
        for (String ignored : RetiredVanillaEnchantments.IDS) {
            assertEquals(4, RetiredVanillaEnchantments.mergedLevel(0, 4, 0, 4)); // custom only
            assertEquals(3, RetiredVanillaEnchantments.mergedLevel(3, 0, 0, 4)); // vanilla only
            assertEquals(4, RetiredVanillaEnchantments.mergedLevel(3, 4, 0, 4));
            assertEquals(4, RetiredVanillaEnchantments.mergedLevel(4, 2, 0, 4));
            assertEquals(3, RetiredVanillaEnchantments.mergedLevel(3, 3, 0, 4));
            assertEquals(4, RetiredVanillaEnchantments.mergedLevel(1, 99, 0, 4));
            int firstPass = RetiredVanillaEnchantments.mergedLevel(3, 4, 0, 4);
            assertEquals(firstPass, RetiredVanillaEnchantments.mergedLevel(firstPass, 4, 0, 4));
        }
        // Swift Sneak's historical vanilla snapshot participates in the same maximum rule.
        assertEquals(3, RetiredVanillaEnchantments.mergedLevel(1, 2, 3, 3));
    }
}
