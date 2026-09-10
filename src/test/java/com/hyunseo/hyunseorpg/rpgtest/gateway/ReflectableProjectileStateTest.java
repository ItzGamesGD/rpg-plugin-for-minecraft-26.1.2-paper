package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReflectableProjectileStateTest {
    @Test void preservesSourceAndUsesExplicitReturnPhases() {
        ReflectableProjectileState state = new ReflectableProjectileState(UUID.randomUUID(), 6, GatewayPayloadType.ARROW);
        state.reflect();
        assertEquals(6, state.sourceGatewayId());
        assertEquals(ReflectableProjectileState.Phase.RETURNING_TO_SOURCE, state.phase());
        state.enterSourceGateway();
        assertEquals(ReflectableProjectileState.Phase.RETURNING_TO_BOSS, state.phase());
    }

    @Test void rejectsNonReflectablePayloads() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReflectableProjectileState(UUID.randomUUID(), 1, GatewayPayloadType.BEAM));
    }

    @Test void crystalHasFuseTransition() {
        ReflectableProjectileState state = new ReflectableProjectileState(UUID.randomUUID(), 1, GatewayPayloadType.END_CRYSTAL_BOMB);
        state.beginFuse();
        assertEquals(ReflectableProjectileState.Phase.FUSE, state.phase());
    }
}
