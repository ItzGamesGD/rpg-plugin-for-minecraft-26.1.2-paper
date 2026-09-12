package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * One bounded Gateway Boss encounter. The gateway owns the combat; the armor stand is only a
 * damageable training target. Every display, Vex and timer is registered with the session.
 */
final class GatewayBossRuntime extends BukkitRunnable {
    private enum WeaponKind { SWORD, AXE, HOE }

    private final Plugin plugin;
    private final GatewaySession session;
    private final GatewayBossConfig config;
    private final Runnable endSession;
    private final GatewayBossState state;
    private final Location core;
    private final List<ItemDisplay> orbitWeapons = new ArrayList<>();
    private final List<WeaponFlight> flights = new ArrayList<>();
    private final List<PendingSummon> pendingSummons = new ArrayList<>();
    private final Map<UUID, Integer> summonAges = new HashMap<>();
    private final Random random = new Random();
    private BlockDisplay coreDisplay;
    private long tick;
    private long nextAttackTick = 45;
    private long nextParryTick;
    private boolean disposed;

    GatewayBossRuntime(Plugin plugin, GatewaySession session, GatewayBossConfig config, Runnable endSession) {
        this.plugin = plugin;
        this.session = session;
        this.config = config;
        this.endSession = endSession;
        this.state = new GatewayBossState(config.maxOrbitWeapons());
        this.core = session.bossTarget();
    }

