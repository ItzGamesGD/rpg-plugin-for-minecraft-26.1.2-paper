package com.hyunseo.hyunseorpg.quest;

/** Persisted state for a player-specific generated quest. */
public enum AutoQuestStatus {
    ACTIVE,
    READY_TO_COMPLETE,
    COMPLETED,
    FAILED,
    ABANDONED,
    EXPIRED,
    CONFIG_INVALIDATED
}
