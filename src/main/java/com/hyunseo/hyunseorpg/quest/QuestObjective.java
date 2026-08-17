package com.hyunseo.hyunseorpg.quest;

public record QuestObjective(
        String objectiveId,
        QuestObjectiveType type,
        String target,
        int amount
) {
    public QuestObjective {
        objectiveId = objectiveId == null ? "" : objectiveId.trim().toLowerCase();
        target = target == null ? "" : target.trim().toLowerCase();
        amount = Math.max(1, amount);
    }
}
