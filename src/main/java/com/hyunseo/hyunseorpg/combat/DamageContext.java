package com.hyunseo.hyunseorpg.combat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Carries the origin of one custom damage application through Bukkit's
 * synchronous damage event. It is intentionally transient and never stored on
 * an entity, so it cannot leak into later unrelated damage events.
 */
public record DamageContext(
        Player sourcePlayer,
        ItemStack sourceItem,
        String enchantId,
        DamageType damageType,
        double baseDamage,
        boolean ignoresInvulnerabilityFrames,
        boolean canTriggerOnHitEffects,
        boolean isSkillDamage
) {
    public DamageContext {
        sourceItem = sourceItem == null ? null : sourceItem.clone();
        enchantId = enchantId == null ? "" : enchantId.trim().toLowerCase(java.util.Locale.ROOT);
        damageType = damageType == null ? DamageType.CUSTOM_SKILL : damageType;
        baseDamage = Math.max(0.0D, baseDamage);
    }
}
