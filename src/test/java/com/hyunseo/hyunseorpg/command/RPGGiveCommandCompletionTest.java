package com.hyunseo.hyunseorpg.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RPGGiveCommandCompletionTest {
    @Test
    void exposesPendingAndClaimCompletions() {
        assertTrue(RPGGiveCommand.rootCompletion("").contains("pending"));
        assertFalse(RPGGiveCommand.rootCompletion("").contains("farming"));
        assertFalse(RPGGiveCommand.rootCompletion("").contains("exploration"));
        assertTrue(RPGGiveCommand.pendingCompletion("").contains("claim"));
    }

    @Test
    void retiredFarmingReloadIsNotSuggested() {
        assertFalse(RPGGiveCommand.reloadCompletion("farm", new String[]{"all", "alchemy"}).contains("farming"));
    }

    @Test
    void exposesReloadPreflightDoctorSection() {
        assertTrue(RPGGiveCommand.rootCompletion("").contains("doctor"));
    }

    @Test
    void exposesMobMigrationTarget() {
        assertTrue(RPGGiveCommand.migrationTargetCompletion("").contains("mobs"));
        assertFalse(RPGGiveCommand.migrationTargetCompletion("").contains("exploration"));
    }

    @Test
    void exposesCanonicalEffectCommandActions() {
        assertTrue(RPGGiveCommand.effectActionCompletion("").containsAll(
                List.of("list", "apply", "remove", "clear", "debug", "reload")));
    }


    @Test
    void potionIdsRequireCanonicalFactoryPath() {
        assertTrue(RPGGiveCommand.requiresCanonicalPotion("potion_vampire"));
        assertTrue(RPGGiveCommand.requiresCanonicalPotion(" POTION_VAMPIRE "));
        assertTrue(!RPGGiveCommand.requiresCanonicalPotion("processed_garlic_concentrate_normal"));
    }
}
