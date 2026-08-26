package com.hyunseo.hyunseorpg.exploration.pyramid;

/** Single durable state vocabulary for Pyramid underground completion. */
public enum PyramidUndergroundCompletionState {
    UNSOLVED("unsolved"),
    COMPLETION_PENDING("completion_pending"),
    COMPLETE("complete");

    private final String value;
    PyramidUndergroundCompletionState(String value) { this.value = value; }
    public String value() { return value; }

    public static PyramidUndergroundCompletionState parse(String raw) {
        for (PyramidUndergroundCompletionState state : values())
            if (state.value.equalsIgnoreCase(raw == null ? "" : raw.trim())) return state;
        return UNSOLVED;
    }

    /**
     * Returns the persisted state that still needs to be reconciled by the caller.
     *
     * A durable complete flag is authoritative for gameplay, but a stale/missing
     * state marker must remain observable long enough for the repository layer to
     * normalize it to COMPLETE. Returning COMPLETE unconditionally for a true flag
     * would hide the contradictory metadata and skip that durable normalization.
     */
    public static PyramidUndergroundCompletionState reconcile(boolean completeFlag, String rawState) {
        PyramidUndergroundCompletionState persisted = parse(rawState);
        if (!completeFlag) return persisted;
        return persisted == COMPLETE ? COMPLETE : persisted;
    }
}
