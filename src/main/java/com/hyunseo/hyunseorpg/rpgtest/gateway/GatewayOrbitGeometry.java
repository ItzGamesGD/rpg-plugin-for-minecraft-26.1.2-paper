package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.util.Vector;

/** Pure orbital geometry so the family rings and their rotating planes stay regression-testable. */
final class GatewayOrbitGeometry {
    private GatewayOrbitGeometry() { }

    static int familyRing(int slot, int familyCount) { return Math.floorMod(slot, familyCount); }
    static int localPosition(int slot, int familyCount) { return Math.floorDiv(slot, familyCount); }

    static Vector offset(int ring, int localPosition, long tick, double orbitRadius, double orbitSpeed,
                         double planeSpeed, boolean burst) {
        double localAngle = tick * orbitSpeed * (burst ? 1.35D : 1.0D) + localPosition * Math.PI;
        double planeAngle = tick * (planeSpeed + ring * .003D) + ring * 1.17D;
        double radius = orbitRadius + ring * .22D;
        Vector local = new Vector(Math.cos(localAngle) * radius, Math.sin(localAngle) * radius, 0);
        return rotateX(rotateY(local, planeAngle), .48D + ring * .42D);
    }

    private static Vector rotateY(Vector value, double angle) {
        return new Vector(value.getX() * Math.cos(angle) + value.getZ() * Math.sin(angle), value.getY(),
                -value.getX() * Math.sin(angle) + value.getZ() * Math.cos(angle));
    }
    private static Vector rotateX(Vector value, double angle) {
        return new Vector(value.getX(), value.getY() * Math.cos(angle) - value.getZ() * Math.sin(angle),
                value.getY() * Math.sin(angle) + value.getZ() * Math.cos(angle));
    }
}