    void start() {
        coreDisplay = core.getWorld().spawn(core, BlockDisplay.class, display -> {
            display.setBlock(Material.END_GATEWAY.createBlockData());
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
            int slot = firstOrbitingSlot();
            if (slot >= 0) {
                state.reserve(slot);
                flights.add(new WeaponFlight(slot, WeaponKind.SWORD, true, config.parryTelegraphTicks()));
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
        updateSummons();
        if (tick >= nextAttackTick && playerWithinRange()) scheduleNextPhase();
    }

    void dispose() {
        disposed = true;
        flights.clear();
        pendingSummons.clear();
        summonAges.clear();
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
            double angle = tick * config.orbitSpeed() * (burst ? 1.35D : 1.0D)
                    + Math.PI * 2D * slot / orbitWeapons.size();
            double radius = config.orbitRadius() + Math.sin(tick * .06D + slot) * .35D;
            Location at = core.clone().add(Math.cos(angle) * radius, .45D + Math.sin(angle * 2D) * .9D,
                    Math.sin(angle) * radius);
            display.teleport(at);
            display.setTransformation(transform(1.45F, (float) (angle + Math.PI / 2D),
                    (float) (tick * .12D + slot), (float) (Math.sin(angle) * .45D)));
        }
    }

    private void scheduleNextPhase() {
        int cadence = isBurst() ? config.burstCooldownTicks() : config.attackCooldownTicks();
        nextAttackTick = tick + cadence;
        int choice = random.nextInt(4);
        if (choice == 3 && state.activeSummons() < config.maxActiveSummons()) {
            pendingSummons.add(new PendingSummon(18));
            pulseGateway(2.8F, Particle.END_ROD);
            return;
        }
        int slot = firstOrbitingSlot();
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

    private void updateSummons() {
        for (int index = pendingSummons.size() - 1; index >= 0; index--) {
            PendingSummon pending = pendingSummons.get(index);
            if (--pending.telegraphTicks > 0) {
                core.getWorld().spawnParticle(Particle.END_ROD, core, 3, .55, .55, .55, .01);
                continue;
            }
            int remaining = config.maxActiveSummons() - state.activeSummons();
            for (int count = 0; count < Math.min(config.vexSummonCount(), remaining); count++) spawnVex(count);
            pendingSummons.remove(index);
        }
        for (var iterator = summonAges.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            Entity entity = plugin.getServer().getEntity(entry.getKey());
            if (!(entity instanceof Vex vex) || !vex.isValid() || entry.getValue() >= config.summonLifetimeTicks()) {
                if (entity != null && entity.isValid()) entity.remove();
                state.removeSummon(entry.getKey()); iterator.remove(); continue;
            }
            Player owner = plugin.getServer().getPlayer(session.ownerId());
            if (owner != null) vex.setTarget(owner);
            entry.setValue(entry.getValue() + 1);
        }
    }

    private void spawnVex(int offset) {
        Location at = core.clone().add((offset - .5D) * 1.4D, .4D, 0);
        Vex vex = core.getWorld().spawn(at, Vex.class, entity -> {
            entity.setPersistent(false);
            entity.setCustomName("§5Gateway Wraith");
            entity.setCustomNameVisible(false);
            entity.setGlowing(true);
        });
        if (!state.addSummon(vex.getUniqueId(), config.maxActiveSummons())) { vex.remove(); return; }
        Player owner = plugin.getServer().getPlayer(session.ownerId());
        if (owner != null) vex.setTarget(owner);
        summonAges.put(vex.getUniqueId(), 0);
        session.entities().add(vex);
        core.getWorld().spawnParticle(Particle.WITCH, at, 14, .35, .35, .35, .04);
        core.getWorld().playSound(at, Sound.ENTITY_VEX_CHARGE, .8F, .75F);
    }

    private int firstOrbitingSlot() {
        for (int offset = 0; offset < state.slotCount(); offset++) {
            int slot = (int) ((tick + offset) % state.slotCount());
            if (state.slotStatus(slot) == GatewayBossState.SlotStatus.ORBITING) return slot;
        }
        return -1;
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
        return switch (kind) { case SWORD -> Material.NETHERITE_SWORD; case AXE -> Material.NETHERITE_AXE; case HOE -> Material.NETHERITE_HOE; };
    }

    private final class WeaponFlight {
        private final int slot;
        private final WeaponKind kind;
        private final boolean counter;
        private int telegraphTicks;
        private int age;
        private boolean launched;
        private boolean hit;
        private Vector velocity;

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
                Vector delta = owner.getEyeLocation().toVector().subtract(display.getLocation().toVector());
                if (delta.lengthSquared() < .001D) delta = new Vector(0, 0, 1);
                velocity = delta.normalize().multiply(counter ? config.parryReturnSpeed() : config.weaponThrowSpeed());
                core.getWorld().playSound(display.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2F, counter ? .65F : .85F);
            }
            age++;
            Location from = display.getLocation();
            Vector step = velocity.clone();
            if (kind == WeaponKind.HOE) {
                Vector side = new Vector(-velocity.getZ(), 0, velocity.getX()).normalize().multiply(Math.sin(age * .22D) * .14D);
                step.add(side);
            }
            display.teleport(from.add(step));
            display.setTransformation(transform(2.65F, (float) (age * .28D), (float) (age * .18D), (float) (age * .33D)));
            display.getWorld().spawnParticle(kind == WeaponKind.AXE ? Particle.CRIT : Particle.END_ROD,
                    display.getLocation(), 5, .12, .12, .12, .01);
            Player owner = plugin.getServer().getPlayer(session.ownerId());
            if (!hit && owner != null && owner.getWorld().equals(display.getWorld())) {
                double radius = kind == WeaponKind.SWORD ? 1.25D : kind == WeaponKind.AXE ? 2.0D : 2.3D;
                if (owner.getLocation().distanceSquared(display.getLocation()) <= radius * radius) {
                    owner.damage(damageFor(kind)); hit = true;
                    display.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, owner.getLocation().add(0, 1, 0), 12, .35, .5, .35, .1);
                }
            }
            if (age >= config.cleanupTimeoutTicks() || hit) {
                display.getWorld().spawnParticle(Particle.END_ROD, display.getLocation(), 18, .25, .25, .25, .05);
                return true;
            }
            return false;
        }
        private double damageFor(WeaponKind weapon) {
            if (counter) return config.swordThrowDamage();
            return switch (weapon) { case SWORD -> config.swordThrowDamage(); case AXE -> config.axeSpinDamage(); case HOE -> config.hoeSweepDamage(); };
        }
    }

    private static final class PendingSummon {
        private int telegraphTicks;
        private PendingSummon(int telegraphTicks) { this.telegraphTicks = telegraphTicks; }
    }
}
