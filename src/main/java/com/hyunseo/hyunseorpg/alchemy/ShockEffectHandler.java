package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

import java.util.UUID;

public final class ShockEffectHandler extends AbstractProductionEffectHandler {
    public ShockEffectHandler(ConfigService config) { super(config, "shock"); }
    @Override public void onTick(UUID targetId, ActiveEffectInstance instance, long currentTick) {
        if (tickDue(instance, currentTick, "tick-interval")) damage(living(targetId), instance, 1.0D);
    }
}
