package com.hyunseo.hyunseorpg.special.flame;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
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

import java.util.*;

/** Current-main-native runtime for the Flame Axe; the global enchant input layer explicitly yields ownership. */
public final class FlameAxeListener implements Listener {
    public static final String ID = "flame_axe";
    private final JavaPlugin plugin;
    private final SpecialEquipmentService specials;
    private final EquipmentInstanceService instances;
    private final CombatService combat;
    private final FlameAxeConfig config;
    private final Map<UUID, Charge> charging = new HashMap<>();
    private final Map<UUID, Session> sessions = new HashMap<>();

    public FlameAxeListener(JavaPlugin plugin, ConfigService config, SpecialEquipmentService specials,
                            EquipmentInstanceService instances, CombatService combat) {
        this.plugin = plugin;
        this.specials = specials;
        this.instances = instances;
        this.combat = combat;
        this.config = FlameAxeConfig.from(config);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player player = event.getPlayer();
        if (!holding(player) || sessions.containsKey(player.getUniqueId())
                || charging.containsKey(player.getUniqueId())) return;
        ItemStack axe = player.getInventory().getItemInMainHand();
        axe.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(Math.max(.05F, config.fullChargeTicks() / 20F))
                .animation(ItemUseAnimation.BOW).hasConsumeParticles(false).build());
        charging.put(player.getUniqueId(), new Charge(instances.ensure(axe), System.nanoTime()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRelease(PlayerStopUsingItemEvent event) {
        Charge charge = charging.remove(event.getPlayer().getUniqueId());
        if (!validCharge(event.getPlayer(), event.getItem(), charge)) return;
        if (FlameAxeMath.isFullCharge(event.getTicksHeldFor(), config.fullChargeTicks())) heavyAttack(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onConsume(PlayerItemConsumeEvent event) {
        Charge charge = charging.remove(event.getPlayer().getUniqueId());
        if (!validCharge(event.getPlayer(), event.getItem(), charge)) return;
        // The consumable component is only a Paper-backed charge animation; the axe is never consumed.
        event.setCancelled(true);
        int heldTicks = (int) Math.max(0L, (System.nanoTime() - charge.startedAtNanos()) / 50_000_000L);
        if (FlameAxeMath.isFullCharge(heldTicks, config.fullChargeTicks())) heavyAttack(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onF(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() || !holding(player)) return;
        event.setCancelled(true);
        UUID id = player.getUniqueId();
        if (charging.containsKey(id) || sessions.containsKey(id)) return;
        start(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && holding(player) && !combat.isInternalDamage()) {
            event.getEntity().getWorld().spawnParticle(Particle.FLAME, event.getEntity().getLocation().add(0, .8, 0), 8, .2, .3, .2, .02);
            event.getEntity().getWorld().spawnParticle(Particle.SMOKE, event.getEntity().getLocation().add(0, .8, 0), 4, .2, .3, .2, .01);
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
        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, .8f);
        owner.getWorld().spawnParticle(Particle.FLAME, owner.getEyeLocation().add(facing.multiply(2)), 35, 1, .7, 1, .05);
    }

    private void start(Player owner) {
        ItemStack visual = owner.getInventory().getItemInMainHand().clone();
        visual.setAmount(1);
        ItemDisplay display = owner.getWorld().spawn(owner.getEyeLocation().add(owner.getEyeLocation().getDirection()), ItemDisplay.class);
        display.setItemStack(visual);
        Transformation transform = display.getTransformation();
        transform.getScale().set(new Vector3f((float) config.displayScale()));
        display.setTransformation(transform);
        Session session = new Session(owner, instances.ensure(owner.getInventory().getItemInMainHand()), display,
                owner.getEyeLocation().getDirection().normalize().multiply(config.speed()));
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
        if (!validOwner(s) || !s.display.isValid() || ++s.age >= config.lifetimeTicks()) { cleanup(s.owner.getUniqueId()); return; }
        Location from = s.display.getLocation();
        if (!s.returning && (s.target == null || !validTarget(s, s.target))) selectTarget(s);
        Location destination = s.returning ? s.owner.getEyeLocation() : s.target.getBoundingBox().getCenter().toLocation(s.display.getWorld());
        Vector desired = destination.toVector().subtract(from.toVector());
        s.velocity = FlameAxeMath.steer(s.velocity, desired, config.turnRadians());
        if (s.velocity.lengthSquared() > 0) s.velocity.normalize().multiply(config.speed());
        Location to = from.clone().add(s.velocity);
        s.rotation += config.spinDegrees();
        s.display.setVelocity(s.velocity);
        s.display.setRotation(from.getYaw() + (float) config.spinDegrees(), from.getPitch());
        contact(s, from, to);
        if (s.returning) {
            if (to.distanceSquared(s.owner.getEyeLocation()) <= config.returnDistance() * config.returnDistance()) cleanup(s.owner.getUniqueId());
        } else if (to.distanceSquared(destination) <= config.contactRadius() * config.contactRadius()) {
            s.visited.add(s.target.getUniqueId()); s.target = null; selectTarget(s);
        }
    }

    private void selectTarget(Session s) {
        if (s.visited.size() >= config.maxTargets()) { s.returning = true; s.target = null; return; }
        s.target = s.display.getWorld().getNearbyEntities(s.display.getLocation(), config.searchRadius(), config.searchRadius(), config.searchRadius()).stream()
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(t -> validTarget(s, t) && FlameAxeMath.maySelectTarget(t.getUniqueId(), s.visited, config.maxTargets()))
                .min(Comparator.comparingDouble(t -> t.getLocation().distanceSquared(s.display.getLocation()))).orElse(null);
        if (s.target == null) s.returning = true;
    }

    private void contact(Session s, Location from, Location to) {
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
        }
    }

    static double distanceToSegment(Vector point, Vector start, Vector end) {
        Vector line = end.clone().subtract(start); if (line.lengthSquared() == 0) return point.distance(start);
        double t = Math.max(0, Math.min(1, point.clone().subtract(start).dot(line) / line.lengthSquared()));
        return point.distance(start.clone().add(line.multiply(t)));
    }

    private boolean holding(Player player) { return specials.getSpecialId(player.getInventory().getItemInMainHand()).equals(ID); }
    private boolean validCharge(Player player, ItemStack item, Charge charge) {
        return charge != null && holding(player) && instances.is(item, charge.instanceId());
    }
    private boolean validOwner(Session s) { return s.owner.isOnline() && !s.owner.isDead() && s.owner.getWorld() == s.display.getWorld() && holding(s.owner) && instances.is(s.owner.getInventory().getItemInMainHand(), s.instanceId); }
    private boolean validTarget(Session s, LivingEntity target) { return target != s.owner && target.isValid() && !target.isDead() && target.getWorld() == s.owner.getWorld(); }

    @EventHandler public void onQuit(PlayerQuitEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onKick(PlayerKickEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onDeath(PlayerDeathEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onTeleport(PlayerTeleportEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onHeld(PlayerItemHeldEvent e) { cleanup(e.getPlayer().getUniqueId()); }
    @EventHandler public void onInventory(InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player p) Bukkit.getScheduler().runTask(plugin, () -> validateHeld(p)); }
    @EventHandler public void onDrag(InventoryDragEvent e) { if (e.getWhoClicked() instanceof Player p) Bukkit.getScheduler().runTask(plugin, () -> validateHeld(p)); }
    private void validateHeld(Player p) { if (!holding(p)) cleanup(p.getUniqueId()); }

    public void cleanup(UUID owner) {
        charging.remove(owner); Session s = sessions.remove(owner); if (s == null) return;
        if (s.task != null) s.task.cancel(); if (s.display.isValid()) s.display.remove();
        s.visited.clear(); s.lastHitRotation.clear(); s.target = null;
    }
    public void shutdown() { new HashSet<>(sessions.keySet()).forEach(this::cleanup); charging.clear(); }

    private record Charge(UUID instanceId, long startedAtNanos) { }

    private static final class Session {
        final Player owner; final UUID instanceId; final ItemDisplay display; final Set<UUID> visited = new HashSet<>();
        final Map<UUID, Double> lastHitRotation = new HashMap<>(); Vector velocity; LivingEntity target;
        boolean returning; int age; double rotation; BukkitTask task;
        Session(Player owner, UUID instanceId, ItemDisplay display, Vector velocity) { this.owner=owner; this.instanceId=instanceId; this.display=display; this.velocity=velocity; }
    }
}
