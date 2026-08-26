package com.hyunseo.hyunseorpg.exploration.pyramid;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PyramidCompletionAndRewardTest {
    @Test void reservationThenPendingThenDeliveredThenFinalized() {
        var s = PyramidRewardTransaction.State.NOT_STARTED;
        s = PyramidRewardTransaction.reserve(s);
        assertEquals(PyramidRewardTransaction.State.RESERVED, s);
        s = PyramidRewardTransaction.pending(s);
        assertEquals(PyramidRewardTransaction.State.DELIVERY_PENDING, s);
        s = PyramidRewardTransaction.delivered(s);
        assertEquals(PyramidRewardTransaction.State.DELIVERED, s);
        assertEquals(PyramidRewardTransaction.State.FINALIZED, PyramidRewardTransaction.finalize(s));
    }

    @Test void duplicateReservationCannotRegressState() {
        assertEquals(PyramidRewardTransaction.State.RESERVED,
                PyramidRewardTransaction.reserve(PyramidRewardTransaction.State.RESERVED));
        assertEquals(PyramidRewardTransaction.State.FINALIZED,
                PyramidRewardTransaction.reserve(PyramidRewardTransaction.State.FINALIZED));
    }

    @Test void crashAfterReservationRecoversToPending() {
        assertEquals(PyramidRewardTransaction.State.DELIVERY_PENDING,
                PyramidRewardTransaction.pending(PyramidRewardTransaction.State.RESERVED));
    }

    @Test void crashBeforeDeliveryCanRetryPending() {
        var state = PyramidRewardTransaction.State.parse("pending");
        assertEquals(PyramidRewardTransaction.State.DELIVERED,
                PyramidRewardTransaction.delivered(state));
    }

    @Test void legacyDeliveringStateMapsToPending() {
        assertEquals(PyramidRewardTransaction.State.DELIVERY_PENDING,
                PyramidRewardTransaction.State.parse("delivering"));
    }

    @Test void duplicateCompletionAfterDeliveredDoesNotRegrant() {
        assertEquals(PyramidRewardTransaction.State.DELIVERED,
                PyramidRewardTransaction.delivered(PyramidRewardTransaction.State.DELIVERED));
        assertEquals(PyramidRewardTransaction.State.FINALIZED,
                PyramidRewardTransaction.finalize(PyramidRewardTransaction.State.FINALIZED));
    }

    @Test void retryPolicyIsBoundedAndDeterministic() {
        assertTrue(PyramidCompletionRetryPolicy.shouldRetry(1));
        assertEquals(40L, PyramidCompletionRetryPolicy.delayTicks(1));
        assertEquals(200L, PyramidCompletionRetryPolicy.delayTicks(5));
        assertFalse(PyramidCompletionRetryPolicy.shouldRetry(6));
        assertEquals(-1L, PyramidCompletionRetryPolicy.delayTicks(6));
    }

    @Test void retryPolicyRejectsZeroAndNegativeAttempts() {
        assertFalse(PyramidCompletionRetryPolicy.shouldRetry(0));
        assertFalse(PyramidCompletionRetryPolicy.shouldRetry(-1));
    }

    @Test void terminalRewardStateCannotBeReopened() {
        assertEquals(PyramidRewardTransaction.State.FINALIZED,
                PyramidRewardTransaction.delivered(PyramidRewardTransaction.State.FINALIZED));
        assertTrue(PyramidRewardTransaction.State.FINALIZED.terminal());
    }

    @Test void unknownStateFailsClosedToNotStarted() {
        assertEquals(PyramidRewardTransaction.State.NOT_STARTED,
                PyramidRewardTransaction.State.parse("garbage"));
    }
}
