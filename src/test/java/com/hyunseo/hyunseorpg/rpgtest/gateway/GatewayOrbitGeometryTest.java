package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GatewayOrbitGeometryTest {
    @Test void slotsOfTheSameWeaponFamilyShareTheirRing() {
        assertEquals(GatewayOrbitGeometry.familyRing(0, 5), GatewayOrbitGeometry.familyRing(5, 5));
        assertNotEquals(GatewayOrbitGeometry.familyRing(0, 5), GatewayOrbitGeometry.familyRing(1, 5));
        assertEquals(1, GatewayOrbitGeometry.localPosition(5, 5));
    }

    @Test void ringPlaneChangesTheWorldOffsetOverTime() {
        Vector early = GatewayOrbitGeometry.offset(2, 1, 20, 5.5, .075, .011, false);
        Vector later = GatewayOrbitGeometry.offset(2, 1, 140, 5.5, .075, .011, false);
        assertNotEquals(early, later);
        assertNotEquals(early.getZ(), later.getZ());
    }
}
