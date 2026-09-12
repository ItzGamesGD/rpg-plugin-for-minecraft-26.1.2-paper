package com.hyunseo.hyunseorpg.rpgtest.basic;

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
            actor.setTarget(target);
            actor.setCharging(true);
            // A Vex is an internal movement driver only. The visible and damaging thing is
            // the paired ItemDisplay weapon below, never a Gateway Wraith/add mob.
            actor.getEquipment().clear();
            actor.setCustomNameVisible(false);
        });
        entities.add(vex);
        return vex;
    }

    private void spawnVexMelee(Player target, Location core, BasicWeaponPattern pattern, int offset) {
        Vex vex = vexActor(target, offset(core, offset), material(pattern));
        ItemDisplay weapon = display(vex.getLocation(), material(pattern), 1.35f);
        int interval = switch (pattern) { case MACE_MELEE -> 32; case SPEAR_MELEE -> 20; case AXE_MELEE -> 25; default -> 17; };
        double damage = switch (pattern) { case MACE_MELEE -> 7; case SPEAR_MELEE -> 4; case AXE_MELEE -> 5; default -> 3; };
        BukkitRunnable task = new BukkitRunnable() {
            int age;
            @Override public void run() {
                if (!valid(vex, target) || age++ > 400) { remove(weapon); remove(vex); cancel(); return; }
                vex.setTarget(target);
                weapon.teleport(vex.getLocation().add(0, .35, 0));
                if (age % interval == 0 && vex.getLocation().distanceSquared(target.getLocation()) < 4.0) {
                    target.damage(damage, vex);
                    Vector push = target.getLocation().toVector().subtract(vex.getLocation().toVector()).normalize()
                            .multiply(pattern == BasicWeaponPattern.MACE_MELEE ? 1.0 : .35).setY(.2);
                    target.setVelocity(push);
                    target.getWorld().playSound(target.getLocation(), sound(pattern), 1f, pattern == BasicWeaponPattern.MACE_MELEE ? .75f : 1.1f);
                }
            }
        };
        tasks.add(task.runTaskTimer(plugin, 1, 1));
    }

    private void spawnTridentThrower(Player target, Location core, int offset) {
        Vex vex = vexActor(target, offset(core, offset), Material.TRIDENT);
        ItemDisplay weapon = display(vex.getLocation(), Material.TRIDENT, 1.25f);
        BukkitRunnable task = new BukkitRunnable() {
            int age;
            enum State { RETREAT, AIM, THROW, RECOVER, RESUME }
            State state = State.RETREAT;
            @Override public void run() {
                if (!valid(vex, target) || age++ > 500) { remove(weapon); remove(vex); cancel(); return; }
                weapon.teleport(vex.getLocation().add(0, .35, 0));
                switch (state) {
                    case RETREAT -> {
                        vex.setTarget(null);
                        Vector away = vex.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(.25);
                        vex.setVelocity(away.setY(.08));
                        if (age >= 25 || vex.getLocation().distanceSquared(target.getLocation()) >= 64) { state = State.AIM; age = 0; }
                    }
                    case AIM -> { vex.setVelocity(new Vector()); if (age >= 18) { state = State.THROW; age = 0; } }
                    case THROW -> {
                        Vector aim = target.getEyeLocation().toVector().subtract(vex.getLocation().toVector()).normalize();
                        Trident trident = vex.getWorld().spawn(vex.getEyeLocation(), Trident.class, projectile -> {
                            projectile.setShooter(vex); projectile.setVelocity(aim.multiply(1.6)); projectile.setPersistent(false);
                        });
                        entities.add(trident); state = State.RECOVER; age = 0;
                    }
                    case RECOVER -> { if (age >= 35) { state = State.RESUME; age = 0; } }
                    case RESUME -> { vex.setTarget(target); if (age >= 55) { state = State.RETREAT; age = 0; } }
                }
            }
        };
        tasks.add(task.runTaskTimer(plugin, 1, 1));
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

    private ItemDisplay display(Location at, Material material, float scale) {
        ItemDisplay display = at.getWorld().spawn(at, ItemDisplay.class, entity -> {
            entity.setItemStack(new ItemStack(material)); entity.setPersistent(false);
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
    /** Vexes are movement-only internal drivers; their vanilla bite is always suppressed. */
    public boolean ownsHiddenDriver(Entity entity) {
        return entity instanceof Vex && entities.stream().anyMatch(owned -> owned.getUniqueId().equals(entity.getUniqueId()));
    }
    public void cleanup() { tasks.forEach(BukkitTask::cancel); tasks.clear(); List.copyOf(entities).forEach(Entity::remove); entities.clear(); }
}
