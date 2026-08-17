package com.hyunseo.hyunseorpg.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RPGGiveCommandCompletionTest {
    @Test
    void exposesPendingAndClaimCompletions() {
        assertTrue(RPGGiveCommand.rootCompletion("").contains("pending"));
        assertTrue(RPGGiveCommand.rootCompletion("").contains("farming"));
        assertTrue(RPGGiveCommand.pendingCompletion("").contains("claim"));
    }

    @Test
    void exposesFarmingReloadAndFarmingSeedIds() {
        assertTrue(RPGGiveCommand.reloadCompletion("farm", new String[]{"all", "farming"}).contains("farming"));
        List<String> seeds = List.of("seed_corn", "seed_onion", "seed_chili", "seed_garlic");
        assertTrue(RPGGiveCommand.giveCompletion("seed_", seeds).containsAll(seeds));
    }

    @Test
    void exposesReloadPreflightDoctorSection() {
        assertTrue(RPGGiveCommand.rootCompletion("").contains("doctor"));
    }

    @Test
    void exposesStage9FarmingAdminActions() {
        List<String> actions = RPGGiveCommand.farmingActionCompletion("");
        assertTrue(actions.containsAll(List.of(
                "status", "unlock", "lock", "setstage", "setharvests", "addharvests",
                "setpoints", "addpoints", "setfavor", "addfavor", "reset", "give", "giveprocessed",
                "giveessence", "givetoken", "settokens", "debugharvest", "debugquality", "debugdelivery", "repairchunk",
                "recalculate", "reloadplayer", "delivery")));
    }

    @Test
    void potionIdsRequireCanonicalFactoryPath() {
        assertTrue(RPGGiveCommand.requiresCanonicalPotion("potion_vampire"));
        assertTrue(RPGGiveCommand.requiresCanonicalPotion(" POTION_VAMPIRE "));
        assertTrue(!RPGGiveCommand.requiresCanonicalPotion("processed_garlic_concentrate_normal"));
    }
}
