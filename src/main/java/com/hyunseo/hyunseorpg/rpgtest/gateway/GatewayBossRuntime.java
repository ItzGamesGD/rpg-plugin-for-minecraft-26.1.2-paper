package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * One bounded Gateway Boss encounter. The gateway owns the combat; the armor stand is only a
 * damageable training target. Every display, Vex and timer is registered with the session.
 */
final class GatewayBossRuntime extends BukkitRunnable {
    private enum WeaponKind { MACE, SPEAR, AXE, HOE, SHIELD }

    private final Plugin plugin;
    private final GatewaySession session;
    private final GatewayBossConfig config;
    private final Runnable endSession;
    private final Runnable gatewayPhase;
    private final GatewayBossState state;
    private final Location core;
    private final List<ItemDisplay> orbitWeapons = new ArrayList<>();
    private final List<WeaponFlight> flights = new ArrayList<>();
    private final Random random = new Random();
    private ItemDisplay coreDisplay;
    private long tick;
    private long nextAttackTick = 45;
    private long nextParryTick;
    private boolean disposed;

    GatewayBossRuntime(Plugin plugin, GatewaySession session, GatewayBossConfig config, Runnable endSession, Runnable gatewayPhase) {
        this.plugin = plugin;
        this.session = session;
        this.config = config;
        this.endSession = endSession;
        this.gatewayPhase = gatewayPhase;
        this.state = new GatewayBossState(config.maxOrbitWeapons());
        this.core = session.bossTarget();
    }

