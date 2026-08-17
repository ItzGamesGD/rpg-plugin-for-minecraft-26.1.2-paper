package com.hyunseo.hyunseorpg.skill;

public record SkillStatInvestmentResult(
        boolean success,
        String message,
        int level,
        int maxLevel,
        int remainingPoints
) {
    public static SkillStatInvestmentResult success(SkillStatType skillStatType, int level, int maxLevel, int remainingPoints) {
        return new SkillStatInvestmentResult(
                true,
                skillStatType.displayName() + " " + level + "/" + maxLevel + " (남은 스킬포인트 " + remainingPoints + ")",
                level,
                maxLevel,
                remainingPoints
        );
    }

    public static SkillStatInvestmentResult failure(String message, int level, int maxLevel, int remainingPoints) {
        return new SkillStatInvestmentResult(false, message, level, maxLevel, remainingPoints);
    }
}
