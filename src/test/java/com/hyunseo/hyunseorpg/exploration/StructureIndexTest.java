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
}
