package com.hyunseo.hyunseorpg.special.thunder;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ThunderAxeChargeStateTest {
    @Test void gameplayChargeUsesServerTicksAndNotPresentationDuration() {
        ThunderAxeChargeState state = new ThunderAxeChargeState();
        UUID owner = UUID.randomUUID(), instance = UUID.randomUUID();
        state.start(owner, instance, 100);
        state.advance(owner, 119, 20);
        assertFalse(state.release(owner, instance).fullCharge());

        state.start(owner, instance, 200);
        state.advance(owner, 220, 20);
        assertTrue(state.release(owner, instance).fullCharge());
        assertTrue(ThunderAxeChargeState.PRESENTATION_SECONDS * 20 > 20 * 100);
    }

    @Test void mismatchedItemCannotReleaseAnotherAxesCharge() {
        ThunderAxeChargeState state = new ThunderAxeChargeState();
        UUID owner = UUID.randomUUID();
        state.start(owner, UUID.randomUUID(), 0);
        state.advanceAll(20, 20);
        ThunderAxeChargeState.Release release = state.release(owner, UUID.randomUUID());
        assertTrue(release.existed());
        assertFalse(release.fullCharge());
        assertFalse(state.contains(owner));
    }

    @Test void cleanupIsIdempotent() {
        ThunderAxeChargeState state = new ThunderAxeChargeState();
        UUID owner = UUID.randomUUID();
        state.start(owner, UUID.randomUUID(), 0);
        state.clear(owner);
        state.clear(owner);
        assertTrue(state.owners().isEmpty());
    }
}
