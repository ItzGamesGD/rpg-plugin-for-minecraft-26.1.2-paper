package com.hyunseo.hyunseorpg.special.thanatos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThanatosVfxTest {
    @Test
    void exponentialFallStartsSuspendedAndStillEndsAtAuthoritativeProgress() {
        assertEquals(0.0D, ThanatosVfx.exponentialIn(0.0D));
        assertTrue(ThanatosVfx.exponentialIn(0.25D) < 0.01D);
        assertTrue(ThanatosVfx.exponentialIn(0.75D) < 0.20D);
        assertEquals(1.0D, ThanatosVfx.exponentialIn(1.0D));
    }

    @Test
    void exponentialFallIsMonotonic() {
        double previous = -1.0D;
        for (int tick = 0; tick <= 24; tick++) {
            double eased = ThanatosVfx.exponentialIn(tick / 24.0D);
            assertTrue(eased >= previous);
            previous = eased;
        }
    }
}
