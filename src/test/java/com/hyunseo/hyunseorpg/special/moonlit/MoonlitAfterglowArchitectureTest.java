package com.hyunseo.hyunseorpg.special.moonlit;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Narrow source-contract checks for lifecycle/API boundaries that do not require a mocked server. */
class MoonlitAfterglowArchitectureTest {
    private static final Path LISTENER = Path.of(
            "src/main/java/com/hyunseo/hyunseorpg/special/moonlit/MoonlitAfterglowListener.java");

    @Test void moonShadowCleanupDoesNotOwnPassiveMitigation() throws IOException {
        String source = Files.readString(LISTENER);
        String cleanup = between(source, "private void cleanupMoonShadow", "private void cancelSlashTask");
        assertTrue(cleanup.contains("states.remove(playerId)"));
        assertFalse(cleanup.contains("mitigationUntilTick"));

        String ownerCleanup = between(source, "private void cleanupOwner", "@EventHandler public void onQuit");
        assertTrue(ownerCleanup.contains("mitigationUntilTick.remove(playerId)"));
    }

    @Test void finalInputUsesSpecificClickAndMeleeEventsNotBroadAnimation() throws IOException {
        String source = Files.readString(LISTENER);
        assertFalse(source.contains("PlayerAnimationEvent"));
        assertTrue(source.contains("Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK"));
        assertTrue(source.contains("MoonShadowState.Phase.WAITING_FOR_FINAL_TRIGGER) triggerFinal(player)"));
    }

    @Test void castSourceIsCapturedAtActivationAndUsedAtRelease() throws IOException {
        String source = Files.readString(LISTENER);
        assertTrue(source.contains("this.sourceItem = sourceItem.clone()"));
        assertTrue(source.contains("releaseSlash(player, runtime.sourceItem, slash)"));
        assertFalse(source.contains("releaseSlash(player, sourceItem, slash)"));
    }

    @Test void moonlitAttackSpeedIsMainHandRestricted() throws IOException {
        String service = Files.readString(Path.of(
                "src/main/java/com/hyunseo/hyunseorpg/special/SpecialEquipmentService.java"));
        String moonlit = between(service, "data.id().equals(\"moonlit_afterglow\")", "EquipmentLoreBuilder lore");
        assertTrue(moonlit.contains("EquipmentSlot.HAND"));
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        assertTrue(from >= 0 && to > from, () -> "missing source boundary: " + start + " -> " + end);
        return source.substring(from, to);
    }
}
