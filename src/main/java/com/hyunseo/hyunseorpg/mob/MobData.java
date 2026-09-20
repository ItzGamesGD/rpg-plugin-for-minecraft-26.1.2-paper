package com.hyunseo.hyunseorpg.mob;

import org.bukkit.entity.EntityType;

import java.util.List;

public record MobData(
        String mobId,
        String displayName,
        EntityType vanillaType,
        int level,
        List<String> tags,
        String abilityProfileId,
        MobAttributeData attributes,
        long baseExp,
        long classExp,
        String dropTableId,
        String region,
        boolean boss,
        boolean elite
) {
    public MobData {
        tags = List.copyOf(tags);
        abilityProfileId = abilityProfileId == null ? "" : abilityProfileId;
        attributes = attributes == null ? new MobAttributeData(0.0D, 0.0D, 0.0D, 0.0D, 0.0D) : attributes;
        dropTableId = dropTableId == null ? "" : dropTableId;
        region = region == null ? "" : region;
    }
}
