package com.hyunseo.hyunseorpg.skill;

import com.hyunseo.hyunseorpg.enchant.EnchantData;
import com.hyunseo.hyunseorpg.enchant.EnchantService;
import com.hyunseo.hyunseorpg.equipment.trigger.EquipmentEffectTriggerEngine;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.mana.ManaService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import com.hyunseo.hyunseorpg.skill.bowmaster.BowmasterSkillService;
import com.hyunseo.hyunseorpg.skill.effect.SkillEffectPhase;
import com.hyunseo.hyunseorpg.skill.effect.SkillEffectPipeline;
import com.hyunseo.hyunseorpg.skill.lancer.LancerSkillService;
import com.hyunseo.hyunseorpg.skill.swordmaster.SwordmasterBladeService;
import com.hyunseo.hyunseorpg.weapon.WeaponProficiencyService;
import com.hyunseo.hyunseorpg.weapon.WeaponService;
import com.hyunseo.hyunseorpg.weapon.WeaponType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.Map;
import java.util.Optional;

/** Routes skill input by held weapon type, never by selected lifestyle profession. */
public final class SkillService {
    private static final double MAGIC_BASIC_FIRE_RANGE = 16.0D;
    private static final double MAGIC_BASIC_FIRE_RAY_SIZE = 0.6D;

    private final ConfigService configService;
    private final PlayerDataService playerDataService;
    private final WeaponService weaponService;
    private final WeaponProficiencyService proficiencyService;
    private final ManaService manaService;
    private final CooldownService cooldownService;
    private final CombatService combatService;
    private final SkillRegistry skillRegistry;
    private final SwordmasterBladeService swordmasterBladeService;
    private final BowmasterSkillService bowmasterSkillService;
    private final LancerSkillService lancerSkillService;
    private final SkillEffectPipeline defaultEffectPipeline = SkillEffectPipeline.empty();
    private EnchantService enchantService;
    private EquipmentEffectTriggerEngine equipmentEffectTriggerEngine;

    public SkillService(
            ConfigService configService,
            PlayerDataService playerDataService,
            WeaponService weaponService,
            WeaponProficiencyService proficiencyService,
            ManaService manaService,
            CooldownService cooldownService,
            CombatService combatService,
            SkillRegistry skillRegistry,
            SwordmasterBladeService swordmasterBladeService,
            BowmasterSkillService bowmasterSkillService,
            LancerSkillService lancerSkillService
    ) {
        this.configService = configService;
        this.playerDataService = playerDataService;
        this.weaponService = weaponService;
        this.proficiencyService = proficiencyService;
        this.manaService = manaService;
        this.cooldownService = cooldownService;
        this.combatService = combatService;
        this.skillRegistry = skillRegistry;
        this.swordmasterBladeService = swordmasterBladeService;
        this.bowmasterSkillService = bowmasterSkillService;
        this.lancerSkillService = lancerSkillService;
    }

    public void setEnchantService(EnchantService enchantService) {
        this.enchantService = enchantService;
    }

    public void setEquipmentEffectTriggerEngine(EquipmentEffectTriggerEngine engine) {
        this.equipmentEffectTriggerEngine = engine;
    }

    public SkillInputResult handleInput(Player player, SkillInputType inputType) {
        return handleInput(player, inputType, inputEquipment(player, inputType));
    }

    public SkillInputResult handleInput(Player player, SkillInputType inputType, ItemStack inputItem) {
        if (!configService.getBoolean("skill-input.enabled", true)) {
            return SkillInputResult.ignored();
        }
        if (equipmentEffectTriggerEngine != null) {
            SkillInputResult triggered = equipmentEffectTriggerEngine.triggerInput(player, inputType, inputItem, this);
            if (triggered.accepted()) return triggered;
        }
        if (enchantService != null) {
            Optional<EnchantData> enchant = enchantService.findForInput(inputItem, inputType);
            if (enchant.isPresent()) return executeEnchantment(player, enchant.get(), inputType);
        }
        if (!configService.getBoolean("skill-input.legacy-class-skills", false)) {
            return SkillInputResult.ignored();
        }
        Optional<WeaponType> weaponType = weaponService.getWeaponType(inputItem);
        if (weaponType.isEmpty()) {
            return SkillInputResult.ignored();
        }
        Optional<SkillData> legacySkill = skillRegistry.getSkill(weaponType.get(), inputType);
        if (enchantService != null && legacySkill.isPresent()
                && enchantService.hasEquippedExecutor(inputItem, legacySkill.get().skillId())) {
            // Once an enchantment owns this executor, the former profession-style input must not fire it again.
            return SkillInputResult.ignored();
        }
        return handleWeaponInput(player, weaponType.get(), inputType);
    }

