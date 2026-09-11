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
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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
    public static final String ID = SpecialEquipmentService.POSEIDON_ID;
    private static final String CONFIG_ID = "poseidons_spear";
    private final ConfigService config;
    private final SpecialEquipmentService specials;
    private final CombatService combat;
    private final CooldownService cooldowns;
    private final WaterTridentState state = new WaterTridentState();
    private final Map<UUID, Flight> flights = new HashMap<>();
    private final Map<UUID, Cast> casts = new HashMap<>();
    private final Map<UUID, TimedPlayerState> movements = new HashMap<>();
    private final Map<UUID, WaterPillar> pillars = new HashMap<>();
    private final Set<UUID> syntheticProjectiles = new HashSet<>();
    private final Set<BukkitTask> tasks = new HashSet<>();
    private final Map<UUID, Long> pendingVanillaThrows = new HashMap<>();

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

    /** Captures identity before vanilla removes the charged item from the player's hand. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPoseidonUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        specials.ensureRuntimeComponents(event.getPlayer().getInventory().getItemInMainHand());
        if (event.isCancelled() || !holding(event.getPlayer())) {
            pendingVanillaThrows.remove(event.getPlayer().getUniqueId());
            return;
        }
        pendingVanillaThrows.put(event.getPlayer().getUniqueId(), System.nanoTime());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTridentLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)
                || !(trident.getShooter() instanceof Player owner)) return;
        boolean projectileIdentity = specials.getSpecialId(trident.getItemStack()).equals(ID);
        Long started = pendingVanillaThrows.remove(owner.getUniqueId());
        // A player may hold the vanilla charge animation before release. Keep the hand-identity
        // fallback for that complete use session instead of expiring it after only five seconds.
        boolean chargedPoseidon = started != null && System.nanoTime() - started <= 60_000_000_000L;
        if (!projectileIdentity && !chargedPoseidon) return;
        attachFlight(owner, trident);
    }

    private void attachFlight(Player owner, Trident trident) {
        if (flights.containsKey(trident.getUniqueId())) return;
        Flight flight = new Flight(owner, trident);
        flights.put(trident.getUniqueId(), flight);
        flight.task = repeat(() -> tickFlight(flight), () -> forgetRealFlightTracking(flight), 1, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onF(PlayerSwapHandItemsEvent event) {
        if (!holding(event.getPlayer())) return;
        event.setCancelled(true);
        startSignature(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        // Q is represented by the drop key in the existing input convention. Only Poseidon
        // consumes it; ordinary inventory drops remain untouched.
        if (!holding(event.getPlayer())) return;
        event.setCancelled(true);
        startWaterPillar(event.getPlayer());
    }

    /** Rain movement decorates a real vanilla Riptide activation, not a separate input. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRiptide(PlayerRiptideEvent event) {
        if (specials.getSpecialId(event.getItem()).equals(ID) && rainyLand(event.getPlayer()))
            startRiptideTrail(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
        if (!trident.isValid()) { forgetRealFlightTracking(flight); return; }
        if (!flight.owner.isOnline() || flight.owner.isDead() || trident.getWorld() != flight.owner.getWorld()) {
            forgetRealFlightTracking(flight); return;
        }
        if (++flight.age >= ticks("current-throw.lifecycle-timeout-ticks", 160)) {
            forgetRealFlightTracking(flight); return;
        }
        Location at = trident.getLocation();
        // Visual identity is independent from the underwater-only combat gate.
        renderPoseidonTrail(at, flight.phase == WaterTridentState.FlightPhase.RETURNING);
        if (WaterTridentState.currentMayAttack(flight.phase)) {
            for (LivingEntity target : targets(flight.owner, at, number("current-throw.trail-radius", 1.8))) {
                if (!flight.trailHits.add(target.getUniqueId())) continue;
                Vector pull = at.toVector().subtract(target.getLocation().toVector());
                if (pull.lengthSquared() > .01) target.setVelocity(target.getVelocity().add(pull.normalize()
                        .multiply(number("current-throw.pull-strength", .18))));
                combat.applySkillDamage(flight.owner, target, number("current-throw.trail-damage", 2));
            }
        }
        if (flight.phase == WaterTridentState.FlightPhase.OUTWARD && trident.isOnGround())
            flight.phase = WaterTridentState.FlightPhase.RETURNING;
    }

    private void renderPoseidonTrail(Location at, boolean returning) {
        at.getWorld().spawnParticle(Particle.BUBBLE, at, returning ? 4 : 7, .16, .16, .16, .025);
        at.getWorld().spawnParticle(Particle.SPLASH, at, returning ? 4 : 7, .22, .22, .22, .035);
        if (returning || at.getWorld().getFullTime() % 8 == 0)
            at.getWorld().spawnParticle(Particle.SONIC_BOOM, at, 1, 0, 0, 0, 0);
    }

    private void startSignature(Player player) {
        if (!cooldown(player, "signature", number("signature.cooldown-seconds", 18))) return;
        clearCast(player.getUniqueId());
        Cast cast = new Cast(player);
        casts.put(player.getUniqueId(), cast);
        cast.task = repeat(() -> tickCast(cast), () -> clearCast(player.getUniqueId()), 1, 1);
    }

    private void tickCast(Cast cast) {
        if (!validOwner(cast.player) || !holding(cast.player)) { clearCast(cast.player.getUniqueId()); return; }
        int releaseTick = ticks("signature.vortex-duration-ticks", 24);
        cast.age++;
        if (!cast.released) {
            renderVortex(cast.player, cast.age, releaseTick);
            if (cast.age == Math.max(1, releaseTick - 4)) eruptVortex(cast.player);
            if (cast.age >= releaseTick) releaseEightWay(cast);
            return;
        }
        for (Synthetic object : cast.objects) {
            if (!object.entity.isValid()) continue;
            object.age++;
            if (object.phase == WaterTridentState.SyntheticPhase.OUTWARD && object.age >= 3)
                object.phase = WaterTridentState.SyntheticPhase.SEEKING;
            if (object.phase == WaterTridentState.SyntheticPhase.SEEKING) prepareSeeking(cast, object);
            Location from = object.entity.getLocation().clone();
            object.logicalPosition = from.clone();
            Vector destination = cast.player.getEyeLocation().toVector().subtract(from.toVector());
            if (object.phase == WaterTridentState.SyntheticPhase.RETURNING && destination.lengthSquared() > .01D) {
                double returnSpeed = number("signature.return-speed", .8);
                Vector desired = destination.normalize().multiply(returnSpeed);
                object.velocity = steer(object.velocity, desired, .20D);
                if (from.distanceSquared(cast.player.getEyeLocation()) <= 1.44D) {
                    removeSyntheticProjectile(object.entity);
                    object.phase = WaterTridentState.SyntheticPhase.DONE;
                    continue;
                }
            }
            if (object.phase != WaterTridentState.SyntheticPhase.RETURNING
                    && object.age >= ticks("signature.attack-duration-ticks", 40))
                object.phase = WaterTridentState.SyntheticPhase.RETURNING;
            Location to = from.clone().add(object.velocity);
            if (object.phase != WaterTridentState.SyntheticPhase.RETURNING && object.velocity.lengthSquared() > .0001D
                    && from.getWorld().rayTraceBlocks(from, object.velocity.clone().normalize(), object.velocity.length()) != null) {
                object.phase = WaterTridentState.SyntheticPhase.RETURNING;
                object.currentTarget = null;
                to = from;
            }
            if (object.phase == WaterTridentState.SyntheticPhase.SEEKING) contactSeeking(cast, object, from, to);
            object.logicalPosition = to;
            object.entity.setVelocity(object.velocity);
            orientSynthetic(object.entity, object.velocity);
            renderSyntheticTrail(to, object.phase == WaterTridentState.SyntheticPhase.RETURNING);
        }
        int attackDuration = ticks("signature.attack-duration-ticks", 40);
        int returnGrace = ticks("signature.return-grace-duration-ticks", attackDuration);
        boolean allDone = cast.objects.size() == WaterTridentState.SYNTHETIC_COUNT && cast.objects.stream()
                .allMatch(object -> object.phase == WaterTridentState.SyntheticPhase.DONE || !object.entity.isValid());
        if (allDone || cast.age >= releaseTick + attackDuration + returnGrace)
            clearCast(cast.player.getUniqueId());
    }

    private void prepareSeeking(Cast cast, Synthetic object) {
        double seekRadius = number("signature.seek-radius", 10);
        if (object.currentTarget == null || !validTarget(cast.player, object.currentTarget)
                || object.hits.hasHit(object.currentTarget.getUniqueId())
                || object.currentTarget.getLocation().distanceSquared(object.logicalPosition) > seekRadius * seekRadius) {
            object.currentTarget = targets(cast.player, object.logicalPosition,
                    seekRadius).stream()
                    .filter(target -> !object.hits.hasHit(target.getUniqueId()))
                    .min(java.util.Comparator.comparingDouble(target -> target.getLocation()
                            .distanceSquared(object.logicalPosition))).orElse(null);
            if (object.currentTarget == null) { object.phase = WaterTridentState.SyntheticPhase.RETURNING; return; }
        }
        Vector delta = object.currentTarget.getBoundingBox().getCenter().subtract(object.logicalPosition.toVector());
        if (delta.lengthSquared() > .0001D) object.velocity = steer(object.velocity,
                delta.normalize().multiply(number("signature.trident-speed", 1.1)), .20D);
    }

    private void contactSeeking(Cast cast, Synthetic object, Location from, Location to) {
        if (object.currentTarget == null) return;
        Vector center = object.currentTarget.getBoundingBox().getCenter();
        if (distanceToSegment(center, from.toVector(), to.toVector())
                <= number("signature.hit-radius", 1.2) && object.hits.tryHit(object.currentTarget.getUniqueId())) {
            LivingEntity hit = object.currentTarget;
            combat.applyMultiHitDamage(cast.player, hit, number("signature.trident-damage", 3));
            hit.getWorld().spawnParticle(Particle.SPLASH, hit.getEyeLocation(), 8, .2, .2, .2, .05);
            object.currentTarget = null;
            if (object.hits.exhausted()) object.phase = WaterTridentState.SyntheticPhase.RETURNING;
        }
    }

    private void orientSynthetic(Trident trident, Vector velocity) {
        if (velocity == null || velocity.lengthSquared() < .0001D) return;
        Location look = trident.getLocation().clone();
        look.setDirection(velocity);
        trident.setRotation(look.getYaw(), look.getPitch());
    }

    private void renderSyntheticTrail(Location at, boolean returning) {
        at.getWorld().spawnParticle(Particle.BUBBLE, at, returning ? 4 : 6, .12, .12, .12, .02);
        at.getWorld().spawnParticle(Particle.SPLASH, at, returning ? 3 : 5, .16, .16, .16, .03);
    }

    static double distanceToSegment(Vector point, Vector start, Vector end) {
        Vector line = end.clone().subtract(start);
        if (line.lengthSquared() < 1.0E-10) return point.distance(start);
        double t = Math.max(0, Math.min(1, point.clone().subtract(start).dot(line) / line.lengthSquared()));
        return point.distance(start.clone().add(line.multiply(t)));
    }

    private void renderVortex(Player player, int age, int duration) {
        Location center = player.getLocation().add(0, .3, 0);
        double progress = Math.min(1D, age / (double) duration);
        double radius = number("signature.vortex-radius", 7) * progress;
        for (int arm = 0; arm < 3; arm++) {
            double angle = age * .32 + arm * Math.PI * 2 / 3;
            Location point = center.clone().add(Math.cos(angle) * radius, .4 + progress * 1.4, Math.sin(angle) * radius);
            center.getWorld().spawnParticle(Particle.BUBBLE, point, 7, .2, .25, .2, .04);
            center.getWorld().spawnParticle(Particle.SPLASH, point, 3, .15, .15, .15, .03);
        }
    }

    private void eruptVortex(Player player) {
        Location center = player.getLocation();
        for (LivingEntity target : targets(player, center, number("signature.vortex-radius", 7))) {
            combat.applySkillDamage(player, target, number("signature.vortex-damage", 7));
            Vector outward = target.getLocation().toVector().subtract(center.toVector());
            if (outward.lengthSquared() > .01) {
                outward.normalize().multiply(number("signature.vortex-knockback", .8));
                outward.setY(Math.max(.18, outward.getY()));
                target.setVelocity(target.getVelocity().add(outward));
            }
        }
        center.getWorld().spawnParticle(Particle.SONIC_BOOM, center.clone().add(0, 1, 0), 1);
    }

    private void releaseEightWay(Cast cast) {
        cast.released = true;
        Location origin = cast.player.getLocation().add(0, 1.1, 0);
        for (int index = 0; index < WaterTridentState.SYNTHETIC_COUNT; index++) {
            double angle = Math.PI * 2 * index / WaterTridentState.SYNTHETIC_COUNT;
            Trident trident = safeTrident(cast.player, origin,
                    new Vector());
            Vector initialVelocity = new Vector(Math.cos(angle), 0, Math.sin(angle))
                    .multiply(number("signature.trident-speed", 1.1));
            trident.setGravity(false);
            syntheticProjectiles.add(trident.getUniqueId());
            cast.objects.add(new Synthetic(trident,
                    new WaterTridentState.SyntheticAttack(ticks("signature.maximum-hits-per-trident", 3)),
                    initialVelocity, trident.getLocation().clone()));
        }
    }

    static Vector steer(Vector current, Vector desired, double maxRadians) {
        if (current == null || current.lengthSquared() < .0001D) {
            return desired == null ? new Vector() : desired.clone();
        }
        if (desired == null || desired.lengthSquared() < .0001D) return current.clone();
        double speed = current.length();
        Vector from = current.clone().normalize();
        Vector to = desired.clone().normalize();
        double dot = Math.max(-1D, Math.min(1D, from.dot(to)));
        double angle = Math.acos(dot);
        if (angle <= maxRadians) return to.multiply(speed);
        Vector axis = from.clone().crossProduct(to);
        if (axis.lengthSquared() < .0001D) {
            axis = from.clone().crossProduct(Math.abs(from.getY()) < .9D
                    ? new Vector(0, 1, 0) : new Vector(1, 0, 0));
        }
        if (axis.lengthSquared() < .0001D) return current.clone();
        Vector result = from.rotateAroundAxis(axis.normalize(), maxRadians).multiply(speed);
        return Double.isFinite(result.getX()) && Double.isFinite(result.getY()) && Double.isFinite(result.getZ())
                ? result : current.clone();
    }

    private void startWaterPillar(Player player) {
        if (!cooldown(player, "water-pillar", number("water-pillar.cooldown-seconds", 8))) return;
        clearPillar(player.getUniqueId());
        WaterPillar pillar = new WaterPillar(player);
        pillars.put(player.getUniqueId(), pillar);
        int duration = ticks("water-pillar.duration-ticks", 30);
        pillar.task = repeat(() -> {
            if (!validOwner(player) || !holding(player)) { clearPillar(player.getUniqueId()); return; }
            Location base = player.getLocation().clone();
            double radius = number("water-pillar.radius", 1.8);
            double height = number("water-pillar.height", 3.5);
            base.getWorld().spawnParticle(Particle.SPLASH, base.clone().add(0, .4, 0), 18,
                    radius, .25, radius, .08);
            for (int i = 0; i < 5; i++) {
                double angle = pillar.age * .45 + i * Math.PI * 2 / 5;
                Location point = base.clone().add(Math.cos(angle) * radius, .6 + (i % 3) * height / 3,
                        Math.sin(angle) * radius);
                base.getWorld().spawnParticle(Particle.BUBBLE, point, 7, .12, .25, .12, .04);
            }
            for (LivingEntity target : targets(player, base.clone().add(0, height / 2, 0), radius)) {
                combat.applySkillDamage(player, target, number("water-pillar.damage", 2));
                Vector knockback = target.getLocation().toVector().subtract(base.toVector());
                if (knockback.lengthSquared() > .01)
                    target.setVelocity(target.getVelocity().add(knockback.normalize()
                            .multiply(number("water-pillar.knockback", .35))));
            }
            if (++pillar.age >= duration) clearPillar(player.getUniqueId());
        }, () -> clearPillar(player.getUniqueId()), 1, 1);
    }

    private void clearPillar(UUID owner) {
        WaterPillar pillar = pillars.remove(owner);
        if (pillar != null && pillar.task != null) {
            pillar.task.cancel();
            tasks.remove(pillar.task);
        }
    }

    private void startRiptideTrail(Player player) {
        if (!cooldown(player, "riptide", number("riptide.cooldown-seconds", 8))) return;
        clearTimed(movements.remove(player.getUniqueId()));
        TimedPlayerState movement = new TimedPlayerState(player);
        movements.put(player.getUniqueId(), movement);
        Deque<Location> trail = new ArrayDeque<>();
        int duration = ticks("riptide.duration-ticks", 24);
        int lightningInterval = ticks("riptide.lightning-interval-ticks", 7);
        movement.task = repeat(() -> {
            if (!validOwner(player) || !holding(player)) { endMovement(player, WaterTridentState.MovementEnd.INVALIDATED); return; }
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
        }, () -> endMovement(player, WaterTridentState.MovementEnd.INVALIDATED), 1, 1);
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
            spawned.setGravity(false);
            spawned.setInvulnerable(true);
            spawned.setItemStack(owner.getInventory().getItemInMainHand().clone());
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
        return config.getSpecialEquipmentDouble("special-equipment.items." + CONFIG_ID + ".abilities." + suffix, fallback);
    }
    private int ticks(String suffix, int fallback) {
        return Math.max(1, config.getSpecialEquipmentInt("special-equipment.items." + CONFIG_ID + ".abilities." + suffix, fallback));
    }

    private BukkitTask repeat(Runnable action, Runnable onFailure, long delay, long period) {
        final BukkitTask[] ref = new BukkitTask[1];
        ref[0] = Bukkit.getScheduler().runTaskTimer(config.getPlugin(), () -> {
            try { action.run(); } catch (RuntimeException exception) {
                config.getPlugin().getLogger().warning("Water trident task cleaned after failure: " + exception.getMessage());
                BukkitTask failed = ref[0];
                if (failed != null) { failed.cancel(); tasks.remove(failed); }
                try { onFailure.run(); } catch (RuntimeException cleanupFailure) {
                    config.getPlugin().getLogger().warning("Water trident failure cleanup also failed: "
                            + cleanupFailure.getMessage());
                }
            }
        }, delay, period);
        tasks.add(ref[0]);
        return ref[0];
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { clearPlayer(event.getPlayer()); }
    @EventHandler public void onKick(PlayerKickEvent event) { clearPlayer(event.getPlayer()); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { clearPlayer(event.getEntity()); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent event) { clearPlayer(event.getPlayer()); }
    @EventHandler public void onHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTask(config.getPlugin(), () -> validateHeld(event.getPlayer()));
    }
    @EventHandler public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player)
            Bukkit.getScheduler().runTask(config.getPlugin(), () -> validateHeld(player));
    }
    @EventHandler public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player)
            Bukkit.getScheduler().runTask(config.getPlugin(), () -> validateHeld(player));
    }

    private void validateHeld(Player player) { if (!holding(player)) clearActivePlayerState(player); }

    private void clearActivePlayerState(Player player) {
        UUID id = player.getUniqueId();
        pendingVanillaThrows.remove(id);
        state.clear(id);
        clearCast(id);
        clearPillar(id);
        clearTimed(movements.remove(id));
    }

    private void clearPlayer(Player player) {
        UUID id = player.getUniqueId();
        clearActivePlayerState(player);
        flights.values().stream().filter(flight -> flight.owner.getUniqueId().equals(id)).toList()
                .forEach(this::forgetRealFlightTracking);
    }

    /**
     * Stops HyunseoRPG effects for a player/vanilla-owned projectile without destroying the item entity.
     * Vanilla and Loyalty remain solely responsible for physical projectile lifetime and retrieval.
     */
    private void forgetRealFlightTracking(Flight flight) {
        flights.remove(flight.trident.getUniqueId());
        if (flight.task != null) { flight.task.cancel(); tasks.remove(flight.task); }
        flight.phase = WaterTridentState.FlightPhase.REMOVED;
    }

    private void clearCast(UUID owner) {
        Cast cast = casts.remove(owner);
        if (cast == null) return;
        if (cast.task != null) { cast.task.cancel(); tasks.remove(cast.task); }
        cast.objects.forEach(object -> {
            removeSyntheticProjectile(object.entity);
        });
    }

    private void removeSyntheticProjectile(Trident trident) {
        syntheticProjectiles.remove(trident.getUniqueId());
        if (WaterTridentState.removePhysicalProjectileOnCleanup(
                WaterTridentState.ProjectileOwnership.PLUGIN_SYNTHETIC) && trident.isValid()) trident.remove();
    }

    private void clearTimed(TimedPlayerState timed) {
        if (timed != null && timed.task != null) { timed.task.cancel(); tasks.remove(timed.task); }
    }

    public void shutdown() {
        pendingVanillaThrows.clear();
        new ArrayList<>(casts.keySet()).forEach(this::clearCast);
        new ArrayList<>(flights.values()).forEach(this::forgetRealFlightTracking);
        movements.values().forEach(this::clearTimed);
        movements.clear();
        new ArrayList<>(pillars.keySet()).forEach(this::clearPillar);
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
        int age; boolean released; BukkitTask task;
        Cast(Player player) { this.player = player; }
    }
    private static final class Synthetic {
        final Trident entity;
        final WaterTridentState.SyntheticAttack hits;
        Vector velocity;
        WaterTridentState.SyntheticPhase phase = WaterTridentState.SyntheticPhase.OUTWARD;
        LivingEntity currentTarget;
        Location logicalPosition;
        int age;
        Synthetic(Trident entity, WaterTridentState.SyntheticAttack hits, Vector velocity, Location logicalPosition) {
            this.entity = entity;
            this.hits = hits;
            this.velocity = velocity;
            this.logicalPosition = logicalPosition;
        }
    }
    private static final class TimedPlayerState {
        final Player player; int age; BukkitTask task;
        TimedPlayerState(Player player) { this.player = player; }
    }
    private static final class WaterPillar {
        final Player player; int age; BukkitTask task;
        WaterPillar(Player player) { this.player = player; }
    }
}
