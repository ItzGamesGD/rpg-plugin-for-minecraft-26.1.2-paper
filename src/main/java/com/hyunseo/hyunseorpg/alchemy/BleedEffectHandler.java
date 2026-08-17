package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

import java.util.UUID;

public final class BleedEffectHandler extends AbstractProductionEffectHandler {
    public BleedEffectHandler(ConfigService config) { super(config, "bleed"); }
    @Override public void onTick(UUID targetId, ActiveEffectInstance instance, long currentTick) {
        if (tickDue(instance, currentTick, "tick-interval")) damage(living(targetId), instance, 1.0D);
    }
}
