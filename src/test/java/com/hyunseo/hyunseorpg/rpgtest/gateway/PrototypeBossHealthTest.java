package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrototypeBossHealthTest {
    @Test void reflectedDamageReducesHpAndCompletesAtZero() {
        PrototypeBossHealth health = new PrototypeBossHealth(100);
        assertTrue(health.damage(22));
        assertEquals(78, health.current());
        assertTrue(health.damage(100));
        assertEquals(0, health.current());
        assertTrue(health.defeated());
        assertFalse(health.damage(1));
    }
}
