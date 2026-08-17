package com.hyunseo.hyunseorpg.farming;

/** Runtime view of a persisted delivery. */
public record DeliverySession(
        String deliveryId,
        DeliveryDefinition definition,
        DeliveryRequirement requirement,
        long createdAt,
        long expiresAt,
        DeliveryStatus status,
        long nextAvailableAt
) {
    public DeliverySession(String deliveryId, DeliveryDefinition definition,
                            DeliveryRequirement requirement, long createdAt,
                            long expiresAt, DeliveryStatus status) {
        this(deliveryId, definition, requirement, createdAt, expiresAt, status,
                status == DeliveryStatus.ACTIVE ? expiresAt : 0L);
    }

    public long remainingSeconds(long now) {
        return Math.max(0L, (nextAvailableAt - Math.max(0L, now)) / 1000L);
    }
}