    private SkillInputResult handleWeaponInput(Player player, WeaponType weaponType, SkillInputType inputType) {
        Optional<SkillData> skillData = skillRegistry.getSkill(weaponType, inputType);
        boolean magicBasicFire = weaponType == WeaponType.MAGIC && inputType == SkillInputType.LEFT_CLICK && skillData.isEmpty();
        if (skillData.isEmpty() && !magicBasicFire) {
            return SkillInputResult.ignored();
        }

        boolean cancelVanillaAction = shouldCancelVanillaAction(weaponType, inputType, skillData);
        if (magicBasicFire) {
            castWizardBasicFire(player);
            return SkillInputResult.accepted(cancelVanillaAction);
        }

        SkillData skill = skillData.orElseThrow();
        String cooldownId = "skill:" + skill.skillId();
        long remainingMillis = cooldownService.getRemainingMillis(player.getUniqueId(), cooldownId);
        if (remainingMillis > 0L) {
            sendCooldownMessage(player, skill.displayName(), remainingMillis);
            return SkillInputResult.accepted(cancelVanillaAction);
        }
        PlayerRPGData data = playerDataService.getOrLoad(player);
        int skillLevel = data.getSkillLevel(skill.skillId());
        boolean alreadyUnlocked = data.hasUnlockedSkill(skill.skillId()) && skillLevel > 0;
        if (!alreadyUnlocked && !proficiencyService.meetsRequirement(player, weaponType, skill.requiredProficiencyLevel())) {
            player.sendMessage(Component.text(
                    skill.displayName() + " 사용에는 " + weaponType.displayName() + " 숙련도 Lv." + skill.requiredProficiencyLevel() + "이 필요합니다.",
                    NamedTextColor.RED
            ));
            return SkillInputResult.accepted(cancelVanillaAction);
        }

        if (!alreadyUnlocked) {
            player.sendMessage(Component.text(skill.displayName() + " 스킬이 잠겨 있습니다.", NamedTextColor.RED));
            return SkillInputResult.accepted(cancelVanillaAction);
        }
        if (!canCastRegisteredSkill(player, skill, skillLevel)) {
            return SkillInputResult.accepted(cancelVanillaAction);
        }
        if (!manaService.hasEnoughMana(player, skill.manaCost())) {
            player.sendMessage(Component.text("마나가 부족합니다. 필요 MP: " + format(skill.manaCost()), NamedTextColor.RED));
            return SkillInputResult.accepted(cancelVanillaAction);
        }
        manaService.consumeMana(player, skill.manaCost());

        SkillCastContext context = createCastContext(player, weaponType, inputType, skill.displayName(), skillLevel);
        try {
            defaultEffectPipeline.executePhase(SkillEffectPhase.PRE_CAST, context);
            defaultEffectPipeline.executePhase(SkillEffectPhase.CAST, context);
            castRegisteredSkill(player, skill, skillLevel);
        } finally {
            defaultEffectPipeline.executePhase(SkillEffectPhase.END, context);
        }

        double cooldownSeconds = getCooldownSeconds(skill);
        if (cooldownSeconds > 0.0D) {
            cooldownService.startCooldown(player.getUniqueId(), cooldownId, Math.round(cooldownSeconds * 1000.0D));
        }
        if (configService.getBoolean("skill-input.debug-messages", true) && player.isOp()) {
            player.sendMessage(Component.text("스킬 입력 감지: " + weaponType.displayName() + " / " + skill.displayName(), NamedTextColor.AQUA));
        }
        return SkillInputResult.accepted(cancelVanillaAction);
    }

