package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationStructureDefinition;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationTriggerPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplorationTriggerPolicyTest {
    private static final UUID WORLD = UUID.randomUUID();
    private static final UUID OTHER_WORLD = UUID.randomUUID();

    private final ExplorationTriggerPolicy policy = new ExplorationTriggerPolicy();
    private final ExplorationStructureDefinition enabled = definition(true);
    private final StructureRecord undiscovered = record(StructureEventState.UNDISCOVERED);
    private final StructureRecord active = record(StructureEventState.ACTIVE);

    @Test
    void allowsUndiscoveredStructureInsideTriggerRadius() {
        assertTrue(policy.isEligible(undiscovered, enabled, WORLD, 0.0, 64.0, 0.0));
    }

    @Test
    void allowsActiveStructureInsideTriggerRadius() {
        assertTrue(policy.isEligible(active, enabled, WORLD, 35.0, 64.0, 0.0));
    }

    @Test
    void rejectsPointOutsideTriggerRadius() {
        assertFalse(policy.isEligible(undiscovered, enabled, WORLD, 40.0, 64.0, 0.0));
    }

    @Test
    void rejectsDifferentWorld() {
        assertFalse(policy.isEligible(undiscovered, enabled, OTHER_WORLD, 0.0, 64.0, 0.0));
    }

    @Test
    void rejectsTerminalStates() {
        assertFalse(policy.isEligible(record(StructureEventState.CLEARED), enabled, WORLD, 0.0, 64.0, 0.0));
        assertFalse(policy.isEligible(record(StructureEventState.VANILLA), enabled, WORLD, 0.0, 64.0, 0.0));
    }

    @Test
    void rejectsDisabledDefinitions() {
        assertFalse(policy.isEligible(undiscovered, definition(false), WORLD, 0.0, 64.0, 0.0));
    }

    private static StructureRecord record(StructureEventState state) {
        return new StructureRecord(
                UUID.randomUUID(),
                WORLD,
                "test-structure",
                state,
                new StructureBounds(-4, 60, -4, 4, 70, 4),
                List.of(),
                null,
                0L
        );
    }

    private static ExplorationStructureDefinition definition(boolean enabled) {
        return new ExplorationStructureDefinition(
                "test-structure",
                enabled,
                32.0,
                0.0,
                List.of(),
                List.of(),
                List.of()
        );
    }
}
