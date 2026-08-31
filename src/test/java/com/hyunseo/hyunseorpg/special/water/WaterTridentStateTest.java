package com.hyunseo.hyunseorpg.special.water;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WaterTridentStateTest {
    @Test void landWaterLandWaterTransitionsControlOnlyCurrentEffects() {
        assertTrue(WaterTridentState.directHitMayAttack(WaterTridentState.FlightPhase.OUTWARD),
                "ordinary direct impact remains legal on land");
        assertFalse(WaterTridentState.currentMayAttack(WaterTridentState.FlightPhase.OUTWARD, false),
                "land prohibits current damage and pull");
        assertTrue(WaterTridentState.currentMayAttack(WaterTridentState.FlightPhase.OUTWARD, true),
                "entering water activates current damage and pull");
        assertFalse(WaterTridentState.currentMayAttack(WaterTridentState.FlightPhase.OUTWARD, false),
                "leaving water immediately deactivates both effects");
        assertTrue(WaterTridentState.currentMayAttack(WaterTridentState.FlightPhase.OUTWARD, true),
                "re-entering water activates them again");
    }

    @Test void loyaltyReturnNeverReactivatesCurrentDamage() {
        assertFalse(WaterTridentState.currentMayAttack(WaterTridentState.FlightPhase.RETURNING, true));
        assertFalse(WaterTridentState.directHitMayAttack(WaterTridentState.FlightPhase.RETURNING));
        assertFalse(WaterTridentState.currentMayAttack(WaterTridentState.FlightPhase.REMOVED, true));
    }

    @Test void thirdConsecutiveHitBurstsThenResets() {
        WaterTridentState state = new WaterTridentState();
        UUID player = UUID.randomUUID(); UUID target = UUID.randomUUID();
        assertEquals(1, state.registerCombo(player, target, 1_000, 500));
        assertEquals(2, state.registerCombo(player, target, 1_100, 500));
        assertEquals(3, state.registerCombo(player, target, 1_200, 500));
        assertEquals(1, state.registerCombo(player, target, 1_300, 500));
    }

    @Test void changingTargetResetsCombo() {
        WaterTridentState state = new WaterTridentState(); UUID player = UUID.randomUUID();
        UUID targetA = UUID.randomUUID(); UUID targetB = UUID.randomUUID();
        assertEquals(1, state.registerCombo(player, targetA, 1_000, 100));
        assertEquals(2, state.registerCombo(player, targetA, 1_010, 100));
        assertEquals(1, state.registerCombo(player, targetB, 1_020, 100));
    }

    @Test void timeoutAndExplicitCleanupPreventStaleCombo() {
        WaterTridentState state = new WaterTridentState(); UUID player = UUID.randomUUID(); UUID target = UUID.randomUUID();
        assertEquals(1, state.registerCombo(player, target, 2_000, 100));
        assertEquals(1, state.registerCombo(player, target, 2_101, 100));
        assertEquals(2, state.registerCombo(player, target, 2_102, 100));
        state.clear(player);
        assertEquals(0, state.activeCombos());
        assertEquals(1, state.registerCombo(player, target, 2_103, 100));
    }

    @Test void eightIndependentObjectsEachAllowAtMostThreeUniqueHits() {
        WaterTridentState.SyntheticAttack[] attacks = new WaterTridentState.SyntheticAttack[WaterTridentState.SYNTHETIC_COUNT];
        assertEquals(8, attacks.length);
        for (int i = 0; i < attacks.length; i++) {
            attacks[i] = new WaterTridentState.SyntheticAttack(3);
            UUID first = UUID.randomUUID();
            assertTrue(attacks[i].tryHit(first)); assertFalse(attacks[i].tryHit(first));
            assertTrue(attacks[i].tryHit(UUID.randomUUID())); assertTrue(attacks[i].tryHit(UUID.randomUUID()));
            assertFalse(attacks[i].tryHit(UUID.randomUUID())); assertEquals(3, attacks[i].hitCount());
        }
    }

    @Test void signatureAlwaysRunsVortexBeforeEightWayAndHasNoOrbitPhase() {
        WaterTridentState.SignaturePhase phase = WaterTridentState.SignaturePhase.VORTEX;
        phase = WaterTridentState.advanceSignature(phase);
        assertEquals(WaterTridentState.SignaturePhase.EIGHT_WAY, phase);
        phase = WaterTridentState.advanceSignature(phase);
        assertEquals(WaterTridentState.SignaturePhase.COMPLETE, phase);
    }

    @Test void onlyNormalRiptideTerminationReceivesBoost() {
        assertTrue(WaterTridentState.applyNormalVerticalBoost(WaterTridentState.MovementEnd.NORMAL));
        assertFalse(WaterTridentState.applyNormalVerticalBoost(WaterTridentState.MovementEnd.COLLISION));
        assertFalse(WaterTridentState.applyNormalVerticalBoost(WaterTridentState.MovementEnd.CANCELLED));
        assertFalse(WaterTridentState.applyNormalVerticalBoost(WaterTridentState.MovementEnd.INVALIDATED));
    }

    @Test void rainRequiresLandStormAndSkyExposure() {
        assertTrue(WaterTridentState.rainExposed(true, true, true, 64, 64));
        assertFalse(WaterTridentState.rainExposed(false, true, true, 64, 64));
        assertFalse(WaterTridentState.rainExposed(true, false, true, 64, 64));
        assertFalse(WaterTridentState.rainExposed(true, true, false, 64, 64));
        assertFalse(WaterTridentState.rainExposed(true, true, true, 70, 64));
    }
}
