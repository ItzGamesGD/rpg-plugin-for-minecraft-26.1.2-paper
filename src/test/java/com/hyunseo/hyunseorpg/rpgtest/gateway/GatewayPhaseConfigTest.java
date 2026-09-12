package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayPhaseConfigTest {
    @Test void longRangePhaseDefaultsKeepSafeGeometryInvariants() {
        GatewayPhaseConfig phase = new GatewayPhaseConfig(7, 16, 30, 10, 5, 24, 16, 6, 64, 900);
        assertTrue(phase.minRadius() < phase.maxRadius());
        assertTrue(phase.minRadius() >= phase.minimumSpacing() * 2);
        assertTrue(phase.volumeMaxRange() >= phase.maxRadius());
        assertTrue(phase.projectileLifetimeTicks() >= 200);
    }
}
