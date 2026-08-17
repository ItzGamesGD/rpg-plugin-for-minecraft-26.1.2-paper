package com.hyunseo.hyunseorpg.equipment.trigger;

import com.hyunseo.hyunseorpg.skill.SkillInputType;

public record EnchantTriggerBinding(
        TriggerType type,
        SkillInputType input,
        TriggerPhase phase
) {
    public boolean matches(TriggerType requestedType, TriggerPhase requestedPhase, SkillInputType requestedInput) {
        return type == requestedType && phase == requestedPhase
                && (type != TriggerType.INPUT || input == requestedInput);
    }
}
