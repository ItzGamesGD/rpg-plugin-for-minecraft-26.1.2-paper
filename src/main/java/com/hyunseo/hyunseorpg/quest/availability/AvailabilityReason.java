package com.hyunseo.hyunseorpg.quest.availability;

/** Stable explanation codes for content filtering and administrator diagnostics. */
public enum AvailabilityReason {
    ELIGIBLE,
    DISABLED,
    LEVEL_LOCKED,
    NOT_NATURALLY_SPAWNABLE,
    NOT_DISCOVERED,
    BOSS,
    MINI_BOSS,
    RARE,
    EVENT_ONLY,
    SUMMON_ONLY,
    ADMIN_ONLY,
    NPC,
    TAMED_ONLY,
    QUEST_EXCLUDED,
    WORLD_UNAVAILABLE,
    NO_ACQUISITION_SOURCE,
    SPECIAL_LOOT,
    BOSS_MATERIAL,
    SPECIAL_EQUIPMENT,
    UPGRADE_MATERIAL,
    LEGACY,
    QUEST_REWARD_ONLY,
    MISSING_DEFINITION,
    INVALID_CONFIGURATION
}
