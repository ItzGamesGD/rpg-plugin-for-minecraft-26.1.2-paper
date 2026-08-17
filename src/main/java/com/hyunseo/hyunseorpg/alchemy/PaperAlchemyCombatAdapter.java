package com.hyunseo.hyunseorpg.alchemy;

import java.util.Locale;
import java.util.UUID;

/** Maps active custom effects to verified player skill/input gates. */
public final class PaperAlchemyCombatAdapter implements CombatEffectAdapter<UUID> {
    private final EffectService effects;

    public PaperAlchemyCombatAdapter(EffectService effects) {
        this.effects = effects;
    }

    @Override
    public boolean blocksActiveSkill(UUID target, String effectId) {
        return target != null && isActive(target, effectId)
                && (CoreEffectIds.SILENCE.equals(normalize(effectId))
                || CoreEffectIds.STUN.equals(normalize(effectId)));
    }

    @Override
    public boolean blocksEnchantmentSkill(UUID target, String effectId) {
        return target != null && isActive(target, effectId)
                && (CoreEffectIds.SILENCE.equals(normalize(effectId))
                || CoreEffectIds.STUN.equals(normalize(effectId)));
    }

    @Override
    public boolean blocksMovement(UUID target, String effectId) {
        return target != null && isActive(target, effectId)
                && (CoreEffectIds.ROOT.equals(normalize(effectId))
                || CoreEffectIds.STUN.equals(normalize(effectId)));
    }

    @Override
    public boolean blocksAttack(UUID target, String effectId) {
        return target != null && isActive(target, effectId)
                && CoreEffectIds.STUN.equals(normalize(effectId));
    }

    @Override
    public void modifyDamage(DamageContext context) {
        // No numeric damage contract is enabled yet. Do not invent a multiplier.
    }

    @Override
    public void modifyHealing(HealingContext context) {
        // No numeric healing contract is enabled yet. Do not invent a multiplier.
    }

    public boolean blocksAnyActiveSkill(UUID target) {
        return blocksActiveSkill(target, CoreEffectIds.SILENCE)
                || blocksActiveSkill(target, CoreEffectIds.STUN);
    }

    public boolean blocksAnyMovement(UUID target) {
        return blocksMovement(target, CoreEffectIds.ROOT)
                || blocksMovement(target, CoreEffectIds.STUN);
    }

    public boolean blocksAnyAttack(UUID target) {
        return blocksAttack(target, CoreEffectIds.STUN);
    }

    private boolean isActive(UUID target, String effectId) {
        return effects != null && effects.isActive(target, effectId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
