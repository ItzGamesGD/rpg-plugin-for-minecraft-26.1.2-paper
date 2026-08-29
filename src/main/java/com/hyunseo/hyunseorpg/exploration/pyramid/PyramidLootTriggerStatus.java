package com.hyunseo.hyunseorpg.exploration.pyramid;

/** Durable diagnostic states for the Pyramid loot-to-room transition. */
public enum PyramidLootTriggerStatus {
    PREPARED,
    REVEAL_SCHEDULED,
    REVEALING,
    REVEALED,
    RETRYABLE,
    RECOVERY_REQUIRED
}
