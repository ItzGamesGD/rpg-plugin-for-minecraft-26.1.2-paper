package com.hyunseo.hyunseorpg.exploration.ocean;

/** Logical phases of the dormant Ocean Monument design. */
public enum MonumentPhase {
    UNDISCOVERED,
    SEALED,
    TRANSITION,
    ENCOUNTER,
    CLEARED,
    ABANDONED;

    public boolean terminal() {
        return this == CLEARED || this == ABANDONED;
    }
}
