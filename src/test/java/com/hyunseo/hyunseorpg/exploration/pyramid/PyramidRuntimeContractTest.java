package com.hyunseo.hyunseorpg.exploration.pyramid;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import com.hyunseo.hyunseorpg.exploration.component.impl.PyramidPushPillarService;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomService;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntimeManager;

class PyramidRuntimeContractTest {
    @Test void revealHasExplicitPuzzleContinuationHook() {
        assertDoesNotThrow(() -> PyramidRoomService.class.getMethod("setRevealCompletion", java.util.function.Consumer.class));
        assertDoesNotThrow(() -> ExplorationRuntimeManager.class.getMethod(
                "continuePyramidPuzzle",
                com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext.class));
    }

    @Test void undergroundServiceOwnsBoundedRetryState() {
        assertTrue(java.util.Arrays.stream(PyramidPushPillarService.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("completionRetryAttempts")));
        assertTrue(java.util.Arrays.stream(PyramidPushPillarService.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("completionRetryTasks")));
    }

    @Test void failedDisplayMoveIsNotAcceptedAsProgress() {
        assertFalse(PyramidPushPillarService.displayMoveSucceeded(false));
        assertTrue(PyramidPushPillarService.displayMoveSucceeded(true));
    }

    @Test void rewardPortExposesDurableIdempotentHook() throws Exception {
        Method method = com.hyunseo.hyunseorpg.exploration.integration.ExplorationPorts.RewardPort.class
                .getMethod("enqueueDurable", org.bukkit.entity.Player.class, String.class, int.class,
                        org.bukkit.Location.class, java.util.Map.class, String.class);
        assertEquals(boolean.class, method.getReturnType());
    }

    @Test void pendingMailboxExposesIdempotentEnqueue() throws Exception {
        Method method = com.hyunseo.hyunseorpg.item.PendingRewardService.class.getMethod(
                "queueItemOnce", java.util.UUID.class, java.util.UUID.class,
                org.bukkit.inventory.ItemStack.class, String.class);
        assertEquals(boolean.class, method.getReturnType());
    }
    @Test void pyramidProtectionCancelsOnlyOwnedGeometry() {
        assertTrue(com.hyunseo.hyunseorpg.exploration.listener.PyramidPuzzleProtectionListener.shouldCancelBlockEdit(true));
        assertFalse(com.hyunseo.hyunseorpg.exploration.listener.PyramidPuzzleProtectionListener.shouldCancelBlockEdit(false));
    }

}
