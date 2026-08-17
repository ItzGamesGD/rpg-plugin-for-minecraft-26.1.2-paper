package com.hyunseo.hyunseorpg.stat;

import java.util.Objects;

public record StatModifier(
        String sourceId,
        StatType statType,
        StatModifierOperation operation,
        double amount
) {
    public StatModifier {
        sourceId = normalizeSourceId(sourceId);
        Objects.requireNonNull(statType, "statType");
        Objects.requireNonNull(operation, "operation");
    }

    private static String normalizeSourceId(String value) {
        String normalized = Objects.requireNonNull(value, "sourceId").trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("sourceId must not be empty");
        }
        return normalized;
    }
}
