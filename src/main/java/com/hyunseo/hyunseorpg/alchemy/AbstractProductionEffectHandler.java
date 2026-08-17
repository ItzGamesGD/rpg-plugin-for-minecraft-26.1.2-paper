package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shared lifecycle and temporary baseline plumbing for the eight production effects. */
public abstract class AbstractProductionEffectHandler implements CombatEffectHandler, CombatEffectModifier {
    protected final ConfigService config;
    private final String handlerId;
    private final Map<UUID, List<AttributeModifier>> modifiers = new HashMap<>();

    protected AbstractProductionEffectHandler(ConfigService config, String handlerId) {
        this.config = config;
        this.handlerId = handlerId;
    }

    @Override
    public final String effectId() { return handlerId; }

    @Override
    public void onApply(UUID targetId, ActiveEffectInstance instance) { }

    @Override
    public void onTick(UUID targetId, ActiveEffectInstance instance, long currentTick) { }

    @Override
    public void onDamage(UUID sourceId, UUID targetId, double amount, DamageKind kind) { }

    @Override
    public void onRemove(UUID targetId, ActiveEffectInstance instance) {
        removeModifiers(targetId, instance.instanceId());
    }

    protected LivingEntity living(UUID entityId) {
        return entityId == null || !(Bukkit.getEntity(entityId) instanceof LivingEntity entity) ? null : entity;
    }

    protected boolean tickDue(ActiveEffectInstance instance, long currentTick, String key) {
        long interval = Math.max(1L, longValue(instance.definition().id(), key, 20L));
        return currentTick % interval == 0L;
    }

    protected double value(String effectId, String key, double fallback) {
        var section = config.getAlchemyEffectsSection("effects." + effectId + ".baseline");
        return section == null ? fallback : section.getDouble(key, fallback);
    }

    protected long longValue(String effectId, String key, long fallback) {
        var section = config.getAlchemyEffectsSection("effects." + effectId + ".baseline");
        return section == null ? fallback : section.getLong(key, fallback);
    }

    protected void addModifier(LivingEntity target, ActiveEffectInstance instance,
                               Attribute attribute, double amount) {
        if (target == null || attribute == null || !Double.isFinite(amount)) return;
        AttributeInstance attributeInstance = target.getAttribute(attribute);
        if (attributeInstance == null) return;
        UUID id = UUID.nameUUIDFromBytes(("alchemy:production:" + instance.instanceId()
                + ":" + attribute.name()).getBytes(StandardCharsets.UTF_8));
        attributeInstance.getModifiers().stream()
                .filter(existing -> existing.getUniqueId().equals(id))
                .toList().forEach(attributeInstance::removeModifier);
        AttributeModifier modifier = new AttributeModifier(id,
                "hyunseorpg_alchemy_" + handlerId, amount,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        attributeInstance.addModifier(modifier);
        modifiers.computeIfAbsent(instance.instanceId(), ignored -> new ArrayList<>()).add(modifier);
    }

    private void removeModifiers(UUID targetId, UUID instanceId) {
        LivingEntity target = living(targetId);
        List<AttributeModifier> owned = modifiers.remove(instanceId);
        if (target == null || owned == null) return;
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = target.getAttribute(attribute);
            if (attributeInstance == null) continue;
            owned.stream().filter(attributeInstance.getModifiers()::contains)
                    .forEach(attributeInstance::removeModifier);
        }
        AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && target.getHealth() > maxHealth.getValue()) {
            target.setHealth(maxHealth.getValue());
        }
    }

    protected void damage(LivingEntity target, ActiveEffectInstance instance, double fallback) {
        if (target == null || target.isDead()) return;
        double amount = Math.max(0.0D, value(instance.definition().id(), "damage", fallback)
                * Math.max(1, instance.stacks()));
        if (amount > 0.0D && Double.isFinite(amount)) target.damage(amount);
    }
}
