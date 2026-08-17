package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

import java.util.EnumMap;
import java.util.Map;

/** Pure delivery preview calculation; it does not mutate player data or items. */
public final class DeliveryRewardCalculator {
    private final CropQualityService qualityService;
    private final ConfigService config;
    private final FavorService favorService;

    /** Stage 8 compatibility constructor. */
    public DeliveryRewardCalculator(CropQualityService qualityService) {
        this(qualityService, null, null);
    }

    public DeliveryRewardCalculator(CropQualityService qualityService,
                                    ConfigService config, FavorService favorService) {
        this.qualityService = java.util.Objects.requireNonNull(qualityService, "qualityService");
        this.config = config;
        this.favorService = favorService;
    }

    public DeliveryPreview preview(DeliverySession session, Map<CropQuality, Integer> counts) {
        return preview(session, counts, 0L, DeliveryHoeBinding.neutral());
    }

    public DeliveryPreview preview(DeliverySession session, Map<CropQuality, Integer> counts,
                                   long currentFavor, DeliveryHoeBinding hoe) {
        if (session == null) {
            return new DeliveryPreview(new DeliveryQualitySummary(Map.of(), 0.0D, 0),
                    0L, 1.0D, 1.0D, 1.0D, currentFavor,
                    1.0D, 0L, 0L, 0L, "unconfigured");
        }
        Map<CropQuality, Integer> safe = new EnumMap<>(CropQuality.class);
        for (CropQuality quality : CropQuality.values()) {
            safe.put(quality, Math.max(0, counts == null ? 0 : counts.getOrDefault(quality, 0)));
        }
        int total = safe.values().stream().mapToInt(Integer::intValue).sum();
        double scoreTotal = safe.entrySet().stream()
                .mapToDouble(entry -> entry.getValue() * qualityService.qualityScore(entry.getKey())).sum();
        double average = total == 0 ? 0.0D : scoreTotal / total;
        DeliveryQualitySummary quality = new DeliveryQualitySummary(safe, average, total);

        long base = Math.max(0L, session.definition().previewBasePoints());
        double deliveryMultiplier = safeMultiplier(session.definition().rewardMultiplier());
        double qualityMultiplier = qualityMultiplier(safe, total);
        DeliveryHoeBinding binding = hoe == null ? DeliveryHoeBinding.neutral() : hoe;
        double hoeMultiplier = binding.abundanceMultiplier();
        long safeFavor = Math.max(0L, currentFavor);
        DeliveryProvider provider = session.definition().provider();
        double favorMultiplier = favorService == null ? 1.0D : favorService.multiplier(provider, safeFavor);
        String band = favorService == null ? "unconfigured" : favorService.band(provider, average);
        long favorIncrease = favorService == null ? 0L
                : favorService.favorIncrease(provider, average, safeFavor);
        long maxFavor = favorService == null ? 0L : favorService.maxFavor(provider);
        long finalPoints = roundProduct(base, deliveryMultiplier, qualityMultiplier, favorMultiplier, hoeMultiplier);
        return new DeliveryPreview(quality, base, deliveryMultiplier, qualityMultiplier, hoeMultiplier,
                safeFavor, favorMultiplier, finalPoints, favorIncrease, maxFavor, band);
    }

    private double qualityMultiplier(Map<CropQuality, Integer> counts, int total) {
        if (config == null || total <= 0) return 1.0D;
        double weighted = 0.0D;
        for (Map.Entry<CropQuality, Integer> entry : counts.entrySet()) {
            CropQuality quality = entry.getKey();
            double value = config.getFarmingDeliveriesDouble(
                    "reward.quality-multipliers." + quality.name().toLowerCase(java.util.Locale.ROOT), 1.0D);
            if (!Double.isFinite(value) || value < 0.0D) value = 1.0D;
            weighted += entry.getValue() * value;
        }
        return safeMultiplier(weighted / total);
    }

    private long roundProduct(long base, double delivery, double quality, double favor, double hoe) {
        if (base <= 0L) return 0L;
        double value = base * safeMultiplier(delivery) * safeMultiplier(quality)
                * safeMultiplier(favor) * safeMultiplier(hoe);
        if (!Double.isFinite(value) || value <= 0.0D) return 0L;
        if (value >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(0L, Math.round(value));
    }

    private double safeMultiplier(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 1.0D;
    }
}
