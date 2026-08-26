package com.hyunseo.hyunseorpg.item;

/**
 * Pure, durable-token policy used by the pending-reward mailbox.
 *
 * <p>The journal is written before inventory mutation. On restart a tagged
 * delivered item finalizes the token; an absent tag leaves the obligation
 * pending. A completed token always suppresses any later queue attempt.</p>
 */
public final class DeterministicRewardClaimPolicy {
    public enum State {
        ABSENT, PENDING, CLAIM_JOURNALED, COMPLETED
    }

    private DeterministicRewardClaimPolicy() { }

    public static State fromDurableEvidence(boolean pending, boolean claimJournal, boolean completed) {
        if (completed) return State.COMPLETED;
        if (claimJournal) return State.CLAIM_JOURNALED;
        return pending ? State.PENDING : State.ABSENT;
    }

    public static boolean suppressesQueue(State state) {
        return state == State.PENDING || state == State.CLAIM_JOURNALED || state == State.COMPLETED;
    }

    public static State beginClaim(State state) {
        return state == State.PENDING ? State.CLAIM_JOURNALED : state;
    }

    /** Reconstructs durable mailbox state after an interrupted inventory delivery. */
    public static State recoverInterruptedClaim(boolean taggedItemPresent) {
        return taggedItemPresent ? State.COMPLETED : State.PENDING;
    }

    public static State finishClaim(State state) {
        return state == State.CLAIM_JOURNALED ? State.COMPLETED : state;
    }
}
