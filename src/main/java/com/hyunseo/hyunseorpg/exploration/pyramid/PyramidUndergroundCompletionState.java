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
     * Returns the persisted state that the repository reconciliation still needs to handle.
     *
     * The boolean completion flag is authoritative for gameplay, but a true flag paired
     * with anything except the durable COMPLETE marker is still a persistence repair.
     * Represent that contradiction as COMPLETION_PENDING so the caller cannot accidentally
     * erase the evidence before it writes the normalized COMPLETE marker.
     */
    public static PyramidUndergroundCompletionState reconcile(boolean completeFlag, String rawState) {
        PyramidUndergroundCompletionState persisted = parse(rawState);
        if (completeFlag && persisted != COMPLETE) return COMPLETION_PENDING;
        return persisted;
    }
}
