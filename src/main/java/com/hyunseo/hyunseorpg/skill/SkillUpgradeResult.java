package com.hyunseo.hyunseorpg.skill;

public record SkillUpgradeResult(
        boolean success,
        String message,
        int level,
        int maxLevel,
        int remainingPoints
) {
    public static SkillUpgradeResult success(SkillData skillData, int level, int remainingPoints) {
        return new SkillUpgradeResult(
                true,
                skillData.displayName() + " " + level + "/" + skillData.maxLevel() + " (남은 스킬포인트 " + remainingPoints + ")",
                level,
                skillData.maxLevel(),
                remainingPoints
        );
    }

    public static SkillUpgradeResult failure(String message, int level, int maxLevel, int remainingPoints) {
        return new SkillUpgradeResult(false, message, level, maxLevel, remainingPoints);
    }
}
