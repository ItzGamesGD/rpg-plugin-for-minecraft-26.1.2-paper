package com.hyunseo.hyunseorpg.exploration.runtime;

import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;

import java.util.Objects;
import java.util.UUID;

/** Immutable diagnostic view for a persistent structure and its optional runtime. */
public record ExplorationStatusSnapshot(
        UUID structureId,
        String structureType,
        String variantId,
        StructureEventState persistentState,
        boolean runtimeActive,
        int participantCount,
        int objectiveCount,
        ExplorationEndReason lastEndReason
) {
    public ExplorationStatusSnapshot {
        Objects.requireNonNull(structureId, "structureId");
        Objects.requireNonNull(structureType, "structureType");
        Objects.requireNonNull(variantId, "variantId");
        Objects.requireNonNull(persistentState, "persistentState");
        if (participantCount < 0) throw new IllegalArgumentException("participantCount < 0");
        if (objectiveCount < 0) throw new IllegalArgumentException("objectiveCount < 0");
    }
}
