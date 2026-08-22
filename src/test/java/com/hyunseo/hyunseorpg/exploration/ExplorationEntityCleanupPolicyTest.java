package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.integration.ExplorationEntityCleanupPolicy;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplorationEntityCleanupPolicyTest {
    private final ExplorationEntityCleanupPolicy policy = new ExplorationEntityCleanupPolicy();
    private final StructureBounds bounds = new StructureBounds(-4, 60, -4, 4, 70, 4);

    @Test
    void removesOnlyUntaggedMatchingEntityInsideBounds() {
        assertTrue(policy.shouldRemove(EntityType.WITCH, EntityType.WITCH,
                false, false, bounds, 0, 64, 0));
    }

    @Test
    void preservesCustomAndRpgEntities() {
        assertFalse(policy.shouldRemove(EntityType.WITCH, EntityType.WITCH,
                true, false, bounds, 0, 64, 0));
        assertFalse(policy.shouldRemove(EntityType.WITCH, EntityType.WITCH,
                false, true, bounds, 0, 64, 0));
    }

    @Test
    void preservesOtherEntityTypesAndOutsideEntities() {
        assertFalse(policy.shouldRemove(EntityType.CAT, EntityType.WITCH,
                false, false, bounds, 0, 64, 0));
        assertFalse(policy.shouldRemove(EntityType.WITCH, EntityType.WITCH,
                false, false, bounds, 20, 64, 20));
    }
}
