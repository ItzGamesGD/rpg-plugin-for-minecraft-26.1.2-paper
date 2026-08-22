package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.model.StructureAnchor;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class StructureRecordTransitionTest {
    @Test
    void lifecycleOnlyAllowsDesignTransitions() {
        StructureRecord base = record(StructureEventState.UNDISCOVERED);
        StructureRecord active = base.transitionTo(StructureEventState.ACTIVE, Instant.ofEpochMilli(2));
        assertEquals(StructureEventState.ACTIVE, active.state());
        StructureRecord cleared = active.transitionTo(StructureEventState.CLEARED, Instant.ofEpochMilli(3));
        assertEquals(StructureEventState.CLEARED, cleared.state());
        assertNotNull(cleared.completedAt());
        assertThrows(IllegalStateException.class, () -> cleared.transitionTo(StructureEventState.ACTIVE, Instant.now()));
    }

    @Test
    void vanillaIsTerminalAndCannotCarryVariant() {
        UUID world = UUID.randomUUID();
        StructureRecord vanilla = new StructureRecord(UUID.randomUUID(), world, "swamp_hut", "minecraft:swamp_hut",
                new StructureAnchor(world, 0, 64, 0), new StructureBounds(-4, 60, -4, 4, 70, 4),
                false, "", StructureEventState.VANILLA, Map.of(), false, Instant.now(), null, 1);
        assertTrue(vanilla.state().terminal());
        assertThrows(IllegalStateException.class, () -> vanilla.transitionTo(StructureEventState.ACTIVE, Instant.now()));
    }

    private StructureRecord record(StructureEventState state) {
        UUID world = UUID.randomUUID();
        return new StructureRecord(UUID.randomUUID(), world, "swamp_hut", "minecraft:swamp_hut",
                new StructureAnchor(world, 0, 64, 0), new StructureBounds(-4, 60, -4, 4, 70, 4),
                true, "elite_witch_prototype", state, Map.of(), false, Instant.ofEpochMilli(1), null, 1);
    }
}
