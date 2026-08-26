package com.hyunseo.hyunseorpg.exploration.runtime;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.model.StructureAnchor;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import org.bukkit.Location;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExplorationRuntimeManagerTest {
    @Test
    void nextWaveRepeatsOnlyTheComponentForTheSelectedTier() {
        assertTrue(ExplorationRuntimeManager.matchesSelectedRaidWave("tier1", ExplorationComponentPhase.CHOICE_TIER_1));
        assertFalse(ExplorationRuntimeManager.matchesSelectedRaidWave("tier1", ExplorationComponentPhase.CHOICE_TIER_2));
        assertTrue(ExplorationRuntimeManager.matchesSelectedRaidWave("tier2", ExplorationComponentPhase.CHOICE_TIER_2));
        assertFalse(ExplorationRuntimeManager.matchesSelectedRaidWave("tier2", ExplorationComponentPhase.CHOICE_TIER_3));
        assertTrue(ExplorationRuntimeManager.matchesSelectedRaidWave("tier3", ExplorationComponentPhase.CHOICE_TIER_3));
        assertFalse(ExplorationRuntimeManager.matchesSelectedRaidWave("flee", ExplorationComponentPhase.CHOICE_TIER_1));
    }
    @Test
    void paddedBoundaryRecognizesCardinalAndDiagonalEntryOnly() {
        UUID world = UUID.randomUUID();
        StructureRecord record = new StructureRecord(UUID.randomUUID(), world, "desert_pyramid",
                "minecraft:desert_pyramid", new StructureAnchor(world, 0, 64, 0),
                new StructureBounds(-5, 50, -5, 5, 80, 5), true, "guardian_trial",
                StructureEventState.ACTIVE, Map.of(), false, Instant.now(), null, 1);
        assertTrue(ExplorationRuntimeManager.crossesPyramidEntryBoundary(record,
                new Location(null, -10, 64, 0), new Location(null, -4, 64, 0), 4.0D));
        assertTrue(ExplorationRuntimeManager.crossesPyramidEntryBoundary(record,
                new Location(null, 10, 64, 10), new Location(null, 4, 64, 4), 4.0D));
        assertFalse(ExplorationRuntimeManager.crossesPyramidEntryBoundary(record,
                new Location(null, 0, 64, 0), new Location(null, 1, 64, 1), 4.0D));
        assertFalse(ExplorationRuntimeManager.crossesPyramidEntryBoundary(record,
                new Location(null, -10, 64, 0), new Location(null, -9, 64, 0), 4.0D));
        assertFalse(ExplorationRuntimeManager.crossesPyramidEntryBoundary(record,
                new Location(null, 0, 64, 0), new Location(null, 10, 64, 0), 4.0D));
    }

    @Test
    void currentPyramidContentVersionIsMonotonic() {
        assertTrue(ExplorationRuntimeManager.CURRENT_PYRAMID_CONTENT_VERSION >= 3);
    }

    @Test
    void entryActorCanBeReplacedAndLootReservationCanRollback() {
        ExplorationRuntime runtime = new ExplorationRuntime(UUID.randomUUID(), UUID.randomUUID(),
                "desert_pyramid", "guardian_trial", StructureEventState.ACTIVE);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        runtime.markPyramidEntry(first, -1.0D, 0.0D);
        runtime.markPyramidEntry(second, 0.0D, 1.0D);
        assertEquals(second, runtime.entryActor());
        assertEquals(0.0D, runtime.entryDeltaX());
        assertEquals(1.0D, runtime.entryDeltaZ());
        runtime.markLootTaken(first, 10L);
        assertTrue(runtime.lootTaken());
        runtime.clearLootTaken();
        assertFalse(runtime.lootTaken());
    }

}
