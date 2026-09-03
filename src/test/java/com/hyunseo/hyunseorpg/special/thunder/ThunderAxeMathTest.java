package com.hyunseo.hyunseorpg.special.thunder;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ThunderAxeMathTest {
    @Test void directHitCounterIsDeterministicAndGeneratedDamageCannotAdvanceIt() {
        int hits = 0;
        for (int i = 0; i < 4; i++) {
            assertFalse(ThunderAxeMath.triggers(hits, 5, true));
            hits = ThunderAxeMath.advanceHit(hits, 5, true);
        }
        assertTrue(ThunderAxeMath.triggers(hits, 5, true));
        assertEquals(0, ThunderAxeMath.advanceHit(hits, 5, true));
        assertFalse(ThunderAxeMath.triggers(3, 5, false));
        assertEquals(3, ThunderAxeMath.advanceHit(3, 5, false));
    }

    @Test void chainIsGreedyUniqueBoundedAndRadiusLimited() {
        Map<String, Map<String, Double>> graph = Map.of(
                "a", Map.of("b", 1.0, "c", 4.0, "far", 26.0),
                "b", Map.of("a", 1.0, "c", 1.0),
                "c", Map.of("a", 4.0, "b", 1.0, "d", 1.0),
                "d", Map.of("c", 1.0));
        assertEquals(List.of("b", "c"), ThunderAxeMath.greedyChain("a", graph, 5, 2));
        List<String> full = ThunderAxeMath.greedyChain("a", graph, 5, 9);
        assertEquals(List.of("b", "c", "d"), full);
        assertEquals(full.size(), new HashSet<>(full).size());
        assertFalse(full.contains("far"));
    }

    @Test void chargedStrikeHasExactlyFiveIncreasingWideningTemporalWaves() {
        assertFalse(ThunderAxeMath.fullCharge(19, 20));
        assertTrue(ThunderAxeMath.fullCharge(20, 20));
        double previousDistance = 0, previousSpread = -1;
        for (int wave = 1; wave <= 5; wave++) {
            List<Vector> points = ThunderAxeMath.fan(wave, new Vector(0, 0, 1), 3, 2, 50);
            assertEquals(wave == 1 ? 1 : 5, points.size());
            double distance = points.getFirst().length();
            assertTrue(distance > previousDistance); previousDistance = distance;
            double spread = points.stream().mapToDouble(Vector::getX).max().orElse(0)
                    - points.stream().mapToDouble(Vector::getX).min().orElse(0);
            if (wave > 2) assertTrue(spread > previousSpread);
            previousSpread = spread;
        }
        assertTrue(ThunderAxeMath.fan(6, new Vector(0, 0, 1), 3, 2, 50).isEmpty());
    }

    @Test void expandingRingHasIncreasingRadiusSaneCoordinatesAndBoundedBudget() {
        List<Vector> small = ThunderAxeMath.ring(1, 1_000);
        List<Vector> large = ThunderAxeMath.ring(8, 1_000);
        assertEquals(ThunderAxeMath.MAX_RING_PARTICLES, small.size());
        assertEquals(ThunderAxeMath.MAX_RING_PARTICLES, large.size());
        assertTrue(large.stream().mapToDouble(v -> Math.hypot(v.getX(), v.getZ())).average().orElseThrow()
                > small.stream().mapToDouble(v -> Math.hypot(v.getX(), v.getZ())).average().orElseThrow());
        assertTrue(large.stream().allMatch(v -> Double.isFinite(v.getX()) && Double.isFinite(v.getY()) && Double.isFinite(v.getZ())));
    }

    @Test void strikeGeometryRemainsAnchoredWhenPlayerMovesAfterRelease() {
        Vector capturedOrigin = new Vector(12, 64, -8);
        Vector facing = new Vector(0, 0, 1);
        List<Vector> waveAtRelease = ThunderAxeMath.anchoredFan(capturedOrigin, 4, facing, 3, 2, 50);

        // A later player position is deliberately irrelevant to cast-space geometry.
        Vector laterPlayerPosition = new Vector(120, 80, 300);
        List<Vector> waveAfterMovement = ThunderAxeMath.anchoredFan(capturedOrigin, 4, facing, 3, 2, 50);

        assertEquals(waveAtRelease, waveAfterMovement);
        assertFalse(waveAfterMovement.equals(ThunderAxeMath.anchoredFan(
                laterPlayerPosition, 4, facing, 3, 2, 50)));
    }

    @Test void burstIsDistanceOrderedChunkedVisitedAndLimited() {
        Map<String, Double> targets = new LinkedHashMap<>();
        for (int i = 9; i >= 0; i--) targets.put("target-" + i, (double) i);
        targets.put("outside", 200.0);
        List<List<String>> waves = ThunderAxeMath.distanceWaves(targets, 10, 4, 9, 3);
        assertEquals(List.of(4, 4, 1), waves.stream().map(List::size).toList());
        List<String> flat = waves.stream().flatMap(Collection::stream).toList();
        assertEquals(9, flat.size()); assertEquals(9, new HashSet<>(flat).size());
        assertFalse(flat.contains("outside"));
        for (int i = 1; i < flat.size(); i++) assertTrue(targets.get(flat.get(i - 1)) <= targets.get(flat.get(i)));
        assertTrue(ThunderAxeMath.distanceWaves(Map.of(), 10, 4, 9, 3).isEmpty());
    }
}
