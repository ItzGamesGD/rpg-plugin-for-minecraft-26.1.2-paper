package com.hyunseo.hyunseorpg.exploration.ocean;

/** Outcome of a pure Ocean Monument state transition. */
public enum MonumentActionResult {
    APPLIED,
    DUPLICATE,
    UNKNOWN_ID,
    INVALID_PHASE,
    ACKNOWLEDGEMENT_REQUIRED,
    NOT_ELIGIBLE,
    TERMINAL
}
