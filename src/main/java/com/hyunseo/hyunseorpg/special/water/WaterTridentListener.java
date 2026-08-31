package com.hyunseo.hyunseorpg.special.water;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Runtime for the Poseidon spear's water-trident abilities. No spawned trident is collectible. */
public final class WaterTridentListener implements Listener {
    private static final String ID = "poseidons_spear";
    private final ConfigService config;
    private final SpecialEquipmentService specials;
    private final CombatService combat;
    private final CooldownService cooldowns;
    private final WaterTridentState state = new WaterTridentState();
    private final Map<UUID, Flight> flights = new HashMap<>();
    private final Map<UUID, Cast> casts = new HashMap<>();
    private final Map<UUID, TimedPlayerState> movements = new HashMap<>();
    private final Set<UUID> syntheticProjectiles = new HashSet<>();
    private final Set<BukkitTask> tasks = new HashSet<>();

    public WaterTridentListener(ConfigService config, SpecialEquipmentService specials,
                                CombatService combat, CooldownService cooldowns) {
        this.config = config;
        this.specials = specials;
        this.combat = combat;
        this.cooldowns = cooldowns;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)
                || !holding(player) || combat.isInternalDamage()) return;
        int combo = state.registerCombo(player.getUniqueId(), target.getUniqueId(), System.currentTimeMillis(),
                ticks("pressure-thrust.combo-timeout-ticks", 60) * 50L);
        Location tip = target.getEyeLocation();
        tip.getWorld().spawnParticle(Particle.BUBBLE, tip, 14, .25, .25, .25, .08);
        tip.getWorld().spawnParticle(Particle.SPLASH, tip, 10, .25, .25, .25, .04);
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location behind = target.getLocation().add(direction.clone().multiply(1.2));
        for (LivingEntity secondary : targets(player, behind, number("pressure-thrust.radius", 1.1))) {
            if (secondary != target && direction.dot(secondary.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize()) > .65) {
                combat.applySkillDamage(player, secondary, number("pressure-thrust.damage", 2.0));
            }
        }
        if (combo == 3) {
            combat.applySkillDamage(player, target, number("pressure-thrust.combo-damage", 4.0));
            target.setVelocity(target.getVelocity().add(direction.multiply(number("pressure-thrust.combo-knockback", .45))));
            tip.getWorld().spawnParticle(Particle.SONIC_BOOM, tip, 1);
        }
    }

    /** Migrates existing spear instances while leaving charging and launch entirely to vanilla. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepareVanillaThrow(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || !holding(event.getPlayer())) return;
        ItemStack spear = event.getPlayer().getInventory().getItemInMainHand();
        if (!spear.containsEnchantment(Enchantment.LOYALTY)) {
            spear.addUnsafeEnchantment(Enchantment.LOYALTY, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanillaTridentLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)
                || !(trident.getShooter() instanceof Player player)
                || !specials.getSpecialId(trident.getItemStack()).equals(ID)) return;
        Flight flight = new Flight(player, trident);
        flights.put(trident.getUniqueId(), flight);
        flight.task = repeat(() -> tickFlight(flight), 1, 1);
        if (rainyLand(player)) startRiptide(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onF(PlayerSwapHandItemsEvent event) {
        if (!holding(event.getPlayer())) return;
        event.setCancelled(true);
        startEightWayCast(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTridentDamage(EntityDamageByEntityEvent event) {
        if (syntheticProjectiles.contains(event.getDamager().getUniqueId())) {
            event.setCancelled(true); // F damage is exclusively governed by its per-object hit ledger.
            return;
        }
        Flight flight = flights.get(event.getDamager().getUniqueId());
        if (flight == null || combat.isInternalDamage() || !(event.getEntity() instanceof LivingEntity target)) return;
        event.setCancelled(true);
        if (WaterTridentState.directHitMayAttack(flight.phase) && validTarget(flight.owner, target)) {
            combat.applySkillDamage(flight.owner, target, number("current-throw.direct-damage", 6.0));
        }
        flight.phase = WaterTridentState.FlightPhase.RETURNING;
    }

    @EventHandler public void onProjectileHit(ProjectileHitEvent event) {
        Flight flight = flights.get(event.getEntity().getUniqueId());
        if (flight != null && event.getHitEntity() == null) flight.phase = WaterTridentState.FlightPhase.RETURNING;
    }

    private void tickFlight(Flight flight) {
        Trident trident = flight.trident;
        if (!trident.isValid() || !flight.owner.isOnline() || flight.owner.isDead()
                || trident.getWorld() != flight.owner.getWorld()) { forgetFlight(flight); return; }
        flight.age++;
        if (flight.age >= ticks("current-throw.lifecycle-timeout-ticks", 160)) { forgetFlight(flight); return; }
        if (flight.phase == WaterTridentState.FlightPhase.OUTWARD) {
            if (WaterTridentState.currentMayAttack(flight.phase, inWater(trident.getLocation()))) {
                Location at = trident.getLocation();
                at.getWorld().spawnParticle(Particle.BUBBLE, at, 5, .15, .15, .15, .03);
                if (flight.age % 4 == 0) at.getWorld().spawnParticle(Particle.SONIC_BOOM, at, 1);
                for (LivingEntity target : targets(flight.owner, at, number("current-throw.trail-radius", 1.8))) {
                    if (!flight.trailHits.add(target.getUniqueId())) continue;
                    Vector pull = at.toVector().subtract(target.getLocation().toVector());
                    if (pull.lengthSquared() > .01) target.setVelocity(target.getVelocity().add(pull.normalize()
                            .multiply(number("current-throw.pull-strength", .18))));
                    combat.applySkillDamage(flight.owner, target, number("current-throw.trail-damage", 2.0));
                }
            }
            if (flight.age >= ticks("current-throw.outward-ticks", 30) || trident.isOnGround())
                flight.phase = WaterTridentState.FlightPhase.RETURNING;
        } else {
            // Vanilla Loyalty owns retrieval. This branch is deliberately visual-only.
            trident.getWorld().spawnParticle(Particle.BUBBLE, trident.getLocation(), 2, .1, .1, .1, .01);
        }
    }

    private void startEightWayCast(Player player) {
        if (!cooldown(player, "signature", number("signature.cooldown-seconds", 16))) return;
        clearCast(player.getUniqueId());
        Cast cast = new Cast(player);
        casts.put(player.getUniqueId(), cast);
        cast.task = repeat(() -> tickCast(cast), 1, 1);
    }

    private void releaseEightWay(Cast cast) {
        for (int index = 0; index < WaterTridentState.SYNTHETIC_COUNT; index++) {
            double angle = Math.PI * 2 * index / WaterTridentState.SYNTHETIC_COUNT;
            Vector direction = new Vector(Math.cos(angle), 0, Math.sin(angle));
            Trident trident = safeTrident(cast.player, cast.player.getLocation().add(0, 1.1, 0), direction.multiply(1.1));
            syntheticProjectiles.add(trident.getUniqueId());
            cast.objects.add(new Synthetic(trident, new WaterTridentState.SyntheticAttack(
                    ticks("signature.maximum-hits-per-trident", 3)), angle));
        }
    }

    private void tickCast(Cast cast) {
        if (!validOwner(cast.player) || !holding(cast.player)) { clearCast(cast.player.getUniqueId()); return; }
        cast.age++;
        if (cast.phase == WaterTridentState.SignaturePhase.VORTEX) {
            renderVortex(cast);
            if (cast.age >= ticks("signature.vortex-telegraph-ticks", 12)) {
                applyVortexImpact(cast.player);
                cast.phase = WaterTridentState.advanceSignature(cast.phase);
                cast.age = 0;
                releaseEightWay(cast);
            }
            return;
        }
        for (Synthetic object : cast.objects) {
            if (!object.entity.isValid()) continue;
            for (LivingEntity target : targets(cast.player, object.entity.getLocation(), 1.2)) {
                if (object.hits.tryHit(target.getUniqueId()))
                    combat.applyMultiHitDamage(cast.player, target, number("signature.trident-damage", 3));
            }
            if (object.hits.hitCount() >= object.hits.maximumHits()
                    || cast.age > ticks("signature.return-start-ticks", 35)) {
                Vector home = cast.player.getEyeLocation().toVector().subtract(object.entity.getLocation().toVector());
                if (home.lengthSquared() > .1) object.entity.setVelocity(home.normalize().multiply(1.5));
            }
            object.entity.getWorld().spawnParticle(Particle.BUBBLE, object.entity.getLocation(), 1);
        }
        if (cast.age >= ticks("signature.attack-duration-ticks", 55)) {
            cast.phase = WaterTridentState.advanceSignature(cast.phase);
            clearCast(cast.player.getUniqueId());
        }
    }

    private void renderVortex(Cast cast) {
        Location center = cast.player.getLocation().add(0, .4, 0);
        double radius = number("signature.vortex-radius", 7.0)
                * cast.age / ticks("signature.vortex-telegraph-ticks", 12);
        for (int degree = 0; degree < 360; degree += 24) {
            double angle = Math.toRadians(degree + cast.age * 14.0);
            Location point = center.clone().add(Math.cos(angle) * radius, cast.age * .04, Math.sin(angle) * radius);
            point.getWorld().spawnParticle(Particle.BUBBLE, point, 2, .12, .18, .12, .02);
            point.getWorld().spawnParticle(Particle.SPLASH, point, 1, .08, .12, .08, .01);
        }
    }

    private void applyVortexImpact(Player player) {
        Location center = player.getLocation();
        double radius = number("signature.vortex-radius", 7.0);
        center.getWorld().spawnParticle(Particle.SONIC_BOOM, center.clone().add(0, 1, 0), 1);
        for (LivingEntity target : targets(player, center, radius)) {
            combat.applySkillDamage(player, target, number("signature.vortex-damage", 7.0));
            Vector push = target.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() > .01D) {
                target.setVelocity(target.getVelocity().add(push.normalize()
                        .multiply(number("signature.vortex-knockback", .8))));
            }
        }
    }

    private void startRiptide(Player player) {
        if (!cooldown(player, "riptide", number("riptide.cooldown-seconds", 8))) return;
        clearTimed(movements.remove(player.getUniqueId()));
        TimedPlayerState movement = new TimedPlayerState(player);
        movements.put(player.getUniqueId(), movement);
        Deque<Location> trail = new ArrayDeque<>();
        player.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(number("riptide.velocity", 1.7)));
        int duration = ticks("riptide.duration-ticks", 24);
        int lightningInterval = ticks("riptide.lightning-interval-ticks", 7);
        movement.task = repeat(() -> {
            if (!validOwner(player)) { endMovement(player, WaterTridentState.MovementEnd.INVALIDATED); return; }
            if (movement.age > 2 && (player.isOnGround() || movementCollisionAhead(player))) {
                endMovement(player, WaterTridentState.MovementEnd.COLLISION); return;
            }
            trail.addLast(player.getLocation().clone());
            if (trail.size() > lightningInterval) {
                Location behind = trail.removeFirst();
                if (movement.age % lightningInterval == 0) strikeBehind(player, behind);
            }
            player.getWorld().spawnParticle(Particle.BUBBLE, player.getLocation(), 8, .3, .3, .3, .05);
            if (++movement.age >= duration) endMovement(player, WaterTridentState.MovementEnd.NORMAL);
        }, 1, 1);
    }

    private void strikeBehind(Player owner, Location location) {
        location.getWorld().strikeLightningEffect(location); // visual lightning never owns combat damage
        for (LivingEntity target : targets(owner, location, number("riptide.lightning-radius", 2.5)))
            combat.applySkillDamage(owner, target, number("riptide.lightning-damage", 4));
    }

    private void endMovement(Player player, WaterTridentState.MovementEnd reason) {
        clearTimed(movements.remove(player.getUniqueId()));
        if (WaterTridentState.applyNormalVerticalBoost(reason) && player.isValid()) {
            Vector velocity = player.getVelocity();
            velocity.setY(velocity.getY() + number("riptide.post-vertical-boost", .28));
            player.setVelocity(velocity);
        }
    }

    private Trident safeTrident(Player owner, Location location, Vector velocity) {
        Trident trident = location.getWorld().spawn(location, Trident.class, spawned -> {
            spawned.setShooter(owner);
            spawned.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            spawned.setPersistent(false);
            spawned.setVelocity(velocity);
        });
        return trident;
    }

    private List<LivingEntity> targets(Player owner, Location center, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius))
            if (entity instanceof LivingEntity living && validTarget(owner, living)) result.add(living);
        return result;
    }

    private boolean validTarget(Player owner, LivingEntity target) {
        return target != owner && !target.isDead() && target.isValid() && !target.isInvulnerable();
    }

    private boolean movementCollisionAhead(Player player) {
        Vector velocity = player.getVelocity();
        if (velocity.lengthSquared() < .01D) return true;
        return player.getWorld().rayTraceBlocks(player.getLocation().add(0, .5, 0), velocity.clone().normalize(),
                Math.max(.4D, velocity.length())) != null;
    }

    private boolean holding(Player player) { return specials.getSpecialId(player.getInventory().getItemInMainHand()).equals(ID); }
    private boolean validOwner(Player player) { return player.isOnline() && player.isValid() && !player.isDead(); }
    private boolean inWater(Location location) { return location.getBlock().getType() == Material.WATER || location.getBlock().isLiquid(); }
    private boolean rainyLand(Player player) {
        double temperature = player.getLocation().getBlock().getTemperature();
        boolean biomeReceivesRain = temperature >= .15D && temperature < .95D;
        return WaterTridentState.rainExposed(!inWater(player.getLocation()), player.getWorld().hasStorm(),
                biomeReceivesRain, player.getWorld().getHighestBlockYAt(player.getLocation()),
                player.getLocation().getBlockY());
    }

    private boolean cooldown(Player player, String ability, double seconds) {
        String id = "special:" + ID + ":" + ability;
        if (cooldowns.getRemainingMillis(player.getUniqueId(), id) > 0) return false;
        cooldowns.startCooldown(player.getUniqueId(), id, Math.max(1, Math.round(seconds * 1000)));
        return true;
    }

    private double number(String suffix, double fallback) {
        return config.getSpecialEquipmentDouble("special-equipment.items." + ID + ".abilities." + suffix, fallback);
    }
    private int ticks(String suffix, int fallback) {
        return Math.max(1, config.getSpecialEquipmentInt("special-equipment.items." + ID + ".abilities." + suffix, fallback));
    }

    private BukkitTask repeat(Runnable action, long delay, long period) {
        final BukkitTask[] ref = new BukkitTask[1];
        ref[0] = Bukkit.getScheduler().runTaskTimer(config.getPlugin(), () -> {
            try { action.run(); } catch (RuntimeException exception) {
                config.getPlugin().getLogger().warning("Water trident task cleaned after failure: " + exception.getMessage());
                shutdown();
            }
        }, delay, period);
        tasks.add(ref[0]);
        return ref[0];
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { clearPlayer(event.getPlayer()); }
    @EventHandler public void onKick(PlayerKickEvent event) { clearPlayer(event.getPlayer()); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { clearPlayer(event.getEntity()); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent event) { clearPlayer(event.getPlayer()); }
    @EventHandler public void onHeld(PlayerItemHeldEvent event) { clearPlayer(event.getPlayer()); }
    @EventHandler public void onDrop(PlayerDropItemEvent event) {
        if (specials.getSpecialId(event.getItemDrop().getItemStack()).equals(ID)) clearPlayer(event.getPlayer());
    }
    @EventHandler public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && holding(player)) clearPlayer(player);
    }
    @EventHandler public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && holding(player)) clearPlayer(player);
    }

    private void clearPlayer(Player player) {
        UUID id = player.getUniqueId();
        state.clear(id);
        clearCast(id);
        clearTimed(movements.remove(id));
        flights.values().stream().filter(flight -> flight.owner.getUniqueId().equals(id)).toList().forEach(this::forgetFlight);
    }

    private void forgetFlight(Flight flight) {
        flights.remove(flight.trident.getUniqueId());
        if (flight.task != null) { flight.task.cancel(); tasks.remove(flight.task); }
        flight.phase = WaterTridentState.FlightPhase.REMOVED;
    }

    private void clearCast(UUID owner) {
        Cast cast = casts.remove(owner);
        if (cast == null) return;
        if (cast.task != null) { cast.task.cancel(); tasks.remove(cast.task); }
        cast.objects.forEach(object -> {
            syntheticProjectiles.remove(object.entity.getUniqueId());
            object.entity.remove();
        });
    }

    private void clearTimed(TimedPlayerState timed) {
        if (timed != null && timed.task != null) { timed.task.cancel(); tasks.remove(timed.task); }
    }

    public void shutdown() {
        new ArrayList<>(casts.keySet()).forEach(this::clearCast);
        new ArrayList<>(flights.values()).forEach(this::forgetFlight);
        movements.values().forEach(this::clearTimed);
        movements.clear();
        syntheticProjectiles.clear();
        tasks.forEach(BukkitTask::cancel); tasks.clear();
    }

    private static final class Flight {
        final Player owner; final Trident trident; final Set<UUID> trailHits = new HashSet<>();
        WaterTridentState.FlightPhase phase = WaterTridentState.FlightPhase.OUTWARD; int age; BukkitTask task;
        Flight(Player owner, Trident trident) { this.owner = owner; this.trident = trident; }
    }
    private static final class Cast {
        final Player player; final List<Synthetic> objects = new ArrayList<>();
        WaterTridentState.SignaturePhase phase = WaterTridentState.SignaturePhase.VORTEX;
        int age; BukkitTask task;
        Cast(Player player) { this.player = player; }
    }
    private record Synthetic(Trident entity, WaterTridentState.SyntheticAttack hits, double angle) { }
    private static final class TimedPlayerState {
        final Player player; int age; BukkitTask task;
        TimedPlayerState(Player player) { this.player = player; }
    }
}