    public boolean shouldCancelVanillaActionForInput(Player player, SkillInputType inputType) {
        ItemStack inputItem = inputEquipment(player, inputType);
        if (enchantService != null && enchantService.hasActiveInputBinding(inputItem, inputType)) {
            return !(inputType == SkillInputType.RIGHT_CLICK
                    && weaponService.getWeaponType(inputItem).orElse(null) == WeaponType.BOW);
        }
        if (!configService.getBoolean("skill-input.legacy-class-skills", false)) return false;
        Optional<WeaponType> weaponType = weaponService.getWeaponType(inputItem);
        return weaponType.map(type -> shouldCancelVanillaAction(type, inputType, skillRegistry.getSkill(type, inputType))).orElse(false);
    }

    public long getPostInputLeftSuppressMillis(Player player, SkillInputType inputType) {
        if (inputType != SkillInputType.SHIFT_LEFT_CLICK) {
            return 0L;
        }
        return weaponService.getWeaponType(player.getInventory().getItemInMainHand())
                .filter(type -> type == WeaponType.BOW)
                .map(ignored -> bowmasterSkillService.getArrowRainInputSuppressMillis())
                .orElse(0L);
    }

    public boolean isSkillWeapon(ItemStack itemStack) {
        return weaponService.isWeapon(itemStack);
    }

    /** True when an item owns an input through either legacy skills or custom enchants. */
    public boolean isInputEquipment(ItemStack itemStack) {
        return isSkillWeapon(itemStack) || (enchantService != null && enchantService.hasInputBinding(itemStack));
    }

    public boolean isHoldingSkillWeapon(Player player) {
        return isSkillWeapon(player.getInventory().getItemInMainHand());
    }

    private ItemStack inputEquipment(Player player, SkillInputType inputType) {
        if (inputType == SkillInputType.SHIFT_JUMP) {
            return player.getInventory().getChestplate();
        }
        if (inputType == SkillInputType.DROP_KEY || inputType == SkillInputType.OFFHAND_QUICK) {
            ItemStack main = player.getInventory().getItemInMainHand();
            if (enchantService != null && enchantService.hasActiveInputBinding(main, inputType)) return main;
            ItemStack off = player.getInventory().getItemInOffHand();
            if (enchantService != null && enchantService.hasActiveInputBinding(off, inputType)) return off;
            return main;
        }
        return player.getInventory().getItemInMainHand();
    }

    public ManaService getManaService() {
        return manaService;
    }

    public CooldownService getCooldownService() {
        return cooldownService;
    }

    private void castRegisteredSkill(Player player, SkillData skill, int skillLevel) {
        switch (skill.skillId()) {
            case "blade_throw" -> swordmasterBladeService.castBladeThrow(player, skillLevel);
            case "blade_launch" -> swordmasterBladeService.castBladeLaunch(player);
            case "light_greatsword" -> swordmasterBladeService.castLightGreatsword(player, skillLevel);
            case "fire_arrow" -> bowmasterSkillService.prepareFireArrow(player, skillLevel);
            case "laser_arrow" -> bowmasterSkillService.castLaserArrow(player, skillLevel);
            case "arrow_rain" -> bowmasterSkillService.castArrowRain(player, skillLevel);
            case "spear_throw" -> lancerSkillService.castSpearThrow(player, skillLevel);
            case "charge" -> lancerSkillService.castCharge(player, 0.0D);
            case "spear_breakthrough" -> lancerSkillService.castSpearBreakthrough(player, skillLevel);
            default -> {
                // Future skill effect modules register their behavior here or through the effect pipeline.
            }
        }
    }

