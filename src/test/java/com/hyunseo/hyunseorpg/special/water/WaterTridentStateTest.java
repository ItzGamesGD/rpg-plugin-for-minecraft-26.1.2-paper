package com.hyunseo.hyunseorpg.special.water;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WaterTridentStateTest {
    @Test void waterStateIsEvaluatedAtTheCurrentProjectilePosition() {
        assertTrue(WaterTridentState.currentMayAttack(WaterTridentState.FlightPhase.OUTWARD, true));
        assertFalse(WaterTridentState.currentMayAttack(WaterTridentState.FlightPhase.OUTWARD, false));
        assertTrue(WaterTridentState.currentMayAttack(WaterTridentState.FlightPhase.OUTWARD, true));
    }

    @Test void loyaltyReturnNeverReactivatesCurrentDamage() {
        assertFalse(WaterTridentState.currentMayAttack(WaterTridentState.FlightPhase.RETURNING, true));
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

    @Test void comboExpiresAndTargetChangeResetsIt() {
        WaterTridentState state = new WaterTridentState(); UUID player = UUID.randomUUID();
        assertEquals(1, state.registerCombo(player, UUID.randomUUID(), 1_000, 100));
        assertEquals(1, state.registerCombo(player, UUID.randomUUID(), 1_050, 100));
        UUID target = UUID.randomUUID();
        assertEquals(1, state.registerCombo(player, target, 2_000, 100));
        assertEquals(1, state.registerCombo(player, target, 2_101, 100));
        state.clear(player); assertEquals(0, state.activeCombos());
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

    @Test void noTargetDecisionRemainsOrbitForTheCast() {
        WaterTridentState.CastMode mode = WaterTridentState.decideCast(false);
        assertEquals(WaterTridentState.CastMode.ORBIT, mode);
        assertEquals(WaterTridentState.CastMode.ORBIT, mode); // later target presence cannot mutate the decision
        assertEquals(WaterTridentState.CastMode.ATTACK, WaterTridentState.decideCast(true));
    }

    @Test void onlyNormalRiptideTerminationReceivesBoost() {
        assertTrue(WaterTridentState.applyNormalVerticalBoost(WaterTridentState.MovementEnd.NORMAL));
        assertFalse(WaterTridentState.applyNormalVerticalBoost(WaterTridentState.MovementEnd.COLLISION));
        assertFalse(WaterTridentState.applyNormalVerticalBoost(WaterTridentState.MovementEnd.CANCELLED));
        assertFalse(WaterTridentState.applyNormalVerticalBoost(WaterTridentState.MovementEnd.INVALIDATED));
    }
}
