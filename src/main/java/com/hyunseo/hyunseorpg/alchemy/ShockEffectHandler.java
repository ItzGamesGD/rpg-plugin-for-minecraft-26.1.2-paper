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

    public ShockEffectHandler(ConfigService config) {
        this(config, null);
    }

    public ShockEffectHandler(ConfigService config, EffectMovementLockService movementLocks) {
        super(config, "shock");
        this.movementLocks = movementLocks;
    }

    @Override
    public void onApply(UUID targetId, ActiveEffectInstance instance) {
        long interval = ShockTimingPolicy.interval(
                longValue(instance.definition().id(), "tick-interval", 320L));
        nextPulseAt.put(instance.instanceId(), ShockTimingPolicy.firstPulseAt(
                Bukkit.getCurrentTick(), interval));
        debug("apply target=" + targetId
                + " firstPulseAt=" + nextPulseAt.get(instance.instanceId())
                + " interval=" + interval, instance);
    }

    @Override
    public void onRemove(UUID targetId, ActiveEffectInstance instance) {
        super.onRemove(targetId, instance);
        nextPulseAt.remove(instance.instanceId());
        if (movementLocks != null) movementLocks.clear(targetId);
        debug("remove target=" + targetId, instance);
    }

    @Override
    public void onTick(UUID targetId, ActiveEffectInstance instance, long currentTick) {
        long interval = ShockTimingPolicy.interval(
                longValue(instance.definition().id(), "tick-interval", 320L));
        long next = nextPulseAt.getOrDefault(instance.instanceId(),
                ShockTimingPolicy.firstPulseAt(currentTick, interval));
        if (currentTick < next) return;

        LivingEntity target = living(targetId);
        if (target == null || target.isDead() || !target.isValid()) {
            debug("pulse target=" + targetId
                    + " type=unresolved tick=" + currentTick
                    + " action=skipped", instance);
            nextPulseAt.put(instance.instanceId(),
                    ShockTimingPolicy.nextPulseAt(currentTick, interval));
            if (movementLocks != null) movementLocks.clear(targetId);
            return;
        }

        debug("pulse target=" + targetId
                + " type=" + target.getType()
                + " tick=" + currentTick
                + " action=damage", instance);
        damage(target, instance, 1.0D);

        if (target.isDead() || !target.isValid()) {
            if (movementLocks != null) movementLocks.clear(targetId);
            debug("pulse target=" + targetId
                    + " tick=" + currentTick
                    + " action=lock-skipped reason=dead-or-invalid-after-damage", instance);
        } else if (movementLocks != null) {
            long configuredRoot = longValue(instance.definition().id(),
                    "root-duration-ticks",
                    longValue(instance.definition().id(), "stun-duration-ticks", 20L));
            long effectiveRoot = ShockTimingPolicy.effectiveRootDuration(configuredRoot, interval);
            movementLocks.lock(target, effectiveRoot);
            debug("pulse target=" + targetId
                    + " tick=" + currentTick
                    + " action=lock duration=" + effectiveRoot
                    + " interval=" + interval, instance);
        }

        nextPulseAt.put(instance.instanceId(),
                ShockTimingPolicy.nextPulseAt(currentTick, interval));
    }

    private void debug(String message, ActiveEffectInstance instance) {
        if (movementLocks != null) {
            movementLocks.debug(message + " instance=" + instance.instanceId());
        }
    }
}
