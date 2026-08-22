package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.IntStream;

final class ComponentLocations {
    private ComponentLocations() { }

    static Location relative(ExplorationEventContext context, ExplorationComponentSpec spec) {
        return relative(context, spec, 0.0D, 0.0D);
    }

    static Location relative(ExplorationEventContext context, ExplorationComponentSpec spec, double extraX, double extraZ) {
        Location anchor = context.anchorLocation().orElseThrow(() -> new IllegalStateException("world is not loaded"));
        Location desired = anchor.clone().add(spec.decimal("dx", 0.0D) + extraX,
                spec.decimal("dy", 0.0D), spec.decimal("dz", 0.0D) + extraZ);
        return spec.bool("safe-spawn", false)
                ? safeSpawnLocation(context, desired).orElse(desired)
                : desired;
    }

    private static Optional<Location> safeSpawnLocation(ExplorationEventContext context, Location desired) {
        var bounds = context.record().bounds();
        var world = Bukkit.getWorld(context.record().worldId());
        if (world == null) return Optional.empty();
        double centerX = bounds.centerX() + 0.5D;
        double centerZ = bounds.centerZ() + 0.5D;
        Location preferred = new Location(world, desired.getX(), desired.getY(), desired.getZ());
        int minX = Math.max(bounds.minX(), (int) Math.floor(centerX) - 6);
        int maxX = Math.min(bounds.maxX(), (int) Math.floor(centerX) + 6);
        int minZ = Math.max(bounds.minZ(), (int) Math.floor(centerZ) - 6);
        int maxZ = Math.min(bounds.maxZ(), (int) Math.floor(centerZ) + 6);
        int minY = Math.max(world.getMinHeight() + 1, bounds.minY());
        int maxY = Math.min(world.getMaxHeight() - 2, bounds.maxY() + 2);

        return IntStream.rangeClosed(minX, maxX).boxed()
                .flatMap(x -> IntStream.rangeClosed(minZ, maxZ).boxed()
                        .flatMap(z -> IntStream.rangeClosed(minY, maxY).mapToObj(y -> new int[]{x, y, z})))
                .filter(pos -> isEntitySafe(world.getBlockAt(pos[0], pos[1], pos[2])))
                .map(pos -> new Location(world, pos[0] + 0.5D, pos[1], pos[2] + 0.5D,
                        desired.getYaw(), desired.getPitch()))
                .min(Comparator.comparingDouble(location -> location.toVector().distanceSquared(preferred.toVector())));
    }

    private static boolean isEntitySafe(Block feet) {
        Block head = feet.getRelative(0, 1, 0);
        Block floor = feet.getRelative(0, -1, 0);
        return isOpen(feet) && isOpen(head) && floor.getType().isSolid();
    }

    private static boolean isOpen(Block block) {
        return block.isPassable() && !block.isLiquid();
    }
}
