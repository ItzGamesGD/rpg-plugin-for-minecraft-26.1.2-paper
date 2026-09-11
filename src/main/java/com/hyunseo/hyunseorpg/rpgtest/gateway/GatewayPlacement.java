package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.function.Predicate;

public final class GatewayPlacement {
    public static final double MIN_SPACING = 3.0;
    public static final int ATTEMPTS_PER_GATEWAY = 32;

    public List<Location> launcherLocations(Location snapshot, int count, double radius, double height,
                                            RandomGenerator random, Predicate<Location> spaceValidator) {
        List<Location> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            boolean accepted = false;
            for (int attempt = 0; attempt < ATTEMPTS_PER_GATEWAY; attempt++) {
                double sector = Math.PI * 2.0 * index / count;
                double angle = sector + (random.nextDouble() - .5) * (Math.PI * 1.6 / count);
                double radial = radius * (.68 + random.nextDouble() * .42);
                double y = height + random.nextDouble() * 3.0;
                Location candidate = snapshot.clone().add(Math.cos(angle) * radial, y, Math.sin(angle) * radial);
                boolean spaced = result.stream().allMatch(existing -> existing.distanceSquared(candidate) >= MIN_SPACING * MIN_SPACING);
                if (spaced && spaceValidator.test(candidate)) {
                    result.add(candidate);
                    accepted = true;
                    break;
                }
            }
            if (!accepted) return List.of();
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
