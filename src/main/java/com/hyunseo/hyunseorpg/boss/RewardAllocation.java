package com.hyunseo.hyunseorpg.boss;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure largest-remainder allocation. It never creates or loses integer units. */
public final class RewardAllocation {
    private RewardAllocation() { }

    public static Map<UUID, Long> allocate(long total, Map<UUID, Double> contributions) {
        if (total <= 0L || contributions == null || contributions.isEmpty()) return Map.of();
        double sum = contributions.values().stream()
                .filter(value -> value != null && Double.isFinite(value) && value > 0.0D)
                .mapToDouble(Double::doubleValue).sum();
        if (!(sum > 0.0D) || !Double.isFinite(sum)) return Map.of();

        Map<UUID, Long> result = new LinkedHashMap<>();
        List<Remainder> remainders = new ArrayList<>();
        long assigned = 0L;
        for (Map.Entry<UUID, Double> entry : contributions.entrySet()) {
            double contribution = entry.getValue() == null ? 0.0D : entry.getValue();
            if (!(contribution > 0.0D) || !Double.isFinite(contribution)) continue;
            double exact = total * (contribution / sum);
            long floor = Math.max(0L, Math.min(total, (long) Math.floor(exact)));
            result.put(entry.getKey(), floor);
            assigned = safeAdd(assigned, floor);
            remainders.add(new Remainder(entry.getKey(), exact - floor));
        }
        long remaining = Math.max(0L, total - assigned);
        remainders.sort(Comparator.comparingDouble(Remainder::fraction).reversed()
                .thenComparing(value -> value.uuid().toString()));
        for (int index = 0; index < remaining && !remainders.isEmpty(); index++) {
            UUID uuid = remainders.get(index % remainders.size()).uuid();
            result.put(uuid, safeAdd(result.getOrDefault(uuid, 0L), 1L));
        }
        return Map.copyOf(result);
    }

    private static long safeAdd(long first, long second) {
        return Long.MAX_VALUE - first < second ? Long.MAX_VALUE : first + second;
    }

    private record Remainder(UUID uuid, double fraction) { }
}
