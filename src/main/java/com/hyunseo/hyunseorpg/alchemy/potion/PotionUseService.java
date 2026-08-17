package com.hyunseo.hyunseorpg.alchemy.potion;

import java.util.UUID;
public interface PotionUseService<I> {
    UseResult use(UUID playerId, I item);
    enum UseResult { USED, NOT_A_POTION, INVALID_PDC, INVALID_ITEM, UNKNOWN_POTION, DISABLED_POTION, UNSUPPORTED_DATA_VERSION, EFFECT_DISABLED, EFFECT_REJECTED }
}
