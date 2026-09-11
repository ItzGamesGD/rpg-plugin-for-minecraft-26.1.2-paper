package com.hyunseo.hyunseorpg.special.thunder;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import com.hyunseo.hyunseorpg.vfx.lightning.CustomLightningParameters;
import com.hyunseo.hyunseorpg.vfx.lightning.CustomLightningRenderer;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import java.util.*;

/** Runtime for Thunder God's Axe. Display lightning is visual-only; combat remains explicit skill damage. */
public final class ThunderAxeListener implements Listener {
    public static final String ID = "thunder_gods_axe";
    private static final String STRIKE_COOLDOWN = ID + ":strike";
    private static final String BURST_COOLDOWN = ID + ":burst";
    private final JavaPlugin plugin;
    private final SpecialEquipmentService specials;
    private final EquipmentInstanceService instances;
    private final CombatService combat;
    private final CooldownService cooldowns;
    private final ThunderAxeConfig config;
    private final CustomLightningRenderer lightning;
    private final ThunderAxeChargeState charging = new ThunderAxeChargeState();
    private final Map<UUID, ItemStack> chargeRecovery = new HashMap<>();
    private final BukkitTask chargeTask;
    private long chargeClock;
    private final Map<UUID, HitState> hits = new HashMap<>();
    private final Map<UUID, Set<BukkitTask>> tasks = new HashMap<>();

    public ThunderAxeListener(JavaPlugin plugin, ConfigService config, SpecialEquipmentService specials,
            EquipmentInstanceService instances, CombatService combat, CooldownService cooldowns) {
        this.plugin = plugin; this.specials = specials; this.instances = instances;
        this.combat = combat; this.cooldowns = cooldowns; this.config = ThunderAxeConfig.from(config);
        this.lightning = new CustomLightningRenderer(plugin);
        this.chargeTask = Bukkit.getScheduler().runTaskTimer(plugin, this::advanceCharges, 1, 1);
    }

