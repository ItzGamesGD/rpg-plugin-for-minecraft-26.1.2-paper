package com.hyunseo.hyunseorpg.special;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SpecialEquipmentEnhancementPolicyTest {
    @Test
    void loreUsesTheCanonicalEnhancementMaximum() {
        assertEquals("Enhancement: available (max +40)",
                SpecialEquipmentService.enhancementCapabilityText(40));
        assertEquals("Enhancement: unavailable",
                SpecialEquipmentService.enhancementCapabilityText(0));
    }
}
