package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.model.StructureAnchor;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PyramidRoomMetadataMergeTest {
    @Test
    void guardianFirstThenRoomPreparePreservesBothModulesMetadata() {
        StructureRecord guardianLatest = pyramid(Map.of(
                "pyramid-guardian-complete", "true", "unrelated", "preserve"));
        StructureRecord merged = PyramidRoomService.mergeUndergroundMetadata(guardianLatest, roomMetadata());

        assertEquals("true", merged.activationMetadata().get("pyramid-guardian-complete"));
        assertEquals("preserve", merged.activationMetadata().get("unrelated"));
        assertEquals("true", merged.activationMetadata().get("pyramid-room-prepared"));
    }

    @Test
    void roomFirstThenGuardianMergePreservesRoomMetadata() {
        StructureRecord roomLatest = PyramidRoomService.mergeUndergroundMetadata(pyramid(Map.of()), roomMetadata());
        StructureRecord guardianLatest = roomLatest.withMetadata("pyramid-guardian-complete", "true");

        assertEquals("1,40,2", guardianLatest.activationMetadata().get("pyramid-room-origin"));
        assertEquals("true", guardianLatest.activationMetadata().get("pyramid-guardian-complete"));
    }

    @Test
    void missingOrWrongAuthoritativeRecordIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> PyramidRoomService.mergeUndergroundMetadata(null, roomMetadata()));
        assertThrows(IllegalArgumentException.class,
                () -> PyramidRoomService.mergeUndergroundMetadata(record("pillager_outpost", Map.of()), roomMetadata()));
    }

    private static Map<String, String> roomMetadata() {
        return Map.of("pyramid-room-prepared", "true", "pyramid-room-origin", "1,40,2",
                "pyramid-room-radius", "4", "pyramid-room-height", "4");
    }

    private static StructureRecord pyramid(Map<String, String> metadata) {
        return record("desert_pyramid", metadata);
    }

    private static StructureRecord record(String type, Map<String, String> metadata) {
        UUID world = UUID.randomUUID();
        return new StructureRecord(UUID.randomUUID(), world, type, "minecraft:" + type,
                new StructureAnchor(world, 0, 64, 0), new StructureBounds(-10, 58, -10, 10, 80, 10),
                true, "guardian_trial", StructureEventState.ACTIVE, metadata, false, Instant.now(), null, 1);
    }
}
