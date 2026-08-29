package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import com.hyunseo.hyunseorpg.exploration.model.StructureAnchor;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntimeManager;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExplorationLootContainerPolicyTest {
    @Test
    void desertPyramidTreasureChestMayBeBelowStructureMinY() {
        UUID world = UUID.randomUUID();
        StructureBounds bounds = new StructureBounds(-10, 64, -10, 10, 78, 10);
        StructureRecord pyramid = new StructureRecord(UUID.randomUUID(), world,
                "desert_pyramid", "minecraft:desert_pyramid",
                new StructureAnchor(world, 0, 71, 0), bounds, true, "guardian_trial",
                StructureEventState.ACTIVE, Map.of(), false, Instant.now(), null, 1);

        assertTrue(ExplorationRuntimeManager.isLootContainerInStructure(pyramid, 0, 58, 0));
        assertTrue(ExplorationRuntimeManager.isLootContainerInStructure(pyramid, 0, 64, 0));
        assertFalse(ExplorationRuntimeManager.isLootContainerInStructure(pyramid, 11, 58, 0));
        assertFalse(ExplorationRuntimeManager.isLootContainerInStructure(pyramid, 0, 47, 0));
    }

    @Test
    void outpostLootStillRequiresItsActualStructureBounds() {
        UUID world = UUID.randomUUID();
        StructureBounds bounds = new StructureBounds(-10, 64, -10, 10, 78, 10);
        StructureRecord outpost = new StructureRecord(UUID.randomUUID(), world,
                "pillager_outpost", "minecraft:pillager_outpost",
                new StructureAnchor(world, 0, 71, 0), bounds, true, "outpost_raid_event",
                StructureEventState.ACTIVE, Map.of(), false, Instant.now(), null, 1);

        assertTrue(ExplorationRuntimeManager.isLootContainerInStructure(outpost, 0, 64, 0));
        assertFalse(ExplorationRuntimeManager.isLootContainerInStructure(outpost, 0, 58, 0));
    }
}
