package com.hyunseo.hyunseorpg.exploration.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Persistent minimal record. Runtime Bukkit objects must never be stored here. */
public record StructureRecord(
        UUID structureId,
        UUID worldId,
        String structureType,
        String minecraftKey,
        StructureAnchor anchor,
        StructureBounds bounds,
        boolean rpgSelected,
        String variantId,
        StructureEventState state,
        Map<String, String> activationMetadata,
        boolean rewardClaimed,
        Instant createdAt,
        Instant completedAt,
        int dataVersion
) {
    public static final int CURRENT_DATA_VERSION = 1;

    public StructureRecord {
        Objects.requireNonNull(structureId, "structureId");
        Objects.requireNonNull(worldId, "worldId");
        structureType = normalize(structureType);
        minecraftKey = normalize(minecraftKey);
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(bounds, "bounds");
        variantId = variantId == null || variantId.isBlank() ? "" : normalize(variantId);
        Objects.requireNonNull(state, "state");
        activationMetadata = Map.copyOf(activationMetadata == null ? Map.of() : activationMetadata);
        Objects.requireNonNull(createdAt, "createdAt");
        if (!worldId.equals(anchor.worldId())) throw new IllegalArgumentException("anchor world mismatch");
        if (!rpgSelected && state != StructureEventState.VANILLA) {
            throw new IllegalArgumentException("non-RPG structure must be VANILLA");
        }
        if (!rpgSelected && !variantId.isBlank()) {
            throw new IllegalArgumentException("non-RPG structure cannot carry a variant");
        }
        if (rpgSelected && variantId.isBlank()) {
            throw new IllegalArgumentException("RPG structure requires variantId");
        }
    }

    public StructureRecord transitionTo(StructureEventState target, Instant now) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(now, "now");
        if (!canTransition(state, target)) {
            throw new IllegalStateException("invalid structure transition: " + state + " -> " + target);
        }
        Instant completed = (target == StructureEventState.CLEARED || target == StructureEventState.ABANDONED)
                ? now : completedAt;
        return new StructureRecord(structureId, worldId, structureType, minecraftKey, anchor, bounds,
                rpgSelected, variantId, target, activationMetadata, rewardClaimed, createdAt, completed, dataVersion);
    }

    public StructureRecord withMetadata(String key, String value) {
        Map<String, String> copy = new LinkedHashMap<>(activationMetadata);
        if (value == null) copy.remove(key); else copy.put(key, value);
        return new StructureRecord(structureId, worldId, structureType, minecraftKey, anchor, bounds,
                rpgSelected, variantId, state, copy, rewardClaimed, createdAt, completedAt, dataVersion);
    }

    public StructureRecord markRewardClaimed() {
        if (rewardClaimed) return this;
        return new StructureRecord(structureId, worldId, structureType, minecraftKey, anchor, bounds,
                rpgSelected, variantId, state, activationMetadata, true, createdAt, completedAt, dataVersion);
    }

    public static boolean canTransition(StructureEventState from, StructureEventState to) {
        if (from == to) return true;
        return switch (from) {
            case UNDISCOVERED -> to == StructureEventState.ACTIVE;
            case ACTIVE -> to == StructureEventState.CLEARED || to == StructureEventState.ABANDONED;
            case VANILLA, CLEARED, ABANDONED -> false;
        };
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("blank id");
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
