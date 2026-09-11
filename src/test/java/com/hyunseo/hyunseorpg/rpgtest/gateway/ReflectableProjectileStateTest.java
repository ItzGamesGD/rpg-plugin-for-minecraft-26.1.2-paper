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

    @Test void reflectionOnlyTransitionsFromOutbound() {
        ReflectableProjectileState state = new ReflectableProjectileState(UUID.randomUUID(), 3, GatewayPayloadType.SHULKER_BULLET);
        state.reflect();
        state.enterSourceGateway();
        state.reflect();
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

    @Test void returningCollisionFailsUnlessBossIsActuallyHit() {
        ReflectableProjectileState toSource = new ReflectableProjectileState(
                UUID.randomUUID(), 1, GatewayPayloadType.ARROW);
        toSource.reflect();
        assertEquals(ReflectableProjectileState.CollisionResult.ROUTE_FAILED, toSource.collide(false));
        assertEquals(ReflectableProjectileState.Phase.FINISHED, toSource.phase());

        ReflectableProjectileState toBoss = new ReflectableProjectileState(
                UUID.randomUUID(), 1, GatewayPayloadType.TRIDENT);
        toBoss.reflect();
        toBoss.enterSourceGateway();
        assertEquals(ReflectableProjectileState.CollisionResult.ROUTE_FAILED, toBoss.collide(false));

        ReflectableProjectileState bossHit = new ReflectableProjectileState(
                UUID.randomUUID(), 1, GatewayPayloadType.TRIDENT);
        bossHit.reflect();
        bossHit.enterSourceGateway();
        assertEquals(ReflectableProjectileState.CollisionResult.BOSS_HIT, bossHit.collide(true));
    }
}
