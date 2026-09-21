package com.hyunseo.hyunseorpg.enchant;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.activity.ActivityBlockRewardValidator;
import com.hyunseo.hyunseorpg.combat.DamageContext;
import com.hyunseo.hyunseorpg.combat.DamageType;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import com.hyunseo.hyunseorpg.equipment.trigger.EquipmentEffectHandler;
import com.hyunseo.hyunseorpg.equipment.trigger.EquipmentEffectResult;
import com.hyunseo.hyunseorpg.equipment.trigger.TriggerContext;
import com.hyunseo.hyunseorpg.equipment.trigger.TriggerType;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.skill.swordmaster.SwordmasterBladeService;
import com.hyunseo.hyunseorpg.skill.bowmaster.BowmasterSkillService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One shared content handler for the new enchant catalogue. Input dispatch is
 * still owned by SkillInputListener and EquipmentEffectTriggerEngine; this
 * listener only owns non-input gameplay events and temporary state cleanup.
 */
public final class EquipmentEnchantContentService implements EquipmentEffectHandler, Listener {
    private static final String TITANS_WRATH_MODIFIER = "hyunseorpg_titans_wrath";
    private static final double DEFAULT_PLAYER_SCALE = 1.0D;

    private static final Set<PotionEffectType> NEGATIVE_VANILLA_EFFECTS = Set.of(
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.WEAKNESS,
            PotionEffectType.SLOWNESS, PotionEffectType.BLINDNESS, PotionEffectType.DARKNESS,
            PotionEffectType.MINING_FATIGUE, PotionEffectType.HUNGER, PotionEffectType.UNLUCK,
            PotionEffectType.NAUSEA, PotionEffectType.GLOWING, PotionEffectType.BAD_OMEN,
            PotionEffectType.TRIAL_OMEN, PotionEffectType.RAID_OMEN
    );

    private final JavaPlugin plugin;
    private final ConfigService config;
    private final CombatService combat;
    private final EnchantService enchants;
    private final EquipmentInstanceService equipmentInstances;
    private final EnchantRuntimeStateService runtimeStates;
    private final EquipmentTierService tiers;
    private final RPGItemService items;
    private final CooldownService cooldowns;
    private final SwordmasterBladeService swordmasterBlades;
    private final BowmasterSkillService bowAbilities;
    private final ActivityBlockRewardValidator blockRewards;
    private final NamespacedKey projectileEnchantKey;
    private final NamespacedKey projectileOwnerKey;
    private final NamespacedKey precisionDebtKey;
    private final Map<UUID, ItemStack> projectileSourceItems = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> projectileTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> bowDrawStarted = new ConcurrentHashMap<>();
    private final Map<UUID, StunState> stunned = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveState> titans = new ConcurrentHashMap<>();
    private final Set<String> recursiveBlocks = ConcurrentHashMap.newKeySet();
    private final Set<UUID> processedProtectionChains = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> precisionLastDurabilityTick = new ConcurrentHashMap<>();
    private final Map<UUID, Long> precisionLastRecoveryTick = new ConcurrentHashMap<>();
    private final Map<UUID, PrecisionSession> precisionSessions = new ConcurrentHashMap<>();
    private final Set<String> warnedInvalidSettings = ConcurrentHashMap.newKeySet();
    private final BukkitTask passiveTicker;
    private final BukkitTask precisionTicker;

    public EquipmentEnchantContentService(JavaPlugin plugin, ConfigService config, CombatService combat,
                                          EnchantService enchants, EquipmentInstanceService equipmentInstances,
                                          EnchantRuntimeStateService runtimeStates,
                                          EquipmentTierService tiers, RPGItemService items,
                                          CooldownService cooldowns,
                                          SwordmasterBladeService swordmasterBlades,
                                          BowmasterSkillService bowAbilities,
                                          ActivityBlockRewardValidator blockRewards) {
        this.plugin = plugin;
        this.config = config;
        this.combat = combat;
        this.enchants = enchants;
        this.equipmentInstances = equipmentInstances;
        this.runtimeStates = runtimeStates;
        this.tiers = tiers;
        this.items = items;
        this.cooldowns = cooldowns;
        this.swordmasterBlades = swordmasterBlades;
        this.bowAbilities = bowAbilities;
        this.blockRewards = blockRewards;
        this.projectileEnchantKey = new NamespacedKey(plugin, "enchant_content_id");
        this.projectileOwnerKey = new NamespacedKey(plugin, "enchant_content_owner");
        this.precisionDebtKey = new NamespacedKey(plugin, "precision_flight_durability_debt");
        this.passiveTicker = Bukkit.getScheduler().runTaskTimer(plugin, this::tickPersistentEffects, 1L, 5L);
        this.precisionTicker = Bukkit.getScheduler().runTaskTimer(plugin, this::tickPrecisionSessions, 1L, 1L);
    }

    @Override
    public EquipmentEffectResult handle(TriggerContext context, EnchantData enchant) {
        return switch (enchant.enchantId()) {
            case "blade_chain" -> handleBladeChain(context, enchant);
            case "light_greatsword" -> handleLightGreatsword(context, enchant);
            case "laser_arrow" -> handleLaserArrow(context, enchant);
            case "fire_arrow_rain" -> handleFireArrowRain(context, enchant);
            case "axe_heavy_strike" -> handleAxeHeavyStrike(context, enchant);
            case "titans_wrath" -> handleTitansWrath(context, enchant);
            case "skill_protection" -> handleArmorProtection(context);
            case "rolling_landing" -> handleRollingLanding(context, enchant);
            case "wind_arrow" -> handleWindArrow(context, enchant);
            case "crossbow_barrage" -> handleCrossbowBarrage(context, enchant);
            case "area_excavation" -> handleAreaExcavation(context, enchant);
            case "auto_replant" -> handleAutoReplant(context, enchant);
            case "auto_smelt" -> handleAutoSmelt(context, enchant);
            case "mining_bonus_drop" -> handleMiningBonusDrop(context, enchant);
            case "chain_logging" -> handleChainLogging(context, enchant);
            case "treasure_finder", "multi_catch" -> EquipmentEffectResult.CONDITION_NOT_MET;
            case "elytra_launch" -> handleElytraLaunch(context, enchant);
            case "precision_flight" -> handlePrecisionFlightBoost(context, enchant);
            case "explosive_mace" -> handleExplosiveMace(context, enchant);
            default -> EquipmentEffectResult.CONDITION_NOT_MET;
        };
    }

    public void shutdown() {
        passiveTicker.cancel();
        precisionTicker.cancel();
        runtimeStates.clearAll();
        for (BukkitTask task : new ArrayList<>(projectileTasks.values())) task.cancel();
        projectileTasks.clear();
        projectileSourceItems.clear();
        bowDrawStarted.clear();
        stunned.values().forEach(StunState::cancel);
        stunned.clear();
        Bukkit.getOnlinePlayers().forEach(player -> clearTitanState(player, true));
        titans.clear();
        precisionSessions.clear();
        precisionLastDurabilityTick.clear();
        precisionLastRecoveryTick.clear();
    }

