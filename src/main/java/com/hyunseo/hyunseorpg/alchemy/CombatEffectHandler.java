package com.hyunseo.hyunseorpg.alchemy;

import java.util.UUID;

/** Handler contract reserved for configured combat effects. */
public interface CombatEffectHandler {
    String effectId();
    void onApply(UUID targetId, ActiveEffectInstance instance);
    void onTick(UUID targetId, ActiveEffectInstance instance, long currentTick);
    void onDamage(UUID sourceId, UUID targetId, double amount, DamageKind kind);
    void onRemove(UUID targetId, ActiveEffectInstance instance);

    enum DamageKind { DIRECT, DOT, AREA, REFLECTED, CHAINED }
}
