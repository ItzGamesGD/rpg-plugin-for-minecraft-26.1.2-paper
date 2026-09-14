package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VanillaAnvilAccessArchitectureTest {
    @Test
    void growthListenerOwnsMenusButDoesNotHijackBlockInteraction() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/enhancement/AnvilGrowthListener.java"));
        assertFalse(source.contains("PlayerInteractEvent"));
        assertFalse(source.contains("setUseInteractedBlock"));
        assertTrue(source.contains("InventoryClickEvent"));
        assertTrue(source.contains("EquipmentGrowthMenuHolder"));
        assertTrue(source.contains("EnhancementInventoryHolder"));
    }
}
