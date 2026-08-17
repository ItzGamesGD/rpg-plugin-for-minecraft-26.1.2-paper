package com.hyunseo.hyunseorpg.farming;

import java.util.Map;

public record DeliveryQualitySummary(Map<CropQuality, Integer> counts, double averageScore, int totalAmount) {
    public DeliveryQualitySummary {
        counts = Map.copyOf(counts == null ? Map.of() : counts);
        averageScore = Double.isFinite(averageScore) ? Math.max(0.0D, averageScore) : 0.0D;
        totalAmount = Math.max(0, totalAmount);
    }
}
