package com.hyunseo.hyunseorpg.exploration.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExplorationSequenceStateTest {
    @Test
    void actionAndPhaseTransitionsAreExactlyOnce() {
        ExplorationSequenceState state = new ExplorationSequenceState();

        assertTrue(state.transitionTo("warning", "trigger"));
        assertFalse(state.transitionTo("warning", "duplicate callback"));
        assertEquals("warning", state.currentPhase());
        assertEquals("trigger", state.lastTransitionReason());

        assertTrue(state.beginAction("spawn-wave"));
        assertFalse(state.beginAction("spawn-wave"));
    }

    @Test
    void flagsAndCountersAreRuntimeLocalAndDeterministic() {
        ExplorationSequenceState state = new ExplorationSequenceState();

        assertTrue(state.setFlag("last-seal-complete"));
        assertFalse(state.setFlag("last-seal-complete"));
        assertTrue(state.flag("LAST-SEAL-COMPLETE"));
        assertEquals(1, state.incrementCounter("seal"));
        assertEquals(2, state.incrementCounter("seal"));
        assertTrue(state.counterAtLeast("seal", 2));
        assertFalse(state.counterAtLeast("seal", 3));
    }

    @Test
    void physicalMoveWaitIgnoresLookOnlyStateUntilExplicitMovement() {
        ExplorationSequenceState state = new ExplorationSequenceState();
        long epoch = state.physicalMoveEpoch();

        assertFalse(state.movedSince(epoch));
        state.markPhysicalMove();
        assertTrue(state.movedSince(epoch));
    }

    @Test
    void pendingTaskCleanupIsIdempotentWithoutTasks() {
        ExplorationSequenceState state = new ExplorationSequenceState();

        state.cancelPendingTasks();
        state.cancelPendingTasks();
        assertEquals(0, state.pendingTaskCount());
    }
}
