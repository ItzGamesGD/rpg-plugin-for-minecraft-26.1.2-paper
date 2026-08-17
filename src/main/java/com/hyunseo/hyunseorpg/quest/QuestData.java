package com.hyunseo.hyunseorpg.quest;

import com.hyunseo.hyunseorpg.progression.Requirement;

import java.util.List;

public record QuestData(
        String questId,
        String displayName,
        List<Requirement> requirements,
        List<QuestObjective> objectives,
        List<QuestReward> rewards
) {
    public QuestData {
        questId = questId == null ? "" : questId.trim().toLowerCase();
        displayName = displayName == null || displayName.isBlank() ? questId : displayName;
        requirements = List.copyOf(requirements);
        objectives = List.copyOf(objectives);
        rewards = List.copyOf(rewards);
    }
}
