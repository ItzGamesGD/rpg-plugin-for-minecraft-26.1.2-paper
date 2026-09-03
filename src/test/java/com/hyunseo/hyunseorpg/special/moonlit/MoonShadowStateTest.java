package com.hyunseo.hyunseorpg.special.moonlit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoonShadowStateTest {
    private static MoonShadowState.SlashSnapshot slash(double x) {
        var origin = new MoonlitAfterglowMath.Point(x, 1, 2);
        var target = new MoonlitAfterglowMath.Point(x + 3, 4, 5);
        return new MoonShadowState.SlashSnapshot(origin, target, new MoonlitAfterglowMath.Point(3, 3, 3));
    }

    @Test void everyFailurePatternStillCompletesAllConfiguredAttempts() {
        assertAttemptOutcome(i -> true, 10);
        assertAttemptOutcome(i -> i != 3, 9);
        assertAttemptOutcome(i -> i % 2 == 0, 5);
        assertAttemptOutcome(i -> false, 0);
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

    @Test void laterTargetPositionsDoNotMutateEarlierSnapshots() {
        MoonShadowState state = new MoonShadowState(2);
        state.attempt(slash(1));
        MoonlitAfterglowMath.Point originalTarget = state.slashes().getFirst().target();
        state.attempt(slash(20));
        assertEquals(new MoonlitAfterglowMath.Point(4, 4, 5), originalTarget);
        assertEquals(originalTarget, state.trigger().getFirst().target());
    }

    private void assertAttemptOutcome(java.util.function.IntPredicate succeeds, int expectedSnapshots) {
        MoonShadowState state = new MoonShadowState(10);
        for (int i = 0; i < 10; i++) assertTrue(state.attempt(succeeds.test(i) ? slash(i) : null));
        assertEquals(10, state.attempts());
        assertEquals(expectedSnapshots, state.slashes().size());
        assertEquals(MoonShadowState.Phase.WAITING_FOR_FINAL_TRIGGER, state.phase());
        assertEquals(expectedSnapshots, state.trigger().size());
    }
}
