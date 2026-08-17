package com.hyunseo.hyunseorpg.equipment.trigger;

public enum EquipmentEffectResult {
    EXECUTED,
    /** Handler owns cooldown timing, for example a first/second input skill. */
    EXECUTED_DEFERRED_COOLDOWN,
    CONDITION_NOT_MET,
    COOLDOWN,
    INVALID_SOURCE,
    CANCELLED,
    NO_TARGET,
    FAILED
}
