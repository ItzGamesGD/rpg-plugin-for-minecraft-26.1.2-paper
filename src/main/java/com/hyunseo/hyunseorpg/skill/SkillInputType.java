package com.hyunseo.hyunseorpg.skill;

public enum SkillInputType {
    OFFHAND_QUICK,
    LEFT_CLICK,
    SHIFT_LEFT_CLICK,
    RIGHT_CLICK,
    SHIFT_RIGHT_CLICK,
    DROP_KEY,
    SHIFT_JUMP,
    FIREWORK_USE;

    public String configKey() {
        return name().toLowerCase(java.util.Locale.ROOT).replace("_", "-");
    }
}
