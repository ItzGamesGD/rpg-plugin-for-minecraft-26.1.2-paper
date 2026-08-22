package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExplorationRuntimeChoiceTest {
    @Test
    void pendingChoiceDoesNotCreateAnObjectiveOrAllowAnotherPlayerToChoose() {
        UUID owner = UUID.randomUUID();
        ExplorationRuntime runtime = new ExplorationRuntime(UUID.randomUUID(), "outpost_raid_event", 10L);
        assertTrue(runtime.beginChoice(owner, "outpost_raid_difficulty", Set.of("tier1", "tier2", "tier3", "flee"), "flee", 410L));
        assertTrue(runtime.choicePending());
        assertFalse(runtime.objectiveMode());
        assertFalse(runtime.choose(UUID.randomUUID(), "tier1"));
        assertTrue(runtime.choose(owner, "tier2"));
        assertFalse(runtime.choicePending());
        assertTrue(runtime.selectedChoice().equals("tier2"));
    }

    @Test
    void promptExpirationUsesConfiguredDeadline() {
        ExplorationRuntime runtime = new ExplorationRuntime(UUID.randomUUID(), "outpost_raid_event", 10L);
        assertTrue(runtime.beginChoice(UUID.randomUUID(), "outpost", Set.of("flee"), "flee", 30L));
        assertFalse(runtime.choiceExpired(29L));
        assertTrue(runtime.choiceExpired(30L));
    }

    @Test
    void lootOwnerIsPreferredForTheExitChoice() {
        UUID activator = UUID.randomUUID();
        UUID looter = UUID.randomUUID();
        ExplorationRuntime runtime = new ExplorationRuntime(UUID.randomUUID(), "outpost_raid_event", 10L);
        runtime.addParticipant(activator);
        runtime.markLootTaken(looter, 20L);

        assertTrue(runtime.lootTaken());
        assertEquals(looter, runtime.looter());
        assertEquals(20L, runtime.lootTakenAtTick());
        assertTrue(runtime.beginChoice(runtime.looter(), "outpost_raid_difficulty",
                Set.of("tier1", "flee"), "flee", 60L));
        assertTrue(runtime.choose(looter, "tier1"));
    }
}