    public SkillInputResult executeEnchantment(Player player, EnchantData enchant, SkillInputType inputType) {
        SkillData skill = skillRegistry.getSkill(enchant.executorId()).orElse(null);
        if (skill == null) return SkillInputResult.accepted(true);
        String cooldownId = "enchant:" + enchant.enchantId();
        long remainingMillis = cooldownService.getRemainingMillis(player.getUniqueId(), cooldownId);
        if (remainingMillis > 0L) {
            sendCooldownMessage(player, skill.displayName(), remainingMillis);
            return SkillInputResult.accepted(true);
        }
        if ("arrow_rain".equals(enchant.executorId()) && !bowmasterSkillService.canCastArrowRain(player)) {
            player.sendActionBar(Component.text("천장이 너무 낮아 불화살 비를 사용할 수 없습니다.", NamedTextColor.YELLOW));
            return SkillInputResult.accepted(true);
        }
        if (!manaService.hasEnoughMana(player, skill.manaCost())) {
            player.sendMessage(Component.text("마나가 부족합니다.", NamedTextColor.RED));
            return SkillInputResult.accepted(true);
        }
        manaService.consumeMana(player, skill.manaCost());
        PlayerRPGData data = playerDataService.getOrLoad(player);
        int level = Math.max(1, data.getSkillLevel(skill.skillId()));
        SkillCastContext context = createCastContext(player, skill.weaponType(), inputType, skill.displayName(), level);
        try {
            defaultEffectPipeline.executePhase(SkillEffectPhase.PRE_CAST, context);
            defaultEffectPipeline.executePhase(SkillEffectPhase.CAST, context);
            castRegisteredSkill(player, skill, level);
        } finally {
            defaultEffectPipeline.executePhase(SkillEffectPhase.END, context);
        }
        double cooldown = getCooldownSeconds(skill);
        if (cooldown > 0.0D) cooldownService.startCooldown(player.getUniqueId(), cooldownId, Math.round(cooldown * 1000.0D));
        return SkillInputResult.accepted(true);
    }

    private boolean canCastRegisteredSkill(Player player, SkillData skillData, int skillLevel) {
        return switch (skillData.skillId()) {
            case "blade_throw" -> swordmasterBladeService.canCastBladeThrow(player, skillLevel);
            case "blade_launch" -> swordmasterBladeService.canCastBladeLaunch(player);
            default -> true;
        };
    }

    private boolean shouldCancelVanillaAction(WeaponType weaponType, SkillInputType inputType, Optional<SkillData> skillData) {
        if (inputType == SkillInputType.OFFHAND_QUICK) {
            return true;
        }
        if (inputType == SkillInputType.DROP_KEY) {
            return false;
        }
        if (weaponType == WeaponType.BOW && inputType == SkillInputType.RIGHT_CLICK) {
            return false;
        }
        return skillData.isPresent() || (weaponType == WeaponType.MAGIC && inputType == SkillInputType.LEFT_CLICK);
    }

    private double getCooldownSeconds(SkillData skillData) {
        String legacyGroup = switch (skillData.skillId()) {
            case "blade_throw", "blade_launch", "light_greatsword" -> "swordmaster";
            case "fire_arrow", "laser_arrow", "arrow_rain" -> "bowmaster";
            case "spear_throw", "charge", "spear_breakthrough" -> "lancer";
            case "stealth", "assassination" -> "assassin";
            default -> "wizard";
        };
        return configService.getDouble(
                "skill-input.cooldowns." + legacyGroup + "." + skillData.inputType().configKey(),
                skillData.cooldownSeconds()
        );
    }

    private SkillCastContext createCastContext(Player player, WeaponType weaponType, SkillInputType inputType, String skillName, int skillLevel) {
        return new SkillCastContext(
                player,
                playerDataService.getOrLoad(player),
                weaponType,
                inputType,
                skillName,
                skillLevel,
                player.getLocation(),
                player.getEyeLocation().getDirection(),
                Map.of()
        );
    }

    private void sendCooldownMessage(Player player, String skillName, long remainingMillis) {
        if (!configService.getBoolean("skill-input.cooldown-messages", true)) {
            return;
        }
        double remainingSeconds = Math.ceil(remainingMillis / 100.0D) / 10.0D;
        player.sendMessage(Component.text(skillName + " 쿨다운: " + remainingSeconds + "초", NamedTextColor.YELLOW));
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private void castWizardBasicFire(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                MAGIC_BASIC_FIRE_RANGE,
                MAGIC_BASIC_FIRE_RAY_SIZE,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );
        if (result == null) {
            return;
        }
        Entity hitEntity = result.getHitEntity();
        if (hitEntity instanceof LivingEntity target) {
            double damage = configService.getDouble("skill-input.wizard-basic-fire-damage", 2.0D);
            combatService.applyDirectDamage(player, target, damage);
            player.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0.0D, 1.0D, 0.0D), 8, 0.25D, 0.35D, 0.25D, 0.01D);
        }
    }
}
