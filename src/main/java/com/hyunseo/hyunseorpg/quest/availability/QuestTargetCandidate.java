package com.hyunseo.hyunseorpg.quest.availability;

/** Immutable candidate selected by the quest generator. */
public record QuestTargetCandidate(
        String id,
        String displayName,
        QuestTargetSource source,
        int difficulty,
        String detail
) { }
