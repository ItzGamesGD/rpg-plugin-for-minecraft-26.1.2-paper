package com.hyunseo.hyunseorpg.special;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecialEquipmentIdentityTest {
    @Test void legacyPoseidonIdNormalizesToCanonicalRuntimeId() {
        assertEquals("poseidon_spear", SpecialEquipmentService.canonicalId("poseidons_spear"));
        assertEquals("poseidon_spear", SpecialEquipmentService.canonicalId("poseidon_spear"));
        assertEquals("poseidon_spear", SpecialEquipmentService.canonicalId(" POSEIDONS_SPEAR "));
    }
}
