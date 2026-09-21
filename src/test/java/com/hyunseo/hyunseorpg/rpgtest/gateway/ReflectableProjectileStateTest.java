package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.util.Vector;

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
    @Test void returnRoutingUsesRecordedSourcePairNotAttackDirectionOrNearestGateway() {
        GatewayPair first = new GatewayPair(1, new Location(null, 100, 0, 0), new Location(null, 1, 0, 0),
                new Vector(1, 0, 0), UUID.randomUUID(), UUID.randomUUID());
        GatewayPair source = new GatewayPair(9, new Location(null, 2, 0, 0), new Location(null, 9, 0, 0),
                new Vector(-1, 0, 0), UUID.randomUUID(), UUID.randomUUID());
        ReflectableProjectileState state = new ReflectableProjectileState(UUID.randomUUID(), 9, GatewayPayloadType.ARROW);
        assertSame(source, ReturnRouting.pairFor(List.of(first, source), state).orElseThrow());
    }
}
