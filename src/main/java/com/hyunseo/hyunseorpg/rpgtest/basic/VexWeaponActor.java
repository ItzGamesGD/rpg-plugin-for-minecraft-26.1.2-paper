package com.hyunseo.hyunseorpg.rpgtest.basic;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Vex;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

/**
 * The one shared implementation of a visible weapon driven by actual vanilla Vex AI.
 * It never steers normal melee movement itself: the display only mirrors the Vex position.
 */
public final class VexWeaponActor extends BukkitRunnable {
    public record Stats(int lifetimeTicks, double maceDamage, double axeDamage, double hoeDamage) { }

    private final Plugin plugin;
    private final Player target;
    private final ItemDisplay display;
    private final BasicWeaponPattern pattern;
    private final Stats stats;
    private final Consumer<Entity> entityRegistry;
    private final Runnable finishedCallback;
    private final Vex driver;
    private int age;
    private int nextStrike;
    private boolean tridentThrown;
    private boolean complete;

    private VexWeaponActor(Plugin plugin, Player target, ItemDisplay display, BasicWeaponPattern pattern, Stats stats,
                           Consumer<Entity> entityRegistry, Runnable finishedCallback) {
        this.plugin = plugin; this.target = target; this.display = display; this.pattern = pattern; this.stats = stats;
        this.entityRegistry = entityRegistry; this.finishedCallback = finishedCallback;
        this.driver = display.getWorld().spawn(display.getLocation(), Vex.class, vex -> {
            vex.setPersistent(false); vex.setInvisible(true); vex.setInvulnerable(true); vex.setSilent(true); vex.setCollidable(false);
            vex.getEquipment().clear(); vex.setTarget(target);
        });
        entityRegistry.accept(driver);
    }

    public static VexWeaponActor start(Plugin plugin, Player target, ItemDisplay display, BasicWeaponPattern pattern, Stats stats,
                                       Consumer<Entity> entityRegistry, Consumer<BukkitTask> taskRegistry, Runnable finishedCallback) {
        VexWeaponActor actor = new VexWeaponActor(plugin, target, display, pattern, stats, entityRegistry, finishedCallback);
        taskRegistry.accept(actor.runTaskTimer(plugin, 1, 1));
        return actor;
    }

    public Vex driver() { return driver; }
    public boolean complete() { return complete; }

    @Override public void run() {
        if (complete || !display.isValid() || !driver.isValid() || !target.isOnline() || target.isDead()
                || !driver.getWorld().equals(target.getWorld()) || ++age > stats.lifetimeTicks()) { finish(); return; }
        display.teleport(driver.getLocation().add(0, .35, 0));
        if (pattern == BasicWeaponPattern.TRIDENT_THROWER) tickTridentThrower();
        else tickMelee();
    }

    private void tickMelee() {
        if (age < nextStrike || target.getLocation().distanceSquared(display.getLocation()) > range() * range()) return;
        target.damage(damage());
        nextStrike = age + cadence();
        Vector push = target.getLocation().toVector().subtract(driver.getLocation().toVector());
        if (push.lengthSquared() > .001D && pattern == BasicWeaponPattern.MACE_MELEE) target.setVelocity(push.normalize().multiply(.9D).setY(.22D));
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 8, .25, .4, .25, .05);
        target.getWorld().playSound(target.getLocation(), sound(), .9F, pattern == BasicWeaponPattern.MACE_MELEE ? .75F : 1.05F);
    }

    private void tickTridentThrower() {
        // A short setup uses velocity only for RETREAT/AIM. Once it throws, target assignment is
        // restored exactly once and the actor resumes normal vanilla Vex pursuit for its lifetime.
        if (age < 18) {
            driver.setTarget(null);
            Vector away = driver.getLocation().toVector().subtract(target.getLocation().toVector());
            if (away.lengthSquared() > .001D) driver.setVelocity(away.normalize().multiply(.16D).setY(.06D));
            return;
        }
        if (age < 30) { driver.setVelocity(new Vector()); return; }
        if (!tridentThrown) {
            Vector aim = target.getEyeLocation().toVector().subtract(driver.getEyeLocation().toVector());
            if (aim.lengthSquared() > .001D) {
                Trident trident = driver.getWorld().spawn(driver.getEyeLocation(), Trident.class, projectile -> {
                    projectile.setPersistent(false); projectile.setVelocity(aim.normalize().multiply(1.2D));
                });
                entityRegistry.accept(trident);
            }
            tridentThrown = true;
            driver.setTarget(target);
            nextStrike = age + 18;
        }
        tickMelee();
    }

    private int cadence() { return switch (pattern) { case MACE_MELEE -> 30; case SPEAR_MELEE -> 18; case AXE_MELEE -> 24; case HOE_MELEE -> 16; default -> 20; }; }
    private double range() { return switch (pattern) { case MACE_MELEE -> 1.25D; case SPEAR_MELEE -> 1.6D; case AXE_MELEE -> 1.8D; case HOE_MELEE -> 2.1D; default -> 1.4D; }; }
    private double damage() { return switch (pattern) { case MACE_MELEE -> stats.maceDamage(); case AXE_MELEE -> stats.axeDamage(); case HOE_MELEE -> stats.hoeDamage(); default -> stats.maceDamage() * .75D; }; }
    private Sound sound() { return switch (pattern) { case MACE_MELEE -> Sound.ITEM_MACE_SMASH_GROUND_HEAVY; case AXE_MELEE -> Sound.ITEM_AXE_STRIP; case HOE_MELEE -> Sound.ITEM_HOE_TILL; default -> Sound.ENTITY_PLAYER_ATTACK_SWEEP; }; }
    public void finish() {
        if (complete) return;
        complete = true; driver.remove(); cancel(); finishedCallback.run();
    }
}
