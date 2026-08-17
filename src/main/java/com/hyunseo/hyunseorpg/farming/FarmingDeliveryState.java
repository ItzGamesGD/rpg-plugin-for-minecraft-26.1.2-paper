package com.hyunseo.hyunseorpg.farming;

import java.util.Objects;

/** Serialized active delivery state. Rolled requirements are persisted for restart safety. */
public record FarmingDeliveryState(
        String deliveryId,
        String definitionId,
        String itemFamily,
        int requiredAmount,
        CropQuality minimumQuality,
        long createdAt,
        long expiresAt,
        DeliveryStatus status,
        long statusAt
) {
    public FarmingDeliveryState(String deliveryId, String definitionId, String itemFamily,
                                int requiredAmount, CropQuality minimumQuality,
                                long createdAt, long expiresAt, DeliveryStatus status) {
        this(deliveryId, definitionId, itemFamily, requiredAmount, minimumQuality,
                createdAt, expiresAt, status, status == DeliveryStatus.ACTIVE ? createdAt : 0L);
    }

    public FarmingDeliveryState {
        deliveryId = normalize(deliveryId, "deliveryId");
        definitionId = normalize(definitionId, "definitionId");
        itemFamily = normalize(itemFamily, "itemFamily");
        if (requiredAmount <= 0) throw new IllegalArgumentException("requiredAmount must be positive");
        minimumQuality = minimumQuality == null ? CropQuality.NORMAL : minimumQuality;
        if (createdAt < 0L || expiresAt < createdAt) {
            throw new IllegalArgumentException("invalid delivery time range");
        }
        if (statusAt < 0L) throw new IllegalArgumentException("invalid delivery status time");
        status = Objects.requireNonNull(status, "status");
    }

    public static FarmingDeliveryState active(String deliveryId, String definitionId, String itemFamily,
                                               int requiredAmount, CropQuality minimumQuality,
                                               long createdAt, long expiresAt) {
        return new FarmingDeliveryState(deliveryId, definitionId, itemFamily, requiredAmount,
                minimumQuality, createdAt, expiresAt, DeliveryStatus.ACTIVE, createdAt);
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
