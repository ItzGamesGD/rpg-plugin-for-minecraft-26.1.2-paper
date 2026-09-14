package com.hyunseo.hyunseorpg.enchant.nativeapi;

import java.util.Map;
import java.util.Set;

/** Exact directed exclusions removed from vanilla registry entries; every other key is retained. */
public final class VanillaEnchantmentCompatibilityPolicy {
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "minecraft:infinity", Set.of("minecraft:mending"),
            "minecraft:mending", Set.of("minecraft:infinity"),
            "minecraft:protection", Set.of("minecraft:fire_protection"),
            "minecraft:fire_protection", Set.of("minecraft:protection")
    );

    private VanillaEnchantmentCompatibilityPolicy() { }

    public static boolean retainConflict(String owner, String candidate) {
        return !ALLOWED.getOrDefault(owner, Set.of()).contains(candidate);
    }

    public static boolean isModified(String owner) {
        return ALLOWED.containsKey(owner);
    }
}
