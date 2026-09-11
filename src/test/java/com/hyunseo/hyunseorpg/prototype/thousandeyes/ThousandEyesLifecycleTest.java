package com.hyunseo.hyunseorpg.prototype.thousandeyes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThousandEyesLifecycleTest {
    @Test void ninthMarkerTransitionsImmediatelyAndOldStateMustStop() {
        ThousandEyesLifecycle lifecycle = new ThousandEyesLifecycle();
        lifecycle.activate();
        assertTrue(lifecycle.start(ThousandEyesState.PATH_RECORDING));
        for (int marker = 1; marker < 9; marker++) assertFalse(lifecycle.markerRecorded(marker));
        assertTrue(lifecycle.markerRecorded(9));
        assertEquals(ThousandEyesState.PATH_DASH, lifecycle.state());
    }

    @Test void validDashRecoveryKeepsBossActiveAndReturnsIdle() {
        ThousandEyesLifecycle lifecycle = new ThousandEyesLifecycle();
        lifecycle.activate();
        lifecycle.start(ThousandEyesState.PATH_RECORDING);
        lifecycle.markerRecorded(9);
        lifecycle.recover();
        assertTrue(lifecycle.active());
        assertEquals(ThousandEyesState.RECOVERING, lifecycle.state());
        lifecycle.idle();
        assertTrue(lifecycle.active());
        assertEquals(ThousandEyesState.IDLE, lifecycle.state());
    }

    @Test void cleanupIsIdempotent() {
        ThousandEyesLifecycle lifecycle = new ThousandEyesLifecycle();
        lifecycle.activate();
        lifecycle.remove();
        lifecycle.remove();
        assertFalse(lifecycle.active());
        assertEquals(ThousandEyesState.IDLE, lifecycle.state());
        assertFalse(lifecycle.start(ThousandEyesState.CENTRAL_LASER_CHARGE));
    }
}
