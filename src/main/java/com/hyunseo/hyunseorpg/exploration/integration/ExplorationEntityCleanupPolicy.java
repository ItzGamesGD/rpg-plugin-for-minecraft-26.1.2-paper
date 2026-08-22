package com.hyunseo.hyunseorpg.exploration.integration;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import org.bukkit.entity.EntityType;

/** Pure safety policy for structure-scoped unmanaged entity cleanup. */
public final class ExplorationEntityCleanupPolicy {
    public boolean shouldRemove(EntityType candidateType, EntityType targetType,
                                boolean rpgMob, boolean customMob,
                                StructureBounds bounds, double x, double y, double z) {
        if (candidateType == null || targetType == null || candidateType != targetType) return false;
        if (rpgMob || customMob || bounds == null) return false;
        return bounds.distanceSquaredTo(x, y, z) <= 1.0D;
    }
}
