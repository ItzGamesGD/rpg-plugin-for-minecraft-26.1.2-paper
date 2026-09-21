package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GatewayPhaseStateTest {
    @Test void normalStateHasNoGatewaySnapshotAndPhaseCapturesP0OnlyOnce() {
        GatewayPhaseState state = new GatewayPhaseState();
        assertEquals(GatewayPhaseState.Phase.NORMAL, state.phase());
        assertNull(state.snapshot());
        Location p0 = new Location(null, 4, 8, 12);
        assertTrue(state.begin(p0));
        p0.setX(99);
        assertEquals(4, state.snapshot().getX());
        assertFalse(state.begin(new Location(null, 1, 1, 1)));
        state.deploy(); assertEquals(GatewayPhaseState.Phase.ACTIVE, state.phase());
        state.close(); assertEquals(GatewayPhaseState.Phase.NORMAL, state.phase()); assertNull(state.snapshot());
    }
}
