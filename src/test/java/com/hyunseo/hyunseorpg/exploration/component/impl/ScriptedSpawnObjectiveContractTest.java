package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScriptedSpawnObjectiveContractTest {
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();

    @Test
    void exactDistinctIdsBecomeObjectives() {
        ExplorationRuntime runtime = runtime();
        List<UUID> ids = ScriptedSpawnComponent.validateAndTrack(runtime, List.of(A, B), 2, true);
        runtime.trackObjectives(ids);

        assertEquals(Set.of(A, B), runtime.objectiveEntities());
        assertEquals(2, trackedCleanupCount(runtime));
    }

    @Test
    void partialResultFailsAfterRegisteringCleanup() {
        assertInvalidResult(List.of(A), 1);
    }

    @Test
    void duplicateIdentityDoesNotSatisfyCountAndIsCleanupOwned() {
        assertInvalidResult(List.of(A, A), 1);
    }

    @Test
    void nullDoesNotSatisfyCountAndValidIdentityIsCleanupOwned() {
        List<UUID> returned = new ArrayList<>();
        returned.add(A);
        returned.add(null);
        assertInvalidResult(returned, 1);
    }

    @Test
    void overflowFailsWithoutTruncatingCleanupOwnership() {
        assertInvalidResult(List.of(A, B, C), 3);
    }

    private static void assertInvalidResult(Collection<UUID> returned, int expectedTracked) {
        ExplorationRuntime runtime = runtime();
        assertThrows(IllegalStateException.class,
                () -> ScriptedSpawnComponent.validateAndTrack(runtime, returned, 2, true));
        assertEquals(expectedTracked, trackedCleanupCount(runtime));
        assertEquals(Set.of(), runtime.objectiveEntities());
    }

    private static ExplorationRuntime runtime() {
        return new ExplorationRuntime(UUID.randomUUID(), "test");
    }

    private static int trackedCleanupCount(ExplorationRuntime runtime) {
        try {
            Field cleanupField = runtime.tracker().getClass().getDeclaredField("cleanup");
            cleanupField.setAccessible(true);
            return ((Deque<?>) cleanupField.get(runtime.tracker())).size();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
