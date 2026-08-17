package com.hyunseo.hyunseorpg.mob;

import java.util.List;

public record MobAbilityProfile(
        String profileId,
        List<MobAbilityData> abilities
) {
    public MobAbilityProfile {
        abilities = List.copyOf(abilities);
    }
}