    private void advanceCharges() {
        try {
            charging.advanceAll(++chargeClock, config.chargeTicks());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Thunder Axe charge state failed", exception);
            charging.clear();
            chargeRecovery.clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player player = event.getPlayer();
        if (!holding(player) || cooldowns.isOnCooldown(player.getUniqueId(), STRIKE_COOLDOWN)) return;
        ItemStack axe = player.getInventory().getItemInMainHand();
        applyPresentationShell(axe);
        UUID instance = instances.ensure(axe);
        charging.start(player.getUniqueId(), instance, chargeClock);
        chargeRecovery.put(player.getUniqueId(), axe.clone());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRelease(PlayerStopUsingItemEvent event) {
        Player player = event.getPlayer();
        UUID instance = instances.get(event.getItem()).orElse(null);
        charging.advance(player.getUniqueId(), chargeClock, config.chargeTicks());
        ThunderAxeChargeState.Release release = charging.release(player.getUniqueId(), instance);
        chargeRecovery.remove(player.getUniqueId());
        if (!release.existed() || !release.fullCharge() || !validOwner(player, instance)) return;
        if (!cooldowns.isOnCooldown(player.getUniqueId(), STRIKE_COOLDOWN))
            startStrike(player, instance);
    }

    private void applyPresentationShell(ItemStack axe) {
        axe.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(ThunderAxeChargeState.PRESENTATION_SECONDS)
                .animation(ItemUseAnimation.BOW)
                .sound(Key.key("minecraft:intentionally_empty"))
                .hasConsumeParticles(false)
                .build());
        axe.unsetData(DataComponentTypes.BLOCKS_ATTACKS);
    }

    /** Defensive guard if the deliberately long presentation lifecycle ever completes. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onConsume(PlayerItemConsumeEvent event) {
        UUID owner = event.getPlayer().getUniqueId();
        if (!specials.getSpecialId(event.getItem()).equals(ID) && !charging.contains(owner)) return;
        event.setCancelled(true);
        ItemStack recovery = chargeRecovery.remove(owner);
        charging.clear(owner);
        if (recovery != null) Bukkit.getScheduler().runTask(plugin,
                () -> restoreIfMissing(event.getPlayer(), recovery));
    }

    private void restoreIfMissing(Player player, ItemStack recovery) {
        UUID expected = instances.get(recovery).orElse(null);
        if (expected == null) return;
        for (ItemStack item : player.getInventory().getContents()) {
            if (instances.is(item, expected)) return;
        }
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            player.getInventory().setItemInMainHand(recovery.clone());
            return;
        }
        player.getInventory().addItem(recovery.clone()).values()
                .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onOffhand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() || !holding(player)) return;
        event.setCancelled(true);
        if (!cooldowns.isOnCooldown(player.getUniqueId(), BURST_COOLDOWN))
            startBurst(player, instances.ensure(player.getInventory().getItemInMainHand()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)
                || target == player || !holding(player) || combat.isInternalDamage()) return;
        UUID instance = instances.ensure(player.getInventory().getItemInMainHand());
        HitState state = hits.get(player.getUniqueId());
        int current = state != null && state.instance.equals(instance) ? state.count : 0;
        boolean trigger = ThunderAxeMath.triggers(current, config.hitThreshold(), true);
        int next = ThunderAxeMath.advanceHit(current, config.hitThreshold(), true);
        hits.put(player.getUniqueId(), new HitState(instance, next));
        if (!trigger) return;
        visualBolt(target.getLocation());
        combat.applySkillDamage(player, target, config.strikeDamage());
        startChain(player, instance, target);
    }

    private void startChain(Player owner, UUID instance, LivingEntity initial) {
        List<UUID> route = new ArrayList<>();
        Set<UUID> visited = new HashSet<>(); visited.add(initial.getUniqueId());
        LivingEntity cursor = initial;
        for (int i = 0; i < config.chainTargets(); i++) {
            LivingEntity source = cursor;
            cursor = source.getWorld().getNearbyEntities(source.getLocation(), config.chainRadius(), config.chainRadius(), config.chainRadius()).stream()
                    .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                    .filter(t -> validTarget(owner, t) && !visited.contains(t.getUniqueId()))
                    .filter(t -> t.getLocation().distanceSquared(source.getLocation()) <= config.chainRadius() * config.chainRadius())
                    .min(Comparator.comparingDouble((LivingEntity t) -> t.getLocation().distanceSquared(source.getLocation()))
                            .thenComparing(t -> t.getUniqueId().toString())).orElse(null);
            if (cursor == null) break;
            visited.add(cursor.getUniqueId()); route.add(cursor.getUniqueId());
        }
        if (route.isEmpty()) return;
        owner.getWorld().playSound(initial.getLocation(), Sound.ITEM_TOTEM_USE, .75f, 1.35f);
        final Location[] source = {initial.getLocation().add(0, initial.getHeight() * .55, 0)};
        final int[] index = {0};
        scheduleTimer(owner.getUniqueId(), () -> {
            if (!validOwner(owner, instance) || index[0] >= route.size()) return false;
            Entity entity = Bukkit.getEntity(route.get(index[0]));
            if (!(entity instanceof LivingEntity target) || !validTarget(owner, target)) { index[0]++; return true; }
            Location end = target.getLocation().add(0, target.getHeight() * .55, 0);
            renderLightning(owner.getUniqueId(), source[0], end, target.getUniqueId().getLeastSignificantBits());
            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, end, 12, .3, .35, .3, .08);
            combat.applySkillDamage(owner, target, config.chainDamage() * Math.pow(config.chainFalloff(), index[0]));
            source[0] = end; index[0]++; return index[0] < route.size();
        }, 0, 2);
    }

    private void startStrike(Player owner, UUID instance) {
        cooldowns.startCooldownTicks(owner.getUniqueId(), STRIKE_COOLDOWN, config.strikeCooldownTicks());
        Location castBase = owner.getLocation().clone();
        Vector facing = flatFacing(owner);
        Location origin = castBase.clone().add(facing.clone().multiply(config.firstDistance()));
        origin.getWorld().spawnParticle(Particle.EXPLOSION, origin.clone().add(0, .2, 0), 3, .3, .15, .3, 0);
        origin.getWorld().playSound(origin, Sound.ITEM_TOTEM_USE, 1.1f, .75f);
        final int[] tick = {0}; final Set<UUID> castHits = new HashSet<>();
        scheduleTimer(owner.getUniqueId(), () -> {
            if (!validOwner(owner, instance)) return false;
            int wave = tick[0] / Math.max(1, config.waveIntervalTicks()) + 1;
            if (tick[0] % Math.max(1, config.waveIntervalTicks()) == 0 && wave <= config.fanWaveCount())
                executeStrikeWave(owner, castBase, wave, facing, castHits);
            double maxRadius = config.firstDistance() + (config.fanWaveCount() - 1) * config.distanceStep();
            double radius = Math.min(maxRadius, maxRadius * tick[0] / Math.max(1.0,
                    (config.fanWaveCount() - 1.0) * config.waveIntervalTicks()));
            for (Vector point : ThunderAxeMath.ring(radius, 40))
                origin.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, origin.clone().add(point), 1, 0, 0, 0, 0);
            return ++tick[0] <= (config.fanWaveCount() - 1) * config.waveIntervalTicks();
        }, 0, 1);
    }

    private void executeStrikeWave(Player owner, Location castBase, int wave, Vector facing, Set<UUID> castHits) {
        Set<UUID> waveHits = new HashSet<>();
        for (Vector point : ThunderAxeMath.anchoredFan(castBase.toVector(), wave,
                config.fanWaveCount(), config.fanDirectionCount(), facing,
                config.firstDistance(), config.distanceStep(), config.fanAngleDegrees())) {
            Location strike = point.toLocation(castBase.getWorld());
            renderLightning(owner.getUniqueId(), strike.clone().add(0, config.verticalLightningHeight(), 0),
                    strike, Double.doubleToLongBits(point.getX()) ^ Double.doubleToLongBits(point.getZ()) ^ wave);
            for (Entity entity : owner.getWorld().getNearbyEntities(strike, config.hitRadius(), 2.25, config.hitRadius())) {
                if (!(entity instanceof LivingEntity target) || !validTarget(owner, target)
                        || !waveHits.add(target.getUniqueId()) || (!config.repeatAcrossWaves() && !castHits.add(target.getUniqueId()))) continue;
                combat.applySkillDamage(owner, target, config.waveDamage());
            }
        }
    }

    private void startBurst(Player owner, UUID instance) {
        cooldowns.startCooldownTicks(owner.getUniqueId(), BURST_COOLDOWN, config.burstCooldownTicks());
        Location center = owner.getLocation();
        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 1, 0), 35, .7, .8, .7, .12);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center.clone().add(0, .5, 0), 2, .3, .3, .3, 0);
        center.getWorld().playSound(center, Sound.ITEM_TOTEM_USE, 1.1f, .85f);
        Map<UUID, Double> distances = new HashMap<>();
        for (Entity e : owner.getWorld().getNearbyEntities(center, config.burstRadius(), config.burstRadius(), config.burstRadius()))
            if (e instanceof LivingEntity target && validTarget(owner, target)) distances.put(target.getUniqueId(), target.getLocation().distanceSquared(center));
        List<List<UUID>> waves = ThunderAxeMath.distanceWaves(distances, config.burstRadius(), config.burstTargetsPerWave(), config.burstMaxTargets(), config.burstMaxWaves());
        final int[] wave = {0}; final Set<UUID> visited = new HashSet<>();
        scheduleTimer(owner.getUniqueId(), () -> {
            if (!validOwner(owner, instance) || wave[0] >= waves.size()) return false;
            for (UUID id : waves.get(wave[0]++)) {
                if (!visited.add(id)) continue;
                Entity entity = Bukkit.getEntity(id);
                if (!(entity instanceof LivingEntity target) || !validTarget(owner, target)
                        || target.getLocation().distanceSquared(center) > config.burstRadius() * config.burstRadius()) continue;
                renderLightning(owner.getUniqueId(), center.clone().add(0, 1, 0),
                        target.getLocation().add(0, target.getHeight() * .5, 0), id.getMostSignificantBits());
                visualBolt(target.getLocation()); combat.applySkillDamage(owner, target, config.burstDamage());
            }
            return wave[0] < waves.size();
        }, 0, config.burstWaveIntervalTicks());
    }

    private void renderLightning(UUID owner, Location from, Location to, long seed) {
        lightning.renderLightning(owner, from, to, new CustomLightningParameters(
                config.coreThickness(), config.glowThickness(), config.segmentLength(), config.pathJitter(),
                config.branchChance(), config.maxBranchDepth(), config.branchLength(),
                config.lightningLifetimeTicks()), seed);
    }

    private void visualBolt(Location base) {
        World world = base.getWorld(); world.spawnParticle(Particle.FLASH, base.clone().add(0, 1, 0), 1);
        Random random = new Random(Double.doubleToLongBits(base.getX()) ^ Double.doubleToLongBits(base.getZ()));
        for (int i = 0; i < 14; i++) world.spawnParticle(Particle.ELECTRIC_SPARK,
                base.clone().add((random.nextDouble() - .5) * .35, i * .24, (random.nextDouble() - .5) * .35), 1, 0, 0, 0, 0);
    }

    private void scheduleTimer(UUID owner, java.util.function.BooleanSupplier tick, long delay, long period) {
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            boolean again;
            try { again = tick.getAsBoolean(); } catch (RuntimeException ex) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Thunder Axe effect failed", ex);
                again = false;
            }
            if (!again) { holder[0].cancel(); Set<BukkitTask> owned = tasks.get(owner); if (owned != null) { owned.remove(holder[0]); if (owned.isEmpty()) tasks.remove(owner); } }
        }, delay, Math.max(1, period));
        tasks.computeIfAbsent(owner, ignored -> new HashSet<>()).add(holder[0]);
    }

    private Vector flatFacing(Player player) { Vector v = player.getEyeLocation().getDirection().setY(0); return v.lengthSquared() == 0 ? new Vector(0, 0, 1) : v.normalize(); }
    private boolean holding(Player p) { return specials.getSpecialId(p.getInventory().getItemInMainHand()).equals(ID); }
    private boolean validOwner(Player p, UUID instance) { return p.isOnline() && !p.isDead() && holding(p) && instances.is(p.getInventory().getItemInMainHand(), instance); }
    private boolean validTarget(Player owner, LivingEntity target) { return target != owner && target.isValid() && !target.isDead() && target.getWorld() == owner.getWorld() && !(target instanceof ArmorStand); }

    @EventHandler public void onQuit(PlayerQuitEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onKick(PlayerKickEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onDeath(PlayerDeathEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onHeld(PlayerItemHeldEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onInventory(InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player p) Bukkit.getScheduler().runTask(plugin, () -> { if (!holding(p)) cleanup(p.getUniqueId()); }); }
    @EventHandler public void onDrag(InventoryDragEvent e) { if (e.getWhoClicked() instanceof Player p) Bukkit.getScheduler().runTask(plugin, () -> { if (!holding(p)) cleanup(p.getUniqueId()); }); }
    public void cleanup(UUID owner) { charging.clear(owner); chargeRecovery.remove(owner); hits.remove(owner); Set<BukkitTask> owned = tasks.remove(owner); if (owned != null) owned.forEach(BukkitTask::cancel); lightning.cleanup(owner); }
    public void shutdown() {
        chargeTask.cancel();
        Set<UUID> owners = new HashSet<>(tasks.keySet());
        owners.addAll(hits.keySet());
        owners.addAll(charging.owners());
        owners.forEach(this::cleanup);
        lightning.shutdown();
        charging.clear();
        chargeRecovery.clear();
        hits.clear();
    }
    private record HitState(UUID instance, int count) {}
}
