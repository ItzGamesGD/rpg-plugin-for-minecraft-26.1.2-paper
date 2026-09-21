package com.hyunseo.hyunseorpg.rpgtest.basic;

import com.hyunseo.hyunseorpg.rpgtest.DisplayMotion;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Vex;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test-only weapon actors. Vanilla Vex AI drives normal melee movement.
 * A trident thrower's emitted Trident remains a native projectile and is not part of the actor cap.
 */
public final class BasicWeaponRuntime {
    private final Plugin plugin;
    private final List<Entity> entities = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final List<VexWeaponActor> actors = new ArrayList<>();

    public BasicWeaponRuntime(Plugin plugin) { this.plugin = plugin; }

    public void start(Player player, Location bossCore, Map<BasicWeaponPattern, Integer> selection) {
        selection.forEach((pattern, count) -> {
            for (int index = 0; index < count; index++) spawn(player, bossCore, pattern, index);
        });
    }

    private void spawn(Player target, Location core, BasicWeaponPattern pattern, int offset) {
        switch (pattern) {
            case MACE_MELEE, SPEAR_MELEE, AXE_MELEE, HOE_MELEE -> spawnVexMelee(target, core, pattern, offset);
            case TRIDENT_THROWER -> spawnTridentThrower(target, core, offset);
            case MACE_DROP -> spawnMaceDrop(target, offset);
            case SPEAR_LUNGE -> spawnLunge(target, core, offset);
            case SHIELD_ORBIT -> spawnShield(target, core, offset);
        }
    }

    private Vex vexActor(Player target, Location location, Material weapon) {
        Vex vex = location.getWorld().spawn(location, Vex.class, actor -> {
            actor.setPersistent(false);
            actor.setInvisible(true);
            actor.setInvulnerable(true);
            actor.setSilent(true);
            actor.setCollidable(false);
            actor.setTarget(target);
            // Do not force charging. Vanilla Vex AI owns its own approach/pass/reposition state.
            // A Vex is an internal movement driver only. The visible and damaging thing is
            // the paired ItemDisplay weapon below, never a Gateway Wraith/add mob.
            actor.getEquipment().clear();
            actor.setCustomNameVisible(false);
        });
        entities.add(vex);
        return vex;
    }

    private void spawnVexMelee(Player target, Location core, BasicWeaponPattern pattern, int offset) {
        ItemDisplay weapon = display(offset(core, offset), material(pattern), .88f);
        actors.add(VexWeaponActor.start(plugin, target, weapon, pattern, new VexWeaponActor.Stats(180, 7, 5, 3),
                entities::add, tasks::add, () -> { }));
    }

    private void spawnTridentThrower(Player target, Location core, int offset) {
        ItemDisplay weapon = display(offset(core, offset), Material.TRIDENT, .88f);
        actors.add(VexWeaponActor.start(plugin, target, weapon, BasicWeaponPattern.TRIDENT_THROWER, new VexWeaponActor.Stats(180, 7, 5, 3),
                entities::add, tasks::add, () -> { }));
    }

    private void spawnMaceDrop(Player target, int offset) {
        Location snapshot = target.getLocation().clone();
        Location start = snapshot.clone().add((offset - 1) * 1.4, 8, 0);
        ItemDisplay display = display(start, Material.MACE, 1.2f);
        BukkitRunnable task = new BukkitRunnable() {
            int age;
            @Override public void run() {
                if (!display.isValid() || !target.isOnline() || age++ > 80) { remove(display); cancel(); return; }
                Vector step = new Vector(0, -.46, 0);
                RayTraceResult blockHit = display.getWorld().rayTraceBlocks(display.getLocation(), step.clone().normalize(), step.length(), FluidCollisionMode.NEVER, true);
                if (display.getBoundingBox().expand(.25).overlaps(target.getBoundingBox())) {
                    target.damage(8); target.setVelocity(new Vector(0, -.3, 0)); impact(display.getLocation(), target, 3.0, 4.0); remove(display); cancel();
                } else if (blockHit != null) {
                    Location hit = blockHit.getHitPosition().toLocation(display.getWorld()); impact(hit, target, 3.0, 4.0); remove(display); cancel();
                } else display.teleport(display.getLocation().add(step));
            }
        };
        tasks.add(task.runTaskTimer(plugin, 1, 1));
    }

    private void spawnLunge(Player target, Location core, int offset) {
        ItemDisplay display = display(offset(core, offset), Material.TRIDENT, 1.15f);
        BukkitRunnable task = new BukkitRunnable() {
            int age; Vector locked;
            @Override public void run() {
                if (!display.isValid() || !target.isOnline() || age++ > 65) { remove(display); cancel(); return; }
                if (age == 20) locked = target.getEyeLocation().toVector().subtract(display.getLocation().toVector()).normalize();
                if (age >= 20 && age < 42) {
                    Vector step = locked.clone().multiply(.45); display.teleport(display.getLocation().add(step));
                    if (display.getBoundingBox().expand(.25).overlaps(target.getBoundingBox())) target.damage(5);
                }
            }
        };
        tasks.add(task.runTaskTimer(plugin, 1, 1));
    }

