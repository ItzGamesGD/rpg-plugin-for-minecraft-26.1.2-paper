package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VanillaAnvilPolicyArchitectureTest {
    @Test
    void policyIsInstalledAtOpenBeforeFirstMeaningfulCalculation() throws Exception {
        String policy = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/enhancement/VanillaAnvilPolicyListener.java"));
        String enhancement = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/enhancement/VanillaAnvilEnhancementListener.java"));
        assertTrue(policy.contains("InventoryOpenEvent"));
        assertTrue(policy.contains("event.getView() instanceof AnvilView"));
        assertTrue(policy.indexOf("initializePolicy(InventoryOpenEvent")
                < policy.indexOf("removeTooExpensiveThreshold(PrepareAnvilEvent"));
        assertTrue(policy.contains("PrepareAnvilEvent"));
        assertTrue(policy.contains("setMaximumRepairCost(MAXIMUM_REPAIR_COST)"));
        assertFalse(policy.contains("setRepairCost("));
        assertFalse(policy.contains("bypassEnchantmentLevelRestriction"));
        assertFalse(policy.contains("InventoryClickEvent"), "vanilla must retain XP pickup enforcement");
        assertFalse(policy.contains("setLevel("), "policy must not grant or rewrite player XP levels");
        assertTrue(enhancement.contains("setRepairCost(enhancements.getXpLevelCost"));
        assertFalse(enhancement.contains("setMaximumRepairCost"));
    }

    @Test
    void configuredMaximumAdmitsAllRegressionCostsWithoutChangingTheirValue() {
        int maximum = com.hyunseo.hyunseorpg.enhancement.VanillaAnvilPolicyListener.MAXIMUM_REPAIR_COST;
        for (int calculated : new int[]{39, 40, 41, 47, 80}) {
            assertTrue(calculated < maximum, "fresh-session first calculation must remain below the installed threshold");
        }
    }
}
