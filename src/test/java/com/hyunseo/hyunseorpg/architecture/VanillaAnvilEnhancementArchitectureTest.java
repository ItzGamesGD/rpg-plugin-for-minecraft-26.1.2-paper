package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VanillaAnvilEnhancementArchitectureTest {
    @Test
    void listenerAddsOnlyPrepareRecipeAndLeavesAtomicTransactionToVanilla() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/hyunseo/hyunseorpg/enhancement/VanillaAnvilEnhancementListener.java"));
        assertTrue(source.contains("PrepareAnvilEvent"));
        assertFalse(source.contains("InventoryClickEvent"));
        assertFalse(source.contains("InventoryDragEvent"));
        assertTrue(source.contains("setRepairItemCountCost(STONE_COST)"));
        assertTrue(source.contains("setRepairCost(enhancements.getXpLevelCost"));
        assertTrue(source.contains("ItemStack result = equipment.clone()"));
        assertFalse(source.contains("CoinService"));
        assertFalse(source.contains("ThreadLocalRandom"));
        assertFalse(source.contains("setLevel("));
        assertFalse(source.contains("getExp("));
        assertFalse(source.contains("setExp("));
        assertTrue(source.contains("event.setResult(null)"), "invalid and capped stone recipes clear stale output");
    }

    @Test
    void cloneBasedResultDoesNotRebuildMetadataOrEnchantments() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/hyunseo/hyunseorpg/enhancement/VanillaAnvilEnhancementListener.java"));
        assertFalse(source.contains("new ItemStack"));
        assertFalse(source.contains("setType("));
        assertFalse(source.contains("removeEnchant"));
        assertFalse(source.contains("setDurability"));
        assertFalse(source.contains("setItemMeta"));
    }
}
