package com.hyunseo.hyunseorpg.special.flame;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import org.bukkit.*;
import org.bukkit.event.block.Action;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.joml.Quaternionf;

import java.util.*;

/** Current-main-native runtime for the Flame Axe; the global enchant input layer explicitly yields ownership. */
public final class FlameAxeListener implements Listener {
    public static final String ID = "flame_axe";
    private final JavaPlugin plugin;
    private final SpecialEquipmentService specials;
    private final EquipmentInstanceService instances;
    private final CombatService combat;
    private final FlameAxeConfig config;
    private final FlameAxeChargeState charging = new FlameAxeChargeState();
    private final BukkitTask chargeTask;
    private long chargeClock;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, ItemStack> chargeRecovery = new HashMap<>();

    public FlameAxeListener(JavaPlugin plugin, ConfigService config, SpecialEquipmentService specials,
                            EquipmentInstanceService instances, CombatService combat) {
        this.plugin = plugin;
        this.specials = specials;
        this.instances = instances;
        this.combat = combat;
        this.config = FlameAxeConfig.from(config);
        this.chargeTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> charging.advanceAll(++chargeClock, this.config.fullChargeTicks()), 1, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player player = event.getPlayer();
        if (!holding(player) || sessions.containsKey(player.getUniqueId())
                || charging.contains(player.getUniqueId())) return;
        ItemStack axe = player.getInventory().getItemInMainHand();
        specials.ensureRuntimeComponents(axe);
        charging.start(player.getUniqueId(), instances.ensure(axe), chargeClock);
        chargeRecovery.put(player.getUniqueId(), axe.clone());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRelease(PlayerStopUsingItemEvent event) {
        UUID instance = instances.get(event.getItem()).orElse(null);
        // Stay in the global server-tick domain used by start() and advanceAll().
        charging.advance(event.getPlayer().getUniqueId(), chargeClock, config.fullChargeTicks());
        FlameAxeChargeState.Release release = charging.release(event.getPlayer().getUniqueId(), instance);
        chargeRecovery.remove(event.getPlayer().getUniqueId());
        if (release.existed() && release.heavyAttack() && holding(event.getPlayer())) heavyAttack(event.getPlayer());
    }

    /** Defensive only: normal holds end through PlayerStopUsingItemEvent centuries of ticks earlier. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!specials.getSpecialId(event.getItem()).equals(ID)) return;
        event.setCancelled(true);
        plugin.getLogger().warning("Prevented unexpected Flame Axe presentation-shell consumption for "
                + event.getPlayer().getName());
        ItemStack original = chargeRecovery.get(event.getPlayer().getUniqueId());
        if (original != null) Bukkit.getScheduler().runTask(plugin, () -> {
            UUID expected = instances.get(original).orElse(null);
            Set<UUID> present = new HashSet<>();
            for (ItemStack item : event.getPlayer().getInventory().getContents()) instances.get(item).ifPresent(present::add);
            instances.get(event.getPlayer().getOpenInventory().getCursor()).ifPresent(present::add);
            if (FlameAxeChargeState.shouldRestore(expected, present))
                event.getPlayer().getInventory().setItemInMainHand(original.clone());
        });
        cleanup(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onF(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() || !holding(player)) return;
        event.setCancelled(true);
        UUID id = player.getUniqueId();
        if (charging.contains(id) || sessions.containsKey(id)) return;
        start(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && holding(player) && !combat.isInternalDamage()) {
            flameBurst(event.getEntity().getLocation().add(0, .8, 0), 18, .32);
        }
    }

    private void heavyAttack(Player owner) {
        Vector facing = owner.getEyeLocation().getDirection().normalize();
        for (Entity entity : owner.getWorld().getNearbyEntities(owner.getLocation(), config.heavyRange(), config.heavyRange(), config.heavyRange())) {
            if (!(entity instanceof LivingEntity target) || target == owner || target.isDead()) continue;
            Vector delta = target.getBoundingBox().getCenter().subtract(owner.getEyeLocation().toVector());
            if (delta.lengthSquared() > config.heavyRange() * config.heavyRange() || delta.lengthSquared() == 0
                    || facing.dot(delta.normalize()) < config.heavyAngleCosine()) continue;
            combat.applySkillDamage(owner, target, config.heavyDamage());
            target.setFireTicks(Math.max(target.getFireTicks(), config.heavyFireTicks()));
            Vector push = target.getLocation().toVector().subtract(owner.getLocation().toVector()).setY(.15);
            if (push.lengthSquared() > 0) target.setVelocity(target.getVelocity().add(push.normalize().multiply(config.heavyKnockback())));
        }
        flameBurst(owner.getEyeLocation().add(facing.multiply(2)), 35, 1.0);
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1, .8f);
    }

    private void start(Player owner) {
        ItemStack visual = owner.getInventory().getItemInMainHand().clone();
        visual.setAmount(1);
        ItemDisplay display = owner.getWorld().spawn(owner.getEyeLocation().add(owner.getEyeLocation().getDirection()), ItemDisplay.class);
        display.setItemStack(visual);
        Transformation transform = display.getTransformation();
        transform.getScale().set(new Vector3f((float) config.displayScale()));
        display.setTransformation(transform);
        display.setTeleportDuration(1);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(2);
        Location logicalPosition = display.getLocation().clone();
        Session session = new Session(owner, instances.ensure(owner.getInventory().getItemInMainHand()), display,
                logicalPosition, owner.getEyeLocation().getDirection().normalize().multiply(config.speed()));
        sessions.put(owner.getUniqueId(), session);
        session.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> safeTick(session), 1, 1);
    }

    private void safeTick(Session s) {
        try { tick(s); } catch (RuntimeException exception) {
            plugin.getLogger().warning("Flame Axe session failed for " + s.owner.getName() + ": " + exception.getMessage());
            cleanup(s.owner.getUniqueId());
        }
    }

    private void tick(Session s) {
        if (!validOwner(s) || !s.display.isValid()) { cleanup(s.owner.getUniqueId()); return; }
        s.age++;
        if (!s.returning && s.age >= config.lifetimeTicks()) beginReturn(s);
        if (s.returning && s.age >= config.lifetimeTicks() + config.returnGraceTicks()) {
            cleanup(s.owner.getUniqueId());
            return;
        }

        Location from = s.logicalPosition.clone();
        if (!s.returning && (s.target == null || !validTarget(s, s.target)))
            selectTarget(s, s.visited.isEmpty());
        if (!s.returning && s.target == null) beginReturn(s);

        Location destination = s.returning ? s.owner.getEyeLocation()
                : s.target.getBoundingBox().getCenter().toLocation(s.display.getWorld());
        Vector desired = destination.toVector().subtract(from.toVector());
        double speed = s.returning ? config.returnSpeed() : config.speed();
        s.velocity = FlameAxeMath.steer(s.velocity, desired, config.turnRadians());
        if (s.velocity.lengthSquared() > 0) s.velocity.normalize().multiply(speed);

        Location to;
        if (!s.returning && s.velocity.lengthSquared() > .0001D
                && from.getWorld().rayTraceBlocks(from, s.velocity.clone().normalize(), s.velocity.length()) != null) {
            beginReturn(s);
            to = from.clone();
        } else {
            to = from.clone().add(s.velocity);
        }

        Set<UUID> hitTargets = contact(s, from, to);
        s.logicalPosition = to.clone();
        s.rotation += config.spinDegrees();
        // Display entities do not participate in normal velocity physics. The logical path is
        // authoritative for both collision and presentation; teleport duration smooths each step.
        s.display.teleport(to);
        updateRotation(s);
        flameTrail(from, to);

        if (s.returning) {
            if (reaches(from, to, s.owner.getEyeLocation(), config.returnDistance())
                    || passedDestination(from, to, s.owner.getEyeLocation()))
                cleanup(s.owner.getUniqueId());
        } else if (s.target != null && (hitTargets.contains(s.target.getUniqueId())
                || reaches(from, to, destination, config.contactRadius())
                || passedDestination(from, to, destination))) {
            if (!hitTargets.contains(s.target.getUniqueId())) confirmTargetHit(s, s.target);
            s.visited.add(s.target.getUniqueId());
            s.target = null;
            selectTarget(s, false);
            if (s.target == null) beginReturn(s);
        }
    }

    private void beginReturn(Session s) {
        s.returning = true;
        s.target = null;
    }

    private void selectTarget(Session s, boolean initial) {
        if (s.visited.size() >= config.maxTargets()) { beginReturn(s); return; }
        double radius = initial ? config.initialSearchRadius()
                : s.visited.size() >= 2 ? config.extendedSearchRadius() : config.searchRadius();
        Vector heading = s.velocity.lengthSquared() > .0001D
                ? s.velocity.clone().normalize()
                : s.owner.getEyeLocation().getDirection().normalize();
        double angleCosine = initial ? config.initialAngleCosine() : -.35D;
        s.target = s.display.getWorld().getNearbyEntities(s.logicalPosition, radius, radius, radius).stream()
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(t -> validTarget(s, t)
                        && FlameAxeMath.maySelectTarget(t.getUniqueId(), s.visited, config.maxTargets()))
                .filter(t -> {
                    Vector delta = t.getBoundingBox().getCenter().subtract(s.logicalPosition.toVector());
                    if (delta.lengthSquared() <= .0001D || heading.dot(delta.normalize()) < angleCosine) return false;
                    return s.display.getWorld().rayTraceBlocks(s.logicalPosition, delta.clone().normalize(),
                            Math.sqrt(delta.lengthSquared())) == null;
                })
                .min(Comparator.comparingDouble(t -> t.getBoundingBox().getCenter()
                        .distanceSquared(s.logicalPosition.toVector()))).orElse(null);
        if (s.target == null) beginReturn(s);
    }

    private Set<UUID> contact(Session s, Location from, Location to) {
        Set<UUID> hitTargets = new HashSet<>();
        Vector segment = to.toVector().subtract(from.toVector());
        double length = segment.length();
        Location middle = from.clone().add(segment.clone().multiply(.5));
        double scan = length / 2 + config.contactRadius();
        for (Entity entity : from.getWorld().getNearbyEntities(middle, scan, scan, scan)) {
            if (!(entity instanceof LivingEntity target) || !validTarget(s, target)) continue;
            double distance = distanceToSegment(target.getBoundingBox().getCenter(), from.toVector(), to.toVector());
            double last = s.lastHitRotation.getOrDefault(target.getUniqueId(), s.rotation - config.hitRotation());
            if (!FlameAxeMath.mayHit(distance, config.contactRadius(), s.rotation - last,
                    config.hitRotation(), target.getNoDamageTicks())) continue;
            combat.applyMultiHitDamage(s.owner, target, config.spinDamage());
            target.setFireTicks(Math.max(target.getFireTicks(), config.heavyFireTicks()));
            s.lastHitRotation.put(target.getUniqueId(), s.rotation);
            hitTargets.add(target.getUniqueId());
            flameBurst(target.getLocation().add(0, .8, 0), 16, .35);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1, .8f);
        }
        return hitTargets;
    }

    private void flameTrail(Location from, Location to) {
        Vector path = to.toVector().subtract(from.toVector());
        double length = path.length();
        int samples = Math.max(1, Math.min(4, (int) Math.ceil(length / .35)));
        for (int i = 0; i <= samples; i++) {
            Location point = from.clone().add(path.clone().multiply(i / (double) samples));
            point.getWorld().spawnParticle(Particle.FLAME, point, 1, .04, .04, .04, .005);
            if (i % 2 == 0) point.getWorld().spawnParticle(Particle.SMOKE, point, 1, .04, .04, .04, .002);
        }
    }

    private void flameBurst(Location center, int count, double spread) {
        center.getWorld().spawnParticle(Particle.FLAME, center, count, spread, spread, spread, .04);
        center.getWorld().spawnParticle(Particle.SMOKE, center, Math.max(4, count / 3),
                spread, spread, spread, .01);
        center.getWorld().spawnParticle(Particle.LAVA, center, Math.max(2, count / 8),
                spread * .7, spread * .7, spread * .7, .02);
    }

    static double distanceToSegment(Vector point, Vector start, Vector end) {
        Vector line = end.clone().subtract(start); if (line.lengthSquared() == 0) return point.distance(start);
        double t = Math.max(0, Math.min(1, point.clone().subtract(start).dot(line) / line.lengthSquared()));
        return point.distance(start.clone().add(line.multiply(t)));
    }

    static boolean reaches(Location from, Location to, Location target, double radius) {
        return from.getWorld() == target.getWorld() && distanceToSegment(target.toVector(), from.toVector(), to.toVector()) <= radius;
    }

    static boolean passedDestination(Location from, Location to, Location target) {
        if (from.getWorld() != target.getWorld()) return false;
        Vector travel = to.toVector().subtract(from.toVector());
        return travel.lengthSquared() > 0
                && target.toVector().subtract(from.toVector()).dot(travel) >= 0
                && target.toVector().subtract(to.toVector()).dot(travel) <= 0;
    }

    private void confirmTargetHit(Session s, LivingEntity target) {
        combat.applyMultiHitDamage(s.owner, target, config.spinDamage());
        target.setFireTicks(Math.max(target.getFireTicks(), config.heavyFireTicks()));
        s.lastHitRotation.put(target.getUniqueId(), s.rotation);
        flameBurst(target.getLocation().add(0, .8, 0), 16, .35);
    }

    private void updateRotation(Session s) {
        if (s.velocity.lengthSquared() < 1.0E-8) return;
        Vector direction = s.velocity.clone().normalize();
        Transformation transform = s.display.getTransformation();
        Quaternionf facing = new Quaternionf().rotationTo(0F, 0F, 1F,
                (float) direction.getX(), (float) direction.getY(), (float) direction.getZ());
        // Roll the model around its local travel axis, independently from entity yaw/pitch.
        facing.rotateZ((float) Math.toRadians(s.rotation));
        transform.getLeftRotation().set(facing);
        s.display.setTransformation(transform); // preserves translation, right rotation and configured scale
    }

    private boolean holding(Player player) { return specials.getSpecialId(player.getInventory().getItemInMainHand()).equals(ID); }
    private boolean validOwner(Session s) { return s.owner.isOnline() && !s.owner.isDead() && s.owner.getWorld() == s.display.getWorld() && holding(s.owner) && instances.is(s.owner.getInventory().getItemInMainHand(), s.instanceId); }
    private boolean validTarget(Session s, LivingEntity target) { return target != s.owner && target.isValid() && !target.isDead() && target.getWorld() == s.owner.getWorld(); }

    @EventHandler public void onQuit(PlayerQuitEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onKick(PlayerKickEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onDeath(PlayerDeathEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onTeleport(PlayerTeleportEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onHeld(PlayerItemHeldEvent e) {
        cleanup(e.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (holding(e.getPlayer())) specials.ensureRuntimeComponents(e.getPlayer().getInventory().getItemInMainHand());
        });
    }
    @EventHandler public void onInventory(InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player p) Bukkit.getScheduler().runTask(plugin, () -> validateHeld(p)); }
    @EventHandler public void onDrag(InventoryDragEvent e) { if (e.getWhoClicked() instanceof Player p) Bukkit.getScheduler().runTask(plugin, () -> validateHeld(p)); }
    private void validateHeld(Player p) {
        if (!holding(p)) cleanup(p.getUniqueId());
        else specials.ensureRuntimeComponents(p.getInventory().getItemInMainHand());
    }

    public void cleanup(UUID owner) {
        charging.clear(owner); chargeRecovery.remove(owner); Session s = sessions.remove(owner); if (s == null) return;
        if (s.task != null) s.task.cancel(); if (s.display.isValid()) s.display.remove();
        s.visited.clear(); s.lastHitRotation.clear(); s.target = null;
    }
    public void shutdown() {
        chargeTask.cancel();
        new HashSet<>(sessions.keySet()).forEach(this::cleanup);
        charging.clear();
    }

    private static final class Session {
        final Player owner; final UUID instanceId; final ItemDisplay display; final Set<UUID> visited = new HashSet<>();
        final Map<UUID, Double> lastHitRotation = new HashMap<>(); Location logicalPosition; Vector velocity; LivingEntity target;
        boolean returning; int age; double rotation; BukkitTask task;
        Session(Player owner, UUID instanceId, ItemDisplay display, Location logicalPosition, Vector velocity) { this.owner=owner; this.instanceId=instanceId; this.display=display; this.logicalPosition=logicalPosition; this.velocity=velocity; }
    }
}