    private void spawnShield(Player target, Location core, int offset) {
        ItemDisplay display = display(core, Material.SHIELD, 1.0f);
        BukkitRunnable task = new BukkitRunnable() {
            int age;
            @Override public void run() {
                if (!display.isValid() || !target.isOnline() || age++ > 400) { remove(display); cancel(); return; }
                double angle = offset * 1.8 + age * .09;
                display.teleport(core.clone().add(Math.cos(angle) * 3, 1.5, Math.sin(angle) * 3));
            }
        };
        tasks.add(task.runTaskTimer(plugin, 1, 1));
    }

    /** Debug-only comparison: a normal visible Vex and the hidden driver start together. */
    public String startAiComparison(Player target, Location core, BasicWeaponPattern pattern) {
        if (pattern != BasicWeaponPattern.MACE_MELEE && pattern != BasicWeaponPattern.SPEAR_MELEE
                && pattern != BasicWeaponPattern.AXE_MELEE && pattern != BasicWeaponPattern.HOE_MELEE) {
            throw new IllegalArgumentException("weapon-ai supports a Vex-driven melee pattern only");
        }
        Vex reference = core.getWorld().spawn(core.clone().add(-2, 1, 0), Vex.class, vex -> {
            vex.setPersistent(false); vex.setInvulnerable(true); vex.setSilent(false); vex.setTarget(target);
            vex.setCustomName("§eRPGTest vanilla Vex reference"); vex.setCustomNameVisible(true);
        });
        entities.add(reference);
        Vex hidden = vexActor(target, core.clone().add(2, 1, 0), material(pattern));
        ItemDisplay weapon = display(hidden.getLocation(), material(pattern), .88f);
        BukkitRunnable task = new BukkitRunnable() {
            int age;
            @Override public void run() {
                if (!valid(hidden, target) || !valid(reference, target) || age++ >= 200) {
                    remove(weapon); remove(hidden); remove(reference); cancel(); return;
                }
                weapon.teleport(hidden.getLocation().add(0, .35, 0));
                if (age % 40 == 0) target.sendMessage("§7weapon-ai " + pattern + " age=" + age
                        + " driver=" + compact(hidden.getLocation()) + " reference=" + compact(reference.getLocation()));
            }
        };
        tasks.add(task.runTaskTimer(plugin, 1, 1));
        return "weapon-ai comparison started: visible vanilla Vex + hidden Vex-driven " + pattern + " weapon (10s).";
    }

    private ItemDisplay display(Location at, Material material, float scale) {
        ItemDisplay display = at.getWorld().spawn(at, ItemDisplay.class, entity -> {
            entity.setItemStack(new ItemStack(material)); entity.setPersistent(false);
            DisplayMotion.configure(entity);
            Transformation transformation = entity.getTransformation(); transformation.getScale().set(new Vector3f(scale)); entity.setTransformation(transformation);
        });
        entities.add(display); return display;
    }

    private void impact(Location location, Player target, double radius, double damage) {
        location.getWorld().spawnParticle(Particle.EXPLOSION, location, 2);
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, .8f, .8f);
        if (target.getLocation().distanceSquared(location) <= radius * radius) target.damage(damage);
    }

    private Location offset(Location core, int index) { return core.clone().add(Math.cos(index * 2.1) * 2, 1.5, Math.sin(index * 2.1) * 2); }
    private boolean valid(Entity entity, Player player) { return entity.isValid() && player.isOnline() && entity.getWorld().equals(player.getWorld()); }
    private Material material(BasicWeaponPattern pattern) { return switch (pattern) { case MACE_MELEE -> Material.MACE; case SPEAR_MELEE -> Material.TRIDENT; case AXE_MELEE -> Material.NETHERITE_AXE; case HOE_MELEE -> Material.NETHERITE_HOE; default -> Material.AIR; }; }
    private Sound sound(BasicWeaponPattern pattern) { return switch (pattern) { case MACE_MELEE -> Sound.ITEM_MACE_SMASH_GROUND_HEAVY; case AXE_MELEE -> Sound.ITEM_AXE_STRIP; case HOE_MELEE -> Sound.ITEM_HOE_TILL; default -> Sound.ENTITY_PLAYER_ATTACK_SWEEP; }; }
    private void remove(Entity entity) { entities.remove(entity); entity.remove(); }
    private String compact(Location location) { return "%.1f,%.1f,%.1f".formatted(location.getX(), location.getY(), location.getZ()); }
    /** Vexes are movement-only internal drivers; their vanilla bite is always suppressed. */
    public boolean ownsHiddenDriver(Entity entity) {
        return entity instanceof Vex && (actors.stream().anyMatch(actor -> actor.driver().getUniqueId().equals(entity.getUniqueId()))
                || entities.stream().anyMatch(owned -> owned.getUniqueId().equals(entity.getUniqueId())));
    }
    public void cleanup() { tasks.forEach(BukkitTask::cancel); tasks.clear(); actors.forEach(VexWeaponActor::finish); actors.clear(); List.copyOf(entities).forEach(Entity::remove); entities.clear(); }
}
