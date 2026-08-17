package com.hyunseo.hyunseorpg.exp;

public record ClassLevelUpResult(
        int oldLevel,
        int newLevel,
        int levelsGained,
        int skillPointsGained,
        int classStatPointsGained,
        long remainingExp,
        long expRequiredForNextLevel
) {
    public boolean leveledUp() {
        return levelsGained > 0;
    }
}
