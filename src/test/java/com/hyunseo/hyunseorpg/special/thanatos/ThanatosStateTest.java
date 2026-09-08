package com.hyunseo.hyunseorpg.special.thanatos;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThanatosStateTest {
    @Test
    void mortalIsFirstApplicationOwnedUntilResolution() {
        ThanatosState state = new ThanatosState();
        UUID target = UUID.randomUUID();
        assertTrue(state.beginMortal(target, 10, 100));
        assertFalse(state.beginMortal(target, 20, 100));
        assertFalse(state.mortalDue(target, 109));
        assertTrue(state.mortalDue(target, 110));
        assertTrue(state.beginMortalFall(target, 110, 8));
        assertEquals(ThanatosState.MortalPhase.FALLING, state.mortalPhase(target));
        assertFalse(state.consumeMortalImpact(target, 117));
        assertTrue(state.consumeMortalImpact(target, 118));
        assertFalse(state.consumeMortalImpact(target, 118));
        assertFalse(state.hasMortal(target));
        assertTrue(state.beginMortal(target, 119, 100));
    }

    @Test
    void clearCleansEveryLifecycle() {
        ThanatosState state = new ThanatosState();
        state.beginMortal(UUID.randomUUID(), 0, 100);
        state.beginUltimatum(UUID.randomUUID(), UUID.randomUUID(), 0, 15);
        state.clear();
        assertEquals(0, state.mortalCount());
    }

    @Test
    void ultimatumRequiresChargeLaunchFallAndMatchingGroundedWorld() {
        ThanatosState state = new ThanatosState();
        UUID player = UUID.randomUUID();
        UUID world = UUID.randomUUID();
        assertTrue(state.beginUltimatum(player, world, 0, 15));
        assertFalse(state.chargeDue(player, 14));
        assertTrue(state.chargeDue(player, 15));
        assertFalse(state.land(player, world, true));
        assertTrue(state.launch(player));
        assertTrue(state.beginFall(player));
        assertFalse(state.land(player, UUID.randomUUID(), true));
        assertFalse(state.land(player, world, false));
        assertTrue(state.land(player, world, true));
        assertFalse(state.hasUltimatum(player));
    }

    @Test
    void cancelledUltimatumCannotBecomeStaleLanding() {
        ThanatosState state = new ThanatosState();
        UUID player = UUID.randomUUID();
        UUID world = UUID.randomUUID();
        state.beginUltimatum(player, world, 0, 15);
        state.cancelUltimatum(player);
        assertFalse(state.land(player, world, true));
    }
}
