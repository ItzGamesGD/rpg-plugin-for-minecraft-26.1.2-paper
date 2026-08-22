package com.hyunseo.hyunseorpg.exploration.model;

/** Persistent world-shared lifecycle defined by the exploration design. */
public enum StructureEventState {
    VANILLA,
    UNDISCOVERED,
    ACTIVE,
    CLEARED,
    ABANDONED;

    public boolean terminal() {
        return this == VANILLA || this == CLEARED || this == ABANDONED;
    }
}
