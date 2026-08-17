package com.hyunseo.hyunseorpg.exp;

public record LevelUpResult(
        int oldLevel,
        int newLevel,
        int levelsGained,
        int statPointsGained,
        long remainingExp,
        long expRequiredForNextLevel
) {
    public boolean leveledUp() {
        return levelsGained > 0;
    }
}
