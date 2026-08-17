package com.hyunseo.hyunseorpg.classsystem;

import org.bukkit.Material;

public record ClassStatData(
        String id,
        RPGClass ownerClass,
        String displayName,
        Material iconMaterial,
        int maxLevel,
        int pointCost,
        String skillId,
        String targetValue,
        double bonusPerLevel
) {
    public String fullId() {
        return ownerClass.id() + "." + id;
    }
}
