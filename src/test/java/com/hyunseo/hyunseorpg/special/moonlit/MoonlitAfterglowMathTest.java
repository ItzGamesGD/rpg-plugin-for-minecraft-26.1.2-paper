package com.hyunseo.hyunseorpg.special.moonlit;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MoonlitAfterglowMathTest {
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
}
