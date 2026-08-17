package com.hyunseo.hyunseorpg.quest;

public record QuestReward(
        QuestRewardType type,
        String target,
        long amount
) {
    public QuestReward {
        target = target == null ? "" : target.trim().toLowerCase();
        amount = Math.max(0L, amount);
    }
}
