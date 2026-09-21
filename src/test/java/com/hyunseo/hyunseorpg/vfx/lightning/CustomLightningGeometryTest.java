package com.hyunseo.hyunseorpg.vfx.lightning;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CustomLightningGeometryTest {
    private static final CustomLightningParameters PARAMETERS = new CustomLightningParameters(
            .025, .065, .7, .16, 1, 2, .7, 5);

    @Test void supportsVerticalHorizontalAndDiagonalEndpoints() {
        assertPath(new Vector(0, 0, 0), new Vector(0, 6, 0));
        assertPath(new Vector(0, 0, 0), new Vector(6, 0, 0));
        assertPath(new Vector(-2, 1, 4), new Vector(3, 7, -1));
    }

    @Test void branchDepthCanNeverExceedTwo() {
        List<CustomLightningGeometry.Segment> segments = CustomLightningGeometry.generate(
                new Vector(0, 0, 0), new Vector(0, 8, 0), PARAMETERS, 42);
        assertTrue(segments.stream().anyMatch(segment -> segment.depth() > 0));
        assertTrue(segments.stream().allMatch(segment -> segment.depth() >= 0 && segment.depth() <= 2));
    }

    @Test void zeroShortInvalidAndNonFiniteDistancesAreSafelyIgnored() {
        assertTrue(CustomLightningGeometry.generate(new Vector(), new Vector(), PARAMETERS, 1).isEmpty());
        assertTrue(CustomLightningGeometry.generate(new Vector(), new Vector(.001, 0, 0), PARAMETERS, 1).isEmpty());
        assertTrue(CustomLightningGeometry.generate(new Vector(Double.NaN, 0, 0), new Vector(1, 0, 0), PARAMETERS, 1).isEmpty());
    }

    @Test void geometryIsDeterministicForSeedAndDoesNotMutateInputs() {
        Vector start = new Vector(1, 2, 3), end = new Vector(5, 6, 7);
        Vector startCopy = start.clone(), endCopy = end.clone();
        assertEquals(CustomLightningGeometry.generate(start, end, PARAMETERS, 99),
                CustomLightningGeometry.generate(start, end, PARAMETERS, 99));
        assertEquals(startCopy, start);
        assertEquals(endCopy, end);
    }

    private void assertPath(Vector start, Vector end) {
        List<CustomLightningGeometry.Segment> main = CustomLightningGeometry.generate(start, end,
                new CustomLightningParameters(.025, .065, .7, .16, 0, 2, .7, 5), 7);
        assertFalse(main.isEmpty());
        assertEquals(start, main.getFirst().start());
        assertEquals(end, main.getLast().end());
        assertTrue(main.stream().allMatch(segment -> segment.depth() == 0));
    }
}
