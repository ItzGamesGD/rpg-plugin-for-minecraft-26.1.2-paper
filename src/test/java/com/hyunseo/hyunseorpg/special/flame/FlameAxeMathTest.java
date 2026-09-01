package com.hyunseo.hyunseorpg.special.flame;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlameAxeMathTest {
    @Test void chargeThresholdIsInclusive() {
        assertFalse(FlameAxeMath.isFullCharge(19, 20));
        assertTrue(FlameAxeMath.isFullCharge(20, 20));
        assertTrue(FlameAxeMath.isFullCharge(30, 20));
    }

    @Test void hitRequiresContactRotationAndVanillaRecovery() {
        assertFalse(FlameAxeMath.mayHit(1.2, 1, 360, 360, 0));
        assertFalse(FlameAxeMath.mayHit(.5, 1, 359, 360, 0));
        assertFalse(FlameAxeMath.mayHit(.5, 1, 360, 360, 1));
        assertTrue(FlameAxeMath.mayHit(.5, 1, 360, 360, 0));
    }

    @Test void destinationLimitDoesNotModelDamageLimit() {
        UUID visited = UUID.randomUUID(), candidate = UUID.randomUUID();
        Set<UUID> routes = new HashSet<>(Set.of(visited));
        assertFalse(FlameAxeMath.maySelectTarget(visited, routes, 2));
        assertTrue(FlameAxeMath.maySelectTarget(candidate, routes, 2));
        routes.add(candidate);
        assertFalse(FlameAxeMath.maySelectTarget(UUID.randomUUID(), routes, 2));
    }

    @Test void steeringIsBoundedAndNeverSnapsOpposite() {
        Vector result = FlameAxeMath.steer(new Vector(1, 0, 0), new Vector(0, 0, 1), .1);
        assertEquals(1, result.length(), 1e-9);
        assertTrue(Math.acos(result.dot(new Vector(1, 0, 0))) <= .100001);
        Vector opposite = FlameAxeMath.steer(new Vector(0, 1, 0), new Vector(0, -1, 0), .2);
        assertTrue(opposite.isFinite());
        assertTrue(opposite.dot(new Vector(0, -1, 0)) < .99);
    }

    @Test void degenerateSteeringIsStable() {
        assertEquals(new Vector(), FlameAxeMath.steer(new Vector(), new Vector(1, 0, 0), .1));
        assertEquals(new Vector(1, 0, 0), FlameAxeMath.steer(new Vector(1, 0, 0), null, .1));
        Vector nan = FlameAxeMath.steer(new Vector(1, 0, 0), new Vector(Double.NaN, 0, 0), .1);
        assertTrue(nan.isFinite());
    }
}
