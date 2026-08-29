package com.hyunseo.hyunseorpg.exploration.runtime;

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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

final class PyramidRecoveryAuthorityTest {
    @Test
    void quarantinePrecedesSuccessfulDurableFailureWrite() throws IOException {
        Storage storage = new Storage(pyramid());
        StructureRepository repository = repository(storage);
        ExplorationRuntimeManager manager = manager(repository);
        ExplorationRuntime runtime = new ExplorationRuntime(storage.id(), "guardian_trial");
        AtomicBoolean stoppedAfterBarrier = new AtomicBoolean();

        manager.markPyramidRecoveryRequired(storage.id(), runtime, "display-missing", () ->
                stoppedAfterBarrier.set(runtime.sequence().flag("pyramid.recovery.required")
                        && manager.isPyramidQuarantined(storage.id())));

        assertTrue(stoppedAfterBarrier.get());
        assertTrue(runtime.sequence().flag("pyramid.recovery.required"));
        assertTrue(manager.isPyramidQuarantined(storage.id()));
        assertNull(manager.progressionRecord(storage.id(), runtime));
        assertEquals("RECOVERY_REQUIRED", repository.get(storage.id()).orElseThrow()
                .activationMetadata().get("pyramid-failure-state"));
    }

    @Test
    void failedFailureWriteRetainsRuntimeBarrierAndQuarantine() throws IOException {
        Storage storage = new Storage(pyramid());
        StructureRepository repository = repository(storage);
        ExplorationRuntimeManager manager = manager(repository);
        ExplorationRuntime runtime = new ExplorationRuntime(storage.id(), "guardian_trial");
        storage.fail = true;

        manager.markPyramidRecoveryRequired(storage.id(), runtime, "display-missing", () -> { });

        assertTrue(runtime.sequence().flag("pyramid.recovery.required"));
        assertTrue(manager.isPyramidQuarantined(storage.id()));
        assertNull(manager.progressionRecord(storage.id(), runtime));
        assertFalse(repository.get(storage.id()).orElseThrow().activationMetadata()
                .containsKey("pyramid-failure-state"));
    }

    private static ExplorationRuntimeManager manager(StructureRepository repository) {
        return new ExplorationRuntimeManager(null, null, repository, null, null, new TeleportExemptionService());
    }

    private static StructureRepository repository(Storage storage) throws IOException {
        StructureRepository repository = new StructureRepository(storage, new StructureIndex());
        repository.ensureWorldLoaded(storage.world);
        return repository;
    }

    private static StructureRecord pyramid() {
        UUID world = UUID.randomUUID();
        return new StructureRecord(UUID.randomUUID(), world, "desert_pyramid", "minecraft:desert_pyramid",
                new StructureAnchor(world, 0, 64, 0), new StructureBounds(-10, 58, -10, 10, 80, 10),
                true, "guardian_trial", StructureEventState.ACTIVE, Map.of(), false, Instant.now(), null, 1);
    }

    private static final class Storage implements StructureStorage {
        private final UUID world;
        private List<StructureRecord> records;
        private boolean fail;
        private Storage(StructureRecord record) { world = record.worldId(); records = List.of(record); }
        private UUID id() { return records.getFirst().structureId(); }
        @Override public List<StructureRecord> loadWorld(UUID worldId) { return List.copyOf(records); }
        @Override public void saveWorld(UUID worldId, List<StructureRecord> next) throws IOException {
            if (fail) throw new IOException("simulated failure");
            records = List.copyOf(next);
        }
    }
}
