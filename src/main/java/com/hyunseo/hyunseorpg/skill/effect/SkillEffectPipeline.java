package com.hyunseo.hyunseorpg.skill.effect;

import com.hyunseo.hyunseorpg.skill.SkillCastContext;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SkillEffectPipeline {
    private final Map<SkillEffectPhase, List<SkillEffect>> effectsByPhase;

    public SkillEffectPipeline() {
        this.effectsByPhase = new EnumMap<>(SkillEffectPhase.class);
        for (SkillEffectPhase phase : SkillEffectPhase.values()) {
            effectsByPhase.put(phase, new ArrayList<>());
        }
    }

    public static SkillEffectPipeline empty() {
        return new SkillEffectPipeline();
    }

    public void addEffect(SkillEffectPhase phase, SkillEffect effect) {
        effectsByPhase.get(phase).add(effect);
    }

    public void executePhase(SkillEffectPhase phase, SkillCastContext context) {
        for (SkillEffect effect : effectsByPhase.get(phase)) {
            effect.execute(context);
        }
    }

    public boolean isEmpty() {
        return effectsByPhase.values().stream().allMatch(List::isEmpty);
    }
}
