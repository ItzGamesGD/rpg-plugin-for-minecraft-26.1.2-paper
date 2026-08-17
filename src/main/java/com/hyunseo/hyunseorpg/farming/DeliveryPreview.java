package com.hyunseo.hyunseorpg.farming;

public record DeliveryPreview(
        DeliveryQualitySummary quality,
        long basePoints,
        double deliveryMultiplier,
        double qualityMultiplier,
        double hoeMultiplier,
        long currentFavor,
        double favorMultiplier,
        long finalPoints,
        long favorIncrease,
        long maxFavor,
        String qualityBand
) {
    public DeliveryPreview {
        quality = quality == null ? new DeliveryQualitySummary(java.util.Map.of(), 0.0D, 0) : quality;
        basePoints = Math.max(0L, basePoints);
        deliveryMultiplier = safeMultiplier(deliveryMultiplier);
        qualityMultiplier = safeMultiplier(qualityMultiplier);
        hoeMultiplier = safeMultiplier(hoeMultiplier);
        currentFavor = Math.max(0L, currentFavor);
        favorMultiplier = safeMultiplier(favorMultiplier);
        finalPoints = Math.max(0L, finalPoints);
        favorIncrease = Math.max(0L, favorIncrease);
        maxFavor = Math.max(0L, maxFavor);
        qualityBand = qualityBand == null || qualityBand.isBlank() ? "unconfigured" : qualityBand;
    }

    /** Backwards-compatible neutral preview constructor for Stage 8 callers. */
    public DeliveryPreview(DeliveryQualitySummary quality, long basePoints,
                           double favorMultiplier, long finalPoints) {
        this(quality, basePoints, 1.0D, 1.0D, 1.0D, 0L, favorMultiplier,
                finalPoints, 0L, 0L, "unconfigured");
    }

    private static double safeMultiplier(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 1.0D;
    }
}
