package com.hyunseo.hyunseorpg.shop;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ShopDialogSelectionTest {
    @Test
    void zeroIsExcludedAndMultipleProductsAreCollectedInOneSubmission() {
        Map<String, Float> response = Map.of("quantity_0", 12.0F, "quantity_1", 0.0F, "quantity_2", 47.0F);

        Map<Integer, Integer> result = ShopDialogSelection.read(List.of(1, 1, 1), response::get, ignored -> 64);

        assertEquals(List.of(12, 47), result.values().stream().toList());
        assertFalse(result.containsKey(1));
    }

    @Test
    void rejectsHardCapCurrentMaximumFractionsAndIncompleteBundles() {
        Map<String, Float> response = Map.of("quantity_0", 65.0F, "quantity_1", 6.0F,
                "quantity_2", 1.5F, "quantity_3", 6.0F);

        Map<Integer, Integer> result = ShopDialogSelection.read(List.of(1, 1, 1, 4), response::get,
                index -> index == 1 ? 5 : 64);

        assertEquals(Map.of(), result);
    }
}
