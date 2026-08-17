package com.hyunseo.hyunseorpg.farming;

import java.util.Map;
import java.util.Set;

/** Immutable view of a player's farming data. */
public record FarmingProfile(
        int dataVersion,
        FarmingStage stage,
        long totalValidHarvests,
        Map<String, Long> cropHarvests,
        Set<String> unlockedCrops,
        Map<String, Integer> statTokenUses,
        long abundancePoints,
        Map<String, Long> favor,
        Map<String, FarmingDeliveryState> deliveries,
        Map<String, Integer> deliveryCompletedCounts
) {
    public FarmingProfile {
        stage = stage == null ? FarmingStage.BASIC : stage;
        cropHarvests = Map.copyOf(cropHarvests == null ? Map.of() : cropHarvests);
        unlockedCrops = Set.copyOf(unlockedCrops == null ? Set.of() : unlockedCrops);
        statTokenUses = Map.copyOf(statTokenUses == null ? Map.of() : statTokenUses);
        abundancePoints = Math.max(0L, abundancePoints);
        favor = Map.copyOf(favor == null ? Map.of() : favor);
        deliveries = Map.copyOf(deliveries == null ? Map.of() : deliveries);
        deliveryCompletedCounts = Map.copyOf(deliveryCompletedCounts == null ? Map.of() : deliveryCompletedCounts);
    }

    public FarmingProfile(int dataVersion, FarmingStage stage, long totalValidHarvests,
                          Map<String, Long> cropHarvests, Set<String> unlockedCrops,
                          Map<String, Integer> statTokenUses,
                          Map<String, FarmingDeliveryState> deliveries,
                          Map<String, Integer> deliveryCompletedCounts) {
        this(dataVersion, stage, totalValidHarvests, cropHarvests, unlockedCrops, statTokenUses,
                0L, Map.of(), deliveries, deliveryCompletedCounts);
    }
}
