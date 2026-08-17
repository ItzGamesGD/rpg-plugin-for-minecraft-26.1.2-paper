package com.hyunseo.hyunseorpg.classsystem;

public record ClassStatInvestmentResult(
        boolean success,
        String message,
        int level,
        int maxLevel,
        int remainingPoints
) {
    public static ClassStatInvestmentResult success(ClassStatData data, int level, int remainingPoints) {
        return new ClassStatInvestmentResult(
                true,
                data.displayName() + " " + level + "/" + data.maxLevel()
                        + " (남은 직업스탯포인트 " + remainingPoints + ")",
                level,
                data.maxLevel(),
                remainingPoints
        );
    }

    public static ClassStatInvestmentResult failure(String message, int level, int maxLevel, int remainingPoints) {
        return new ClassStatInvestmentResult(false, message, level, maxLevel, remainingPoints);
    }
}
