package com.hyunseo.hyunseorpg.exploration.ocean;

/**
 * Pure logical progression for the Ocean Monument content. Runtime presentation,
 * world mutation and entity work intentionally live outside this model.
 */
public enum MonumentPhase {
    DISCOVERY,
    SEAL_OBJECTIVES,
    FINAL_SEAL_READY,
    TRANSITION_PENDING,
    ENCOUNTER_ACTIVE,
    BOSS_ELIGIBLE,
    CLEAR_ELIGIBLE,
    CLEARED,
    ABANDONED;

    public boolean terminal() {
        return this == CLEARED || this == ABANDONED;
    }
}
