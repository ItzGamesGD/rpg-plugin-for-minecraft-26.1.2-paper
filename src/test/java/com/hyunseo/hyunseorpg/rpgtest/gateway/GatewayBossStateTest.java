package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayBossStateTest {
    @Test void slotCannotOverlapAndReturnsToOrbitAfterFlight() {
        GatewayBossState state = new GatewayBossState(3);
        assertTrue(state.reserve(1));
        assertFalse(state.reserve(1));
        assertEquals(GatewayBossState.SlotStatus.TELEGRAPHING, state.slotStatus(1));
        state.launch(1);
        assertEquals(GatewayBossState.SlotStatus.FLYING, state.slotStatus(1));
        state.recover(1);
        assertEquals(GatewayBossState.SlotStatus.ORBITING, state.slotStatus(1));
    }

    @Test void hiddenDriversRespectCapAndCleanupResetsAllEncounterState() {
        GatewayBossState state = new GatewayBossState(2);
        UUID first = UUID.randomUUID();
        assertTrue(state.addDriver(first, 1));
        assertFalse(state.addDriver(UUID.randomUUID(), 1));
        state.reserve(0);
        state.cleanup();
        assertEquals(0, state.activeDrivers());
        assertEquals(GatewayBossState.SlotStatus.ORBITING, state.slotStatus(0));
    }

    @Test void eligibleOrbitSlotsAreSampledRatherThanAlwaysTakingTheFirst() {
        GatewayBossState state = new GatewayBossState(4);
        boolean sawNonFirst = false;
        for (int seed = 0; seed < 100; seed++) {
            int selected = state.randomOrbitingSlot(List.of(1, 2, 3), new Random(seed));
            assertTrue(selected >= 1 && selected <= 3);
            sawNonFirst |= selected != 1;
        }
        assertTrue(sawNonFirst);
    }
}
