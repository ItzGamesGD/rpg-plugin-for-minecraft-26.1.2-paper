package com.hyunseo.hyunseorpg.special.flame;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class FlameAxeChargeStateTest {
    @Test void duplicateStartCannotResetAChargeThatStaysFullUntilRelease() {
        FlameAxeChargeState state = new FlameAxeChargeState();
        UUID player = UUID.randomUUID(), item = UUID.randomUUID();
        assertTrue(state.start(player, item, 10));
        assertFalse(state.start(player, item, 19));
        state.advanceAll(30, 20);
        assertEquals(FlameAxeChargeState.Phase.FULL, state.phase(player));
        state.advance(player, 300, 20);
        assertEquals(FlameAxeChargeState.Phase.FULL, state.phase(player));
        assertTrue(state.release(player, item).heavyAttack());
        assertFalse(state.release(player, item).heavyAttack(), "release is consumed exactly once");
    }

    @Test void earlyOrWrongItemReleaseNeverAttacksAndCleanupRemovesSession() {
        FlameAxeChargeState state = new FlameAxeChargeState();
        UUID player = UUID.randomUUID(), item = UUID.randomUUID();
        state.start(player, item, 0); state.advance(player, 19, 20);
        assertFalse(state.release(player, item).heavyAttack());
        state.start(player, item, 0); state.advance(player, 20, 20);
        assertFalse(state.release(player, UUID.randomUUID()).heavyAttack());
        state.start(player, item, 0); state.clear(player);
        assertFalse(state.contains(player));
    }
}
