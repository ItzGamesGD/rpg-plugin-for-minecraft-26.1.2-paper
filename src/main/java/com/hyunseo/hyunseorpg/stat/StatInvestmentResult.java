package com.hyunseo.hyunseorpg.stat;

public record StatInvestmentResult(
        boolean success,
        String message,
        int level,
        int maxLevel,
        int remainingPoints
) {
    public static StatInvestmentResult success(StatType statType, int level, int maxLevel, int remainingPoints) {
        return new StatInvestmentResult(
                true,
                statType.name() + " " + level + "/" + maxLevel + " (남은 스탯포인트 " + remainingPoints + ")",
                level,
                maxLevel,
                remainingPoints
        );
    }

    public static StatInvestmentResult failure(String message, int level, int maxLevel, int remainingPoints) {
        return new StatInvestmentResult(false, message, level, maxLevel, remainingPoints);
    }
}
