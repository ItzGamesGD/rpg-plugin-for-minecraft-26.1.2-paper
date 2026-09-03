package com.hyunseo.hyunseorpg.special.moonlit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoonShadowStateTest {
    private static MoonShadowState.SlashSnapshot slash(double x) {
        var origin = new MoonlitAfterglowMath.Point(x, 1, 2);
        var target = new MoonlitAfterglowMath.Point(x + 3, 4, 5);
        return new MoonShadowState.SlashSnapshot(origin, target, new MoonlitAfterglowMath.Point(3, 3, 3));
    }

    @Test void failedIterationDoesNotStopTenAttempts() {
        MoonShadowState state = new MoonShadowState(10);
        for (int i = 0; i < 10; i++) assertTrue(state.attempt(i == 3 ? null : slash(i)));
        assertEquals(10, state.attempts());
        assertEquals(9, state.slashes().size());
        assertEquals(MoonShadowState.Phase.WAITING_FOR_FINAL_TRIGGER, state.phase());
    }

    @Test void snapshotsAreImmutableAndFinalTriggerIsSingleUse() {
        MoonShadowState state = new MoonShadowState(2);
        state.attempt(slash(1)); state.attempt(slash(2));
        var released = state.trigger();
        assertEquals(2, released.size());
        assertEquals(new MoonlitAfterglowMath.Point(4, 4, 5), released.getFirst().target());
        assertThrows(UnsupportedOperationException.class, () -> released.add(slash(9)));
        assertTrue(state.trigger().isEmpty());
    }
}
