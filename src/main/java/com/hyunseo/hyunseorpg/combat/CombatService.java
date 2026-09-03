package com.hyunseo.hyunseorpg.combat;

import com.hyunseo.hyunseorpg.stat.StatService;
import com.hyunseo.hyunseorpg.stat.StatType;
import com.hyunseo.hyunseorpg.enhancement.EquipmentEnhancementService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentPromotionService;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CombatService {
    private final ThreadLocal<Boolean> internalDamage = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<DamageContext> activeDamageContext = new ThreadLocal<>();
    private StatService statService;
    private EquipmentEnhancementService equipmentEnhancementService;
    private EquipmentPromotionService equipmentPromotionService;
    private EquipmentTierService equipmentTierService;
    private ConfigService configService;

    public void setStatService(StatService statService) {
        this.statService = statService;
    }

    public void setEquipmentEnhancementService(EquipmentEnhancementService equipmentEnhancementService) {
        this.equipmentEnhancementService = equipmentEnhancementService;
    }
    public void setEquipmentPromotionService(EquipmentPromotionService equipmentPromotionService) { this.equipmentPromotionService = equipmentPromotionService; }
    public void setEquipmentTierService(EquipmentTierService equipmentTierService) { this.equipmentTierService = equipmentTierService; }
    public void setConfigService(ConfigService configService) { this.configService = configService; }
    public boolean isInternalDamage() { return internalDamage.get(); }
    public DamageContext getActiveDamageContext() { return activeDamageContext.get(); }

    public void applyDirectDamage(Player attacker, LivingEntity target, double damage) {
        applyContextDamage(new DamageContext(attacker, held(attacker), "", DamageType.DIRECT,
                damage, false, true, false), target, true, false);
    }

    /** Additional strike paired with a confirmed melee hit; bypasses the same hit's invulnerability frame. */
    public void applyAdditionalMeleeDamage(Player attacker, ItemStack sourceItem, LivingEntity target, double damage) {
        applyContextDamage(new DamageContext(attacker, sourceItem, "", DamageType.DIRECT,
                damage, true, true, false), target, true, false);
    }

    public void applyMultiHitDamage(Player attacker, LivingEntity target, double damage) {
        applyMultiHitDamage(attacker, held(attacker), target, damage);
    }

    /** Applies delayed multi-hit damage using the item that created the attack, not the later held item. */
    public void applyMultiHitDamage(Player attacker, ItemStack sourceItem, LivingEntity target, double damage) {
        applyContextDamage(new DamageContext(attacker, sourceItem, "", DamageType.CUSTOM_SKILL,
                damage, true, true, true), target, false, true);
    }

    public void applySkillDamage(Player attacker, LivingEntity target, double damage) {
        // Legacy skills keep their existing promotion multiplier semantics.
        applyContextDamage(new DamageContext(attacker, held(attacker), "", DamageType.CUSTOM_SKILL,
                damage, true, true, true), target, false, true);
    }

    public void applyPiercingSkillDamage(Player attacker, LivingEntity target, double damage) {
        applyContextDamage(new DamageContext(attacker, held(attacker), "", DamageType.CUSTOM_SKILL,
                damage, true, true, true), target, false, true);
    }

    public void applyUltimateDamage(Player attacker, LivingEntity target, double damage) {
        if (damage <= 0.0D || target.isDead()) {
            return;
        }

        DamageContext context = new DamageContext(attacker, held(attacker), "", DamageType.CUSTOM_SKILL,
                damage, true, true, true);
        double finalDamage = calculateFinalDamage(context, target, false, true);
        double healthBefore = target.getHealth();
        target.setNoDamageTicks(0);
        applyDamage(target, finalDamage, attacker, context);
        target.setNoDamageTicks(0);
        if (!target.isDead() && target.getHealth() >= healthBefore) {
            target.setHealth(Math.max(0.0D, healthBefore - finalDamage));
        }
    }

    /**
     * New custom enchantments use this path so protection, recursive trigger
     * handling and invulnerability-frame rules see the same source metadata.
     */
    public void applyEnchantDamage(Player attacker, ItemStack sourceItem, String enchantId,
                                   DamageType damageType, LivingEntity target, double damage,
                                   boolean ignoreInvulnerabilityFrames) {
        applyContextDamage(new DamageContext(attacker, sourceItem, enchantId, damageType, damage,
                ignoreInvulnerabilityFrames, true, true), target, false, false);
    }

    private void applyContextDamage(DamageContext context, LivingEntity target,
                                    boolean includeAttackerAttackStat, boolean legacySkillMultiplier) {
        if (context.sourcePlayer() == null || context.baseDamage() <= 0.0D || target == null || target.isDead()) return;
        int originalNoDamageTicks = target.getNoDamageTicks();
        if (context.ignoresInvulnerabilityFrames()) target.setNoDamageTicks(0);
        double finalDamage = calculateFinalDamage(context, target, includeAttackerAttackStat, legacySkillMultiplier);
        applyDamage(target, finalDamage, context.sourcePlayer(), context);
        if (context.ignoresInvulnerabilityFrames()) target.setNoDamageTicks(Math.min(originalNoDamageTicks, 2));
    }

    private double calculateFinalDamage(DamageContext context, LivingEntity target,
                                        boolean includeAttackerAttackStat, boolean legacySkillMultiplier) {
        Player attacker = context.sourcePlayer();
        double finalDamage = context.baseDamage();
        if (statService != null) {
            if (includeAttackerAttackStat) {
                finalDamage += statService.getEffectiveStat(attacker, StatType.ATTACK);
            }
            if (target instanceof Player targetPlayer) {
                double reduction = Math.min(0.9D, statService.getEffectiveStat(targetPlayer, StatType.DAMAGE_REDUCTION));
                finalDamage *= Math.max(0.0D, 1.0D - reduction);
            }
        }
        if (target instanceof Player targetPlayer && equipmentEnhancementService != null) {
            double equipmentReduction = 0.0D;
            for (org.bukkit.inventory.ItemStack armor : targetPlayer.getInventory().getArmorContents()) {
                equipmentReduction += equipmentEnhancementService.getDamageReductionBonus(armor);
                if (equipmentPromotionService != null) equipmentReduction += equipmentPromotionService.getDamageReduction(armor);
            }
            org.bukkit.inventory.ItemStack offHand = targetPlayer.getInventory().getItemInOffHand();
            equipmentReduction += equipmentEnhancementService.getDamageReductionBonus(offHand);
            if (equipmentPromotionService != null) equipmentReduction += equipmentPromotionService.getDamageReduction(offHand);
            double cap = configService == null ? 0.8D
                    : Math.max(0.0D, Math.min(1.0D,
                    configService.getDouble("equipment-effects.damage-reduction-cap", 0.8D)));
            finalDamage *= Math.max(0.0D, 1.0D - Math.min(cap, equipmentReduction));
        }
        ItemStack mainHand = context.sourceItem() == null ? held(attacker) : context.sourceItem();
        boolean tool = equipmentTierService != null
                && equipmentTierService.getCategory(mainHand) == EquipmentTierService.Category.TOOL;
        if (!tool && equipmentEnhancementService != null) {
            finalDamage += equipmentEnhancementService.getAttackBonus(mainHand);
        }
        if (!tool && equipmentPromotionService != null) {
            finalDamage += equipmentPromotionService.getWeaponDamageBonus(mainHand);
            if (context.damageType() == DamageType.PROJECTILE) {
                finalDamage += equipmentPromotionService.getOptionValue(mainHand, "projectile-damage");
            }
            if (legacySkillMultiplier || context.isSkillDamage()) {
                finalDamage *= equipmentPromotionService.getSkillDamageMultiplier(mainHand);
            }
        }
        return Math.max(0.0D, finalDamage);
    }

    private ItemStack held(Player attacker) {
        return attacker == null ? null : attacker.getInventory().getItemInMainHand();
    }

    private void applyDamage(LivingEntity target, double damage, Player attacker, DamageContext context) {
        boolean previous = internalDamage.get();
        DamageContext previousContext = activeDamageContext.get();
        internalDamage.set(true);
        activeDamageContext.set(context);
        try {
            target.damage(damage, attacker);
        } finally {
            internalDamage.set(previous);
            if (previousContext == null) activeDamageContext.remove();
            else activeDamageContext.set(previousContext);
        }
    }
}
