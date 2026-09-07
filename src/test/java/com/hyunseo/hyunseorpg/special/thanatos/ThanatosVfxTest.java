package com.hyunseo.hyunseorpg.special.thanatos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThanatosVfxTest {
    @Test
    void exponentialFallStartsSuspendedAndStillEndsAtAuthoritativeProgress() {
        assertEquals(0.0D, ThanatosVfx.exponentialIn(0.0D));
        assertTrue(ThanatosVfx.exponentialIn(0.25D) < 0.01D);
        assertTrue(ThanatosVfx.exponentialIn(0.75D) < 0.20D);
        assertEquals(1.0D, ThanatosVfx.exponentialIn(1.0D));
    }

    @Test
    void exponentialFallIsMonotonic() {
        double previous = -1.0D;
        for (int tick = 0; tick <= 24; tick++) {
            double eased = ThanatosVfx.exponentialIn(tick / 24.0D);
            assertTrue(eased >= previous);
            previous = eased;
        }
    }

    @Test
    void oppressionUsesTwoOppositelyOrientedThreeEdgeTriangles() {
        assertEquals(3, ThanatosVfx.TRIANGLE_EDGES);
        assertEquals(0.0D, ThanatosVfx.trianglePhase(0));
        assertEquals(Math.PI, ThanatosVfx.trianglePhase(1));

        double rotation = 0.731D;
        double radius = 3.2D;
        for (int triangle = 0; triangle < 2; triangle++) {
            for (int edge = 0; edge < ThanatosVfx.TRIANGLE_EDGES; edge++) {
                ThanatosVfx.SegmentPose pose = ThanatosVfx.starSegmentPose(triangle, edge, 0.25D, radius, rotation);
                assertTrue(Double.isFinite(pose.x()));
                assertTrue(Double.isFinite(pose.z()));
                assertTrue(Double.isFinite(pose.angle()));
            }
        }
    }

    @Test
    void oppressionRotationPreservesSegmentRadiusAndOppositeOrientation() {
        ThanatosVfx.SegmentPose base = ThanatosVfx.starSegmentPose(0, 1, 0.5D, 4.0D, 0.0D);
        ThanatosVfx.SegmentPose rotated = ThanatosVfx.starSegmentPose(0, 1, 0.5D, 4.0D, 1.2D);
        ThanatosVfx.SegmentPose opposite = ThanatosVfx.starSegmentPose(1, 1, 0.5D, 4.0D, 0.0D);
        assertEquals(Math.hypot(base.x(), base.z()), Math.hypot(rotated.x(), rotated.z()), 1.0E-10D);
        assertEquals(-base.x(), opposite.x(), 1.0E-10D);
        assertEquals(-base.z(), opposite.z(), 1.0E-10D);
        assertFalse(base.angle() == opposite.angle());
    }

    @Test
    void mortalParticleLodOnlyChangesCadence() {
        assertEquals(2, ThanatosVfx.mortalParticleInterval(1));
        assertEquals(2, ThanatosVfx.mortalParticleInterval(4));
        assertEquals(3, ThanatosVfx.mortalParticleInterval(5));
        assertEquals(3, ThanatosVfx.mortalParticleInterval(8));
        assertEquals(4, ThanatosVfx.mortalParticleInterval(9));
    }
}
