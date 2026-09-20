package com.hyunseo.hyunseorpg.enchant.nativeapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaEnchantmentCompatibilityPolicyTest {
    @Test
    void allowsExactlyRequestedPairsInBothDirections() {
        assertFalse(VanillaEnchantmentCompatibilityPolicy.retainConflict("minecraft:infinity", "minecraft:mending"));
        assertFalse(VanillaEnchantmentCompatibilityPolicy.retainConflict("minecraft:mending", "minecraft:infinity"));
        assertFalse(VanillaEnchantmentCompatibilityPolicy.retainConflict("minecraft:protection", "minecraft:fire_protection"));
        assertFalse(VanillaEnchantmentCompatibilityPolicy.retainConflict("minecraft:fire_protection", "minecraft:protection"));
    }

    @Test
    void retainsEveryOtherProtectionConflict() {
        String[] protection = {"protection", "fire_protection", "blast_protection", "projectile_protection"};
        for (int left = 0; left < protection.length; left++) for (int right = left + 1; right < protection.length; right++) {
            boolean requested = left == 0 && right == 1;
            String first = "minecraft:" + protection[left];
            String second = "minecraft:" + protection[right];
            assertEquals(!requested, VanillaEnchantmentCompatibilityPolicy.retainConflict(first, second), first + " -> " + second);
            assertEquals(!requested, VanillaEnchantmentCompatibilityPolicy.retainConflict(second, first), second + " -> " + first);
        }
        assertTrue(VanillaEnchantmentCompatibilityPolicy.retainConflict("minecraft:infinity", "minecraft:sharpness"));
        assertTrue(NativeEnchantDefinitions.conflicts("wind_arrow", "fire_arrow_rain"));
    }
}
