package com.hyunseo.hyunseorpg.boss;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RewardAllocationTest {
    @Test
    void preservesTotalForUnevenContributions() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Map<UUID, Double> contributions = new LinkedHashMap<>();
        contributions.put(first, 60.0D);
        contributions.put(second, 30.0D);
        Map<UUID, Long> shares = RewardAllocation.allocate(10L, contributions);
        assertEquals(10L, shares.values().stream().mapToLong(Long::longValue).sum());
        assertEquals(7L, shares.get(first));
        assertEquals(3L, shares.get(second));
    }

    @Test
    void ignoresZeroAndInvalidContributors() {
        UUID valid = UUID.randomUUID();
        Map<UUID, Double> contributions = new LinkedHashMap<>();
        contributions.put(valid, 1.0D);
        contributions.put(UUID.randomUUID(), 0.0D);
        contributions.put(UUID.randomUUID(), Double.NaN);
        Map<UUID, Long> shares = RewardAllocation.allocate(5L, contributions);
        assertEquals(5L, shares.get(valid));
        assertEquals(1, shares.size());
    }
}
