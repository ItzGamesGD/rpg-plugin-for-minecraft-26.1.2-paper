package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShockEffectHandler extends AbstractProductionEffectHandler {
    private final EffectMovementLockService movementLocks;
    private final Map<UUID, Long> nextPulseAt = new HashMap<>();

    public ShockEffectHandler(ConfigService config) { this(config, null); }

    public ShockEffectHandler(ConfigService config, EffectMovementLockService movementLocks) {
        super(config, "shock");
        this.movementLocks = movementLocks;
    }

    @Override public void onApply(UUID targetId, ActiveEffectInstance instance) {
        nextPulseAt.put(instance.instanceId(), Bukkit.getCurrentTick()
                + Math.max(1L, longValue(instance.definition().id(), "tick-interval", 60L)));
    }

    @Override public void onRemove(UUID targetId, ActiveEffectInstance instance) {
        super.onRemove(targetId, instance);
        nextPulseAt.remove(instance.instanceId());
        if (movementLocks != null) movementLocks.clear(targetId);
    }

    @Override public void onTick(UUID targetId, ActiveEffectInstance instance, long currentTick) {
        long interval = Math.max(1L, longValue(instance.definition().id(), "tick-interval", 60L));
        long next = nextPulseAt.getOrDefault(instance.instanceId(), currentTick + interval);
        if (currentTick < next) return;
        LivingEntity target = living(targetId);
        damage(target, instance, 1.0D);
        if (movementLocks != null) {
            movementLocks.lock(target, Math.min(20L, Math.max(1L,
                    longValue(instance.definition().id(), "stun-duration-ticks", 20L))));
        }
        nextPulseAt.put(instance.instanceId(), currentTick + interval);
    }
}
