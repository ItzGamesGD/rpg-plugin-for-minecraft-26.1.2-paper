package com.hyunseo.hyunseorpg.exploration.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Map;

class PyramidGeometryProtectionPolicyTest {
    @Test
    void roomFloorWallsAndCeilingAreProtected() {
        assertTrue(ExplorationRuntimeManager.pyramidGeometryProtected(0, 99, 0,
                0, 100, 0, 4, 4, 0, 110, 0));
        assertTrue(ExplorationRuntimeManager.pyramidGeometryProtected(4, 104, -4,
                0, 100, 0, 4, 4, 0, 110, 0));
        assertFalse(ExplorationRuntimeManager.pyramidGeometryProtected(5, 104, 0,
                0, 100, 0, 4, 4, 0, 90, 0));
    }

    @Test
    void completeThreeByThreeShaftVerticalRangeIsProtected() {
        for (int y = 104; y <= 110; y++) {
            assertTrue(ExplorationRuntimeManager.pyramidGeometryProtected(1, y, -1,
                    0, 100, 0, 4, 4, 0, 110, 0));
        }
        assertFalse(ExplorationRuntimeManager.pyramidGeometryProtected(2, 110, 0,
                0, 100, 0, 4, 4, 0, 110, 0));
        assertFalse(ExplorationRuntimeManager.pyramidGeometryProtected(0, 111, 0,
                0, 100, 0, 4, 4, 0, 110, 0));
    }

    @Test
    void unrelatedNearbyCoordinatesRemainUnprotected() {
        assertFalse(ExplorationRuntimeManager.pyramidGeometryProtected(10, 95, 10,
                0, 100, 0, 4, 4, 0, 110, 0));
    }

    @Test
    void ownershipStartsAtPrepareAndEndsAfterCleanup() {
        assertTrue(ExplorationRuntimeManager.pyramidGeometryOwned(
                Map.of("pyramid-room-prepared", "true"), false, false, false, false));
        assertTrue(ExplorationRuntimeManager.pyramidGeometryOwned(
                Map.of(), false, true, false, false));
        assertTrue(ExplorationRuntimeManager.pyramidGeometryOwned(
                Map.of("pyramid-room-created", "true"), false, false, false, false));
        assertFalse(ExplorationRuntimeManager.pyramidGeometryOwned(
                Map.of(), false, false, false, false));
    }
}
