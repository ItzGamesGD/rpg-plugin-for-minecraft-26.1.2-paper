package com.hyunseo.hyunseorpg.mob;

import java.util.Map;

public record MobAbilityData(
        String abilityId,
        MobAbilityType type,
        String trigger,
        long cooldownTicks,
        Map<String, String> parameters
) {
    public MobAbilityData {
        parameters = Map.copyOf(parameters);
    }
}
