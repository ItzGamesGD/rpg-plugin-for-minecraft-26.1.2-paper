package com.hyunseo.hyunseorpg.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DeterministicRewardClaimPolicyTest {
    @Test
    void queueIsIdempotentBeforeAndAfterRestartEvidence() {
        var pending = DeterministicRewardClaimPolicy.fromDurableEvidence(true, false, false);
        assertTrue(DeterministicRewardClaimPolicy.suppressesQueue(pending));

        var reloaded = DeterministicRewardClaimPolicy.fromDurableEvidence(true, false, false);
        assertEquals(DeterministicRewardClaimPolicy.State.PENDING, reloaded);
        assertTrue(DeterministicRewardClaimPolicy.suppressesQueue(reloaded));
    }

    @Test
    void successfulClaimCompletesTokenAndFutureQueueCannotCreateAnotherObligation() {
        var journalled = DeterministicRewardClaimPolicy.beginClaim(
                DeterministicRewardClaimPolicy.State.PENDING);
        assertEquals(DeterministicRewardClaimPolicy.State.CLAIM_JOURNALED, journalled);

        var completed = DeterministicRewardClaimPolicy.finishClaim(journalled);
        assertEquals(DeterministicRewardClaimPolicy.State.COMPLETED, completed);
        assertTrue(DeterministicRewardClaimPolicy.suppressesQueue(completed));
        assertEquals(DeterministicRewardClaimPolicy.State.COMPLETED,
                DeterministicRewardClaimPolicy.fromDurableEvidence(false, false, true));
    }

    @Test
    void crashAfterPhysicalDeliveryUsesJournalAndTaggedItemToFinalizeWithoutRedelivery() {
        var afterRestart = DeterministicRewardClaimPolicy.recoverInterruptedClaim(true);
        assertEquals(DeterministicRewardClaimPolicy.State.COMPLETED, afterRestart);
        assertTrue(DeterministicRewardClaimPolicy.suppressesQueue(afterRestart));
    }

    @Test
    void journalWithoutTaggedDeliveryReturnsToTheSameSinglePendingObligation() {
        var afterRestart = DeterministicRewardClaimPolicy.recoverInterruptedClaim(false);
        assertEquals(DeterministicRewardClaimPolicy.State.MANUAL_RECOVERY_REQUIRED, afterRestart);
        assertTrue(DeterministicRewardClaimPolicy.suppressesQueue(afterRestart));
    }

    @Test
    void quarantinedOrCompletedEvidenceSuppressesStaleMailboxRows() {
        assertTrue(DeterministicRewardClaimPolicy.suppressesQueue(
                DeterministicRewardClaimPolicy.State.MANUAL_RECOVERY_REQUIRED));
        assertTrue(DeterministicRewardClaimPolicy.suppressesQueue(
                DeterministicRewardClaimPolicy.State.COMPLETED));
    }
}
