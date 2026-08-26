package com.hyunseo.hyunseorpg.exploration.pyramid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Behavioural contract for the only durable underground-completion vocabulary. */
class PyramidUndergroundCompletionStateTest {
    @Test void pendingStateSurvivesRestartAndConvergesToComplete() {
        var afterRestart = PyramidUndergroundCompletionState.reconcile(false, "completion_pending");
        assertEquals(PyramidUndergroundCompletionState.COMPLETION_PENDING, afterRestart);
        var afterDurableRetry = PyramidUndergroundCompletionState.reconcile(true, afterRestart.value());
        assertEquals(PyramidUndergroundCompletionState.COMPLETE, afterDurableRetry);
    }

    @Test void repeatedRecoveryIsIdempotent() {
        var first = PyramidUndergroundCompletionState.reconcile(true, "completion_pending");
        var second = PyramidUndergroundCompletionState.reconcile(true, first.value());
        assertEquals(PyramidUndergroundCompletionState.COMPLETE, first);
        assertEquals(first, second);
    }

    @Test void durableCompleteFlagWinsOverStalePendingMarker() {
        assertEquals(PyramidUndergroundCompletionState.COMPLETE,
                PyramidUndergroundCompletionState.reconcile(true, "completion_pending"));
    }

    @Test void unknownLegacyStateFailsClosedToUnsolved() {
        assertEquals(PyramidUndergroundCompletionState.UNSOLVED,
                PyramidUndergroundCompletionState.reconcile(false, "legacy-garbage"));
    }
}
