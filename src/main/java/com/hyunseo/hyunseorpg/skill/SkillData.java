package com.hyunseo.hyunseorpg.skill;

import com.hyunseo.hyunseorpg.weapon.WeaponType;
import org.bukkit.Material;

public record SkillData(
        String skillId,
        WeaponType weaponType,
        SkillInputType inputType,
        String displayName,
        Material iconMaterial,
        int maxLevel,
        double manaCost,
        double cooldownSeconds,
        int requiredProficiencyLevel
) {
}
