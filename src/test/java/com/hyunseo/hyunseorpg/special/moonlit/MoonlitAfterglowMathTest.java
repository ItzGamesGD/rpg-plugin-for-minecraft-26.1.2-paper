package com.hyunseo.hyunseorpg.special.moonlit;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MoonlitAfterglowMathTest {
    private static void assertPointEquals(MoonlitAfterglowMath.Point expected, MoonlitAfterglowMath.Point actual) {
        assertNotNull(actual);
        assertEquals(expected.x(), actual.x(), 1.0e-12);
        assertEquals(expected.y(), actual.y(), 1.0e-12);
        assertEquals(expected.z(), actual.z(), 1.0e-12);
    }
    @Test void sphericalOffsetsStayInsideRadialBounds() {
        Random random = new Random(41);
        for (int i = 0; i < 10_000; i++) {
            double length = MoonlitAfterglowMath.sphericalOffset(random, 2, 12).length();
            assertTrue(length >= 2 && length <= 12, () -> "radius=" + length);
        }
    }

    @Test void seededGenerationIsDeterministicAndNotIndependentXyz() {
        Random first = new Random(7), second = new Random(7);
        for (int i = 0; i < 100; i++) {
            var a = MoonlitAfterglowMath.sphericalOffset(first, 2, 12);
            assertEquals(a, MoonlitAfterglowMath.sphericalOffset(second, 2, 12));
            assertTrue(a.length() <= 12); // independent [-12,12] axes could reach sqrt(3)*12.
        }
    }

    @Test void movementDirectionSupportsWasdAndNormalizesDiagonals() {
        var facing = new MoonlitAfterglowMath.Point(0, 0, 1);
        var right = new MoonlitAfterglowMath.Point(-1, 0, 0);
        assertEquals(facing, MoonlitAfterglowMath.movementDirection(true, false, false, false, facing, right));
        assertPointEquals(new MoonlitAfterglowMath.Point(0, 0, -1),
                MoonlitAfterglowMath.movementDirection(false, true, false, false, facing, right));
        assertEquals(right, MoonlitAfterglowMath.movementDirection(false, false, false, true, facing, right));
        var diagonal = MoonlitAfterglowMath.movementDirection(true, false, false, true, facing, right);
        assertNotNull(diagonal);
        assertEquals(1.0, diagonal.length(), 1.0e-12);
    }

    @Test void stationaryOrOpposingInputUsesExplicitFallbackSignal() {
        var facing = new MoonlitAfterglowMath.Point(0, 0, 1);
        var right = new MoonlitAfterglowMath.Point(-1, 0, 0);
        assertNull(MoonlitAfterglowMath.movementDirection(false, false, false, false, facing, right));
        assertNull(MoonlitAfterglowMath.movementDirection(true, true, false, false, facing, right));
    }
}
