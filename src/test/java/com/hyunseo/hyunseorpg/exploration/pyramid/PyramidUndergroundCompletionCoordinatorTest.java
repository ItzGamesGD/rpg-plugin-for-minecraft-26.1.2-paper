package com.hyunseo.hyunseorpg.exploration.pyramid;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PyramidUndergroundCompletionCoordinatorTest {
    @Test
    void pendingCompletionConvergesOnceAndSurvivesRepositoryRestart() throws IOException {
        PersistedStorage storage = new PersistedStorage(pyramid(Map.of(
                "pyramid-underground-completion-state", "completion_pending")));
        StructureRepository first = repository(storage);

        StructureRecord completed = PyramidUndergroundCompletionCoordinator.complete(first, storage.recordId());
        assertTrue(Boolean.parseBoolean(completed.activationMetadata().get("pyramid-underground-complete")));
        assertEquals("complete", completed.activationMetadata().get("pyramid-underground-completion-state"));
        assertEquals(1, storage.saveCalls);

        PyramidUndergroundCompletionCoordinator.complete(first, storage.recordId());
        assertEquals(1, storage.saveCalls, "repeated reconciliation is idempotent");

        StructureRepository restarted = repository(storage);
        StructureRecord durable = restarted.get(storage.recordId()).orElseThrow();
        assertTrue(Boolean.parseBoolean(durable.activationMetadata().get("pyramid-underground-complete")));
        assertEquals("complete", durable.activationMetadata().get("pyramid-underground-completion-state"));
    }

    @Test
    void legacyCompletePendingContradictionNormalizesThroughTheSameTransaction() throws IOException {
        PersistedStorage storage = new PersistedStorage(pyramid(Map.of(
                "pyramid-underground-complete", "true",
                "pyramid-underground-completion-state", "completion_pending")));
        StructureRecord completed = PyramidUndergroundCompletionCoordinator.complete(repository(storage), storage.recordId());

        assertEquals("true", completed.activationMetadata().get("pyramid-underground-complete"));
        assertEquals("complete", completed.activationMetadata().get("pyramid-underground-completion-state"));
        assertEquals(1, storage.saveCalls);
    }

    @Test
    void recoveryRequiredPendingStateCannotCompleteOrReward() throws IOException {
        PersistedStorage storage = new PersistedStorage(pyramid(Map.of(
                "pyramid-underground-completion-state", "completion_pending",
                "pyramid-failure-state", "RECOVERY_REQUIRED")));
        StructureRecord unchanged = PyramidUndergroundCompletionCoordinator.complete(repository(storage), storage.recordId());
        assertEquals("completion_pending", unchanged.activationMetadata().get("pyramid-underground-completion-state"));
        assertFalse(Boolean.parseBoolean(unchanged.activationMetadata().getOrDefault("pyramid-underground-complete", "false")));
        assertEquals(0, storage.saveCalls);
    }

    @Test
    void unsolvedRecordCannotBeCompletedByRecoveryOrAnArbitraryCaller() throws IOException {
        PersistedStorage storage = new PersistedStorage(pyramid(Map.of()));
        StructureRecord unchanged = PyramidUndergroundCompletionCoordinator.complete(repository(storage), storage.recordId());

        assertFalse(Boolean.parseBoolean(unchanged.activationMetadata()
                .getOrDefault("pyramid-underground-complete", "false")));
        assertEquals(0, storage.saveCalls);
    }

    @Test
    void failedIntentIsNotVisibleToTheSameLiveRepositoryAndRetryCanPersistIt() throws IOException {
        PersistedStorage storage = new PersistedStorage(pyramid(Map.of()));
        StructureRepository repository = repository(storage);
        storage.failNextSave = true;

        assertThrows(IOException.class, () -> PyramidUndergroundCompletionCoordinator
                .persistSolvedIntent(repository, storage.recordId()));
        assertEquals(PyramidUndergroundCompletionState.UNSOLVED,
                PyramidUndergroundCompletionState.parse(repository.get(storage.recordId()).orElseThrow()
                        .activationMetadata().get("pyramid-underground-completion-state")));

        assertTrue(PyramidUndergroundCompletionCoordinator.persistSolvedIntent(repository, storage.recordId()));
        assertEquals(PyramidUndergroundCompletionState.COMPLETION_PENDING,
                PyramidUndergroundCompletionState.parse(repository.get(storage.recordId()).orElseThrow()
                        .activationMetadata().get("pyramid-underground-completion-state")));
    }

    @Test
    void atomicSolvedIntentIncludesLogicalPositionAndFailedSaveLeavesBothUnchanged() throws IOException {
        PersistedStorage storage = new PersistedStorage(pyramid(Map.of()));
        StructureRepository repository = repository(storage);
        storage.failNextSave = true;

        assertThrows(IOException.class, () -> PyramidUndergroundCompletionCoordinator
                .persistSolvedIntentAndLogicalPosition(repository, storage.recordId(), "a", new PyramidGridPoint(1, 2)));

        StructureRecord unchanged = repository.get(storage.recordId()).orElseThrow();
        assertEquals(PyramidUndergroundCompletionState.UNSOLVED,
                PyramidUndergroundCompletionState.parse(unchanged.activationMetadata()
                        .get("pyramid-underground-completion-state")));
        assertFalse(unchanged.activationMetadata().containsKey("pyramid-pillar-position-a"));

        assertTrue(PyramidUndergroundCompletionCoordinator.persistSolvedIntentAndLogicalPosition(
                repository, storage.recordId(), "a", new PyramidGridPoint(1, 2)));
        StructureRecord durable = repository.get(storage.recordId()).orElseThrow();
        assertEquals(PyramidUndergroundCompletionState.COMPLETION_PENDING,
                PyramidUndergroundCompletionState.parse(durable.activationMetadata()
                        .get("pyramid-underground-completion-state")));
        assertEquals("1,2", durable.activationMetadata().get("pyramid-pillar-position-a"));
        assertEquals("true", durable.activationMetadata().get("pyramid-pillar-solved-a"));
    }

    @Test
    void failedIntentWriteLeavesNoSolvedEvidenceForRestartBecauseTheFinalMoveMustNotProceed() throws IOException {
        PersistedStorage storage = new PersistedStorage(pyramid(Map.of()));
        StructureRepository first = repository(storage);
        storage.failNextSave = true;

        assertThrows(IOException.class, () -> PyramidUndergroundCompletionCoordinator
                .persistSolvedIntent(first, storage.recordId()));

        StructureRepository restarted = repository(storage);
        StructureRecord durable = restarted.get(storage.recordId()).orElseThrow();
        assertEquals(PyramidUndergroundCompletionState.UNSOLVED,
                PyramidUndergroundCompletionState.parse(durable.activationMetadata()
                        .get("pyramid-underground-completion-state")));
        assertFalse(Boolean.parseBoolean(durable.activationMetadata()
                .getOrDefault("pyramid-underground-complete", "false")));
    }

    private static StructureRepository repository(PersistedStorage storage) throws IOException {
        StructureRepository repository = new StructureRepository(storage, new StructureIndex());
        repository.ensureWorldLoaded(storage.worldId);
        return repository;
    }

    private static StructureRecord pyramid(Map<String, String> metadata) {
        UUID world = UUID.randomUUID();
        return new StructureRecord(UUID.randomUUID(), world, "desert_pyramid", "minecraft:desert_pyramid",
                new StructureAnchor(world, 0, 64, 0),
                new StructureBounds(-12, 58, -12, 12, 80, 12),
                true, "desert_pyramid", StructureEventState.ACTIVE, metadata, false, Instant.now(), null, 1);
    }

    private static final class PersistedStorage implements StructureStorage {
        private final UUID worldId;
        private List<StructureRecord> durable;
        private int saveCalls;
        private boolean failNextSave;

        private PersistedStorage(StructureRecord record) {
            this.worldId = record.worldId();
            this.durable = List.of(record);
        }

        private UUID recordId() {
            return durable.getFirst().structureId();
        }

        @Override
        public List<StructureRecord> loadWorld(UUID worldId) {
            return List.copyOf(durable);
        }

        @Override
        public void saveWorld(UUID worldId, List<StructureRecord> records) throws IOException {
            if (failNextSave) {
                failNextSave = false;
                throw new IOException("simulated persistence failure");
            }
            saveCalls++;
            durable = List.copyOf(records);
        }
    }
}
