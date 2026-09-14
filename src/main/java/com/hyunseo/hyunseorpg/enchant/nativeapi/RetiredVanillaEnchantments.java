package com.hyunseo.hyunseorpg.enchant.nativeapi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Serialization-only registry shims for former Hyunseo copies of vanilla enchantments.
 * They have no supported items, acquisition tags, or runtime definitions. Existing stacks
 * can therefore deserialize and be lazily converted without making the entries obtainable.
 */
public final class RetiredVanillaEnchantments {
    public static final Map<String, String> VANILLA_TARGETS;
    public static final Set<String> IDS;
    public static final Map<String, String> LEGACY_ALIASES = Map.of("durability_save_pickaxe", "unbreaking");

    static {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("protection", "protection");
        values.put("fire_protection", "fire_protection");
        values.put("blast_protection", "blast_protection");
        values.put("projectile_protection", "projectile_protection");
        values.put("thorns", "thorns");
        values.put("respiration", "respiration");
        values.put("aqua_affinity", "aqua_affinity");
        values.put("swift_sneak", "swift_sneak");
        values.put("depth_strider", "depth_strider");
        values.put("soul_speed", "soul_speed");
        values.put("frost_walker", "frost_walker");
        values.put("unbreaking", "unbreaking");
        VANILLA_TARGETS = Map.copyOf(values);
        IDS = VANILLA_TARGETS.keySet();
    }

    private RetiredVanillaEnchantments() { }

    public static int mergedLevel(int vanillaLevel, int retiredLevel, int snapshotLevel, int vanillaMaxLevel) {
        return Math.min(Math.max(0, vanillaMaxLevel),
                Math.max(Math.max(0, vanillaLevel), Math.max(Math.max(0, retiredLevel), Math.max(0, snapshotLevel))));
    }
}
