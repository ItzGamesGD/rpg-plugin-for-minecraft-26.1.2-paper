package com.hyunseo.hyunseorpg.shop;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopDialogSelectionTest {
    @Test
    void acceptsBuyQuantity64AndSkipsOnlyZero() {
        ShopDialogSelection.Result result = ShopDialogSelection.read(List.of(1, 1),
                Map.of("quantity_0", 64.0F, "quantity_1", 0.0F)::get);

        assertTrue(result.valid());
        assertEquals(Map.of(0, 64), result.quantities());
        assertFalse(result.quantities().containsKey(1));
    }

    @Test
    void fractionalOverCapMalformedAndInvalidBundleFailWholeSelection() {
        assertFalse(readOne(1.5F, 1).valid());
        assertFalse(readOne(65.0F, 1).valid());
        assertFalse(ShopDialogSelection.read(List.of(1), ignored -> null).valid());
        assertFalse(readOne(6.0F, 4).valid());
    }

    @Test
    void sellClampUsesOwnedQuantityAndLargestCompleteBundle() {
        assertEquals(17, ShopDialogSelection.clampSellQuantity(48, 17, 1));
        assertEquals(16, ShopDialogSelection.clampSellQuantity(48, 17, 4));
        assertEquals(0, ShopDialogSelection.clampSellQuantity(48, 3, 4));
    }

    private ShopDialogSelection.Result readOne(float value, int bundleSize) {
        return ShopDialogSelection.read(List.of(bundleSize), ignored -> value);
    }
}
