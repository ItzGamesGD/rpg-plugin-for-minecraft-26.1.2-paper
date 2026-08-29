package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bounded, deterministic room candidate lookup for the Desert Pyramid only. */
public final class PyramidRoomLocator {
    private PyramidRoomLocator() { }

    public static Optional<PyramidRoomCandidate> find(World world, StructureBounds bounds,
                                                       int radius, int height, boolean rejectContainers) {
        if (world == null || bounds == null) return Optional.empty();
        int safeRadius = Math.max(0, Math.min(2, radius));
        int safeHeight = Math.max(1, Math.min(3, height));
        int centerX = (int) Math.floor(bounds.centerX());
        int centerZ = (int) Math.floor(bounds.centerZ());
        int minY = Math.max(world.getMinHeight() + 1, bounds.minY() + 1);
        int maxY = Math.min(world.getMaxHeight() - safeHeight - 1, bounds.maxY() - safeHeight);
        if (minY > maxY) return Optional.empty();

        List<PyramidRoomCandidate> candidates = new ArrayList<>();
        for (PyramidRoomCandidate.Slot slot : PyramidRoomCandidate.Slot.values()) {
            int x = centerX;
            int z = centerZ;
            switch (slot) {
                case NORTH -> z -= 4;
                case SOUTH -> z += 4;
                case EAST -> x += 4;
                case WEST -> x -= 4;
                case CENTER -> { }
            }
            for (int y = minY; y <= maxY; y++) {
                candidates.add(new PyramidRoomCandidate(slot,
                        new PyramidBlockPosition(x, y, z), PyramidRoomOrientation.NORTH));
            }
        }
        return PyramidRoomPreflight.firstUsable(candidates,
                candidate -> usable(world, bounds, candidate.origin(), safeRadius, safeHeight, rejectContainers));
    }

    private static boolean usable(World world, StructureBounds bounds, PyramidBlockPosition origin,
                                  int radius, int height, boolean rejectContainers) {
        if (!bounds.contains(origin.x(), origin.y(), origin.z())) return false;
        if (!world.isChunkLoaded(origin.x() >> 4, origin.z() >> 4)) return false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy < height; dy++) {
                    int x = origin.x() + dx;
                    int y = origin.y() + dy;
                    int z = origin.z() + dz;
                    if (!bounds.contains(x, y, z)) return false;
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.isPassable() || block.isLiquid()
                            || protectedBlock(block.getState(), rejectContainers)) return false;
                }
                Block floor = world.getBlockAt(origin.x() + dx, origin.y() - 1, origin.z() + dz);
                if (!floor.getType().isSolid() || floor.isLiquid()) return false;
            }
        }
        return true;
    }

    private static boolean protectedBlock(BlockState state, boolean rejectContainers) {
        return (rejectContainers && state instanceof InventoryHolder)
                || state.getType().name().contains("PORTAL")
                || state.getType().name().contains("SPAWNER");
    }
}
