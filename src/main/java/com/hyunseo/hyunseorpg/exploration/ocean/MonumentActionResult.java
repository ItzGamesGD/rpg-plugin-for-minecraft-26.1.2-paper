package com.hyunseo.hyunseorpg.exploration.ocean;

/**
 * Immutable result for an attempted logical state transition.
 */
public record MonumentActionResult(boolean applied, Reason reason, MonumentPhase phase) {
    public enum Reason {
        APPLIED,
        DUPLICATE,
        UNKNOWN_ID,
        INVALID_PHASE,
        FINAL_SEAL_WARNING_REQUIRED,
        REWARD_ALREADY_CLAIMED,
        TERMINAL
    }

    static MonumentActionResult applied(MonumentPhase phase) {
        return new MonumentActionResult(true, Reason.APPLIED, phase);
    }

    static MonumentActionResult rejected(Reason reason, MonumentPhase phase) {
        return new MonumentActionResult(false, reason, phase);
    }
}
