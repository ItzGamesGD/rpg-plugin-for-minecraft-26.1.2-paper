package com.hyunseo.hyunseorpg.farming;

/** Immutable hoe snapshot bound to a delivery GUI session. */
public record DeliveryHoeBinding(String instanceId, double abundanceMultiplier) {
    public DeliveryHoeBinding {
        instanceId = instanceId == null ? "" : instanceId.trim();
        abundanceMultiplier = Double.isFinite(abundanceMultiplier)
                ? Math.max(0.0D, abundanceMultiplier) : 1.0D;
    }

    public static DeliveryHoeBinding neutral() {
        return new DeliveryHoeBinding("", 1.0D);
    }

    public boolean isBound() {
        return !instanceId.isBlank();
    }
}
