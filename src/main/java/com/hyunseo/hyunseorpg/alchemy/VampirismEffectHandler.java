package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public final class VampirismEffectHandler extends AbstractProductionEffectHandler {
    public VampirismEffectHandler(ConfigService config) { super(config, "vampirism"); }

    @Override
    public void onDamage(UUID sourceId, UUID targetId, double amount, DamageKind kind) {
        LivingEntity source = living(sourceId);
        if (source == null || amount <= 0.0D || source.isDead()) return;
        double healed = calculateHeal(amount, value("effect_vampire", "heal-ratio", 0.05D));
        AttributeInstance maxHealth = source.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && Double.isFinite(healed) && healed > 0.0D)
            source.setHealth(Math.min(maxHealth.getValue(), source.getHealth() + healed));
    }

    static double calculateHeal(double dealtDamage, double healRatio) {
        if (!Double.isFinite(dealtDamage) || !Double.isFinite(healRatio)
                || dealtDamage <= 0.0D || healRatio <= 0.0D) return 0.0D;
        return dealtDamage * healRatio;
    }
}
