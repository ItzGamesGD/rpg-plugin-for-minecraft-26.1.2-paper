package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class GatewayPlacement {
    public List<Location> launcherLocations(Location snapshot, int count, double radius, double height) {
        List<Location> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2.0 * index / count;
            double y = height + (index % 2) * 1.35;
            result.add(snapshot.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
        }
        return result;
    }

    public List<Location> returnLocations(Location boss, int count) {
        List<Location> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2.0 * index / count;
            result.add(boss.clone().add(Math.cos(angle) * 2.5, 1.0 + (index % 2) * 0.8, Math.sin(angle) * 2.5));
        }
        return result;
    }

    public Vector snapshotForward(Location launcher, Location snapshot) {
        Vector direction = snapshot.toVector().subtract(launcher.toVector());
        return direction.lengthSquared() == 0.0 ? new Vector(0, -1, 0) : direction.normalize();
    }
}
