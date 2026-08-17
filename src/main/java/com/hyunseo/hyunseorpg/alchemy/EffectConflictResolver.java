package com.hyunseo.hyunseorpg.alchemy;

import java.util.Map;

public final class EffectConflictResolver {
    public boolean canApply(ActiveEffectInstance current, CustomEffectDefinition next) {
        if (current == null) return true;
        return current.definition().priority() <= next.priority();
    }
    public Map<String, Integer> priorities() { return Map.of(); }
}
