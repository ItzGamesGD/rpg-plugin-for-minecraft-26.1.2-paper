package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationEndReason;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationStatusSnapshot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExplorationStatusSnapshotTest {
    @Test
    void acceptsPersistentAndRuntimeDiagnosticState() {
        UUID id = UUID.randomUUID();
        ExplorationStatusSnapshot snapshot = new ExplorationStatusSnapshot(
                id, "swamp_hut", "elite_witch_prototype", StructureEventState.ACTIVE,
                true, 2, 1, ExplorationEndReason.ACTIVATION_FAILURE);

        assertEquals(id, snapshot.structureId());
        assertEquals(2, snapshot.participantCount());
        assertEquals(1, snapshot.objectiveCount());
        assertEquals(ExplorationEndReason.ACTIVATION_FAILURE, snapshot.lastEndReason());
    }

    @Test
    void rejectsNegativeCounters() {
        assertThrows(IllegalArgumentException.class, () -> new ExplorationStatusSnapshot(
                UUID.randomUUID(), "swamp_hut", "variant", StructureEventState.ACTIVE,
                true, -1, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new ExplorationStatusSnapshot(
                UUID.randomUUID(), "swamp_hut", "variant", StructureEventState.ACTIVE,
                true, 0, -1, null));
    }
}
