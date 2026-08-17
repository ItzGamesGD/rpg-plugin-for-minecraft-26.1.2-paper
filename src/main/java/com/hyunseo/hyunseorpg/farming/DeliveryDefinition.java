package com.hyunseo.hyunseorpg.farming;

import java.util.List;

/** Immutable provider definition loaded from farming/deliveries.yml. */
public record DeliveryDefinition(
        String id,
        DeliveryProvider provider,
        String displayName,
        List<String> itemFamilies,
        int amountMin,
        int amountMax,
        long previewBasePoints,
        double rewardMultiplier,
        int weight
) {
    public DeliveryDefinition(String id, DeliveryProvider provider, String displayName,
                              List<String> itemFamilies, int amountMin, int amountMax,
                              long previewBasePoints) {
        this(id, provider, displayName, itemFamilies, amountMin, amountMax, previewBasePoints, 1.0D, 1);
    }

    public DeliveryDefinition(String id, DeliveryProvider provider, String displayName,
                              List<String> itemFamilies, int amountMin, int amountMax,
                              long previewBasePoints, double rewardMultiplier) {
        this(id, provider, displayName, itemFamilies, amountMin, amountMax, previewBasePoints,
                rewardMultiplier, 1);
    }

    public DeliveryDefinition {
        id = normalize(id, "id");
        provider = java.util.Objects.requireNonNull(provider, "provider");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        itemFamilies = itemFamilies == null ? List.of() : itemFamilies.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(java.util.Locale.ROOT))
                .distinct().toList();
        if (amountMin <= 0 || amountMax < amountMin) throw new IllegalArgumentException("invalid delivery amount range");
        if (previewBasePoints < 0L) throw new IllegalArgumentException("previewBasePoints must be non-negative");
        if (!Double.isFinite(rewardMultiplier) || rewardMultiplier < 0.0D) {
            throw new IllegalArgumentException("rewardMultiplier must be finite and non-negative");
        }
        if (weight < 0) throw new IllegalArgumentException("weight must be non-negative");
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