    private EquipmentEffectResult handleBladeChain(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.INPUT || context.triggeringItem() == null) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        Player player = context.player();
        ItemStack equipment = context.triggeringItem();
        int level = Math.max(1, getLevel(equipment, enchant.enchantId()));
        if (context.inputType() != com.hyunseo.hyunseorpg.skill.SkillInputType.OFFHAND_QUICK) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        if (swordmasterBlades.hasLaunchableBlades(player)) {
            if (!swordmasterBlades.canCastBladeLaunch(player)) return EquipmentEffectResult.CONDITION_NOT_MET;
            swordmasterBlades.castBladeLaunch(player);
            return EquipmentEffectResult.EXECUTED;
        }
        if (!swordmasterBlades.canCastBladeThrow(player, level)) return EquipmentEffectResult.CONDITION_NOT_MET;
        swordmasterBlades.castBladeThrow(player, level);
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleLightGreatsword(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.INPUT || context.triggeringItem() == null) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        swordmasterBlades.castLightGreatsword(context.player(),
                Math.max(1, getLevel(context.triggeringItem(), enchant.enchantId())));
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleLaserArrow(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.INPUT || context.triggeringItem() == null) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        bowAbilities.castLaserArrow(context.player(),
                Math.max(1, getLevel(context.triggeringItem(), enchant.enchantId())));
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleFireArrowRain(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.INPUT || context.triggeringItem() == null) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        int level = Math.max(1, getLevel(context.triggeringItem(), enchant.enchantId()));
        return bowAbilities.castArrowRain(context.player(), level)
                ? EquipmentEffectResult.EXECUTED : EquipmentEffectResult.CONDITION_NOT_MET;
    }

    private EquipmentEffectResult handleAxeHeavyStrike(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() == TriggerType.INPUT && context.triggeringItem() != null) {
            Player player = context.player();
            ItemStack equipment = context.triggeringItem();
            EnchantRuntimeStateService.RuntimeState state = runtimeStates.state(player, equipment, enchant.enchantId());
            long duration = Math.max(1L, integer(enchant.enchantId(), "prepare-duration-ticks", 200)) * 50L;
            state.expiresAtMillis(System.currentTimeMillis() + duration);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    Math.max(1, integer(enchant.enchantId(), "prepare-duration-ticks", 200)),
                    Math.max(0, integer(enchant.enchantId(), "prepare-slowness-amplifier", 0)), false, false, true));
            return EquipmentEffectResult.EXECUTED_DEFERRED_COOLDOWN;
        }
        if (context.triggerType() != TriggerType.ATTACK_HIT || context.livingTarget() == null || context.triggeringItem() == null) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        EnchantRuntimeStateService.RuntimeState state = runtimeStates.find(context.player(), context.triggeringItem(), enchant.enchantId());
        if (state == null || state.isExpired()) return EquipmentEffectResult.CONDITION_NOT_MET;
        runtimeStates.clear(context.player(), context.triggeringItem(), enchant.enchantId());
        LivingEntity center = context.livingTarget();
        double bonusMultiplier = boundedNumber(enchant.enchantId(), "bonus-damage-multiplier", 1.0D, 0.0D, 10.0D);
        double bonusDamage = Math.max(0.0D, context.finalDamage()) * bonusMultiplier;
        if (bonusDamage > 0.0D) {
            combat.applyEnchantDamage(context.player(), context.triggeringItem(), enchant.enchantId(),
                    DamageType.ENCHANT_SKILL, center, bonusDamage, true);
        }
        double radius = Math.max(0.1D, number(enchant.enchantId(), "stun-radius", 4.0D));
        int ticks = adjustedStunTicks(center, integer(enchant.enchantId(), "stun-duration-ticks", 40), enchant.enchantId());
        for (Entity entity : center.getWorld().getNearbyEntities(center.getLocation(), radius, radius, radius)) {
            if (entity instanceof LivingEntity target && target != context.player() && !target.isDead()) stun(target, ticks);
        }
        center.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, center.getLocation(), 32, radius * .25D, .2D, radius * .25D,
                Material.STONE.createBlockData());
        center.getWorld().playSound(center.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0F, .65F);
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleTitansWrath(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.INPUT || context.triggeringItem() == null) return EquipmentEffectResult.CONDITION_NOT_MET;
        Player player = context.player();
        ItemStack equipment = context.triggeringItem();
        // A repeated activation must replace the previous runtime state instead of
        // leaving an old scale modifier or cleanup callback behind.
        clearTitanState(player, true);
        runtimeStates.clear(player, equipment, enchant.enchantId());
        UUID equipmentId = equipmentInstances.ensure(equipment);
        int ticks = Math.max(1, integer(enchant.enchantId(), "duration-ticks", 160));
        EnchantRuntimeStateService.RuntimeState state = runtimeStates.state(player, equipment, enchant.enchantId());
        ActiveState active = new ActiveState(equipmentId, System.currentTimeMillis() + ticks * 50L);
        titans.put(player.getUniqueId(), active);
        state.expiresAtMillis(active.expiresAtMillis());
        state.addCleanup(() -> clearTitanState(player, true));
        addModifier(player, Attribute.SCALE, TITANS_WRATH_MODIFIER, number(enchant.enchantId(), "scale-add", 0.5D));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks,
                Math.max(0, integer(enchant.enchantId(), "strength-amplifier", 0)), false, true, true));
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            if (NEGATIVE_VANILLA_EFFECTS.contains(effect.getType())) player.removePotionEffect(effect.getType());
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin,
                () -> runtimeStates.clear(player, equipment, enchant.enchantId()), ticks);
        state.addTask(task);
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleArmorProtection(TriggerContext context) {
        if (context.triggerType() != TriggerType.DAMAGED || !(context.originalEvent() instanceof EntityDamageEvent event)) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        if (!processedProtectionChains.add(context.chainId())) return EquipmentEffectResult.CONDITION_NOT_MET;
        Bukkit.getScheduler().runTask(plugin, () -> processedProtectionChains.remove(context.chainId()));
        Player player = context.player();
        double reduction = 0.0D;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            DamageContext damage = context.damageContext();
            if (damage != null && damage.isSkillDamage()) reduction += protectionReduction(armor, "skill_protection");
        }
        double cap = clamp(config.getDouble("enchant-content.protection.final-reduction-cap", 0.8D), 0.0D, 0.95D);
        reduction = clamp(reduction, 0.0D, cap);
        if (reduction <= 0.0D) return EquipmentEffectResult.CONDITION_NOT_MET;
        event.setDamage(event.getDamage() * (1.0D - reduction));
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleRollingLanding(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.DAMAGED || !(context.originalEvent() instanceof EntityDamageEvent event)
                || event.getCause() != EntityDamageEvent.DamageCause.FALL || !context.player().isSneaking()) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        Player player = context.player();
        if (player.getFallDistance() < number(enchant.enchantId(), "minimum-fall-distance", 4.0D)
                || player.isGliding() || player.isInWater() || player.isInLava()) return EquipmentEffectResult.CONDITION_NOT_MET;
        ItemStack boots = findItemWith(player, enchant.enchantId(), EquipmentTierService.Category.ARMOR);
        if (boots == null || !isBoots(boots) || !roll(chance(enchant.enchantId(), "chance", 0.25D))) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        event.setCancelled(true);
        Vector direction = player.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() > 0.0001D) player.setVelocity(direction.normalize()
                .multiply(number(enchant.enchantId(), "roll-horizontal-speed", 0.9D)).setY(0.05D));
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleWindArrow(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.INPUT || context.triggeringItem() == null) return EquipmentEffectResult.CONDITION_NOT_MET;
        EnchantRuntimeStateService.RuntimeState state = runtimeStates.state(context.player(), context.triggeringItem(), enchant.enchantId());
        int ticks = Math.max(1, integer(enchant.enchantId(), "arm-duration-ticks", 200));
        state.expiresAtMillis(System.currentTimeMillis() + ticks * 50L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin,
                () -> runtimeStates.clear(context.player(), context.triggeringItem(), enchant.enchantId()), ticks);
        state.addTask(task);
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleCrossbowBarrage(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.INPUT || context.triggeringItem() == null) return EquipmentEffectResult.CONDITION_NOT_MET;
        Player player = context.player();
        ItemStack crossbow = context.triggeringItem();
        EnchantRuntimeStateService.RuntimeState state = runtimeStates.state(player, crossbow, enchant.enchantId());
        int max = Math.max(1, integer(enchant.enchantId(), "maximum-stacks", 3));
        int current = (int) state.getLong("stacks", 0L);
        if (current < max) {
            if (!consumeOne(player.getInventory(), Material.ARROW)) return EquipmentEffectResult.CONDITION_NOT_MET;
            current++;
            state.putLong("stacks", current);
            state.expiresAtMillis(System.currentTimeMillis() + Math.max(1, integer(enchant.enchantId(), "stack-timeout-ticks", 400)) * 50L);
            player.getWorld().spawnParticle(Particle.CRIT, player.getEyeLocation(),
                    boundedInteger(enchant.enchantId(), "charge-particle-count", 4, 0, 16),
                    .12D, .12D, .12D, .02D);
            float pitch = (float) boundedNumber(enchant.enchantId(), "charge-sound-pitch-" + current,
                    .75D + current * .2D, .5D, 2.0D);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START,
                    (float) boundedNumber(enchant.enchantId(), "charge-sound-volume", .45D, 0.0D, 2.0D), pitch);
            player.sendActionBar(Component.text(current < max
                    ? "연발 사격 " + current + "/" + max : "연발 사격 발동", NamedTextColor.AQUA));
            if (current < max) return EquipmentEffectResult.EXECUTED_DEFERRED_COOLDOWN;
        }
        int interval = Math.max(1, integer(enchant.enchantId(), "shot-interval-ticks", 4));
        UUID equipmentId = equipmentInstances.ensure(crossbow);
        for (int index = 0; index < current; index++) {
            int delay = index * interval;
            int shotIndex = index;
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || !holdsInstance(player, equipmentId)) return;
                player.getWorld().spawnParticle(Particle.CRIT, player.getEyeLocation(),
                        boundedInteger(enchant.enchantId(), "muzzle-particle-count", 3, 0, 12),
                        .08D, .08D, .08D, .01D);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT,
                        (float) boundedNumber(enchant.enchantId(), "shot-sound-volume", .55D, 0.0D, 2.0D),
                        1.0F + shotIndex * .08F);
                Vector direction = player.getEyeLocation().getDirection().normalize();
                Location spawn = player.getEyeLocation().clone().add(direction.clone().multiply(.35D));
                Arrow arrow = player.getWorld().spawn(spawn, Arrow.class);
                double speed = boundedNumber(enchant.enchantId(), "projectile-speed", 2.6D, 0.1D, 8.0D);
                arrow.setGravity(false);
                arrow.setCritical(false);
                arrow.setDamage(0.0D);
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                arrow.setVelocity(direction.clone().multiply(speed));
                tagProjectile(arrow, enchant.enchantId(), player, crossbow);
                trackStraightProjectile(arrow, direction, speed,
                        boundedInteger(enchant.enchantId(), "maximum-lifetime-ticks", 100, 1, 200));
            }, delay);
            state.addTask(task);
        }
        state.putLong("stacks", 0L);
        state.expiresAtMillis(0L);
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleAreaExcavation(TriggerContext context, EnchantData enchant) {
        if (context.triggeringItem() == null) return EquipmentEffectResult.CONDITION_NOT_MET;
        if (context.triggerType() == TriggerType.INPUT && isHoe(context.triggeringItem())) {
            return tillArea(context.player(), context.triggeringItem(), enchant.enchantId())
                    ? EquipmentEffectResult.EXECUTED : EquipmentEffectResult.CONDITION_NOT_MET;
        }
        if (context.triggerType() != TriggerType.GATHER_SUCCESS || context.block() == null || isHoe(context.triggeringItem())) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        String root = blockKey(context.block());
        if (!recursiveBlocks.add(root)) return EquipmentEffectResult.CONDITION_NOT_MET;
        try {
            breakArea(context.player(), context.triggeringItem(), context.block(), enchant.enchantId());
            return EquipmentEffectResult.EXECUTED;
        } finally {
            recursiveBlocks.remove(root);
        }
    }

    private EquipmentEffectResult handleAutoReplant(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.GATHER_SUCCESS || context.block() == null || context.triggeringItem() == null) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        Block block = context.block();
        Material crop = block.getType();
        if (!(block.getBlockData() instanceof Ageable)) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        if (!stringList(enchant.enchantId(), "allowed-crops").contains(crop.name())) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        Material seed = seedFor(crop);
        if (seed == null) return EquipmentEffectResult.CONDITION_NOT_MET;
        Player player = context.player();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || block.getType() != Material.AIR || !consumeOne(player.getInventory(), seed)) return;
            block.setType(crop, false);
            if (block.getBlockData() instanceof Ageable replanted) {
                replanted.setAge(0);
                block.setBlockData(replanted, false);
            }
        });
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleAutoSmelt(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.GATHER_ATTEMPT
                || !(context.originalEvent() instanceof BlockBreakEvent event) || context.triggeringItem() == null) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        Material result = smeltResult(enchant.enchantId(), event.getBlock().getType());
        if (result == null || hasSilkTouch(context.triggeringItem())) return EquipmentEffectResult.CONDITION_NOT_MET;
        Collection<ItemStack> drops = event.getBlock().getDrops(context.triggeringItem(), context.player());
        if (drops.isEmpty()) return EquipmentEffectResult.CONDITION_NOT_MET;
        event.setDropItems(false);
        for (ItemStack drop : drops) {
            ItemStack smelted = new ItemStack(result, drop.getAmount());
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), smelted);
        }
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleMiningBonusDrop(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.GATHER_SUCCESS || context.block() == null
                || context.triggeringItem() == null || !roll(chance(enchant.enchantId(), "chance", 0.12D))) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        Collection<ItemStack> drops = context.block().getDrops(context.triggeringItem(), context.player());
        if (drops.isEmpty()) return EquipmentEffectResult.CONDITION_NOT_MET;
        for (ItemStack drop : drops) {
            if (drop != null && !drop.getType().isAir() && drop.getAmount() > 0) {
                context.block().getWorld().dropItemNaturally(context.block().getLocation(), drop.clone());
            }
        }
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleChainLogging(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.GATHER_SUCCESS || context.block() == null
                || context.triggeringItem() == null || !context.triggeringItem().getType().name().endsWith("_AXE")
                || !isLog(context.block())) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        if (recursiveBlocks.contains(blockKey(context.block()))) return EquipmentEffectResult.CONDITION_NOT_MET;
        int maximum = boundedInteger(enchant.enchantId(), "maximum-blocks", 48, 1, 256);
        int radius = boundedInteger(enchant.enchantId(), "search-radius", 8, 1, 16);
        int abortLimit = boundedInteger(enchant.enchantId(), "abort-if-log-count-exceeds", 64, 0, 512);
        int scanLimit = abortLimit > 0 ? abortLimit + 1 : maximum + 1;
        List<Block> pending = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        pending.add(context.block());
        seen.add(blockKey(context.block()));
        int processed = 0;
        for (int index = 0; index < pending.size(); index++) {
            Block block = pending.get(index);
            for (BlockFace face : List.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
                Block nearby = block.getRelative(face);
                if (!isLog(nearby) || nearby.getLocation().distanceSquared(context.block().getLocation()) > radius * radius) continue;
                if (seen.add(blockKey(nearby))) {
                    pending.add(nearby);
                    if (abortLimit > 0 && pending.size() > abortLimit) {
                        context.player().sendActionBar(Component.text("나무가 너무 커서 연쇄 벌목을 중단했습니다.", NamedTextColor.YELLOW));
                        return EquipmentEffectResult.CONDITION_NOT_MET;
                    }
                    if (pending.size() >= scanLimit) break;
                }
            }
            if (pending.size() >= scanLimit) break;
        }
        for (Block block : pending) {
            if (block.equals(context.block()) || processed >= maximum) continue;
            String key = blockKey(block);
            if (!recursiveBlocks.add(key)) continue;
            try {
                BlockBreakEvent nested = new BlockBreakEvent(block, context.player());
                blockRewards.markSynthetic(nested);
                Bukkit.getPluginManager().callEvent(nested);
                if (nested.isCancelled()) continue;
                block.breakNaturally(context.triggeringItem(), true);
                damageTool(context.triggeringItem(), context.player());
                processed++;
            } finally {
                recursiveBlocks.remove(key);
            }
        }
        return processed > 0 ? EquipmentEffectResult.EXECUTED : EquipmentEffectResult.CONDITION_NOT_MET;
    }

    private EquipmentEffectResult handleElytraLaunch(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.INPUT || context.triggeringItem() == null) return EquipmentEffectResult.CONDITION_NOT_MET;
        Player player = context.player();
        if (player.isInWater() && !bool(enchant.enchantId(), "allow-water", false)) return EquipmentEffectResult.CONDITION_NOT_MET;
        if (player.isInLava() && !bool(enchant.enchantId(), "allow-lava", false)) return EquipmentEffectResult.CONDITION_NOT_MET;
        Vector direction = player.getEyeLocation().getDirection().normalize();
        double ceiling = Math.max(0.1D, number(enchant.enchantId(), "minimum-ceiling-clearance", 2.0D));
        if (player.getWorld().rayTraceBlocks(player.getEyeLocation(), new Vector(0, 1, 0), ceiling) != null) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        Vector velocity = direction.multiply(number(enchant.enchantId(), "forward-speed", 1.35D));
        velocity.setY(Math.max(velocity.getY(), number(enchant.enchantId(), "vertical-speed", 0.85D)));
        player.setVelocity(velocity);
        UUID equipmentId = equipmentInstances.ensure(context.triggeringItem());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isDead() && !player.isOnGround() && holdsInstance(player, equipmentId)) {
                player.setGliding(true);
            }
        }, 1L);
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handlePrecisionFlightBoost(TriggerContext context, EnchantData enchant) {
        if (!(context.originalEvent() instanceof PlayerElytraBoostEvent event)) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        Player player = context.player();
        ItemStack chest = player.getInventory().getChestplate();
        debugPrecisionBoost(event, player, chest, "event-received");
        if (event.isCancelled()) {
            debugPrecisionBoost(event, player, chest, "event-already-cancelled");
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        if (!player.isGliding()) {
            debugPrecisionBoost(event, player, chest, "not-gliding");
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        if (chest == null || !enchants.hasActiveEquipped(chest, enchant.enchantId())) {
            debugPrecisionBoost(event, player, chest, "enchant-not-active");
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        if (!(chest.getItemMeta() instanceof Damageable damageable)) {
            debugPrecisionBoost(event, player, chest, "elytra-not-damageable");
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        int remaining = chest.getType().getMaxDurability() - damageable.getDamage();
        if (remaining <= 2) {
            player.sendActionBar(Component.text("Elytra durability is too low.", NamedTextColor.RED));
            debugPrecisionBoost(event, player, chest, "durability-limit");
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        if (!hasItemInHand(player, event.getHand(), Material.FIREWORK_ROCKET)) {
            debugPrecisionBoost(event, player, chest, "firework-missing");
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        event.setCancelled(true);
        event.setShouldConsume(false);
        if (event.getFirework() != null && event.getFirework().isValid()) event.getFirework().remove();
        if (!consumeOneFromHand(player, event.getHand())) {
            debugPrecisionBoost(event, player, chest, "firework-consume-failed");
            return EquipmentEffectResult.FAILED;
        }
        activatePrecisionFlight(player, chest);
        debugPrecisionBoost(event, player, chest, "session-activated");
        return EquipmentEffectResult.EXECUTED;
    }

    private EquipmentEffectResult handleExplosiveMace(TriggerContext context, EnchantData enchant) {
        if (context.triggerType() != TriggerType.ATTACK_HIT || context.livingTarget() == null || context.triggeringItem() == null) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        Player player = context.player();
        if (player.getFallDistance() < boundedNumber(enchant.enchantId(), "minimum-fall-distance", 2.0D, 0.0D, 100.0D)) {
            return EquipmentEffectResult.CONDITION_NOT_MET;
        }
        LivingEntity impact = context.livingTarget();
        double radius = boundedNumber(enchant.enchantId(), "radius", 4.5D, 0.1D, 12.0D);
        double damage = boundedNumber(enchant.enchantId(), "area-damage", 2.0D, 0.0D, 40.0D);
        double knockback = boundedNumber(enchant.enchantId(), "knockback", 1.0D, 0.0D, 8.0D);
        int maximumTargets = boundedInteger(enchant.enchantId(), "maximum-targets", 20, 1, 20);
        Set<UUID> primaryAffected = new HashSet<>();
        long startedAt = System.nanoTime();
        List<LivingEntity> directTargets = applyMaceExplosion(player, context.triggeringItem(), enchant.enchantId(),
                impact.getLocation(), radius, damage, knockback, maximumTargets, primaryAffected, true);
        Set<UUID> primaryTargets = directTargets.stream().map(Entity::getUniqueId).collect(java.util.stream.Collectors.toSet());
        int chainDelay = boundedInteger(enchant.enchantId(), "chain-delay-ticks", 15, 1, 100);
        double chainRadius = boundedNumber(enchant.enchantId(), "chain-radius", 3.5D, 0.1D, 12.0D);
        double chainDamage = boundedNumber(enchant.enchantId(), "chain-damage", damage, 0.0D, 40.0D);
        double chainKnockback = boundedNumber(enchant.enchantId(), "chain-knockback", knockback, 0.0D, 8.0D);
        int maximumCenters = boundedInteger(enchant.enchantId(), "maximum-centers", 20, 1, 20);
        if (bool(enchant.enchantId(), "chain-enabled", true) && !primaryTargets.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    Set<UUID> secondaryDamagedTargets = new HashSet<>();
                    int centers = 0;
                    for (UUID targetId : primaryTargets) {
                        if (centers++ >= maximumCenters) break;
                        LivingEntity target = entity(targetId, LivingEntity.class);
                        if (target == null || target.isDead() || !target.isValid()
                                || !target.getWorld().equals(impact.getWorld())) continue;
                        applyMaceExplosion(player, context.triggeringItem(), enchant.enchantId(), target.getLocation(),
                                chainRadius, chainDamage, chainKnockback, maximumTargets,
                                secondaryDamagedTargets, false);
                    }
                } catch (RuntimeException exception) {
                    plugin.getLogger().warning("Explosive mace chain cancelled: "
                            + exception.getClass().getSimpleName());
                }
            }, chainDelay);
        }
        debugMaceTiming(enchant.enchantId(), startedAt, primaryAffected.size(), primaryTargets.size());
        return EquipmentEffectResult.EXECUTED;
    }

    private List<LivingEntity> applyMaceExplosion(Player player, ItemStack source, String enchantId, Location center,
                                                   double radius, double damage, double knockback, int maximumTargets,
                                                   Set<UUID> affected, boolean primary) {
        if (center == null || center.getWorld() == null || affected.size() >= maximumTargets) return List.of();
        if (!Double.isFinite(radius) || !Double.isFinite(damage) || !Double.isFinite(knockback)
                || radius <= 0.0D || damage < 0.0D || knockback < 0.0D) return List.of();
        if (primary) {
            center.getWorld().spawnParticle(Particle.EXPLOSION, center, boundedInteger(enchantId, "particle-count", 1, 0, 32));
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, .95F, .9F);
        } else {
            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center,
                    boundedInteger(enchantId, "chain-particle-count", 1, 0, 32));
        }
        List<LivingEntity> targets = center.getWorld().getNearbyEntities(center, radius, radius, radius).stream()
                .filter(entity -> entity instanceof LivingEntity && entity != player && !entity.isDead())
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> isMaceTarget(player, (LivingEntity) entity))
                .sorted(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(center)))
                .filter(entity -> affected.size() < maximumTargets)
                .filter(entity -> affected.add(entity.getUniqueId()))
                .toList();
        for (LivingEntity target : targets) {
            if (!target.isValid() || target.isDead() || !target.getWorld().equals(center.getWorld())) continue;
            Vector push = target.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() < .0001D) push = new Vector(0.0D, .45D, 0.0D);
            else push = push.normalize().multiply(knockback).setY(Math.max(.2D, knockback * .28D));
            target.setVelocity(target.getVelocity().add(push));
            if (damage > 0.0D) combat.applyEnchantDamage(player, source, enchantId,
                    DamageType.EXPLOSION, target, damage, false);
        }
        return targets;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onContentProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile projectile)) return;
        String enchantId = projectileEnchant(projectile);
        if (enchantId.isBlank()) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (enchantId.equals("wind_arrow")) {
            event.setCancelled(true);
            return;
        }
        Player source = owner(projectile);
        ItemStack sourceItem = projectileSourceItems.getOrDefault(projectile.getUniqueId(), source == null ? null
                : source.getInventory().getItemInMainHand());
        if (source == null) return;
        if (enchantId.equals("crossbow_barrage")) {
            event.setCancelled(true);
            combat.applyEnchantDamage(source, sourceItem, enchantId, DamageType.PROJECTILE, target,
                    number(enchantId, "damage", 3.0D), true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getProjectile() instanceof Projectile projectile)) return;
        if (event.isCancelled()) return;
        ItemStack bow = event.getBow();
        if (bow == null) return;
        if (enchants.hasActiveEquipped(bow, "wind_arrow") && runtimeStates.has(player, bow, "wind_arrow")) {
            bowDrawStarted.remove(player.getUniqueId());
            double minimumForce = boundedNumber("wind_arrow", "minimum-force", .95D, 0.0D, 1.0D);
            if (event.getForce() < minimumForce) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("바람 화살을 더 오래 당겨야 합니다.", NamedTextColor.YELLOW));
                return;
            }
            runtimeStates.clear(player, bow, "wind_arrow");
            tagProjectile(projectile, "wind_arrow", player, bow);
            if (projectile instanceof Arrow arrow) {
                arrow.setDamage(0.0D);
                arrow.setCritical(false);
                arrow.setPierceLevel(0);
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBowDrawStart(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        ItemStack bow = event.getItem();
        if (bow != null && bow.getType() == Material.BOW
                && enchants.hasActiveEquipped(bow, "wind_arrow")
                && runtimeStates.has(event.getPlayer(), bow, "wind_arrow")) {
            bowDrawStarted.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        stopProjectileTask(projectile.getUniqueId());
        String enchantId = projectileEnchant(projectile);
        if (enchantId.isBlank()) return;
        Player source = owner(projectile);
        projectileSourceItems.remove(projectile.getUniqueId());
        if (enchantId.equals("wind_arrow") && source != null) {
            Location center = event.getHitEntity() == null ? projectile.getLocation().clone()
                    : event.getHitEntity().getLocation().clone();
            LivingEntity direct = event.getHitEntity() instanceof LivingEntity living ? living : null;
            Bukkit.getScheduler().runTask(plugin, () -> applyWindImpact(source, center, direct, "wind_arrow"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (rod.getType() != Material.FISHING_ROD) rod = player.getInventory().getItemInOffHand();
        if (rod == null || rod.getType() != Material.FISHING_ROD) return;
        if (enchants.hasActiveEquipped(rod, "treasure_finder") && roll(chance("treasure_finder", "chance", 0.15D))) {
            rollFishingTreasure(player, event.getHook().getLocation(), "treasure_finder");
        }
        if (enchants.hasActiveEquipped(rod, "multi_catch") && event.getCaught() instanceof Item caught) {
            int extra = Math.max(0, integer("multi_catch", "extra-catches", 1));
            UUID caughtId = caught.getUniqueId();
            Bukkit.getScheduler().runTask(plugin, () -> {
                Entity entity = Bukkit.getEntity(caughtId);
                if (!(entity instanceof Item liveCaught) || !liveCaught.isValid()) return;
                ItemStack stack = liveCaught.getItemStack();
                stack.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() + extra));
                liveCaught.setItemStack(stack);
            });
            event.setExpToDrop(event.getExpToDrop() * Math.max(1, extra + 1));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onContentDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onStunnedAttack(EntityDamageByEntityEvent event) {
        if (isStunned(event.getDamager())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getNewEffect() == null) return;
        if (isTitan(player) && NEGATIVE_VANILLA_EFFECTS.contains(event.getNewEffect().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.isGliding()) {
            stopPrecisionFlight(player, "활공 종료");
        }
    }

    @EventHandler
    public void onPrecisionSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) stopPrecisionFlight(event.getPlayer(), "웅크리기");
    }

    // Legacy right-click interception is intentionally retained only as dead compatibility code.
    // The authoritative path is PlayerElytraBoostEvent -> ELYTRA_BOOST.
    private void onLegacyPrecisionFirework(PlayerInteractEvent event) {
        if (event.isCancelled() || event.getHand() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.isSneaking() || !player.isGliding()) return;
        ItemStack item = event.getItem();
        ItemStack chest = player.getInventory().getChestplate();
        if (item == null || item.getType() != Material.FIREWORK_ROCKET || chest == null
                || !enchants.hasActiveEquipped(chest, "precision_flight")) return;
        Damageable elytra = chest.getItemMeta() instanceof Damageable value ? value : null;
        if (elytra == null || chest.getType().getMaxDurability() - elytra.getDamage() <= 2) {
            player.sendActionBar(Component.text("겉날개 내구도가 부족합니다.", NamedTextColor.RED));
            return;
        }
        if (!consumeOneFromHand(player, event.getHand())) return;
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        activatePrecisionFlight(player, chest);
    }

    @EventHandler
    public void onHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack old = player.getInventory().getItem(event.getPreviousSlot());
        runtimeStates.clear(player, old);
        bowDrawStarted.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        runtimeStates.clear(event.getPlayer(), item);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        clearPlayer(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        clearTitanState(event.getPlayer(), true);
        scheduleTitanScaleSync(event.getPlayer(), 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        clearTitanState(event.getPlayer(), false);
        scheduleTitanScaleSync(event.getPlayer(), 1L);
        scheduleTitanScaleSync(event.getPlayer(), 10L);
        scheduleTitanScaleSync(event.getPlayer(), 40L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        clearPlayer(event.getPlayer());
    }

    /** Persistent equipment effects must not depend on PlayerMoveEvent firing. */
    private void tickPersistentEffects() {
        runtimeStates.clearExpired();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isTitan(player)) sanitizeInactiveTitanScale(player, false);
        }
        tickWindDrawGauge();
    }

    private void tickWindDrawGauge() {
        long now = System.currentTimeMillis();
        long tick = plugin.getServer().getCurrentTick();
        for (Map.Entry<UUID, Long> entry : new ArrayList<>(bowDrawStarted.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            ItemStack bow = player == null ? null : player.getInventory().getItemInMainHand();
            if (player == null || !player.isOnline() || bow == null || bow.getType() != Material.BOW
                    || !enchants.hasActiveEquipped(bow, "wind_arrow")
                    || !runtimeStates.has(player, bow, "wind_arrow")) {
                bowDrawStarted.remove(entry.getKey(), entry.getValue());
                continue;
            }
            int maximumTicks = boundedInteger("wind_arrow", "maximum-charge-ticks", 20, 1, 100);
            long elapsedTicks = Math.max(0L, (now - entry.getValue()) / 50L);
            if (elapsedTicks > maximumTicks + 20L) {
                bowDrawStarted.remove(entry.getKey(), entry.getValue());
                continue;
            }
            if ((tick & 1L) != 0L) continue;
            double progress = clamp(elapsedTicks / (double) maximumTicks, 0.0D, 1.0D);
            int filled = (int) Math.round(progress * 10.0D);
            String bar = "[" + "■".repeat(filled) + "□".repeat(10 - filled) + "]";
            String message = progress >= 1.0D
                    ? "Wind Arrow " + bar + " READY"
                    : "Wind Arrow " + bar + " " + Math.round(progress * 100.0D) + "%";
            player.sendActionBar(Component.text(message, progress >= 1.0D
                    ? NamedTextColor.AQUA : NamedTextColor.GRAY));
        }
    }

    private void tagProjectile(Projectile projectile, String enchantId, Player owner, ItemStack equipment) {
        projectile.getPersistentDataContainer().set(projectileEnchantKey, PersistentDataType.STRING, enchantId);
        projectile.getPersistentDataContainer().set(projectileOwnerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        projectileSourceItems.put(projectile.getUniqueId(), equipment.clone());
    }

    private void trackStraightProjectile(Projectile projectile, Vector direction, double speed, int lifetimeTicks) {
        UUID id = projectile.getUniqueId();
        Vector fixedDirection = direction.clone().normalize();
        BukkitTask tracker = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!projectile.isValid() || projectile.isDead()) {
                stopProjectileTask(id);
                projectileSourceItems.remove(id);
                return;
            }
            int age = projectile.getTicksLived();
            if (age >= lifetimeTicks) {
                stopProjectileTask(id);
                projectileSourceItems.remove(id);
                projectile.remove();
                return;
            }
            projectile.setGravity(false);
            projectile.setVelocity(fixedDirection.clone().multiply(speed));
        }, 1L, 1L);
        projectileTasks.put(id, tracker);
    }

    private String projectileEnchant(Projectile projectile) {
        return projectile.getPersistentDataContainer().getOrDefault(projectileEnchantKey, PersistentDataType.STRING, "");
    }

    private Player owner(Projectile projectile) {
        String raw = projectile.getPersistentDataContainer().get(projectileOwnerKey, PersistentDataType.STRING);
        try { return raw == null ? null : Bukkit.getPlayer(UUID.fromString(raw)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private void applyWindImpact(Player source, Location center, LivingEntity direct, String enchantId) {
        if (center == null || center.getWorld() == null) return;
        double radius = boundedNumber(enchantId, "impact-radius", 4.0D, 0.1D, 12.0D);
        double knockback = boundedNumber(enchantId, "impact-knockback", 1.8D, 0.0D, 8.0D);
        double directKnockback = boundedNumber(enchantId, "direct-knockback", 2.8D, 0.0D, 10.0D);
        int maximum = boundedInteger(enchantId, "maximum-targets", 20, 1, 20);
        Set<UUID> affected = new HashSet<>();
        if (direct != null && isWindTarget(source, direct) && affected.add(direct.getUniqueId())) {
            pushFrom(direct, center, directKnockback);
        }
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || !isWindTarget(source, target)
                    || affected.size() >= maximum || !affected.add(target.getUniqueId())) continue;
            pushFrom(target, center, knockback);
        }
        center.getWorld().spawnParticle(Particle.CLOUD, center,
                boundedInteger(enchantId, "impact-particle-count", 18, 0, 64),
                radius * .2D, radius * .2D, radius * .2D, .04D);
        center.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, .9F, 1.1F);
    }

    private boolean isWindTarget(Player source, LivingEntity target) {
        return target != source && !(target instanceof Player) && !target.isDead() && target.isValid();
    }

    private void pushFrom(LivingEntity target, Location center, double strength) {
        Vector push = target.getLocation().toVector().subtract(center.toVector());
        if (push.lengthSquared() < .0001D) push = new Vector(0.0D, .45D, 0.0D);
        else push.normalize().multiply(strength).setY(Math.max(.25D, strength * .25D));
        target.setVelocity(target.getVelocity().add(push));
    }

    private boolean isMaceTarget(Player player, LivingEntity target) {
        return target != player && !(target instanceof Player)
                && target.getType() != org.bukkit.entity.EntityType.ARMOR_STAND
                && !target.isDead() && target.isValid();
    }

    private void stopProjectileTask(UUID projectileId) {
        BukkitTask task = projectileTasks.remove(projectileId);
        if (task != null) task.cancel();
    }

    private void breakArea(Player player, ItemStack tool, Block origin, String enchantId) {
        int radius = Math.max(1, integer(enchantId, "radius", 1));
        BlockFace face = dominantFace(player.getEyeLocation().getDirection());
        for (Block block : plane(origin, face, radius)) {
            if (block.equals(origin) || !canExcavate(block, tool)) continue;
            String key = blockKey(block);
            if (!recursiveBlocks.add(key)) continue;
            try {
                BlockBreakEvent nested = new BlockBreakEvent(block, player);
                blockRewards.markSynthetic(nested);
                Bukkit.getPluginManager().callEvent(nested);
                if (nested.isCancelled()) continue;
                block.breakNaturally(tool, true);
                damageTool(tool, player);
            } finally {
                recursiveBlocks.remove(key);
            }
        }
    }

    private boolean tillArea(Player player, ItemStack hoe, String enchantId) {
        Block origin = player.getTargetBlockExact(Math.max(1, integer(enchantId, "target-range", 5)));
        if (origin == null) return false;
        int radius = Math.max(1, integer(enchantId, "radius", 1));
        boolean changed = false;
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            Block block = origin.getRelative(x, 0, z);
            if (!isTillable(block) || !block.getRelative(BlockFace.UP).isPassable()) continue;
            block.setType(Material.FARMLAND, false);
            damageTool(hoe, player);
            changed = true;
        }
        return changed;
    }

    private List<Block> plane(Block origin, BlockFace face, int radius) {
        List<Block> blocks = new ArrayList<>();
        for (int first = -radius; first <= radius; first++) for (int second = -radius; second <= radius; second++) {
            if (face == BlockFace.UP || face == BlockFace.DOWN) blocks.add(origin.getRelative(first, 0, second));
            else if (face == BlockFace.EAST || face == BlockFace.WEST) blocks.add(origin.getRelative(0, first, second));
            else blocks.add(origin.getRelative(first, second, 0));
        }
        return blocks;
    }

    private boolean canExcavate(Block block, ItemStack tool) {
        if (block.getType().isAir() || block.getState() instanceof Container) return false;
        String name = block.getType().name();
        if (name.contains("BEDROCK") || name.contains("BARRIER") || name.contains("PORTAL") || name.contains("COMMAND")) return false;
        return block.isPreferredTool(tool) || isHoe(tool);
    }

    private boolean isTillable(Block block) {
        return switch (block.getType()) {
            case DIRT, GRASS_BLOCK, DIRT_PATH, COARSE_DIRT, ROOTED_DIRT -> true;
            default -> false;
        };
    }

    private boolean isLog(Block block) {
        if (block == null) return false;
        String type = block.getType().name();
        return type.endsWith("_LOG") || type.endsWith("_WOOD") || type.endsWith("_STEM") || type.endsWith("_HYPHAE");
    }

    private void rollFishingTreasure(Player player, Location location, String enchantId) {
        var section = config.getEnchantsSection("enchants." + enchantId + ".settings.rewards");
        if (section == null) return;
        for (String itemId : section.getKeys(false)) {
            int amount = Math.max(1, section.getInt(itemId, 1));
            items.create(itemId, amount).ifPresent(stack -> location.getWorld().dropItemNaturally(location, stack));
            return;
        }
    }

    private void tickPrecisionSessions() {
        long tick = plugin.getServer().getCurrentTick();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PrecisionSession session = precisionSessions.get(player.getUniqueId());
            if (session != null) {
                ItemStack chest = player.getInventory().getChestplate();
                if (!player.isGliding() || player.isSneaking() || chest == null
                        || !enchants.hasActiveEquipped(chest, "precision_flight")
                        || !equipmentInstances.is(chest, session.equipmentId())) {
                    stopPrecisionFlight(player, player.isSneaking() ? "웅크리기" : "장비 또는 활공 상태 변경");
                    continue;
                }
                int level = Math.max(1, Math.min(3, getLevel(chest, "precision_flight")));
                long fireworkInterval = Math.max(1, integer("precision_flight", "firework-consume-interval-ticks", 200));
                if (tick - session.lastFuelTick >= fireworkInterval) {
                    if (!consumeOne(player.getInventory(), Material.FIREWORK_ROCKET)) {
                        stopPrecisionFlight(player, "폭죽 부족");
                        continue;
                    }
                    session.lastFuelTick = tick;
                }
                long durabilityInterval = Math.max(1, integer("precision_flight", "durability-consume-interval-ticks", 200));
                if (tick - session.lastDurabilityTick >= durabilityInterval) {
                    if (!consumePrecisionDurability(chest)) {
                        stopPrecisionFlight(player, "겉날개 내구도 한계");
                        continue;
                    }
                    session.lastDurabilityTick = tick;
                }
                int slot = player.getInventory().getHeldItemSlot() + 1;
                double targetSpeed = Math.max(0.0D, number("precision_flight", "hotbar-speed." + slot, 1.0D));
                double acceleration = clamp(number("precision_flight", "acceleration", .20D), .01D, 1.0D);
                Vector direction = player.getEyeLocation().getDirection().normalize();
                Vector targetVelocity = direction.multiply(targetSpeed);
                Vector next = player.getVelocity().clone().multiply(1.0D - acceleration)
                        .add(targetVelocity.multiply(acceleration));
                if (next.lengthSquared() > targetSpeed * targetSpeed && targetSpeed > 0.0D) {
                    next.normalize().multiply(targetSpeed);
                }
                player.setVelocity(next);
                player.sendActionBar(Component.text("정밀 비행 " + slot + "단계", NamedTextColor.AQUA));
            } else if (!player.isGliding()) {
                recoverPrecisionDebt(player, tick);
            }
        }
    }

    private void activatePrecisionFlight(Player player, ItemStack chest) {
        UUID equipmentId = equipmentInstances.ensure(chest);
        long tick = plugin.getServer().getCurrentTick();
        precisionSessions.put(player.getUniqueId(), new PrecisionSession(equipmentId, tick, tick, tick));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, .7F, 1.5F);
        player.sendActionBar(Component.text("정밀 비행 활성화", NamedTextColor.AQUA));
    }

    private void stopPrecisionFlight(Player player, String reason) {
        UUID id = player.getUniqueId();
        precisionSessions.remove(id);
        precisionLastDurabilityTick.remove(id);
        if (player.isOnline() && bool("precision_flight", "debug", false)) {
            plugin.getLogger().info("precision_flight stopped player=" + id + " reason=" + reason);
        }
    }

    private boolean consumePrecisionDurability(ItemStack chest) {
        if (!(chest.getItemMeta() instanceof Damageable damageable)) return false;
        int maximum = chest.getType().getMaxDurability();
        int currentDamage = damageable.getDamage();
        if (maximum - currentDamage <= 2) return false;
        int unbreaking = chest.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreaking > 0 && ThreadLocalRandom.current().nextInt(unbreaking + 1) > 0) return true;
        damageable.setDamage(Math.min(maximum - 2, currentDamage + 1));
        int debt = damageable.getPersistentDataContainer().getOrDefault(
                precisionDebtKey, PersistentDataType.INTEGER, 0);
        damageable.getPersistentDataContainer().set(precisionDebtKey, PersistentDataType.INTEGER, debt + 1);
        chest.setItemMeta(damageable);
        return true;
    }

    private void recoverPrecisionDebt(Player player, long tick) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || !enchants.hasActiveEquipped(chest, "precision_flight")) return;
        UUID equipmentId = equipmentInstances.ensure(chest);
        int debt = chest.getItemMeta() == null ? 0 : chest.getItemMeta().getPersistentDataContainer()
                .getOrDefault(precisionDebtKey, PersistentDataType.INTEGER, 0);
        if (debt <= 0) return;
        long last = precisionLastRecoveryTick.getOrDefault(player.getUniqueId(), 0L);
        int level = Math.max(1, Math.min(3, getLevel(chest, "precision_flight")));
        int configured = integer("precision_flight", "recovery-interval-ticks-level-" + level, 120);
        if (tick - last < Math.max(1, configured)) return;
        if (!equipmentInstances.is(chest, equipmentId) || !(chest.getItemMeta() instanceof Damageable damageable)
                || damageable.getDamage() <= 0) return;
        damageable.setDamage(damageable.getDamage() - 1);
        damageable.getPersistentDataContainer().set(precisionDebtKey, PersistentDataType.INTEGER, debt - 1);
        chest.setItemMeta(damageable);
        precisionLastRecoveryTick.put(player.getUniqueId(), tick);
    }

    private boolean hasItemInHand(Player player, EquipmentSlot hand, Material material) {
        ItemStack stack = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();
        return stack != null && stack.getType() == material && stack.getAmount() > 0;
    }

    private void debugPrecisionBoost(PlayerElytraBoostEvent event, Player player, ItemStack chest, String result) {
        if (!bool("precision_flight", "debug", false)) return;
        String active = chest == null ? "[]" : enchants.getActiveEquipped(chest).toString();
        UUID equipmentId = chest == null ? null : equipmentInstances.ensure(chest);
        int remaining = chest == null || !(chest.getItemMeta() instanceof Damageable damageable)
                ? -1 : chest.getType().getMaxDurability() - damageable.getDamage();
        ItemStack firework = event.getItemStack();
        plugin.getLogger().info("precision_flight event=" + result
                + " cancelled=" + event.isCancelled()
                + " gliding=" + player.isGliding()
                + " chest=" + (chest == null ? "null" : chest.getType())
                + " active=" + active
                + " equipment=" + equipmentId
                + " remaining-durability=" + remaining
                + " hand=" + event.getHand()
                + " firework-amount=" + (firework == null ? 0 : firework.getAmount())
                + " session=" + precisionSessions.containsKey(player.getUniqueId()));
    }

    private boolean consumeOneFromHand(Player player, EquipmentSlot hand) {
        ItemStack stack = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) return false;
        stack.setAmount(stack.getAmount() - 1);
        if (hand == EquipmentSlot.OFF_HAND) player.getInventory().setItemInOffHand(stack);
        else player.getInventory().setItemInMainHand(stack);
        return true;
    }

    private void stun(LivingEntity target, int ticks) {
        if (ticks <= 0 || target.isDead()) return;
        long until = System.currentTimeMillis() + ticks * 50L;
        StunState prior = stunned.remove(target.getUniqueId());
        if (prior != null) prior.cancel();
        Location anchor = target.getLocation().clone();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            StunState state = stunned.get(target.getUniqueId());
            if (state == null || !state.isValid() || target.isDead()) {
                StunState removed = stunned.remove(target.getUniqueId());
                if (removed != null) removed.cancel();
                return;
            }
            if (target.getWorld().equals(state.anchor().getWorld())
                    && target.getLocation().distanceSquared(state.anchor()) > .0001D) {
                target.teleport(state.anchor());
            }
            target.setVelocity(new Vector());
        }, 0L, 1L);
        stunned.put(target.getUniqueId(), new StunState(anchor, until, task));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 255, false, true, true));
    }

    private int adjustedStunTicks(LivingEntity target, int baseTicks, String enchantId) {
        double multiplier = isBoss(target) ? number(enchantId, "boss-stun-duration-multiplier", .25D) : 1.0D;
        return Math.max(0, (int) Math.round(Math.max(0, baseTicks) * multiplier));
    }

    private boolean isBoss(LivingEntity target) {
        return target.getType() == org.bukkit.entity.EntityType.ENDER_DRAGON
                || target.getType() == org.bukkit.entity.EntityType.WITHER
                || target.getType() == org.bukkit.entity.EntityType.WARDEN;
    }

    private Player attackingPlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    private LivingEntity attacker(Entity entity) {
        if (entity instanceof LivingEntity living) return living;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) return living;
        return null;
    }

    private boolean isStunned(Entity entity) {
        StunState state = entity == null ? null : stunned.get(entity.getUniqueId());
        if (state == null) return false;
        if (state.isValid()) return true;
        stunned.remove(entity.getUniqueId());
        state.cancel();
        return false;
    }

    private boolean isTitan(Player player) {
        return isActiveForEquipment(player, titans);
    }

    private boolean isActiveForEquipment(Player player, Map<UUID, ActiveState> states) {
        ActiveState state = states.get(player.getUniqueId());
        if (state == null) return false;
        if (state.isValid() && holdsInstance(player, state.equipmentId())) return true;
        states.remove(player.getUniqueId());
        runtimeStates.clearEquipment(player, state.equipmentId());
        return false;
    }

    private void clearPlayer(Player player) {
        runtimeStates.clearPlayer(player);
        clearTitanState(player, true);
        bowDrawStarted.remove(player.getUniqueId());
        precisionLastDurabilityTick.remove(player.getUniqueId());
        precisionLastRecoveryTick.remove(player.getUniqueId());
        precisionSessions.remove(player.getUniqueId());
        swordmasterBlades.clearPlayer(player);
    }

    private void clearTitanState(Player player, boolean forceClientSync) {
        if (player == null) return;
        titans.remove(player.getUniqueId());
        sanitizeInactiveTitanScale(player, forceClientSync);
    }

    private void scheduleTitanScaleSync(Player player, long delayTicks) {
        if (player == null || !plugin.isEnabled()) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !isTitan(player)) sanitizeInactiveTitanScale(player, true);
        }, delayTicks);
    }

    private void sanitizeInactiveTitanScale(Player player, boolean forceClientSync) {
        AttributeInstance scale = player.getAttribute(Attribute.SCALE);
        if (scale == null) return;
        removeTitanScaleModifiers(player, scale);
        scale.setBaseValue(DEFAULT_PLAYER_SCALE);
    }

    private void removeTitanScaleModifiers(Player player, AttributeInstance scale) {
        UUID expectedId = UUID.nameUUIDFromBytes(
                (TITANS_WRATH_MODIFIER + ':' + player.getUniqueId()).getBytes(StandardCharsets.UTF_8));
        scale.getModifiers().stream()
                .filter(modifier -> expectedId.equals(modifier.getUniqueId()) || isHyunseoScaleModifier(modifier))
                .toList()
                .forEach(scale::removeModifier);
    }

    private boolean isHyunseoScaleModifier(AttributeModifier modifier) {
        String name = modifier.getName().toLowerCase(Locale.ROOT);
        return name.equals(TITANS_WRATH_MODIFIER)
                || name.contains("titans_wrath")
                || name.contains("titan")
                || name.startsWith("hyunseorpg");
    }

    private boolean holdsInstance(Player player, UUID id) {
        return equipmentInstances.is(player.getInventory().getItemInMainHand(), id)
                || equipmentInstances.is(player.getInventory().getItemInOffHand(), id)
                || equipmentInstances.is(player.getInventory().getChestplate(), id);
    }

    private ItemStack findItemWith(Player player, String enchantId, EquipmentTierService.Category category) {
        List<ItemStack> candidates = new ArrayList<>();
        candidates.add(player.getInventory().getItemInMainHand());
        candidates.add(player.getInventory().getItemInOffHand());
        for (ItemStack armor : player.getInventory().getArmorContents()) candidates.add(armor);
        for (ItemStack item : candidates) {
            if (item == null || item.getType().isAir()) continue;
            if (category != null && tiers.getCategory(item) != category) continue;
            if (enchants.hasActiveEquipped(item, enchantId)) return item;
        }
        return null;
    }

    private int getLevel(ItemStack item, String enchantId) { return enchants.getEnchantLevel(item, enchantId); }

    private int amplifierForLevel(String enchantId, int level) {
        int perLevel = Math.max(1, integer(enchantId, "effect-amplifier-per-level", 1));
        return Math.max(0, Math.max(1, level) * perLevel - 1);
    }

    private double protectionReduction(ItemStack item, String enchantId) {
        int level = getLevel(item, enchantId);
        return level <= 0 ? 0.0D : Math.max(0.0D, number(enchantId, "reduction-per-level", .04D) * level);
    }

    private boolean isHoe(ItemStack item) { return item != null && item.getType().name().endsWith("_HOE"); }
    private boolean isBoots(ItemStack item) { return item != null && item.getType().name().endsWith("_BOOTS"); }

    private Material seedFor(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            default -> null;
        };
    }

    private Material smeltResult(String enchantId, Material block) {
        String raw = string(enchantId, "smelt-results." + block.name(), "");
        return raw.isBlank() ? null : Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
    }

    private boolean hasSilkTouch(ItemStack item) {
        return item != null && item.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH);
    }

    private boolean consumeOne(PlayerInventory inventory, Material material) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() != material || item.getAmount() <= 0) continue;
            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() <= 0) inventory.setItem(slot, null);
            return true;
        }
        return false;
    }

    private void damageTool(ItemStack tool, Player player) {
        ItemStack target = mutableTool(player, tool);
        if (target == null || target.getItemMeta() == null || target.getItemMeta().isUnbreakable()) return;
        if (shouldPreserveManualDurability(target)) return;
        if (!(target.getItemMeta() instanceof Damageable damageable)) return;
        int next = damageable.getDamage() + 1;
        if (next >= target.getType().getMaxDurability()) return;
        damageable.setDamage(next);
        target.setItemMeta(damageable);
    }

    private ItemStack mutableTool(Player player, ItemStack source) {
        if (player == null || source == null || source.getType().isAir()) return null;
        UUID sourceId = equipmentInstances.get(source).orElse(null);
        if (sourceId != null) {
            ItemStack[] contents = player.getInventory().getContents();
            for (ItemStack item : contents) {
                if (equipmentInstances.is(item, sourceId)) return item;
            }
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && main.getType() == source.getType()) return main;
        ItemStack off = player.getInventory().getItemInOffHand();
        return off != null && off.getType() == source.getType() ? off : source;
    }

    private boolean shouldPreserveManualDurability(ItemStack tool) {
        int vanillaUnbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (vanillaUnbreaking > 0 && ThreadLocalRandom.current().nextInt(vanillaUnbreaking + 1) > 0) {
            return true;
        }
        return false;
    }

    private void addModifier(Player player, Attribute attribute, String name, double amount) {
        updateModifier(player, attribute, name, amount, AttributeModifier.Operation.ADD_NUMBER);
    }

    private void updateModifier(Player player, Attribute attribute, String name, double amount,
                                AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        UUID id = UUID.nameUUIDFromBytes((name + ':' + player.getUniqueId()).getBytes(StandardCharsets.UTF_8));
        List<AttributeModifier> existing = instance.getModifiers().stream()
                .filter(modifier -> modifier.getName().equals(name) || modifier.getUniqueId().equals(id))
                .toList();
        if (existing.size() == 1 && existing.get(0).getOperation() == operation
                && Math.abs(existing.get(0).getAmount() - amount) < .000001D) return;
        existing.forEach(instance::removeModifier);
        if (Math.abs(amount) < .000001D) return;
        instance.addModifier(new AttributeModifier(id, name, amount, operation));
    }

    private void removeModifier(Player player, Attribute attribute, String name) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.getModifiers().stream().filter(modifier -> modifier.getName().equals(name)).toList().forEach(instance::removeModifier);
    }

    private BlockFace dominantFace(Vector direction) {
        double x = Math.abs(direction.getX()), y = Math.abs(direction.getY()), z = Math.abs(direction.getZ());
        if (y >= x && y >= z) return direction.getY() >= 0 ? BlockFace.UP : BlockFace.DOWN;
        if (x >= z) return direction.getX() >= 0 ? BlockFace.EAST : BlockFace.WEST;
        return direction.getZ() >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private String blockKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private <T extends Entity> T entity(UUID id, Class<T> type) {
        Entity entity = Bukkit.getEntity(id);
        return type.isInstance(entity) ? type.cast(entity) : null;
    }

    private double number(String enchantId, String key, double fallback) {
        double value = config.getEnchantsDouble("enchants." + enchantId + ".settings." + key, fallback);
        return Double.isFinite(value) ? value : fallback;
    }

    private int integer(String enchantId, String key, int fallback) {
        return config.getEnchantsInt("enchants." + enchantId + ".settings." + key, fallback);
    }

    private double boundedNumber(String enchantId, String key, double fallback, double minimum, double maximum) {
        double value = number(enchantId, key, fallback);
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            String warning = enchantId + "." + key;
            if (warnedInvalidSettings.add(warning)) {
                plugin.getLogger().warning("Invalid enchant setting " + warning + "; using bounded fallback.");
            }
            return clamp(fallback, minimum, maximum);
        }
        return value;
    }

    private int boundedInteger(String enchantId, String key, int fallback, int minimum, int maximum) {
        int value = integer(enchantId, key, fallback);
        if (value < minimum || value > maximum) {
            String warning = enchantId + "." + key;
            if (warnedInvalidSettings.add(warning)) {
                plugin.getLogger().warning("Invalid enchant setting " + warning + "; using bounded fallback.");
            }
            return Math.max(minimum, Math.min(maximum, fallback));
        }
        return value;
    }

    private void debugMaceTiming(String enchantId, long startedAt, int affected, int scheduledCenters) {
        if (!bool(enchantId, "debug-timing", false)) return;
        plugin.getLogger().info("Explosive mace: " + ((System.nanoTime() - startedAt) / 1_000_000.0D)
                + "ms, affected=" + affected + ", chain-centers=" + scheduledCenters);
    }

    private boolean bool(String enchantId, String key, boolean fallback) {
        return config.getEnchantsBoolean("enchants." + enchantId + ".settings." + key, fallback);
    }

    private String string(String enchantId, String key, String fallback) {
        return config.getEnchantsString("enchants." + enchantId + ".settings." + key, fallback);
    }

    private List<String> stringList(String enchantId, String key) {
        return config.getEnchantsStringList("enchants." + enchantId + ".settings." + key).stream()
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .toList();
    }

    private double chance(String enchantId, String key, double fallback) { return clamp(number(enchantId, key, fallback), 0.0D, 1.0D); }
    private boolean roll(double chance) { return ThreadLocalRandom.current().nextDouble() <= chance; }
    private double clamp(double value, double minimum, double maximum) { return Math.max(minimum, Math.min(maximum, value)); }

    private record ActiveState(UUID equipmentId, long expiresAtMillis) {
        boolean isValid() { return System.currentTimeMillis() < expiresAtMillis; }
    }

    private record StunState(Location anchor, long expiresAtMillis, BukkitTask task) {
        boolean isValid() { return System.currentTimeMillis() < expiresAtMillis; }
        void cancel() { if (task != null) task.cancel(); }
    }

    private static final class PrecisionSession {
        private final UUID equipmentId;
        private final long activationTick;
        private long lastFuelTick;
        private long lastDurabilityTick;

        private PrecisionSession(UUID equipmentId, long activationTick, long lastFuelTick, long lastDurabilityTick) {
            this.equipmentId = equipmentId;
            this.activationTick = activationTick;
            this.lastFuelTick = lastFuelTick;
            this.lastDurabilityTick = lastDurabilityTick;
        }

        private UUID equipmentId() { return equipmentId; }
    }

}
