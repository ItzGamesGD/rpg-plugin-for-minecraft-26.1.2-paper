package com.hyunseo.hyunseorpg.farming;

import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryGuiPolicyTest {
    @Test
    void onlySafePickupPlacementActionsAreAllowed() {
        assertTrue(DeliveryGuiService.isSafeInputAction(InventoryAction.PICKUP_ALL));
        assertTrue(DeliveryGuiService.isSafeInputAction(InventoryAction.PICKUP_HALF));
        assertTrue(DeliveryGuiService.isSafeInputAction(InventoryAction.PLACE_ALL));
        assertTrue(DeliveryGuiService.isSafeInputAction(InventoryAction.PLACE_ONE));
        assertTrue(DeliveryGuiService.isSafeInputAction(InventoryAction.SWAP_WITH_CURSOR));
        assertFalse(DeliveryGuiService.isSafeInputAction(InventoryAction.MOVE_TO_OTHER_INVENTORY));
        assertFalse(DeliveryGuiService.isSafeInputAction(InventoryAction.HOTBAR_SWAP));
        assertFalse(DeliveryGuiService.isSafeInputAction(InventoryAction.COLLECT_TO_CURSOR));
    }

    @Test
    void onlyConfiguredInputSlotsAcceptDrag() {
        assertTrue(DeliveryGuiService.isInputSlot(10));
        assertTrue(DeliveryGuiService.isInputSlot(25));
        assertFalse(DeliveryGuiService.isInputSlot(40));
        assertTrue(DeliveryGuiService.allowsInputDrag(Set.of(10, 11), 54));
        assertFalse(DeliveryGuiService.allowsInputDrag(Set.of(10, 40), 54));
        assertFalse(DeliveryGuiService.allowsInputDrag(Set.of(10, 54), 54));
        assertFalse(DeliveryGuiService.allowsInputDrag(Set.of(), 54));
    }
}
