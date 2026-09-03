package com.hyunseo.hyunseorpg.special.thunder;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
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

/** Runtime for Thunder God's Axe. All lightning is particle-rendered; combat is explicit skill damage. */
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
    private final Map<UUID, UUID> charging = new HashMap<>();
    private final Map<UUID, HitState> hits = new HashMap<>();
    private final Map<UUID, Set<BukkitTask>> tasks = new HashMap<>();

    public ThunderAxeListener(JavaPlugin plugin, ConfigService config, SpecialEquipmentService specials,
            EquipmentInstanceService instances, CombatService combat, CooldownService cooldowns) {
        this.plugin = plugin; this.specials = specials; this.instances = instances;
        this.combat = combat; this.cooldowns = cooldowns; this.config = ThunderAxeConfig.from(config);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player player = event.getPlayer();
        if (!holding(player) || cooldowns.isOnCooldown(player.getUniqueId(), STRIKE_COOLDOWN)) return;
        ItemStack axe = player.getInventory().getItemInMainHand();
        axe.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(Math.max(.05F, config.chargeTicks() / 20F)).animation(ItemUseAnimation.BOW)
                .hasConsumeParticles(false).build());
        charging.put(player.getUniqueId(), instances.ensure(axe));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRelease(PlayerStopUsingItemEvent event) {
        Player player = event.getPlayer();
        UUID expected = charging.remove(player.getUniqueId());
        if (expected == null || !instances.is(event.getItem(), expected) || !validOwner(player, expected)) return;
        if (ThunderAxeMath.fullCharge(event.getTicksHeldFor(), config.chargeTicks())
                && !cooldowns.isOnCooldown(player.getUniqueId(), STRIKE_COOLDOWN)) startStrike(player, expected);
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
            jaggedPath(source[0], end, target.getUniqueId());
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
            if (tick[0] % Math.max(1, config.waveIntervalTicks()) == 0 && wave <= ThunderAxeMath.WAVE_COUNT)
                executeStrikeWave(owner, castBase, wave, facing, castHits);
            double maxRadius = config.firstDistance() + 4 * config.distanceStep();
            double radius = Math.min(maxRadius, maxRadius * tick[0] / Math.max(1.0, 4.0 * config.waveIntervalTicks()));
            for (Vector point : ThunderAxeMath.ring(radius, 40))
                origin.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, origin.clone().add(point), 1, 0, 0, 0, 0);
            return ++tick[0] <= 4 * config.waveIntervalTicks();
        }, 0, 1);
    }

    private void executeStrikeWave(Player owner, Location castBase, int wave, Vector facing, Set<UUID> castHits) {
        Set<UUID> waveHits = new HashSet<>();
        for (Vector point : ThunderAxeMath.anchoredFan(castBase.toVector(), wave, facing,
                config.firstDistance(), config.distanceStep(), config.fanAngleDegrees())) {
            Location strike = point.toLocation(castBase.getWorld()); visualBolt(strike);
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
                jaggedPath(center.clone().add(0, 1, 0), target.getLocation().add(0, target.getHeight() * .5, 0), id);
                visualBolt(target.getLocation()); combat.applySkillDamage(owner, target, config.burstDamage());
            }
            return wave[0] < waves.size();
        }, 0, config.burstWaveIntervalTicks());
    }

    private void jaggedPath(Location from, Location to, UUID seedId) {
        Vector delta = to.toVector().subtract(from.toVector()); Vector side = delta.clone().crossProduct(new Vector(0, 1, 0));
        if (side.lengthSquared() == 0) side.setX(1); else side.normalize();
        Random random = new Random(seedId.getMostSignificantBits() ^ seedId.getLeastSignificantBits());
        for (int i = 0; i <= 12; i++) {
            double t = i / 12.0; Vector point = from.toVector().add(delta.clone().multiply(t));
            if (i != 0 && i != 12) point.add(side.clone().multiply((random.nextDouble() - .5) * .55)).setY(point.getY() + (random.nextDouble() - .5) * .3);
            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point.toLocation(from.getWorld()), 1, 0, 0, 0, 0);
        }
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
            try { again = tick.getAsBoolean(); } catch (RuntimeException ex) { plugin.getLogger().warning("Thunder Axe effect failed: " + ex.getMessage()); again = false; }
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
    public void cleanup(UUID owner) { charging.remove(owner); hits.remove(owner); Set<BukkitTask> owned = tasks.remove(owner); if (owned != null) owned.forEach(BukkitTask::cancel); }
    public void shutdown() { new HashSet<>(tasks.keySet()).forEach(this::cleanup); charging.clear(); hits.clear(); }
    private record HitState(UUID instance, int count) {}
}
