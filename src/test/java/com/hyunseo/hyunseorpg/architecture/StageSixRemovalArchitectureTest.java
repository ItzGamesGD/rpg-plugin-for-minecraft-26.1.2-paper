package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StageSixRemovalArchitectureTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void farmingAndCookingRuntimeAndResourcesAreAbsent() throws Exception {
        assertFalse(Files.exists(MAIN.resolve("java/com/hyunseo/hyunseorpg/farming")));
        assertFalse(Files.exists(MAIN.resolve("resources/farming")));
        String plugin = Files.readString(MAIN.resolve("java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java"));
        assertFalse(plugin.contains("hyunseorpg.farming"));
        assertFalse(plugin.contains("reloadFarming"));
    }

    @Test
    void vanillaBrewingFrontendReplacesBlanketBlockerAndCustomGui() throws Exception {
        Path alchemy = MAIN.resolve("java/com/hyunseo/hyunseorpg/alchemy");
        assertFalse(Files.exists(alchemy.resolve("AlchemyVanillaBypassListener.java")));
        String listener = Files.readString(alchemy.resolve("brewing/BrewingStandAlchemyListener.java"));
        assertTrue(listener.contains("AlchemyRecipeRegistry"));
        assertTrue(listener.contains("PotionFactory"));
        assertTrue(listener.contains("Status.UNMATCHED) return"));
        assertFalse(listener.contains("Bukkit.createInventory"));
    }
}
