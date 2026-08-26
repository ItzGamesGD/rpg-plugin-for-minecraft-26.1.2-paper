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

    /** A durable complete flag always wins over a stale pending marker. */
    public static PyramidUndergroundCompletionState reconcile(boolean completeFlag, String rawState) {
        return completeFlag ? COMPLETE : parse(rawState);
    }
}
