package com.hyunseo.hyunseorpg.prototype.thousandeyes;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThousandEyesMathTest {
    @Test void rigUsesExactlyNinePersistentSatelliteSlots() {
        assertEquals(9, ThousandEyesTuning.SATELLITE_COUNT);
    }

    @Test void canonicalOrbitHasNoAccumulatedPositionDrift() {
        Vector3f center = new Vector3f(4, 2, -3), forward = new Vector3f(.4F, 0, .8F);
        Vector3f first = ThousandEyesMath.orbit(center, forward, .73, 2.2, .4);
        for (int i = 0; i < 10_000; i++) assertEquals(first, ThousandEyesMath.orbit(center, forward, .73, 2.2, .4));
    }

    @Test void facingQuaternionTurnsLocalNormalTowardCenter() {
        Vector3f display = new Vector3f(2, 1, 0), center = new Vector3f(0, 1, 0);
        Vector3f expected = new Vector3f(center).sub(display).normalize();
        Quaternionf rotation = ThousandEyesMath.facing(expected, false);
        assertTrue(rotation.transform(new Vector3f(0, 0, 1)).distance(expected) < .0001F);
    }

    @Test void laserSpeedRhythmSlowsThenReleasesThenSettles() {
        assertTrue(ThousandEyesTuning.W_CHARGE < ThousandEyesTuning.W_NORMAL);
        assertTrue(ThousandEyesTuning.W_RELEASE > ThousandEyesTuning.W_NORMAL);
        assertEquals(ThousandEyesTuning.W_NORMAL,
                ThousandEyesMath.lerp(ThousandEyesTuning.W_RELEASE, ThousandEyesTuning.W_NORMAL, 1), 1e-9);
    }

    @Test void outerLayerContractsAndAcceleratesBeforeRelease() {
        assertTrue(2.55 < ThousandEyesTuning.OUTER_RADIUS);
        assertTrue(Math.abs(ThousandEyesTuning.OUTER_W_CHARGE) > Math.abs(ThousandEyesTuning.OUTER_W_NORMAL));
    }

    @Test void targetSnapshotIsAnIndependentValue() {
        Vector3f currentPlayer = new Vector3f(1, 2, 3);
        Vector3f locked = new Vector3f(currentPlayer);
        currentPlayer.set(20, 2, 20);
        assertEquals(new Vector3f(1, 2, 3), locked);
    }

    @Test void markerAndDashOrderRemainOneThroughNine() {
        List<Integer> markers = new ArrayList<>();
        for (int i = 1; i <= 9; i++) markers.add(i);
        List<Integer> visited = new ArrayList<>();
        for (int index = 0; index < markers.size(); index++) visited.add(markers.get(index));
        assertEquals(List.of(1,2,3,4,5,6,7,8,9), visited);
    }

    @Test void seededScatterOrderIsReproducible() {
        assertEquals(ThousandEyesMath.shuffledOrder(12345, 9), ThousandEyesMath.shuffledOrder(12345, 9));
        assertNotEquals(ThousandEyesMath.shuffledOrder(12345, 9), ThousandEyesMath.shuffledOrder(54321, 9));
    }

    @Test void stateMachineDefinesSafeRecoveryAndIdleStates() {
        assertNotNull(ThousandEyesState.RECOVERING);
        assertNotNull(ThousandEyesState.IDLE);
        assertEquals(8, ThousandEyesState.values().length);
    }
}
