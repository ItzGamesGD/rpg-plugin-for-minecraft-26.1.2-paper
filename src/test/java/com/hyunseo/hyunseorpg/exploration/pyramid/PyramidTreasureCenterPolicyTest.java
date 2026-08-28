package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PyramidTreasureCenterPolicyTest {
    @Test
    void allFourCanonicalChestOffsetsResolveToOneCenter() {
        StructureBounds bounds = new StructureBounds(-5, 50, -5, 5, 80, 5);
        PyramidTreasureCenterPolicy.Center expected = PyramidTreasureCenterPolicy.from(bounds);
        int chestY = bounds.minY() + PyramidTreasureCenterPolicy.CHEST_Y_OFFSET_FROM_BOUNDS_MIN;
        int[][] offsets = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        for (int[] offset : offsets) {
            assertTrue(PyramidTreasureCenterPolicy.isVanillaTreasureSlot(bounds,
                    expected.x() + offset[0], chestY, expected.z() + offset[1]));
            assertEquals(expected, PyramidTreasureCenterPolicy.from(bounds),
                    "each accepted chest offset must use the same canonical chamber center");
        }
    }

    @Test
    void rejectsDiagonalCenterAndWrongHeightSlots() {
        StructureBounds bounds = new StructureBounds(-5, 50, -5, 5, 80, 5);
        int chestY = bounds.minY() + PyramidTreasureCenterPolicy.CHEST_Y_OFFSET_FROM_BOUNDS_MIN;

        assertFalse(PyramidTreasureCenterPolicy.isVanillaTreasureSlot(bounds, 2, chestY, 2));
        assertFalse(PyramidTreasureCenterPolicy.isVanillaTreasureSlot(bounds, 0, chestY, 0));
        assertFalse(PyramidTreasureCenterPolicy.isVanillaTreasureSlot(bounds, 2, bounds.minY(), 0));
        assertFalse(PyramidTreasureCenterPolicy.isVanillaTreasureSlot(null, 2, chestY, 0));
    }

    @Test
    void centerUsesFloorForOddAndNegativeBounds() {
        StructureBounds bounds = new StructureBounds(-6, 50, -5, 5, 80, 6);
        assertEquals(new PyramidTreasureCenterPolicy.Center(-1, 0),
                PyramidTreasureCenterPolicy.from(bounds));
    }
}
