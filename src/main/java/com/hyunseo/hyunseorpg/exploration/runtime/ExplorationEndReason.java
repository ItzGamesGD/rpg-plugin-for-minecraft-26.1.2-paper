package com.hyunseo.hyunseorpg.exploration.runtime;

/** Diagnostic lifecycle outcomes; this does not replace persistent StructureEventState. */
public enum ExplorationEndReason {
    FINAL_CLEAR,
    ABANDON_GRACE,
    ACTIVATION_FAILURE,
    COMPLETION_FAILURE,
    ABANDON_FAILURE,
    STATE_INVALID,
    PLUGIN_DISABLE
}
