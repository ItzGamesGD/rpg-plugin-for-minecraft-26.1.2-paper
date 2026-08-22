package com.hyunseo.hyunseorpg.exploration.model;

import java.util.Objects;
import java.util.UUID;

/** Stable structure observation produced by a Paper/Bukkit adapter. */
public record StructureCandidate(
        UUID worldId,
        String minecraftKey,
        StructureBounds bounds,
        StructureAnchor anchor
) {
    public StructureCandidate {
        Objects.requireNonNull(worldId, "worldId");
        minecraftKey = normalize(minecraftKey);
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(anchor, "anchor");
        if (!worldId.equals(anchor.worldId())) throw new IllegalArgumentException("anchor world mismatch");
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("minecraftKey is blank");
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
