package com.hyunseo.hyunseorpg.special.solaris;

import java.util.*;

/** Pure, deterministic state and geometry used by the Solaris Paper adapter. */
public final class SolarisLogic {
    private SolarisLogic() {}

    public static int orbCount(int kills, double multiplier, int cap) {
        if (kills <= 0 || multiplier <= 0 || cap <= 0) return 0;
        return Math.min(cap, (int) Math.round(kills * multiplier));
    }

    public static void pruneKills(Deque<Long> kills, long nowMillis, long windowMillis) {
        while (!kills.isEmpty() && nowMillis - kills.peekFirst() >= windowMillis) kills.removeFirst();
    }

    public static boolean claimDawn(Map<UUID, Long> lastOrigins, UUID target, long nowMillis, long resetMillis) {
        lastOrigins.entrySet().removeIf(entry -> isExpired(entry.getValue(), nowMillis, resetMillis));
        Long previous = lastOrigins.get(target);
        if (previous != null && nowMillis - previous < resetMillis) return false;
        lastOrigins.put(target, nowMillis);
        return true;
    }

    public static <T> boolean registerDawnDamage(Set<T> damaged, T target) {
        return damaged.add(target);
    }

    public static boolean isExpired(long timestampMillis, long nowMillis, long validityMillis) {
        return nowMillis - timestampMillis >= validityMillis;
    }

    public record Position(double x, double y, double z) {}
    public static Position judgmentRisePosition(double playerX, double playerY, double playerZ,
                                                double facingX, double facingZ,
                                                double behindDistance, double riseHeight) {
        double lengthSquared = facingX * facingX + facingZ * facingZ;
        double offsetX = 0;
        double offsetZ = 0;
        if (lengthSquared > 1.0e-12 && Double.isFinite(lengthSquared)) {
            double scale = -behindDistance / Math.sqrt(lengthSquared);
            offsetX = facingX * scale;
            offsetZ = facingZ * scale;
        }
        return new Position(playerX + offsetX, playerY + 1 + riseHeight, playerZ + offsetZ);
    }

    public record Candidate<T>(T value, double distanceSquared, boolean valid) {}

    public static <T> List<T> distribute(List<Candidate<T>> candidates, int strikes, int maximumHits) {
        if (strikes <= 0 || maximumHits <= 0) return List.of();
        List<Candidate<T>> sorted = candidates.stream().filter(Candidate::valid)
                .sorted(Comparator.comparingDouble(Candidate::distanceSquared)).toList();
        List<T> result = new ArrayList<>();
        Map<T, Integer> counts = new HashMap<>();
        while (result.size() < strikes) {
            boolean assigned = false;
            for (Candidate<T> candidate : sorted) {
                int count = counts.getOrDefault(candidate.value(), 0);
                if (count >= maximumHits) continue;
                result.add(candidate.value()); counts.put(candidate.value(), count + 1); assigned = true;
                if (result.size() == strikes) break;
            }
            if (!assigned) break;
        }
        return List.copyOf(result);
    }

    public record MeteorOffset(double x, double z) {}
    public static List<MeteorOffset> meteorOffsets(long seed, int count, double radius) {
        Random random = new Random(seed); List<MeteorOffset> result = new ArrayList<>();
        for (int i=0; i<Math.max(0, count); i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(random.nextDouble()) * Math.max(0, radius);
            result.add(new MeteorOffset(Math.cos(angle)*distance, Math.sin(angle)*distance));
        }
        return List.copyOf(result);
    }

    public static boolean withinImpact(double dx, double dy, double dz, double radius) {
        return dx*dx + dy*dy + dz*dz <= radius*radius;
    }

    public static List<Integer> scheduleTicks(int count, int durationTicks) {
        if (count <= 0) return List.of(); List<Integer> ticks = new ArrayList<>();
        for (int i=0; i<count; i++) ticks.add((int)Math.floor((double)i * Math.max(1, durationTicks) / count));
        return List.copyOf(ticks);
    }
}
