package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VanillaAnvilPolicyArchitectureTest {
    @Test
    void unlimitedThresholdIsGlobalButDoesNotBypassXpOrEnchantLevels() throws Exception {
        String policy = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/enhancement/VanillaAnvilPolicyListener.java"));
        String enhancement = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/enhancement/VanillaAnvilEnhancementListener.java"));
        assertTrue(policy.contains("PrepareAnvilEvent"));
        assertTrue(policy.contains("setMaximumRepairCost(MAXIMUM_REPAIR_COST)"));
        assertFalse(policy.contains("setRepairCost("));
        assertFalse(policy.contains("bypassEnchantmentLevelRestriction"));
        assertTrue(enhancement.contains("setRepairCost(enhancements.getXpLevelCost"));
        assertFalse(enhancement.contains("setMaximumRepairCost"));
    }
}
