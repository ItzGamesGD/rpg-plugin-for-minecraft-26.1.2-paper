package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FrostbiteEffectHandler extends AbstractProductionEffectHandler {
    private final Set<UUID> ownedSlowness = new HashSet<>();

    public FrostbiteEffectHandler(ConfigService config) { super(config, "frostbite"); }

    @Override public void onApply(UUID targetId, ActiveEffectInstance instance) {
        LivingEntity target = living(targetId);
        addModifier(target, instance, Attribute.MOVEMENT_SPEED,
                value("effect_frostbite", "movement-multiplier", -0.15D));
        if (target == null || target.hasPotionEffect(PotionEffectType.SLOWNESS)) return;
        applySlowness(target, instance);
        ownedSlowness.add(instance.instanceId());
    }

    @Override public void onTick(UUID targetId, ActiveEffectInstance instance, long currentTick) {
        if (currentTick % 10L != 0L || !ownedSlowness.contains(instance.instanceId())) return;
        LivingEntity target = living(targetId);
        if (target == null || target.isDead()) return;
        PotionEffect current = target.getPotionEffect(PotionEffectType.SLOWNESS);
        int amplifier = slownessAmplifier(instance);
        if (current == null || (current.getAmplifier() <= amplifier && current.getDuration() < 20)) {
            applySlowness(target, instance);
        }
    }

    @Override public void onRemove(UUID targetId, ActiveEffectInstance instance) {
        LivingEntity target = living(targetId);
        if (ownedSlowness.remove(instance.instanceId()) && target != null) {
            PotionEffect current = target.getPotionEffect(PotionEffectType.SLOWNESS);
            if (current != null && current.getAmplifier() <= slownessAmplifier(instance)) {
                target.removePotionEffect(PotionEffectType.SLOWNESS);
            }
        }
        super.onRemove(targetId, instance);
    }

    private void applySlowness(LivingEntity target, ActiveEffectInstance instance) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40,
                slownessAmplifier(instance), false, false, true), true);
    }

    private int slownessAmplifier(ActiveEffectInstance instance) {
        return (int) Math.max(0L, Math.min(10L,
                longValue(instance.definition().id(), "slowness-amplifier", 0L)));
    }
}
