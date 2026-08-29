package com.hyunseo.hyunseorpg.exploration.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hyunseo.hyunseorpg.exploration.model.StructureAnchor;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.time.Instant;

class PyramidGeometryProtectionPolicyTest {
    @Test
    void roomFloorWallsAndCeilingAreProtected() {
        assertTrue(ExplorationRuntimeManager.pyramidGeometryProtected(0, 99, 0,
                0, 100, 0, 4, 4, 0, 110, 0));
        assertTrue(ExplorationRuntimeManager.pyramidGeometryProtected(4, 104, -4,
                0, 100, 0, 4, 4, 0, 110, 0));
        assertFalse(ExplorationRuntimeManager.pyramidGeometryProtected(5, 104, 0,
                0, 100, 0, 4, 4, 0, 110, 0));
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

    @Test
    void authoritativeGeometryMetadataRejectsMissingOrMalformedFieldsInsteadOfDefaulting() {
        Map<String, String> valid = Map.of(
                "pyramid-room-origin", "0,40,0", "pyramid-room-radius", "4",
                "pyramid-room-height", "4", "pyramid-treasure-center-x", "0",
                "pyramid-treasure-center-z", "0");
        assertTrue(ExplorationRuntimeManager.pyramidGeometryFromMetadata(pyramid(valid))
                .protectedBlock(0, 39, 0));
        for (String key : valid.keySet()) {
            var missing = new java.util.LinkedHashMap<>(valid);
            missing.remove(key);
            assertThrows(IllegalArgumentException.class,
                    () -> ExplorationRuntimeManager.pyramidGeometryFromMetadata(pyramid(missing)), key);
        }
        var malformed = new java.util.LinkedHashMap<>(valid);
        malformed.put("pyramid-room-height", "broken");
        assertThrows(IllegalArgumentException.class,
                () -> ExplorationRuntimeManager.pyramidGeometryFromMetadata(pyramid(malformed)));
    }

    private static StructureRecord pyramid(Map<String, String> metadata) {
        UUID world = UUID.randomUUID();
        return new StructureRecord(UUID.randomUUID(), world, "desert_pyramid", "minecraft:desert_pyramid",
                new StructureAnchor(world, 0, 64, 0), new StructureBounds(-10, 58, -10, 10, 80, 10),
                true, "guardian_trial", StructureEventState.ACTIVE, metadata, false, Instant.now(), null, 1);
    }
}
