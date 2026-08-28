package com.hyunseo.hyunseorpg.exploration.listener;

import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExplorationChestLootListenerTest {
    @Test
    void actualRemovalSemanticsIncludeNormalShiftClickAndHotbarTransfer() {
        assertTrue(ExplorationChestLootListener.isLootRemovalAction(InventoryAction.PICKUP_ALL));
        assertTrue(ExplorationChestLootListener.isLootRemovalAction(InventoryAction.MOVE_TO_OTHER_INVENTORY));
        assertTrue(ExplorationChestLootListener.isLootRemovalAction(InventoryAction.HOTBAR_SWAP));
        assertTrue(ExplorationChestLootListener.isLootRemovalAction(InventoryAction.HOTBAR_MOVE_AND_READD));
    }

    @Test
    void openingOrPlacingItemsIsNotLootRemoval() {
        assertFalse(ExplorationChestLootListener.isLootRemovalAction(InventoryAction.PLACE_ALL));
        assertFalse(ExplorationChestLootListener.isLootRemovalAction(InventoryAction.NOTHING));
    }
}
