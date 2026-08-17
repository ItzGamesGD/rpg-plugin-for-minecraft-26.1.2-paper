package com.hyunseo.hyunseorpg.skill;

import org.bukkit.Material;

public enum SkillStatType {
    SKILL_DAMAGE("스킬 데미지", Material.IRON_SWORD),
    SKILL_RADIUS("스킬 범위", Material.ENDER_PEARL),
    SKILL_DURATION("스킬 지속시간", Material.CLOCK),
    SKILL_PROJECTILE_COUNT("투사체/소환 개수", Material.ARROW),
    SKILL_COOLDOWN_REDUCTION("쿨타임 감소", Material.REDSTONE),
    SKILL_MANA_EFFICIENCY("마나 효율", Material.LAPIS_LAZULI);

    private final String displayName;
    private final Material iconMaterial;

    SkillStatType(String displayName, Material iconMaterial) {
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    public Material iconMaterial() {
        return iconMaterial;
    }
}
