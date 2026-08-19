package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.component.impl.ScriptedSpawnComponent;
import com.hyunseo.hyunseorpg.exploration.integration.ExplorationPorts;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime;
import com.hyunseo.hyunseorpg.exploration.runtime.TeleportExemptionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptedSpawnComponentTest {
    private static final UUID WORLD = UUID.randomUUID();
    private static final UUID SPAWNED = UUID.randomUUID();

    @Test
    void objectiveTracksOnlyValidSpawnIds() {
        ExplorationRuntime runtime = new ExplorationRuntime(UUID.randomUUID(), "elite_witch_prototype");
        ExplorationPorts ports = new ExplorationPorts(
                (mobId, location, count, options) -> List.of(SPAWNED, null),
                null, null, null, null, null, null);

        new ScriptedSpawnComponent().execute(context(runtime, ports), spec(true));

        assertTrue(runtime.objectiveMode());
        assertEquals(Set.of(SPAWNED), runtime.objectiveEntities());
    }

    @Test
    void emptyObjectiveSpawnFailsActivationContract() {
        ExplorationRuntime runtime = new ExplorationRuntime(UUID.randomUUID(), "elite_witch_prototype");
        ExplorationPorts ports = new ExplorationPorts(
                (mobId, location, count, options) -> List.of(),
                null, null, null, null, null, null, null);

        assertThrows(IllegalStateException.class,
                () -> new ScriptedSpawnComponent().execute(context(runtime, ports), spec(true)));
        assertFalse(runtime.objectiveMode());
        assertTrue(runtime.objectiveEntities().isEmpty());
    }

    private static ExplorationEventContext context(ExplorationRuntime runtime, ExplorationPorts ports) {
        StructureRecord record = new StructureRecord(
                runtime.structureId(),
                WORLD,
                "swamp_hut",
                StructureEventState.ACTIVE,
                new StructureBounds(-4, 60, -4, 4, 70, 4),
                List.of(),
                "elite_witch_prototype",
                0L);
        return new ExplorationEventContext(
                null, record, runtime, ports, new TeleportExemptionService(), 0L);
    }

    private static ExplorationComponentSpec spec(boolean objective) {
        return new ExplorationComponentSpec("scripted_spawn",
                Map.of("mob-id", "vanilla:witch", "count", 1, "objective", objective));
    }
}
