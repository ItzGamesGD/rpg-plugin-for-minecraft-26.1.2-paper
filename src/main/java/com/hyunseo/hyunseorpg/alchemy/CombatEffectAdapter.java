package com.hyunseo.hyunseorpg.alchemy;

import java.util.UUID;

/**
 * Narrow bridge contract for the real combat and skill boundary.
 * Damage and healing modifiers intentionally remain identity operations until
 * their configured effect semantics and balance values are approved.
 */
public interface CombatEffectAdapter<T> {
    boolean blocksActiveSkill(T target, String effectId);
    boolean blocksEnchantmentSkill(T target, String effectId);
    boolean blocksMovement(T target, String effectId);
    boolean blocksAttack(T target, String effectId);
    void modifyDamage(DamageContext context);
    void modifyHealing(HealingContext context);

    record DamageContext(UUID sourceId, UUID targetId, double amount, String cause) { }
    record HealingContext(UUID sourceId, UUID targetId, double amount, String cause) { }
}
