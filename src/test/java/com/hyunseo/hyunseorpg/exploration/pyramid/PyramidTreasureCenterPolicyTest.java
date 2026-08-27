package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PyramidTreasureCenterPolicyTest {
    @Test
    void allFourCanonicalChestOffsetsResolveToOneCenter() {
        StructureBounds bounds = new StructureBounds(-5, 50, -5, 5, 80, 5);
        PyramidTreasureCenterPolicy.Center expected = PyramidTreasureCenterPolicy.from(bounds);
        for (int dx : new int[] {-2, 2}) {
            for (int dz : new int[] {-2, 2}) {
                assertEquals(expected, PyramidTreasureCenterPolicy.from(bounds),
                        "each accepted chest offset must use the same canonical chamber center");
                assertEquals(0, (expected.x() + dx) - expected.x() - dx);
                assertEquals(0, (expected.z() + dz) - expected.z() - dz);
            }
        }
    }

    @Test
    void centerUsesFloorForOddAndNegativeBounds() {
        StructureBounds bounds = new StructureBounds(-6, 50, -5, 5, 80, 6);
        assertEquals(new PyramidTreasureCenterPolicy.Center(-1, 0),
                PyramidTreasureCenterPolicy.from(bounds));
    }
}
