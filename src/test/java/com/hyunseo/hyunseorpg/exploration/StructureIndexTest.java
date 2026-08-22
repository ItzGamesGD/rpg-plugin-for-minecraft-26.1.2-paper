package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.model.*;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureIndex;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class StructureIndexTest {
    @Test
    void indexesEveryChunkTouchedByBoundsWithoutDuplicates() {
        UUID world = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        StructureRecord record = new StructureRecord(id, world, "ocean_monument", "minecraft:monument",
                new StructureAnchor(world, 16, 60, 16), new StructureBounds(0, 30, 0, 40, 80, 40),
                true, "seal_trial", StructureEventState.UNDISCOVERED, Map.of(), false, Instant.now(), null, 1);
        StructureIndex index = new StructureIndex();
        assertTrue(index.register(record));
        assertFalse(index.register(record));
        assertEquals(1, index.nearby(world, 20, 20, 80).size());
        assertEquals(id, index.getChunk(world, 2, 2).getFirst().structureId());
    }

    @Test
    void upsertRemovesStaleChunkEntriesAndKeepsWorldsIsolated() {
        UUID world = UUID.randomUUID();
        UUID otherWorld = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        StructureIndex index = new StructureIndex();

        StructureRecord original = record(id, world, new StructureBounds(0, 60, 0, 4, 70, 4));
        StructureRecord moved = record(id, world, new StructureBounds(64, 60, 64, 68, 70, 68));
        StructureRecord other = record(UUID.randomUUID(), otherWorld,
                new StructureBounds(64, 60, 64, 68, 70, 68));

        assertTrue(index.register(original));
        index.upsert(moved);
        index.register(other);

        assertTrue(index.getChunk(world, 0, 0).isEmpty());
        assertEquals(1, index.getChunk(world, 4, 4).size());
        assertTrue(index.nearby(world, 66, 66, 8).stream()
                .allMatch(candidate -> candidate.worldId().equals(world)));
        assertEquals(1, index.getChunk(otherWorld, 4, 4).size());
    }

    private StructureRecord record(UUID id, UUID world, StructureBounds bounds) {
        return new StructureRecord(id, world, "swamp_hut", "minecraft:swamp_hut",
                new StructureAnchor(world, bounds.centerX(), bounds.centerY(), bounds.centerZ()),
                bounds, false, "", StructureEventState.VANILLA, Map.of(), false,
                Instant.now(), null, 1);
    }
}
