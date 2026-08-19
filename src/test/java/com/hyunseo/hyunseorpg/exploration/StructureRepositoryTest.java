package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.model.StructureAnchor;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureIndex;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureRepository;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureStorage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class StructureRepositoryTest {
    @Test
    void failedWorldLoadRemainsRetryable() throws IOException {
        UUID world = UUID.randomUUID();
        StructureRecord record = record(world);
        RetryingStorage storage = new RetryingStorage(record);
        StructureRepository repository = new StructureRepository(storage, new StructureIndex());

        assertThrows(IOException.class, () -> repository.ensureWorldLoaded(world));
        repository.ensureWorldLoaded(world);

        assertEquals(2, storage.loadAttempts);
        assertEquals(record, repository.get(record.structureId()).orElseThrow());
    }

    private StructureRecord record(UUID world) {
        return new StructureRecord(UUID.randomUUID(), world, "swamp_hut", "minecraft:swamp_hut",
                new StructureAnchor(world, 0, 64, 0),
                new StructureBounds(-4, 60, -4, 4, 70, 4),
                false, "", StructureEventState.VANILLA, Map.of(), false,
                Instant.now(), null, 1);
    }

    private static final class RetryingStorage implements StructureStorage {
        private final StructureRecord record;
        private int loadAttempts;

        private RetryingStorage(StructureRecord record) {
            this.record = record;
        }

        @Override
        public List<StructureRecord> loadWorld(UUID worldId) throws IOException {
            loadAttempts++;
            if (loadAttempts == 1) throw new IOException("simulated load failure");
            return List.of(record);
        }

        @Override
        public void saveWorld(UUID worldId, List<StructureRecord> records) {
            // Not used by this regression test.
        }
    }
}
