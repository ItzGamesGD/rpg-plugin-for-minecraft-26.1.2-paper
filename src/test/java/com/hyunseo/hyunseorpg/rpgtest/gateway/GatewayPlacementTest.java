package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class GatewayPlacementTest {
    @Test void randomizedUpperVolumeHonorsRequestedMinimumSpacing() {
        List<Location> gateways = new GatewayPlacement().launcherLocations(new Location(null, 0, 0, 0),
                7, 14, 5, 4, new Random(42), location -> true);
        assertEquals(7, gateways.size());
        for (int left = 0; left < gateways.size(); left++) for (int right = left + 1; right < gateways.size(); right++)
            assertTrue(squaredDistance(gateways.get(left), gateways.get(right)) >= 16);
    }

    private double squaredDistance(Location left, Location right) {
        double x = left.getX() - right.getX();
        double y = left.getY() - right.getY();
        double z = left.getZ() - right.getZ();
        return x * x + y * y + z * z;
    }

    @Test void randomizedDeploymentStaysInsideExplicitP0RadialBounds() {
        Location p0 = new Location(null, 3, 10, -2);
        List<Location> gateways = new GatewayPlacement().launcherLocations(p0, 6, 8, 14, 5, 3,
                new Random(7), location -> true);
        assertEquals(6, gateways.size());
        for (Location gateway : gateways) {
            double horizontal = Math.hypot(gateway.getX() - p0.getX(), gateway.getZ() - p0.getZ());
            assertTrue(horizontal >= 8 && horizontal <= 14);
        }
    }
}
