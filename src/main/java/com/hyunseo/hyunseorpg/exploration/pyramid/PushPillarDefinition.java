package com.hyunseo.hyunseorpg.exploration.pyramid;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, pyramid-specific logical definition for one push pillar.
 *
 * <p>Configuration loading must catch {@link IllegalArgumentException} and
 * fail-safe disable the affected underground module instead of creating a
 * partially valid puzzle.</p>
 */
public record PushPillarDefinition(
    String id,
    String symbolId,
    String color,
    PyramidGridPoint initialPosition,
    PyramidGridPoint targetPosition,
    Set<PyramidGridPoint> allowedCells
) {
    public PushPillarDefinition {
        requireText(id, "id");
        requireText(symbolId, "symbolId");
        requireText(color, "color");
        Objects.requireNonNull(initialPosition, "initialPosition");
        Objects.requireNonNull(targetPosition, "targetPosition");
        Objects.requireNonNull(allowedCells, "allowedCells");

        allowedCells = Set.copyOf(new LinkedHashSet<>(allowedCells));
        if (allowedCells.isEmpty()) {
            throw new IllegalArgumentException("allowedCells must not be empty");
        }
        if (!allowedCells.contains(initialPosition)) {
            throw new IllegalArgumentException("initialPosition must be an allowed cell");
        }
        if (!allowedCells.contains(targetPosition)) {
            throw new IllegalArgumentException("targetPosition must be an allowed cell");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
