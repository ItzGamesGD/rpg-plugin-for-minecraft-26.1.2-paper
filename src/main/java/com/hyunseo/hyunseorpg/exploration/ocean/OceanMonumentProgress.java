package com.hyunseo.hyunseorpg.exploration.ocean;

/**
 * Runtime-independent logical state for a possible future Ocean Monument encounter.
 * This type deliberately has no world, persistence, listener, or plugin dependencies.
 */
public final class OceanMonumentProgress {
    private static final int REQUIRED_ELDER_GUARDIANS = 3;

    private MonumentPhase phase = MonumentPhase.DORMANT;
    private int defeatedElderGuardians;

    public MonumentPhase phase() {
        return phase;
    }

    public int defeatedElderGuardians() {
        return defeatedElderGuardians;
    }

    public MonumentActionResult begin() {
        if (phase != MonumentPhase.DORMANT) return MonumentActionResult.ALREADY_APPLIED;
        phase = MonumentPhase.ELDER_GUARDIANS;
        return MonumentActionResult.ACCEPTED;
    }

    public MonumentActionResult defeatElderGuardian() {
        if (phase != MonumentPhase.ELDER_GUARDIANS) return MonumentActionResult.INVALID_PHASE;
        if (defeatedElderGuardians >= REQUIRED_ELDER_GUARDIANS) return MonumentActionResult.ALREADY_APPLIED;
        defeatedElderGuardians++;
        if (defeatedElderGuardians == REQUIRED_ELDER_GUARDIANS) phase = MonumentPhase.CORE_EXPOSED;
        return MonumentActionResult.ACCEPTED;
    }

    public MonumentActionResult complete() {
        if (phase == MonumentPhase.COMPLETED) return MonumentActionResult.ALREADY_APPLIED;
        if (phase != MonumentPhase.CORE_EXPOSED) return MonumentActionResult.INVALID_PHASE;
        phase = MonumentPhase.COMPLETED;
        return MonumentActionResult.ACCEPTED;
    }
}
