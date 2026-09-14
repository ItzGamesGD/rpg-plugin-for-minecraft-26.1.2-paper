package com.hyunseo.hyunseorpg.enchant.nativeapi;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LegacyEnchantMigrationPlannerTest {
    @Test
    void migratesAliasClampsLevelAndIsIdempotentAgainstExistingNativeLevel() {
        var first = LegacyEnchantMigrationPlanner.plan("blade_throw@99", id ->
                new LegacyEnchantMigrationPlanner.Target("blade_chain", 1, true, 0));
        assertEquals(Map.of("blade_chain", 1), first.additions());
        assertTrue(first.preserved().isEmpty());
        var repeated = LegacyEnchantMigrationPlanner.plan("blade_throw@99", id ->
                new LegacyEnchantMigrationPlanner.Target("blade_chain", 1, true, 2));
        assertTrue(repeated.additions().isEmpty());
        assertTrue(repeated.preserved().isEmpty());
    }

    @Test
    void preservesUnknownMissingNativeAndMixedFailuresWithoutLosingSuccessfulCopies() {
        var plan = LegacyEnchantMigrationPlanner.plan("known@2,unknown@3,missing@1", id -> switch (id) {
            case "known" -> new LegacyEnchantMigrationPlanner.Target("known", 4, true, 0);
            case "missing" -> new LegacyEnchantMigrationPlanner.Target("missing", 1, false, 0);
            default -> null;
        });
        assertEquals(Map.of("known", 2), plan.additions());
        assertEquals("unknown@3,missing@1", plan.preservedEncoding());
        assertEquals(LegacyEnchantMigrationPlanner.FailureReason.UNKNOWN_RUNTIME_ID, plan.preserved().get(0).reason());
        assertEquals(LegacyEnchantMigrationPlanner.FailureReason.MISSING_NATIVE_TARGET, plan.preserved().get(1).reason());
    }

    @Test
    void preExistingHigherNativeLevelIsNeverDowngraded() {
        var plan = LegacyEnchantMigrationPlanner.plan("known@2", id ->
                new LegacyEnchantMigrationPlanner.Target("known", 4, true, 4));
        assertTrue(plan.additions().isEmpty());
    }
}
