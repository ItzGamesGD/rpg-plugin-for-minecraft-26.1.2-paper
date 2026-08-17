package com.hyunseo.hyunseorpg.mob;

import org.bukkit.Location;

import java.util.List;

public record MobSpawnZone(
        String zoneId,
        String worldName,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        int mobLevel,
        String mobIdPrefix,
        List<String> tags
) {
    public MobSpawnZone {
        int safeMinX = Math.min(minX, maxX);
        int safeMinY = Math.min(minY, maxY);
        int safeMinZ = Math.min(minZ, maxZ);
        int safeMaxX = Math.max(minX, maxX);
        int safeMaxY = Math.max(minY, maxY);
        int safeMaxZ = Math.max(minZ, maxZ);

        minX = safeMinX;
        minY = safeMinY;
        minZ = safeMinZ;
        maxX = safeMaxX;
        maxY = safeMaxY;
        maxZ = safeMaxZ;
        mobLevel = Math.max(1, mobLevel);
        tags = List.copyOf(tags);
    }

    public boolean contains(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) {
            return false;
        }

        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();
        return blockX >= minX && blockX <= maxX
                && blockY >= minY && blockY <= maxY
                && blockZ >= minZ && blockZ <= maxZ;
    }
}
