package com.hyunseo.hyunseorpg.alchemy;

import java.util.UUID;

public record EffectSource(UUID applicatorId, EffectSourceType type, String sourceId) {
    public EffectSource {
        type = type == null ? EffectSourceType.OTHER : type;
        sourceId = sourceId == null ? "" : sourceId.trim().toLowerCase();
    }
}
