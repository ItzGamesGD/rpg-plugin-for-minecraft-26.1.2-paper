package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class Stage6FinalArchitectureTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test void retiredLifestyleRuntimeAndResourcesAreAbsent() {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/hyunseo/hyunseorpg/farming")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/farming")));
    }

    @Test void blanketBrewingBlockerIsAbsentAndNativeIntegrationIsWired() throws Exception {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/hyunseo/hyunseorpg/alchemy/AlchemyVanillaBypassListener.java")));
        String plugin = Files.readString(ROOT.resolve("src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java"));
        assertTrue(plugin.contains("VanillaBrewingStandAlchemyListener"));
        String listener = Files.readString(ROOT.resolve("src/main/java/com/hyunseo/hyunseorpg/alchemy/brewing/VanillaBrewingStandAlchemyListener.java"));
        assertFalse(listener.contains("setCancelled"));
        assertFalse(listener.contains("Bukkit.createInventory"));
        assertTrue(listener.contains("PotionFactory"));
        assertTrue(listener.contains("findTransition"));
    }
}
