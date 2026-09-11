package com.hyunseo.hyunseorpg.special;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DedicatedWeaponIdsTest {
    @Test void preservesTheUnionOfAllDedicatedInputOwners() {
        assertEquals(Set.of("flame_axe", "poseidon_spear", "thanatos_mace", "thunder_gods_axe"),
                DedicatedWeaponIds.all());
        assertFalse(DedicatedWeaponIds.owns("ordinary_weapon"));
    }
}
