package com.hyunseo.hyunseorpg.alchemy;

import java.util.UUID;

public record EffectContext(UUID applicatorId, EffectSourceType sourceType, String sourceId,
                            UUID targetId, String chainId) {
    public EffectSource source() { return new EffectSource(applicatorId, sourceType, sourceId); }
}
