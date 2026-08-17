package com.hyunseo.hyunseorpg.weapon;

import com.hyunseo.hyunseorpg.classsystem.RPGClass;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Combat categories are independent from lifestyle professions. A player can
 * progress every category and combine their unlocked skills freely.
 */
public enum WeaponType {
    SWORD("sword", "검"),
    BOW("bow", "활"),
    SPEAR("spear", "창"),
    AXE("axe", "도끼"),
    MAGIC("magic", "마법");

    private final String id;
    private final String displayName;

    WeaponType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean matchesMaterial(Material material) {
        if (material == null || material.isAir()) {
            return false;
        }
        return switch (this) {
            case SWORD -> material.name().endsWith("_SWORD");
            case BOW -> material == Material.BOW || material == Material.CROSSBOW;
            case SPEAR -> material == Material.TRIDENT;
            case AXE -> material.name().endsWith("_AXE");
            case MAGIC -> material == Material.BLAZE_ROD || material == Material.STICK;
        };
    }

    public static Optional<WeaponType> fromInput(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace("-", "_");
        return Arrays.stream(values())
                .filter(type -> type.id.equals(normalized) || type.name().equalsIgnoreCase(normalized))
                .findFirst();
    }

    public static WeaponType fromLegacyClass(RPGClass legacyClass) {
        return switch (legacyClass) {
            case SWORDMASTER, ASSASSIN -> SWORD;
            case BOWMASTER -> BOW;
            case LANCER -> SPEAR;
            case WIZARD -> MAGIC;
        };
    }
}
