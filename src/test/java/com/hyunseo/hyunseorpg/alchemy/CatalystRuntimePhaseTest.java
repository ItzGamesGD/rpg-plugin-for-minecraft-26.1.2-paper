package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.alchemy.catalyst.CatalystRuntimePhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalystRuntimePhaseTest {
    @Test
    void shockTimelineIsFreeRootFreeRoot() {
        assertEquals(CatalystRuntimePhase.Shock.FREE, CatalystRuntimePhase.shockAt(0, 60, 20));
        assertEquals(CatalystRuntimePhase.Shock.FREE, CatalystRuntimePhase.shockAt(59, 60, 20));
        assertEquals(CatalystRuntimePhase.Shock.ROOT_20T, CatalystRuntimePhase.shockAt(60, 60, 20));
        assertEquals(CatalystRuntimePhase.Shock.ROOT_20T, CatalystRuntimePhase.shockAt(79, 60, 20));
        assertEquals(CatalystRuntimePhase.Shock.FREE, CatalystRuntimePhase.shockAt(80, 60, 20));
        assertEquals(CatalystRuntimePhase.Shock.FREE, CatalystRuntimePhase.shockAt(119, 60, 20));
        assertEquals(CatalystRuntimePhase.Shock.ROOT_20T, CatalystRuntimePhase.shockAt(120, 60, 20));
    }

    @Test
    void slimeReachesFinalSplashOnlyOnTheNextCollision() {
        assertEquals(CatalystRuntimePhase.Slime.BOUNCE,
                CatalystRuntimePhase.slimeAfterCollision(CatalystRuntimePhase.Slime.BOUNCE, 1, 4));
        assertEquals(CatalystRuntimePhase.Slime.FINAL_SPLASH,
                CatalystRuntimePhase.slimeAfterCollision(CatalystRuntimePhase.Slime.BOUNCE, 4, 4));
        assertEquals(CatalystRuntimePhase.Slime.FINISHED,
                CatalystRuntimePhase.slimeAfterCollision(CatalystRuntimePhase.Slime.FINAL_SPLASH, 4, 4));
    }

    @Test
    void sculkPropagationWaitsForTheNextGeneration() {
        assertFalse(CatalystRuntimePhase.sculkPropagationDue(109, 110));
        assertTrue(CatalystRuntimePhase.sculkPropagationDue(110, 110));
    }
}
