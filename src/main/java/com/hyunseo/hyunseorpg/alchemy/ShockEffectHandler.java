package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public final class ShockEffectHandler extends AbstractProductionEffectHandler {
    private final EffectMovementLockService movementLocks;

    public ShockEffectHandler(ConfigService config) { this(config, null); }

    public ShockEffectHandler(ConfigService config, EffectMovementLockService movementLocks) {
        super(config, "shock");
        this.movementLocks = movementLocks;
    }

    @Override public void onRemove(UUID targetId, ActiveEffectInstance instance) {
        super.onRemove(targetId, instance);
        if (movementLocks != null) movementLocks.clear(targetId);
    }

    @Override public void onTick(UUID targetId, ActiveEffectInstance instance, long currentTick) {
        if (!tickDue(instance, currentTick, "tick-interval")) return;
        LivingEntity target = living(targetId);
        damage(target, instance, 1.0D);
        if (movementLocks != null) {
            movementLocks.lock(target, Math.max(1L,
                    longValue(instance.definition().id(), "stun-duration-ticks", 40L)));
        }
    }
}
