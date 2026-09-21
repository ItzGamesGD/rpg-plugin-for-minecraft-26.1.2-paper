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

    @Test void visualOrientationUsesEachElementsActualLocalForwardAxis() {
        Vector3f expected = new Vector3f(-.6F, .3F, .7F).normalize();
        for (ThousandEyesMath.VisualElement element : ThousandEyesMath.VisualElement.values()) {
            Quaternionf rotation = ThousandEyesMath.visualFacing(expected, element);
            assertTrue(rotation.transform(element.localForward()).distance(expected) < .0001F, element.name());
        }
    }

    @Test void facingCalculationDoesNotMutateMovementVector() {
        Vector3f movement = new Vector3f(7, 4, -2);
        Vector3f original = new Vector3f(movement);
        ThousandEyesMath.horizontal(movement);
        ThousandEyesMath.visualFacing(movement, ThousandEyesMath.VisualElement.ITEM_EYE);
        assertEquals(original, movement);
    }

    @Test void dashPreservesThreeDimensionalDisplacementAndReachesEndpoint() {
        Vector3f start = new Vector3f(2, -4, 7);
        Vector3f destination = new Vector3f(-8, 13, .5F);
        Vector3f movement = ThousandEyesMath.movementDelta(start, destination);
        assertEquals(new Vector3f(-10, 17, -6.5F), movement);
        assertEquals(start, ThousandEyesMath.dashPosition(start, destination, 0));
        assertTrue(destination.distance(ThousandEyesMath.dashPosition(start, destination, 1)) < .0001F);
    }

    @Test void nineMarkerDashVisitsEveryRealCoordinateInOrder() {
        Vector3f start = new Vector3f(0, 2, 0);
        List<Vector3f> markers = new ArrayList<>();
        for (int i = 1; i <= 9; i++) markers.add(new Vector3f(i * 1.5F, 2 + i * .4F, -i));
        List<Vector3f> visited = new ArrayList<>();
        Vector3f segmentStart = new Vector3f(start);
        for (Vector3f marker : markers) {
            for (int tick = 1; tick <= ThousandEyesTuning.DASH_SEGMENT_TICKS; tick++) {
                Vector3f position = ThousandEyesMath.dashPosition(segmentStart, marker,
                        tick / (double) ThousandEyesTuning.DASH_SEGMENT_TICKS);
                if (tick == ThousandEyesTuning.DASH_SEGMENT_TICKS) visited.add(position);
            }
            segmentStart = new Vector3f(marker);
        }
        assertEquals(markers.size(), visited.size());
        for (int i = 0; i < markers.size(); i++) assertTrue(markers.get(i).distance(visited.get(i)) < .0001F);
    }

    @Test void laserAimIsFullThreeDimensionalAndHandlesVerticalOrZeroDirections() {
        Vector3f origin = new Vector3f(2, 10, -3);
        Vector3f target = new Vector3f(4, 1, 5);
        Vector3f expected = new Vector3f(target).sub(origin).normalize();
        assertTrue(expected.distance(ThousandEyesMath.attackAim(origin, target)) < .0001F);
        assertEquals(new Vector3f(0, -1, 0), ThousandEyesMath.attackAim(origin, new Vector3f(2, 1, -3)));
        assertEquals(new Vector3f(0, 0, 1), ThousandEyesMath.attackAim(origin, origin));
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

    @Test void stateMachineDefinesSafeRecoveryAndIdleStates() { assertEquals(8, ThousandEyesState.values().length); }
}
