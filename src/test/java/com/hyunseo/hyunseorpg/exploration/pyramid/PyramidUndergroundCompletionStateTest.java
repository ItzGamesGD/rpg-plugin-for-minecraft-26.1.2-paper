package com.hyunseo.hyunseorpg.exploration.pyramid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Behavioural contract for the only durable underground-completion vocabulary. */
class PyramidUndergroundCompletionStateTest {
    @Test void pendingStateSurvivesRestartUntilRepositoryNormalizesIt() {
        var afterRestart = PyramidUndergroundCompletionState.reconcile(false, "completion_pending");
        assertEquals(PyramidUndergroundCompletionState.COMPLETION_PENDING, afterRestart);

        // The complete flag is authoritative for gameplay, but the stale marker must
        // remain visible so ExplorationRuntimeManager performs the durable write.
        var contradictory = PyramidUndergroundCompletionState.reconcile(true, afterRestart.value());
        assertEquals(PyramidUndergroundCompletionState.COMPLETION_PENDING, contradictory);
        assertNotEquals(PyramidUndergroundCompletionState.COMPLETE, contradictory);
    }

    @Test void alreadyNormalizedCompleteStateIsIdempotent() {
        var first = PyramidUndergroundCompletionState.reconcile(true, "complete");
        var second = PyramidUndergroundCompletionState.reconcile(true, first.value());
        assertEquals(PyramidUndergroundCompletionState.COMPLETE, first);
        assertEquals(first, second);
    }

    @Test void completeFlagTurnsMissingOrUnknownPersistedStateIntoRepairPending() {
        assertEquals(PyramidUndergroundCompletionState.COMPLETION_PENDING,
                PyramidUndergroundCompletionState.reconcile(true, null));
        assertEquals(PyramidUndergroundCompletionState.COMPLETION_PENDING,
                PyramidUndergroundCompletionState.reconcile(true, "legacy-garbage"));
    }

    @Test void unknownLegacyStateWithoutCompleteEvidenceFailsClosedToUnsolved() {
        assertEquals(PyramidUndergroundCompletionState.UNSOLVED,
                PyramidUndergroundCompletionState.reconcile(false, "legacy-garbage"));
    }
}
