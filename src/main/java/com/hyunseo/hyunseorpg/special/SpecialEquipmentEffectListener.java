package com.hyunseo.hyunseorpg.special;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentPromotionService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.scheduler.BukkitTask;
import java.util.concurrent.ThreadLocalRandom;

/** Runtime effects for the explicitly defined late-game equipment set. */
public final class SpecialEquipmentEffectListener implements Listener {
    private final ConfigService config;
    private final SpecialEquipmentService specials;
    private final RPGItemService items;
    private final CombatService combat;
    private final CooldownService cooldowns;
    private final EquipmentPromotionService promotion;
    private final FireElementDamageUtil fireDamage;
    private final NamespacedKey projectileSpecialKey;
    private final NamespacedKey projectileAbilityKey;
    private final NamespacedKey empoweredProjectileKey;
    private final NamespacedKey poseidonModeKey;
    private final Map<UUID, Map<UUID, StackState>> fireStacks = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, StackState>> iceStacks = new ConcurrentHashMap<>();
    private final Set<UUID> internalDamage = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastFlameCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastFlamePenalty = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> homingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> projectileOwners = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> ownedProjectiles = new ConcurrentHashMap<>();
    private final Map<UUID, Long> empoweredShots = new ConcurrentHashMap<>();
    private final Map<UUID, Set<BukkitTask>> fireTasks = new ConcurrentHashMap<>();
    private static final UUID LAST_FLAME_HEALTH_MODIFIER = UUID.fromString("f7de7f06-6d0f-4d6a-a1b3-11d8e68a3a30");

    public SpecialEquipmentEffectListener(ConfigService config, SpecialEquipmentService specials, RPGItemService items,
                                          CombatService combat, CooldownService cooldowns,
                                          EquipmentPromotionService promotion) {
        this.config = config;
        this.specials = specials;
        this.items = items;
        this.combat = combat;
        this.cooldowns = cooldowns;
        this.promotion = promotion;
        this.fireDamage = new FireElementDamageUtil(promotion);
        this.projectileSpecialKey = new NamespacedKey(config.getPlugin(), "special_projectile_equipment");
        this.projectileAbilityKey = new NamespacedKey(config.getPlugin(), "special_projectile_ability");
        this.empoweredProjectileKey = new NamespacedKey(config.getPlugin(), "special_projectile_empowered");
        this.poseidonModeKey = new NamespacedKey(config.getPlugin(), "poseidon_mode");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        String projectileId = projectileSpecial(event.getDamager());
        if (projectileId.equals("fireball_consumable")) {
            event.setCancelled(true);
            return;
        }
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null || internalDamage.contains(attacker.getUniqueId())) return;
        Long penaltyUntil = lastFlamePenalty.get(attacker.getUniqueId());
        if (penaltyUntil != null && penaltyUntil > System.currentTimeMillis()) {
            event.setDamage(event.getDamage() * value("burning_sword", "abilities.last-flame.penalty.outgoing-damage-multiplier", 0.50D));
        }
        String specialId = projectileSpecial(event.getDamager());
        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (specialId.isBlank()) specialId = specials.getSpecialId(item);
        if (specialId.isBlank()) return;
        LivingEntity target = event.getEntity() instanceof LivingEntity living ? living : null;
        if (target == null) return;

