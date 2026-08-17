package com.hyunseo.hyunseorpg.item;

import org.bukkit.Material;

/** Strict allow-list for simple, non-durable vanilla items. */
public final class VanillaStackingPolicy {
    private VanillaStackingPolicy() {
    }

    public enum Group {
        POTION,
        VEHICLES,
        UTILITY,
        NONE
    }

    public static Group group(Material material) {
        if (material == null) return Group.NONE;
        return groupName(material.name());
    }

    /** Pure name-based form used by diagnostics and tests without a running Bukkit server. */
    public static Group groupName(String materialName) {
        if (materialName == null) return Group.NONE;
        String name = materialName.trim().toUpperCase(java.util.Locale.ROOT);
        if (name.equals("POTION") || name.equals("SPLASH_POTION") || name.equals("LINGERING_POTION")) {
            return Group.POTION;
        }
        if (name.endsWith("_BOAT") || name.endsWith("_RAFT") || name.equals("MINECART") || name.equals("CHEST_MINECART")
                || name.equals("FURNACE_MINECART") || name.equals("HOPPER_MINECART")
                || name.equals("TNT_MINECART")) {
            return Group.VEHICLES;
        }
        if (name.equals("SADDLE")) return Group.UTILITY;
        return Group.NONE;
    }

    public static boolean isAllowed(Material material) {
        return group(material) != Group.NONE;
    }
}
