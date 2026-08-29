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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

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

    @ParameterizedTest
    @MethodSource("cardinalAndDiagonalEntries")
    void allEightPaddedEntryDirectionsCrossOnce(double fromX, double fromZ, double toX, double toZ) {
        StructureRecord record = pyramidRecord();
        assertTrue(ExplorationRuntimeManager.crossesPyramidEntryBoundary(record,
                new Location(null, fromX, 64, fromZ), new Location(null, toX, 64, toZ), 4.0D));
    }

    static Stream<Arguments> cardinalAndDiagonalEntries() {
        return Stream.of(
                Arguments.of(-10D, 0D, -4D, 0D), Arguments.of(10D, 0D, 4D, 0D),
                Arguments.of(0D, -10D, 0D, -4D), Arguments.of(0D, 10D, 0D, 4D),
                Arguments.of(-10D, -10D, -4D, -4D), Arguments.of(10D, -10D, 4D, -4D),
                Arguments.of(-10D, 10D, -4D, 4D), Arguments.of(10D, 10D, 4D, 4D));
    }

    @Test
    void boundaryOnlyTriggersOutsideToInside() {
        StructureRecord record = pyramidRecord();
        assertFalse(ExplorationRuntimeManager.crossesPyramidEntryBoundary(record,
                new Location(null, -4D, 64, 0D), new Location(null, -3D, 64, 0D), 4.0D));
        assertFalse(ExplorationRuntimeManager.crossesPyramidEntryBoundary(record,
                new Location(null, -10D, 64, 0D), new Location(null, -9D, 64, 0D), 4.0D));
        assertFalse(ExplorationRuntimeManager.crossesPyramidEntryBoundary(record,
                new Location(null, 0D, 64, 0D), new Location(null, 10D, 64, 0D), 4.0D));
    }

    private static StructureRecord pyramidRecord() {
        UUID world = UUID.randomUUID();
        return new StructureRecord(UUID.randomUUID(), world, "desert_pyramid",
                "minecraft:desert_pyramid", new StructureAnchor(world, 0, 64, 0),
                new StructureBounds(-5, 50, -5, 5, 80, 5), true, "guardian_trial",
                StructureEventState.ACTIVE, Map.of(), false, Instant.now(), null, 1);
    }

    @Test
    void exactlyFourVanillaTreasureSlotsShareOneCanonicalCenterAndRejectOtherContainers() {
        StructureRecord record = pyramidRecord();
        int centerX = (int) Math.floor(record.bounds().centerX());
        int centerZ = (int) Math.floor(record.bounds().centerZ());
        int chestY = record.bounds().minY() - 11;
        for (int[] offset : new int[][] {{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            assertTrue(ExplorationRuntimeManager.isValidPyramidTreasureSlot(record,
                    org.bukkit.Material.CHEST, centerX + offset[0], chestY, centerZ + offset[1]));
        }
        assertFalse(ExplorationRuntimeManager.isValidPyramidTreasureSlot(record,
                org.bukkit.Material.CHEST, centerX, chestY, centerZ));
        assertFalse(ExplorationRuntimeManager.isValidPyramidTreasureSlot(record,
                org.bukkit.Material.TRAPPED_CHEST, centerX + 2, chestY, centerZ));
        assertFalse(ExplorationRuntimeManager.isValidPyramidTreasureSlot(record,
                org.bukkit.Material.BARREL, centerX + 2, chestY, centerZ));
        assertFalse(ExplorationRuntimeManager.isValidPyramidTreasureSlot(record,
                org.bukkit.Material.SHULKER_BOX, centerX + 2, chestY, centerZ));
        assertFalse(ExplorationRuntimeManager.isValidPyramidTreasureSlot(record,
                org.bukkit.Material.CHEST, centerX + 2, chestY, centerZ + 2));
        assertFalse(ExplorationRuntimeManager.isValidPyramidTreasureSlot(record,
                org.bukkit.Material.CHEST, centerX + 2, record.bounds().minY(), centerZ));
    }

    @Test
    void committedRoomReloadPolicyAllowsPillarsWithoutRetryState() {
        UUID world = UUID.randomUUID();
        StructureRecord committed = new StructureRecord(UUID.randomUUID(), world, "desert_pyramid",
                "minecraft:desert_pyramid", new StructureAnchor(world, 0, 64, 0),
                new StructureBounds(-5, 50, -5, 5, 80, 5), true, "guardian_trial",
                StructureEventState.ACTIVE, Map.of("pyramid-room-created", "true"), false,
                Instant.now(), null, 1);
        assertTrue(ExplorationRuntimeManager.isCommittedPyramidRoomReadyForReload(committed, false));
        assertFalse(ExplorationRuntimeManager.isCommittedPyramidRoomReadyForReload(committed, true));
        assertFalse(ExplorationRuntimeManager.isCommittedPyramidRoomReadyForReload(
                committed.withMetadata("pyramid-underground-complete", "true"), false));
    }

    @Test
    void preparedActivePyramidWithoutRuntimeIsEligibleForTargetedRehydration() {
        StructureRecord prepared = pyramidRecord()
                .withMetadata("loot-taken", "true")
                .withMetadata("pyramid-room-prepared", "true")
                .withMetadata("pyramid-loot-trigger-status", "PREPARED")
                .withMetadata("pyramid-room-origin", "0,35,0")
                .withMetadata("pyramid-room-radius", "4")
                .withMetadata("pyramid-room-height", "4");

        assertTrue(ExplorationRuntimeManager.shouldRehydratePyramid(prepared, null, prepared));
        assertFalse(ExplorationRuntimeManager.shouldRehydratePyramid(prepared,
                new ExplorationRuntime(prepared.structureId(), prepared.variantId()), prepared));
        assertFalse(ExplorationRuntimeManager.shouldRehydratePyramid(
                prepared.withMetadata("pyramid-room-created", "true"), null,
                prepared.withMetadata("pyramid-room-created", "true")));
    }

    @Test
    void currentPyramidContentVersionIsMonotonic() {
        assertTrue(ExplorationRuntimeManager.CURRENT_PYRAMID_CONTENT_VERSION >= 3);
    }

    @Test
    void outpostLootExitGraceSurvivesProximityAndWaveSchedulingKeepsTarget() {
        ExplorationRuntime runtime = new ExplorationRuntime(UUID.randomUUID(), "tier1");
        UUID looter = UUID.randomUUID();
        runtime.addParticipant(looter);
        runtime.markLootTaken(looter, 100L);
        runtime.markLootTriggerExit(140L);

        runtime.configureRaidWaveSequence(java.util.List.of("tier1", "tier2"), 20L);
        runtime.setRaidTarget(looter);
        assertTrue(runtime.scheduleNextRaidWave(200L));
        assertEquals(220L, runtime.nextRaidWaveAtTick());
        assertFalse(runtime.nextRaidWaveDue(219L));
        assertTrue(runtime.nextRaidWaveDue(220L));
        assertEquals(looter, runtime.raidTarget());
        assertEquals(140L, runtime.lootTriggerExitAtTick(),
                "proximity activation must not erase an armed loot-exit grace timer");
        assertTrue(runtime.advanceRaidWave());
        assertEquals(2, runtime.raidWaveNumber());
        assertEquals(140L, runtime.lootTriggerExitAtTick());
    }

    @Test
    void outpostGraceTimerCanBeClearedOnlyByExplicitReturnOrTeleport() {
        ExplorationRuntime runtime = new ExplorationRuntime(UUID.randomUUID(), "tier1");
        runtime.markLootTriggerExit(10L);
        assertEquals(10L, runtime.lootTriggerExitAtTick());
        runtime.clearLootTriggerExit();
        assertEquals(null, runtime.lootTriggerExitAtTick());
        runtime.markLootTriggerExit(20L);
        runtime.clearCombatAbandonExit();
        assertEquals(20L, runtime.lootTriggerExitAtTick());
    }

    @Test
    void entryActorCanBeReplacedAndLootReservationCanRollback() {
        ExplorationRuntime runtime = new ExplorationRuntime(UUID.randomUUID(), "guardian_trial");
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

    @Test
    void recoveryRequiredPyramidCannotResumePillarProgression() {
        StructureRecord record = pyramidRecord()
                .withMetadata("pyramid-room-created", "true")
                .withMetadata("pyramid-failure-state", "RECOVERY_REQUIRED");
        assertFalse(ExplorationRuntimeManager.isCommittedPyramidRoomReadyForReload(record, false));
        assertEquals("RECOVERY_REQUIRED", record.activationMetadata().get("pyramid-failure-state"));
    }

    @Test
    void interruptedRevealEvidenceIsExplicitAndNotAReplayCheckpoint() {
        StructureRecord record = pyramidRecord().withMetadata("pyramid-reveal-in-progress", "true");
        assertEquals("true", record.activationMetadata().get("pyramid-reveal-in-progress"));
        assertFalse(ExplorationRuntimeManager.isCommittedPyramidRoomReadyForReload(record, false));
    }

}