    void start() {
        // This is a normal combat core, deliberately not an END_GATEWAY. Gateways are special-phase only.
        coreDisplay = core.getWorld().spawn(core, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.END_CRYSTAL));
            display.setPersistent(false);
        });
        session.entities().add(coreDisplay);
        for (int slot = 0; slot < state.slotCount(); slot++) {
            final WeaponKind kind = weaponFor(slot);
            ItemDisplay display = core.getWorld().spawn(core, ItemDisplay.class, item -> {
                item.setItemStack(new ItemStack(materialFor(kind)));
                item.setPersistent(false);
            });
            orbitWeapons.add(display);
            session.entities().add(display);
        }
        BukkitTask task = runTaskTimer(plugin, 1, 1);
        session.tasks().add(task);
    }

    boolean ownsDummy(Entity entity) {
        return session.dummy() != null && session.dummy().entity().getUniqueId().equals(entity.getUniqueId());
    }

    boolean ownsHiddenDriver(Entity entity) {
        return flights.stream().anyMatch(flight -> flight.hiddenDriver != null
                && flight.hiddenDriver.getUniqueId().equals(entity.getUniqueId()));
    }

    void playerStruckBoss(double damage) {
        if (disposed || session.dummy() == null) return;
        if (session.dummy().damage(Math.max(1.0D, damage))) {
            pulseGateway(1.25F, Particle.ENCHANTED_HIT);
            if (session.dummy().health() <= 0.0D) {
                endSession.run();
                return;
            }
        }
        if (tick >= nextParryTick) {
            int slot = randomOrbitingSlot();
            if (slot >= 0) {
                state.reserve(slot);
                flights.add(new WeaponFlight(slot, WeaponKind.SPEAR, true, config.parryTelegraphTicks()));
                nextParryTick = tick + config.parryCooldownTicks();
            }
        }
    }

    @Override public void run() {
        if (disposed || !sessionStillUsable()) { endSession.run(); return; }
        tick++;
        updateCore();
        updateOrbitWeapons();
        updateFlights();
        if (tick >= nextAttackTick && playerWithinRange()) scheduleNextPhase();
    }

    void dispose() {
        disposed = true;
        flights.clear();
        state.cleanup();
        cancel();
    }

    private boolean sessionStillUsable() {
        Player owner = plugin.getServer().getPlayer(session.ownerId());
        return owner != null && owner.isOnline() && !owner.isDead()
                && owner.getWorld().equals(core.getWorld()) && coreDisplay != null && coreDisplay.isValid();
    }

    private boolean playerWithinRange() {
        Player owner = plugin.getServer().getPlayer(session.ownerId());
        return owner != null && owner.getWorld().equals(core.getWorld())
                && owner.getLocation().distanceSquared(core) <= config.attackRange() * config.attackRange();
    }

    private void updateCore() {
        float pulse = 2.4F + (float) (Math.sin(tick * .11D) * .18D);
        coreDisplay.setTransformation(transform(pulse, (float) (tick * .025D), 0, 0));
        if (tick % 8 == 0) core.getWorld().spawnParticle(Particle.WITCH, core, 7, .75, .85, .75, .01);
    }

    private void updateOrbitWeapons() {
        boolean burst = isBurst();
        for (int slot = 0; slot < orbitWeapons.size(); slot++) {
            if (state.slotStatus(slot) != GatewayBossState.SlotStatus.ORBITING) continue;
            ItemDisplay display = orbitWeapons.get(slot);
            if (!display.isValid()) { endSession.run(); return; }
            // Two local weapon slots per family ring.  The local orbit moves first, then its
            // orbital plane itself rotates: an armillary core, not stacked horizontal circles.
            int ring = weaponFor(slot).ordinal();
            int localSlot = slot / WeaponKind.values().length;
            double localAngle = tick * config.orbitSpeed() * (burst ? 1.35D : 1.0D) + localSlot * Math.PI;
            double planeAngle = tick * (.011D + ring * .003D) + ring * 1.17D;
            double radius = config.orbitRadius() + ring * .55D;
            Vector localPoint = new Vector(Math.cos(localAngle) * radius, Math.sin(localAngle) * radius, 0);
            Vector planeRotated = rotateX(rotateY(localPoint, planeAngle), .48D + ring * .42D);
            Location at = core.clone().add(planeRotated);
            display.teleport(at);
            display.setTransformation(transform(1.45F, (float) (localAngle + planeAngle + Math.PI / 2D),
                    (float) (tick * .12D + slot), (float) (Math.sin(localAngle) * .45D)));
        }
    }

    private Vector rotateY(Vector value, double angle) {
        return new Vector(value.getX() * Math.cos(angle) + value.getZ() * Math.sin(angle), value.getY(),
                -value.getX() * Math.sin(angle) + value.getZ() * Math.cos(angle));
    }
    private Vector rotateX(Vector value, double angle) {
        return new Vector(value.getX(), value.getY() * Math.cos(angle) - value.getZ() * Math.sin(angle),
                value.getY() * Math.sin(angle) + value.getZ() * Math.cos(angle));
    }

    private void scheduleNextPhase() {
        int cadence = isBurst() ? config.burstCooldownTicks() : config.attackCooldownTicks();
        nextAttackTick = tick + cadence;
        int choice = random.nextInt(5);
        if (choice == 4) {
            // The network appears only for this transient special phase, then the service removes it.
            gatewayPhase.run();
            return;
        }
        int slot = randomOrbitingSlot();
        if (slot < 0) return;
        state.reserve(slot);
        flights.add(new WeaponFlight(slot, weaponFor(slot), false, config.weaponThrowTelegraphTicks()));
    }

    private void updateFlights() {
        for (int index = flights.size() - 1; index >= 0; index--) {
            WeaponFlight flight = flights.get(index);
            if (flight.tick()) {
                state.recover(flight.slot);
                flights.remove(index);
            }
        }
    }


    private int randomOrbitingSlot() {
        List<Integer> eligible = new ArrayList<>();
        for (int slot = 0; slot < state.slotCount(); slot++) {
            if (state.slotStatus(slot) == GatewayBossState.SlotStatus.ORBITING) eligible.add(slot);
        }
        eligible.removeIf(slot -> weaponFor(slot) == WeaponKind.SHIELD);
        return eligible.isEmpty() ? -1 : eligible.get(random.nextInt(eligible.size()));
    }

    private boolean isBurst() {
        return session.dummy() != null && session.dummy().health() / PrototypeBossDummy.MAX_HEALTH <= config.burstHealthThreshold();
    }

    private void pulseGateway(float scale, Particle particle) {
        if (coreDisplay != null && coreDisplay.isValid()) coreDisplay.setTransformation(transform(scale, (float) tick * .2F, .4F, .2F));
        core.getWorld().spawnParticle(particle, core, 24, .7, .8, .7, .04);
        core.getWorld().playSound(core, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.1F, .8F);
    }

    private Transformation transform(float scale, float yaw, float pitch, float roll) {
        return new Transformation(new Vector3f(-scale / 2F), new Quaternionf().rotationYXZ(yaw, pitch, roll),
                new Vector3f(scale), new Quaternionf());
    }

    private WeaponKind weaponFor(int slot) { return WeaponKind.values()[slot % WeaponKind.values().length]; }
    private Material materialFor(WeaponKind kind) {
        return switch (kind) { case MACE -> Material.MACE; case SPEAR -> Material.TRIDENT; case AXE -> Material.NETHERITE_AXE; case HOE -> Material.NETHERITE_HOE; case SHIELD -> Material.SHIELD; };
    }

    private final class WeaponFlight {
        private final int slot;
        private final WeaponKind kind;
        private final boolean counter;
        private int telegraphTicks;
        private int age;
        private boolean launched;
        private boolean hit;
        private Vex hiddenDriver;

        private WeaponFlight(int slot, WeaponKind kind, boolean counter, int telegraphTicks) {
            this.slot = slot; this.kind = kind; this.counter = counter; this.telegraphTicks = telegraphTicks;
        }
        boolean tick() {
            ItemDisplay display = orbitWeapons.get(slot);
            if (!display.isValid()) return true;
            if (!launched) {
                if (--telegraphTicks > 0) {
                    double orbit = tick * .20D + slot;
                    display.teleport(core.clone().add(Math.cos(orbit) * 1.35D, .75D + Math.sin(orbit) * .3D, Math.sin(orbit) * 1.35D));
                    display.setTransformation(transform(2.45F, (float) orbit, (float) (tick * .24D), .45F));
                    if (telegraphTicks % 4 == 0) core.getWorld().spawnParticle(Particle.END_ROD, display.getLocation(), 4, .18, .18, .18, .01);
                    return false;
                }
                launched = true; state.launch(slot);
                Player owner = plugin.getServer().getPlayer(session.ownerId());
                if (owner == null) return true;
                hiddenDriver = display.getWorld().spawn(display.getLocation(), Vex.class, vex -> {
                    vex.setPersistent(false); vex.setInvisible(true); vex.setInvulnerable(true); vex.setSilent(true);
                    vex.setTarget(owner); vex.setCharging(true);
                });
                session.entities().add(hiddenDriver);
                core.getWorld().playSound(display.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2F, counter ? .65F : .85F);
            }
            age++;
            if (hiddenDriver == null || !hiddenDriver.isValid()) return true;
            Player owner = plugin.getServer().getPlayer(session.ownerId());
            if (owner == null || !owner.isOnline()) return true;
            // The invisible Vex is strictly a movement driver.  The visible, damaging actor is this
            // detached orbital weapon display; it follows the driver's vanilla aerial pursuit.
            hiddenDriver.setTarget(owner);
            display.teleport(hiddenDriver.getLocation().add(0, .35, 0));
            display.setTransformation(transform(2.65F, (float) (age * .28D), (float) (age * .18D), (float) (age * .33D)));
            display.getWorld().spawnParticle(kind == WeaponKind.AXE ? Particle.CRIT : Particle.END_ROD,
                    display.getLocation(), 5, .12, .12, .12, .01);
            if (!hit && owner.getWorld().equals(display.getWorld())) {
                double radius = kind == WeaponKind.MACE ? 1.25D : kind == WeaponKind.SPEAR ? 1.45D : kind == WeaponKind.AXE ? 2.0D : 2.3D;
                if (owner.getLocation().distanceSquared(display.getLocation()) <= radius * radius) {
                    owner.damage(damageFor(kind)); hit = true;
                    display.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, owner.getLocation().add(0, 1, 0), 12, .35, .5, .35, .1);
                }
            }
            if (age >= config.cleanupTimeoutTicks() || hit) {
                display.getWorld().spawnParticle(Particle.END_ROD, display.getLocation(), 18, .25, .25, .25, .05);
                hiddenDriver.remove();
                return true;
            }
            return false;
        }
        private double damageFor(WeaponKind weapon) {
            if (counter) return config.swordThrowDamage();
            return switch (weapon) { case MACE -> config.swordThrowDamage(); case SPEAR -> config.swordThrowDamage() * .75D; case AXE -> config.axeSpinDamage(); case HOE -> config.hoeSweepDamage(); case SHIELD -> 0.0D; };
        }
    }

}