        switch (specialId) {
            case "burning_sword" -> burningSword(event, attacker, target, specialId);
            case "flowing_water_sword" -> { }
            case "wind_cutting_sword" -> windSlash(event, attacker, target, specialId);
            case "earth_special_sword" -> earthSword(event, target, specialId);
            case "ice_special_sword" -> iceHit(event, attacker, target, specialId);
            case "dark_energy_sword" -> darkSword(event, attacker, target, specialId);
            case "burning_bow" -> burningBow(event, attacker, target, specialId);
            case "earth_heavy_bow" -> earthBow(event, target, specialId);
            case "freezing_bow" -> iceHit(event, attacker, target, specialId);
            case "dark_energy_bow" -> darkBow(event, target, specialId);
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIncomingDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!items.isItem(player.getInventory().getItemInMainHand(), "burning_sword")) return;
        if (!boolValue("burning_sword", "abilities.last-flame.enabled", true)) return;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (lastFlameCooldown.getOrDefault(uuid, 0L) > now) return;
        Bukkit.getScheduler().runTask(config.getPlugin(), () -> {
            if (!player.isOnline() || player.isDead()) return;
            double max = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null ? 20.0D
                    : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            double ratio = value("burning_sword", "abilities.last-flame.trigger-health-ratio", 0.20D);
            if (player.getHealth() > max * ratio) return;
            long cooldown = Math.max(1L, intValue("burning_sword", "abilities.last-flame.cooldown-ticks", 2400)) * 50L;
            lastFlameCooldown.put(uuid, System.currentTimeMillis() + cooldown);
            int invulnerability = intValue("burning_sword", "abilities.last-flame.invulnerability-duration-ticks", 40);
            player.setNoDamageTicks(Math.max(player.getNoDamageTicks(), invulnerability));
            org.bukkit.attribute.AttributeInstance attribute = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (attribute == null) return;
            removeLastFlameModifier(attribute);
            attribute.addTransientModifier(new org.bukkit.attribute.AttributeModifier(
                    LAST_FLAME_HEALTH_MODIFIER, "hyunseorpg_last_flame_health", .20D,
                    org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            double temporaryMax = attribute.getValue();
            player.setHealth(temporaryMax);
            long duration = Math.max(1L, intValue("burning_sword", "abilities.last-flame.penalty.duration-ticks", 2400));
            lastFlamePenalty.put(uuid, System.currentTimeMillis() + duration * 50L);
            Bukkit.getScheduler().runTaskLater(config.getPlugin(), () -> {
                removeLastFlameModifier(attribute);
                lastFlamePenalty.remove(uuid);
                if (player.isOnline() && player.getHealth() > attribute.getValue()) player.setHealth(attribute.getValue());
            }, duration);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        if (!items.isItem(player.getInventory().getItemInMainHand(), "flowing_water_sword")) return;
        double ratio = value("flowing_water_sword", "abilities.on-kill.heal-ratio", 0.05D);
        double heal = Math.max(0.0D, player.getHealth() * ratio);
        double max = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                ? 20.0D : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(max, player.getHealth() + heal));
        int duration = intValue("flowing_water_sword", "abilities.on-kill.speed-duration-ticks", 100);
        int amplifier = Math.max(0, intValue("flowing_water_sword", "abilities.on-kill.speed-amplifier", 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier, false, true, true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        String id = specials.getSpecialId(item);
        if (id.isBlank()) return;

        if (id.equals("poseidons_spear") && event.getPlayer().isSneaking()) {
            togglePoseidonMode(event.getPlayer(), item);
            event.setCancelled(true);
            return;
        }
        if (id.equals("fireball_consumable")) {
            if (!startCooldown(event.getPlayer(), item, id, "active-1", "abilities.active-1.cooldown-seconds")) {
                event.setCancelled(true);
                return;
            }
            SmallFireball fireball = event.getPlayer().launchProjectile(SmallFireball.class);
            fireball.getPersistentDataContainer().set(projectileSpecialKey, PersistentDataType.STRING, id);
            fireball.getPersistentDataContainer().set(projectileAbilityKey, PersistentDataType.STRING, "active-1");
            fireball.setVelocity(event.getPlayer().getEyeLocation().getDirection().normalize()
                    .multiply(Math.max(0.1D, value(id, "abilities.active-1.projectile-speed", 1.2D))));
            if (boolValue(id, "abilities.active-1.consume-on-use", true)) consumeOne(item, event.getPlayer());
            event.setCancelled(true);
            return;
        }
        if (id.equals("spirit_of_flame")) {
            if (!boolValue(id, "abilities.active-1.enabled", true)) return;
            Player player = event.getPlayer();
            boolean activated = fireDamage.activateBuff(player,
                    Math.max(1L, intValue(id, "abilities.active-1.duration-seconds", 60)) * 1000L,
                    Math.max(0.0D, value(id, "abilities.active-1.fire-damage-bonus-percent", 0.15D)),
                    boolValue(id, "abilities.active-1.allow-refresh", true),
                    boolValue(id, "abilities.active-1.allow-stack", false));
            if (!activated) {
                player.sendActionBar(Component.text("불꽃의 정신 효과가 이미 적용 중입니다.", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,
                    Math.max(1, intValue(id, "abilities.active-1.duration-seconds", 60) * 20),
                    Math.max(0, intValue(id, "abilities.active-1.fire-resistance-amplifier", 0)), false, true, true));
            if (boolValue(id, "abilities.active-1.consume-on-use", true)) consumeOne(item, player);
            player.sendActionBar(Component.text("불꽃의 정신이 적용되었습니다.", NamedTextColor.GOLD));
            event.setCancelled(true);
            return;
        }
        if (id.equals("burning_sword")) {
            activateFireSlash(event.getPlayer(), item);
            event.setCancelled(true);
            return;
        }
        if (id.equals("burning_bow") && event.getPlayer().isSneaking()) {
            armCondensedFlameArrow(event.getPlayer());
            event.setCancelled(true);
            return;
        }
        if (id.equals("breath_of_poseidon")) {
            applyConfiguredEffect(event.getPlayer(), PotionEffectType.WATER_BREATHING, id, "abilities.water-breathing");
            applyConfiguredEffect(event.getPlayer(), PotionEffectType.DOLPHINS_GRACE, id, "abilities.dolphins-grace");
            applyConfiguredEffect(event.getPlayer(), PotionEffectType.CONDUIT_POWER, id, "abilities.conduit-power");
            consumeOne(item, event.getPlayer());
            event.setCancelled(true);
            return;
        }
        if (id.equals("blessing_of_earth")) {
            applyConfiguredEffect(event.getPlayer(), PotionEffectType.HASTE, id, "abilities.haste");
            consumeOne(item, event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSpecialDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().isSneaking()) return;
        ItemStack item = event.getItemDrop().getItemStack();
        String id = specials.getSpecialId(item);
        if (id.equals("burning_sword")) {
            event.setCancelled(true);
            activateHeatWave(event.getPlayer());
        } else if (id.equals("burning_bow")) {
            event.setCancelled(true);
            activateFireRain(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        String id = specials.getSpecialId(event.getBow());
        if (id.isBlank()) return;
        if (!(event.getProjectile() instanceof Projectile projectile)) return;
        projectile.getPersistentDataContainer().set(projectileSpecialKey, PersistentDataType.STRING, id);
        Long armedUntil = empoweredShots.get(player.getUniqueId());
        if (id.equals("burning_bow") && armedUntil != null && armedUntil > System.currentTimeMillis()) {
            empoweredShots.remove(player.getUniqueId());
            projectile.getPersistentDataContainer().set(projectileAbilityKey, PersistentDataType.STRING, "active-1");
            projectile.getPersistentDataContainer().set(empoweredProjectileKey, PersistentDataType.BYTE, (byte) 1);
        } else if (id.equals("burning_bow")) {
            empoweredShots.remove(player.getUniqueId());
        }
        if (id.equals("wind_archers_bow")) startHoming(player, projectile);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        String specialId = projectileSpecial(event.getEntity());
        BukkitTask task = homingTasks.remove(event.getEntity().getUniqueId());
        if (task != null) task.cancel();
        forgetProjectile(event.getEntity().getUniqueId());
        if (specialId.isBlank()) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        Location impact = event.getHitEntity() instanceof LivingEntity target
                ? target.getLocation() : event.getEntity().getLocation();
        if (specialId.equals("fireball_consumable")) {
            applyFireballExplosion(player, impact, specialId);
            if (event.getHitEntity() instanceof LivingEntity target) {
                internalDamage.add(player.getUniqueId());
                try { combat.applySkillDamage(player, target, value(specialId, "abilities.active-1.direct-damage", 3.0D)); }
                finally { internalDamage.remove(player.getUniqueId()); }
            }
            playFireImpact(impact, specialId, "active-1");
            event.getEntity().remove();
        } else if (specialId.equals("burning_bow")
                && event.getEntity().getPersistentDataContainer().has(empoweredProjectileKey, PersistentDataType.BYTE)) {
            applyFireAreaDamage(player, impact, value(specialId, "abilities.active-1.impact-radius", 2.0D),
                    value(specialId, "abilities.active-1.impact-damage", 5.0D),
                    intValue(specialId, "abilities.active-1.fire-ticks", 80), specialId, "active-1");
            playFireImpact(impact, specialId, "active-1");
        } else if (specialId.equals("burning_bow") && event.getHitEntity() instanceof LivingEntity target) {
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 8, .25, .35, .25, .02);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSpecialExplosion(EntityExplodeEvent event) {
        if (event.getEntity() instanceof SmallFireball fireball
                && projectileSpecial(fireball).equals("fireball_consumable")) {
            event.blockList().clear();
            event.setYield(0.0F);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSpecialBlockIgnite(BlockIgniteEvent event) {
        if (event.getIgnitingEntity() instanceof SmallFireball fireball
                && projectileSpecial(fireball).equals("fireball_consumable")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpecialItemDamage(PlayerItemDamageEvent event) {
        SpecialEquipmentData data = specials.getData(event.getItem());
        if (data == null) return;
        String type = data.equipmentType().toUpperCase(java.util.Locale.ROOT);
        if (!type.equals("CONSUMABLE") && !type.equals("BUFF") && !type.equals("UNKNOWN")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { clearPlayerState(event.getPlayer()); }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) { clearPlayerState(event.getPlayer()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { clearPlayerState(event.getPlayer()); }

    @EventHandler
    public void onKick(PlayerKickEvent event) { clearPlayerState(event.getPlayer()); }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) { clearPlayerState(event.getEntity()); }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack previous = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack next = player.getInventory().getItem(event.getNewSlot());
        if (!specials.getSpecialId(previous).equalsIgnoreCase(specials.getSpecialId(next))) {
            clearPlayerState(player);
        }
        cleanupExpired(player.getUniqueId());
    }

    private void burningSword(EntityDamageByEntityEvent event, Player player, LivingEntity target, String id) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!boolValue(id, "abilities.passive.enabled", true)) return;
        if (ThreadLocalRandom.current().nextDouble() > Math.max(0.0D,
                Math.min(1.0D, value(id, "abilities.passive.activation-chance", 1.0D)))) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        double extra = fireDamage.boost(player, item, value(id, "abilities.passive.extra-damage", 1.0D));
        event.setDamage(event.getDamage() + extra);
        target.setFireTicks(Math.max(target.getFireTicks(), fireTicks(player, id,
                intValue(id, "abilities.passive.fire-ticks", intValue(id, "abilities.ignite-duration-ticks", 80)))));
        if (boolValue(id, "abilities.passive.particle-enabled", true)) {
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 6, .2, .3, .2, .02);
        }
        if (boolValue(id, "abilities.passive.sound-enabled", true)) {
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_FIRECHARGE_USE, .55F, 1.2F);
        }
    }

    private void windSlash(EntityDamageByEntityEvent event, Player player, LivingEntity primary, String id) {
        if (event.getDamager() instanceof Projectile) return;
        double range = value(id, "abilities.slash.range", 7.0D);
        double damage = value(id, "abilities.slash.damage", 1.0D);
        Set<UUID> hit = new HashSet<>();
        hit.add(primary.getUniqueId());
        Location start = player.getEyeLocation();
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, start, 3, .15, .15, .15, .05);
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity living) || living == player || hit.contains(living.getUniqueId())) continue;
            if (start.getDirection().normalize().dot(living.getLocation().add(0, 1, 0).subtract(start).toVector().normalize()) < .75) continue;
            hit.add(living.getUniqueId());
            internalDamage.add(player.getUniqueId());
            try { living.damage(damage, player); } finally { internalDamage.remove(player.getUniqueId()); }
        }
    }

    private void earthSword(EntityDamageByEntityEvent event, LivingEntity target, String id) {
        int duration = intValue(id, "abilities.on-hit.slowness.duration-ticks", 40);
        int amplifier = intValue(id, "abilities.on-hit.slowness.amplifier", 1);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amplifier, false, true, true));
        if (boolValue(id, "abilities.on-hit.knockback.enabled", true)) {
            Vector direction = target.getLocation().toVector().subtract(event.getDamager().getLocation().toVector()).normalize();
            target.setVelocity(target.getVelocity().add(direction.multiply(value(id, "abilities.on-hit.knockback.strength", .35D))));
        }
    }

    private void iceHit(EntityDamageByEntityEvent event, Player player, LivingEntity target, String id) {
        int maximum = Math.max(1, intValue(id, "abilities.ice-stacks.maximum", 5));
        long duration = Math.max(1L, intValue(id, "abilities.ice-stacks.duration-ticks", 80));
        Map<UUID, StackState> states = iceStacks.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>());
        StackState old = states.get(target.getUniqueId());
        int stack = old == null || old.expiresAt() < System.currentTimeMillis() ? 1 : Math.min(maximum, old.stack() + 1);
        states.put(target.getUniqueId(), new StackState(stack, System.currentTimeMillis() + duration * 50L));
        int slowDuration = intValue(id, "abilities.ice-stacks.slowness-duration-ticks", 40);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, Math.max(0, stack - 1), false, true, true));
        if (stack >= maximum && !target.isInvulnerable()) {
            if (target.getType().name().contains("BOSS") || target.getType().name().equals("ENDER_DRAGON")) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, maximum, false, true, true));
            } else {
                target.setFreezeTicks(Math.max(target.getFreezeTicks(), intValue(id, "abilities.ice-stacks.freeze-ticks", 60)));
            }
            states.remove(target.getUniqueId());
        }
    }

    private void darkSword(EntityDamageByEntityEvent event, Player player, LivingEntity target, String id) {
        double ratio = value(id, "abilities.life-steal.damage-ratio", .05D);
        double max = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null ? 20.0D
                : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(max, player.getHealth() + Math.max(0.0D, event.getFinalDamage() * ratio)));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,
                intValue(id, "abilities.weakness.duration-ticks", 60),
                intValue(id, "abilities.weakness.amplifier", 0), false, true, true));
    }

    private void burningBow(EntityDamageByEntityEvent event, Player player, LivingEntity target, String id) {
        if (!(event.getDamager() instanceof Projectile)) return;
        if (!boolValue(id, "abilities.passive.enabled", true)) return;
        if (ThreadLocalRandom.current().nextDouble() > Math.max(0.0D,
                Math.min(1.0D, value(id, "abilities.passive.activation-chance", 1.0D)))) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        event.setDamage(event.getDamage() + fireDamage.boost(player, item,
                value(id, "abilities.passive.extra-damage", 1.0D)));
        boolean alreadyBurning = target.getFireTicks() > 0;
        target.setFireTicks(Math.max(target.getFireTicks(), fireTicks(player, id,
                intValue(id, "abilities.passive.fire-ticks", intValue(id, "abilities.ignite-duration-ticks", 80)))));
        if (boolValue(id, "abilities.passive.particle-trail-enabled", true)) {
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 8, .25, .35, .25, .02);
        }
        if (alreadyBurning && boolValue(id, "abilities.explosion.enabled", false)) {
            internalDamage.add(player.getUniqueId());
            try {
                target.getWorld().createExplosion(target.getLocation(), (float) value(id, "abilities.explosion.radius", 1.5D), false, false, player);
            } finally {
                internalDamage.remove(player.getUniqueId());
            }
        }
    }

    private void earthBow(EntityDamageByEntityEvent event, LivingEntity target, String id) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                intValue(id, "abilities.slowness.duration-ticks", 60),
                intValue(id, "abilities.slowness.amplifier", 1), false, true, true));
        Vector direction = target.getLocation().toVector().subtract(event.getDamager().getLocation().toVector()).normalize();
        target.setVelocity(target.getVelocity().add(direction.multiply(value(id, "abilities.knockback", .45D))));
    }

    private void darkBow(EntityDamageByEntityEvent event, LivingEntity target, String id) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,
                intValue(id, "abilities.wither.duration-ticks", 60),
                intValue(id, "abilities.wither.amplifier", 0), false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,
                intValue(id, "abilities.weakness.duration-ticks", 80),
                intValue(id, "abilities.weakness.amplifier", 0), false, true, true));
    }

    private void activateFireSlash(Player player, ItemStack item) {
        String id = "burning_sword";
        if (!boolValue(id, "abilities.active-1.enabled", true)
                || !startCooldown(player, item, id, "active-1", "abilities.active-1.cooldown-seconds")) return;
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        double range = Math.max(1.0D, value(id, "abilities.active-1.range", 5.0D));
        double angle = Math.max(-1.0D, Math.min(1.0D, value(id, "abilities.active-1.radius-or-angle", .70D)));
        double radius = Math.max(.25D, value(id, "abilities.active-1.hit-radius", .85D)
                + option(player, "radius-bonus"));
        Set<UUID> hit = new HashSet<>();
        for (int step = 1; step <= Math.ceil(range * 2.0D); step++) {
            Location point = start.clone().add(direction.clone().multiply(step / 2.0D));
            if (boolValue(id, "abilities.active-1.particle-enabled", true)) {
                point.getWorld().spawnParticle(Particle.FLAME, point, 3, .12, .12, .12, .01);
            }
            for (Entity entity : point.getWorld().getNearbyEntities(point, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity target) || target == player || target.isDead()
                        || hit.contains(target.getUniqueId())) continue;
                Vector toTarget = target.getEyeLocation().toVector().subtract(start.toVector()).normalize();
                if (direction.dot(toTarget) < angle) continue;
                hit.add(target.getUniqueId());
                damageFire(player, target, id, "active-1");
            }
        }
        if (boolValue(id, "abilities.active-1.sound-enabled", true)) {
            player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, .8F, .8F);
        }
    }

    private void activateHeatWave(Player player) {
        String id = "burning_sword";
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!boolValue(id, "abilities.active-2.enabled", true)
                || !startCooldown(player, item, id, "active-2", "abilities.active-2.cooldown-seconds")) return;
        Location center = player.getLocation().add(player.getLocation().getDirection().normalize()
                .multiply(value(id, "abilities.active-2.range", 2.0D)));
        double radius = Math.max(.5D, value(id, "abilities.active-2.radius", 4.0D)
                + option(player, "radius-bonus"));
        applyFireAreaDamage(player, center, radius, value(id, "abilities.active-2.damage", 6.0D),
                intValue(id, "abilities.active-2.fire-ticks", 80), id, "active-2");
        double knockback = Math.max(0.0D, value(id, "abilities.active-2.knockback", .25D));
        if (knockback > 0.0D) {
            for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (entity instanceof LivingEntity target && target != player) {
                    Vector push = target.getLocation().toVector().subtract(center.toVector()).normalize();
                    target.setVelocity(target.getVelocity().add(push.multiply(knockback)));
                }
            }
        }
        playFireImpact(center, id, "active-2");
    }

    private void armCondensedFlameArrow(Player player) {
        String id = "burning_bow";
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!boolValue(id, "abilities.active-1.enabled", true)
                || !startCooldown(player, item, id, "active-1", "abilities.active-1.cooldown-seconds")) return;
        long duration = Math.max(1L, intValue(id, "abilities.active-1.empowered-duration-seconds", 10)) * 1000L;
        empoweredShots.put(player.getUniqueId(), System.currentTimeMillis() + duration);
        player.sendActionBar(Component.text("다음 화살이 응축된 불꽃 화살로 강화됩니다.", NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, .7F, 1.1F);
    }

    private void activateFireRain(Player player) {
        String id = "burning_bow";
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!boolValue(id, "abilities.active-2.enabled", true)
                || !startCooldown(player, item, id, "active-2", "abilities.active-2.cooldown-seconds")) return;
        Location center = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize()
                .multiply(value(id, "abilities.active-2.target-range", 8.0D)));
        long delay = Math.max(0L, intValue(id, "abilities.active-2.delay-ticks", 15));
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskLater(config.getPlugin(), () -> {
            try {
                if (!player.isOnline() || player.isDead()) return;
                applyFireAreaDamage(player, center, Math.max(.5D, value(id, "abilities.active-2.radius", 4.0D)
                                + option(player, "radius-bonus")),
                        value(id, "abilities.active-2.damage", 5.0D),
                        intValue(id, "abilities.active-2.fire-ticks", 80), id, "active-2");
                int visuals = Math.max(0, Math.min(32, intValue(id, "abilities.active-2.visual-arrow-count", 8)));
                center.getWorld().spawnParticle(Particle.FLAME, center, visuals, 1.0, .2, 1.0, .04);
                playFireImpact(center, id, "active-2");
            } finally {
                Set<BukkitTask> tasks = fireTasks.get(player.getUniqueId());
                if (tasks != null) {
                    tasks.remove(holder[0]);
                    if (tasks.isEmpty()) fireTasks.remove(player.getUniqueId(), tasks);
                }
            }
        }, delay);
        fireTasks.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(holder[0]);
    }

    private void applyFireAreaDamage(Player player, Location center, double radius, double damage, int fireTicks) {
        applyFireAreaDamage(player, center, radius, damage, fireTicks, "burning_sword", "active-2");
    }

    private void applyFireAreaDamage(Player player, Location center, double radius, double damage, int fireTicks,
                                     String id, String ability) {
        if (center == null || center.getWorld() == null) return;
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || target == player || target.isDead()) continue;
            target.setFireTicks(Math.max(target.getFireTicks(), Math.max(0, fireTicks)
                    + Math.max(0, (int) Math.round(option(player, "burn-duration-bonus-ticks")))));
            damageFire(player, target, id, ability, damage);
        }
    }

    private void applyFireballExplosion(Player player, Location center, String id) {
        if (center == null || center.getWorld() == null) return;
        double radius = finitePositive(value(id, "abilities.active-1.explosion-radius",
                value(id, "abilities.active-1.impact-radius", 2.6D)), 2.6D);
        double propulsionRadius = finitePositive(value(id, "abilities.active-1.propulsion-radius", radius), radius);
        double effectiveRadius = Math.max(radius, propulsionRadius);
        double fullForceRadius = clampFinite(value(id, "abilities.active-1.full-force-radius", effectiveRadius * .45D),
                .1D, effectiveRadius);
        double minimumForceRadius = clampFinite(value(id, "abilities.active-1.minimum-force-radius", effectiveRadius),
                fullForceRadius, effectiveRadius);
        double damage = finiteNonNegative(value(id, "abilities.active-1.explosion-damage",
                value(id, "abilities.active-1.impact-damage", 1.0D)));
        double knockback = finiteNonNegative(value(id, "abilities.active-1.explosion-knockback", 1.75D));
        double selfKnockback = finiteNonNegative(value(id, "abilities.active-1.self-knockback", 1.95D));
        double selfHorizontal = finiteNonNegative(value(id, "abilities.active-1.self-horizontal-knockback", selfKnockback));
        double selfMinimumHorizontal = finiteNonNegative(value(id, "abilities.active-1.self-min-horizontal-knockback", 0.0D));
        double verticalBoost = finiteNonNegative(value(id, "abilities.active-1.self-vertical-boost",
                value(id, "abilities.active-1.vertical-boost", 0.55D)));
        boolean selfDamage = boolValue(id, "abilities.active-1.self-damage", false);
        int fireTicks = intValue(id, "abilities.active-1.fire-ticks", 60);

        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        center.getWorld().spawnParticle(Particle.FLAME, center, 24, radius * .35D, radius * .25D, radius * .35D, .05D);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.15F);

        for (Entity entity : center.getWorld().getNearbyEntities(center, effectiveRadius, effectiveRadius, effectiveRadius)) {
            if (!(entity instanceof LivingEntity target) || target.isDead()) continue;
            double distance = Math.max(0.35D, target.getLocation().distance(center));
            double falloff = forceFalloff(distance, fullForceRadius, minimumForceRadius);
            Vector push;
            if (target == player) {
                push = radialPropulsion(target.getLocation(), center, selfHorizontal,
                        Math.max(selfMinimumHorizontal, selfHorizontal * .35D), verticalBoost,
                        0.35D + falloff * .65D, player.getEyeLocation().getDirection().multiply(-1.0D));
            } else {
                push = radialPropulsion(target.getLocation(), center, knockback,
                        knockback * .35D, verticalBoost, 0.35D + falloff * .65D,
                        player.getEyeLocation().getDirection().multiply(-1.0D));
            }
            target.setVelocity(target.getVelocity().add(push));

            if (target == player && !selfDamage) continue;
            if (distance > radius) continue;
            if (damage <= 0.0D) continue;
            target.setFireTicks(Math.max(target.getFireTicks(), Math.max(0, fireTicks)));
            damageFire(player, target, id, "active-1", damage * Math.max(0.25D, falloff));
        }
    }

    private Vector radialPropulsion(Location entityLocation, Location center, double horizontalForce,
                                    double minimumHorizontalForce, double verticalForce, double multiplier,
                                    Vector fallback) {
        Vector away = entityLocation.toVector().subtract(center.toVector());
        if (!isFinite(away) || away.lengthSquared() < .0001D) {
            away = fallback == null || !isFinite(fallback) || fallback.lengthSquared() < .0001D
                    ? new Vector(0.0D, 1.0D, 0.0D) : fallback.clone();
        }
        Vector horizontal = new Vector(away.getX(), 0.0D, away.getZ());
        double horizontalLength = horizontal.length();
        double force = Math.max(minimumHorizontalForce, horizontalForce * multiplier);
        if (horizontalLength > .0001D) horizontal.normalize().multiply(force);
        else horizontal.zero();
        double y = clampFinite((away.getY() / Math.max(1.0D, center.distance(entityLocation))) * verticalForce,
                -verticalForce, verticalForce);
        if (y >= 0.0D) y = Math.max(y, verticalForce * .85D);
        Vector result = horizontal.setY(y);
        return isFinite(result) ? result : new Vector(0.0D, Math.max(0.0D, verticalForce), 0.0D);
    }

    private double forceFalloff(double distance, double fullForceRadius, double minimumForceRadius) {
        if (!Double.isFinite(distance)) return 0.0D;
        if (distance <= fullForceRadius) return 1.0D;
        if (minimumForceRadius <= fullForceRadius) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D,
                1.0D - ((distance - fullForceRadius) / (minimumForceRadius - fullForceRadius))));
    }

    private double finitePositive(double value, double fallback) {
        return Double.isFinite(value) && value > 0.0D ? value : fallback;
    }

    private double finiteNonNegative(double value) {
        return Double.isFinite(value) && value >= 0.0D ? value : 0.0D;
    }

    private double clampFinite(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) return minimum;
        return Math.max(minimum, Math.min(maximum, value));
    }

    private boolean isFinite(Vector vector) {
        return vector != null && Double.isFinite(vector.getX()) && Double.isFinite(vector.getY())
                && Double.isFinite(vector.getZ());
    }

    private void damageFire(Player player, LivingEntity target, String id, String ability) {
        damageFire(player, target, id, ability, value(id, "abilities." + ability + ".damage", 4.0D));
    }

    private void damageFire(Player player, LivingEntity target, String id, String ability, double damage) {
        ItemStack item = player.getInventory().getItemInMainHand();
        double boosted = fireDamage.boost(player, item, damage);
        internalDamage.add(player.getUniqueId());
        try { combat.applySkillDamage(player, target, boosted); }
        finally { internalDamage.remove(player.getUniqueId()); }
        target.setFireTicks(Math.max(target.getFireTicks(), fireTicks(player, id,
                intValue(id, "abilities." + ability + ".fire-ticks", 80))));
    }

    private void playFireImpact(Location location, String id, String ability) {
        if (location == null || location.getWorld() == null) return;
        if (boolValue(id, "abilities." + ability + ".particle-enabled", true)) {
            location.getWorld().spawnParticle(Particle.FLAME, location.clone().add(0, .8, 0), 14, .5, .4, .5, .03);
        }
        if (boolValue(id, "abilities." + ability + ".sound-enabled", true)) {
            location.getWorld().playSound(location, Sound.ITEM_FIRECHARGE_USE, .8F, .9F);
        }
    }

    private boolean startCooldown(Player player, ItemStack item, String id, String ability, String secondsPath) {
        double seconds = Math.max(0.0D, value(id, secondsPath, 0.0D));
        if (seconds <= 0.0D) return true;
        String cooldownId = "special:" + id + ":" + ability;
        long remaining = cooldowns.getRemainingMillis(player.getUniqueId(), cooldownId);
        if (remaining > 0L) {
            player.sendActionBar(Component.text("재사용 대기시간: " + ((remaining + 999L) / 1000L) + "초", NamedTextColor.RED));
            return false;
        }
        double reduction = promotion == null ? 0.0D
                : Math.max(0.0D, Math.min(.9D, promotion.getLegacyOptionValue(item, "cooldown-reduction-percent")));
        cooldowns.startCooldown(player.getUniqueId(), cooldownId,
                Math.max(1L, Math.round(seconds * 1000.0D * (1.0D - reduction))));
        return true;
    }

    private int fireTicks(Player player, String id, int base) {
        return Math.max(0, base + (int) Math.round(option(player, "burn-duration-bonus-ticks")));
    }

    private double option(Player player, String id) {
        if (promotion == null || player == null) return 0.0D;
        return Math.max(0.0D, promotion.getLegacyOptionValue(player.getInventory().getItemInMainHand(), id));
    }

    private void togglePoseidonMode(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        int current = meta.getPersistentDataContainer().getOrDefault(poseidonModeKey, PersistentDataType.INTEGER, 1);
        int next = current == 1 ? 2 : 1;
        meta.getPersistentDataContainer().set(poseidonModeKey, PersistentDataType.INTEGER, next);
        item.setItemMeta(meta);
        player.sendActionBar(Component.text("Poseidon mode: " + next, NamedTextColor.AQUA));
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.7F, next == 1 ? 0.8F : 1.2F);
    }

    private void applyConfiguredEffect(Player player, PotionEffectType type, String id, String path) {
        if (!boolValue(id, path + ".enabled", true)) return;
        player.addPotionEffect(new PotionEffect(type, intValue(id, path + ".duration-ticks", 600),
                intValue(id, path + ".amplifier", 0), false, true, true));
    }

    private void consumeOne(ItemStack item, Player player) {
        if (item == null || item.getAmount() <= 0) return;
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) player.getInventory().setItemInMainHand(null);
    }

    private Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    private String projectileSpecial(Entity entity) {
        if (!(entity instanceof Projectile projectile)) return "";
        return projectile.getPersistentDataContainer().getOrDefault(projectileSpecialKey, PersistentDataType.STRING, "");
    }

    public void clearAllStates() {
        homingTasks.values().forEach(BukkitTask::cancel);
        homingTasks.clear();
        ownedProjectiles.values().stream().flatMap(Set::stream).map(this::findEntity).flatMap(java.util.Optional::stream)
                .forEach(Entity::remove);
        ownedProjectiles.clear();
        projectileOwners.clear();
        fireStacks.clear();
        iceStacks.clear();
        empoweredShots.clear();
        fireTasks.values().stream().flatMap(Set::stream).forEach(BukkitTask::cancel);
        fireTasks.clear();
        fireDamage.clearAll();
        internalDamage.clear();
        for (Player player : Bukkit.getOnlinePlayers()) clearPlayerState(player);
        lastFlameCooldown.clear();
    }

    private void clearPlayerState(Player player) {
        Set<UUID> projectiles = ownedProjectiles.remove(player.getUniqueId());
        if (projectiles != null) {
            for (UUID projectileId : projectiles) {
                BukkitTask task = homingTasks.remove(projectileId);
                if (task != null) task.cancel();
                projectileOwners.remove(projectileId);
                findEntity(projectileId).ifPresent(Entity::remove);
            }
        }
        fireStacks.remove(player.getUniqueId());
        iceStacks.remove(player.getUniqueId());
        empoweredShots.remove(player.getUniqueId());
        Set<BukkitTask> tasks = fireTasks.remove(player.getUniqueId());
        if (tasks != null) tasks.forEach(BukkitTask::cancel);
        fireDamage.clear(player);
        lastFlamePenalty.remove(player.getUniqueId());
        org.bukkit.attribute.AttributeInstance attribute = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attribute != null) removeLastFlameModifier(attribute);
    }

    private void startHoming(Player shooter, Projectile projectile) {
        UUID projectileId = projectile.getUniqueId();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(config.getPlugin(), () -> {
            if (!projectile.isValid() || projectile.isDead() || !shooter.isOnline()) {
                BukkitTask current = homingTasks.remove(projectileId);
                if (current != null) current.cancel();
                forgetProjectile(projectileId);
                return;
            }
            double range = value("wind_archers_bow", "abilities.homing.range", 12.0D);
            LivingEntity target = projectile.getNearbyEntities(range, range, range).stream()
                    .filter(entity -> entity instanceof LivingEntity)
                    .map(entity -> (LivingEntity) entity)
                    .filter(entity -> entity != shooter && !entity.isDead() && shooter.hasLineOfSight(entity))
                    .min(java.util.Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(projectile.getLocation())))
                    .orElse(null);
            if (target == null) return;
            double turnRate = Math.max(0.01D, Math.min(1.0D, value("wind_archers_bow", "abilities.homing.turn-rate", .12D)));
            double speed = Math.max(.1D, projectile.getVelocity().length());
            Vector desired = target.getEyeLocation().toVector().subtract(projectile.getLocation().toVector()).normalize();
            Vector next = projectile.getVelocity().normalize().multiply(1.0D - turnRate).add(desired.multiply(turnRate)).normalize().multiply(speed);
            projectile.setVelocity(next);
        }, 1L, 1L);
        homingTasks.put(projectileId, task);
        projectileOwners.put(projectileId, shooter.getUniqueId());
        ownedProjectiles.computeIfAbsent(shooter.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(projectileId);
    }

    private void forgetProjectile(UUID projectileId) {
        UUID owner = projectileOwners.remove(projectileId);
        if (owner == null) return;
        Set<UUID> projectiles = ownedProjectiles.get(owner);
        if (projectiles == null) return;
        projectiles.remove(projectileId);
        if (projectiles.isEmpty()) ownedProjectiles.remove(owner, projectiles);
    }

    private java.util.Optional<Entity> findEntity(UUID entityId) {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) return java.util.Optional.of(entity);
        }
        return java.util.Optional.empty();
    }

    private void cleanupExpired(UUID playerId) {
        Map<UUID, StackState> fire = fireStacks.get(playerId);
        if (fire != null) fire.entrySet().removeIf(entry -> entry.getValue().expiresAt() < System.currentTimeMillis());
        Map<UUID, StackState> ice = iceStacks.get(playerId);
        if (ice != null) ice.entrySet().removeIf(entry -> entry.getValue().expiresAt() < System.currentTimeMillis());
    }

    private String path(String id, String suffix) { return "special-equipment.items." + id + "." + suffix; }
    private double value(String id, String suffix, double fallback) { return config.getSpecialEquipmentDouble(path(id, suffix), fallback); }
    private int intValue(String id, String suffix, int fallback) { return config.getSpecialEquipmentInt(path(id, suffix), fallback); }
    private boolean boolValue(String id, String suffix, boolean fallback) { return config.getSpecialEquipmentBoolean(path(id, suffix), fallback); }

    private void removeLastFlameModifier(org.bukkit.attribute.AttributeInstance attribute) {
        attribute.getModifiers().stream()
                .filter(modifier -> LAST_FLAME_HEALTH_MODIFIER.equals(modifier.getUniqueId()))
                .toList().forEach(attribute::removeModifier);
    }

    private record StackState(int stack, long expiresAt) { }
}
