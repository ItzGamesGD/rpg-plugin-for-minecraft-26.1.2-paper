package com.hyunseo.hyunseorpg.exploration.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PyramidGeometryProtectionPolicyTest {
    @Test
    void roomFloorWallsAndCeilingAreProtected() {
        assertTrue(ExplorationRuntimeManager.pyramidGeometryProtected(0, 99, 0,
                0, 100, 0, 4, 4, 0, 90, 0));
        assertTrue(ExplorationRuntimeManager.pyramidGeometryProtected(4, 104, -4,
                0, 100, 0, 4, 4, 0, 90, 0));
        assertFalse(ExplorationRuntimeManager.pyramidGeometryProtected(5, 104, 0,
                0, 100, 0, 4, 4, 0, 90, 0));
    }

    @Test
    void completeThreeByThreeShaftVerticalRangeIsProtected() {
        assertTrue(ExplorationRuntimeManager.pyramidGeometryProtected(0, 90, 0,
                0, 100, 0, 4, 4, 0, 90, 0));
        assertTrue(ExplorationRuntimeManager.pyramidGeometryProtected(1, 104, -1,
                0, 100, 0, 4, 4, 0, 90, 0));
        assertFalse(ExplorationRuntimeManager.pyramidGeometryProtected(2, 90, 0,
                0, 100, 0, 4, 4, 0, 90, 0));
        assertFalse(ExplorationRuntimeManager.pyramidGeometryProtected(0, 89, 0,
                0, 100, 0, 4, 4, 0, 90, 0));
    }

    @Test
    void unrelatedNearbyCoordinatesRemainUnprotected() {
        assertFalse(ExplorationRuntimeManager.pyramidGeometryProtected(10, 95, 10,
                0, 100, 0, 4, 4, 0, 90, 0));
    }
}
