package com.hyunseo.hyunseorpg.rpgtest.gateway;

import com.hyunseo.hyunseorpg.rpgtest.basic.BasicWeaponPattern;
import com.hyunseo.hyunseorpg.rpgtest.basic.BasicWeaponPatternSelector;
import com.hyunseo.hyunseorpg.rpgtest.basic.VexWeaponActor;
import com.hyunseo.hyunseorpg.rpgtest.DisplayMotion;
import org.bukkit.Location;
import org.bukkit.FluidCollisionMode;
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
import org.bukkit.util.Transformation;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final BasicWeaponPatternSelector patternSelector = new BasicWeaponPatternSelector();
    private ItemDisplay coreDisplay;
    private long tick;
    private long nextAttackTick = 45;
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
            DisplayMotion.configure(display);
        });
        session.entities().add(coreDisplay);
        for (int slot = 0; slot < state.slotCount(); slot++) {
            final WeaponKind kind = weaponFor(slot);
            ItemDisplay display = core.getWorld().spawn(core, ItemDisplay.class, item -> {
                item.setItemStack(new ItemStack(materialFor(kind)));
                item.setPersistent(false);
                DisplayMotion.configure(item);
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
            int ring = GatewayOrbitGeometry.familyRing(slot, WeaponKind.values().length);
            int localSlot = GatewayOrbitGeometry.localPosition(slot, WeaponKind.values().length);
            double localAngle = tick * config.orbitSpeed() * (burst ? 1.35D : 1.0D) + localSlot * Math.PI;
            double planeAngle = tick * (config.ringPlaneSpeed() + ring * .003D) + ring * 1.17D;
            Vector planeRotated = GatewayOrbitGeometry.offset(ring, localSlot, tick, config.orbitRadius(), config.orbitSpeed(), config.ringPlaneSpeed(), burst);
            Location at = core.clone().add(planeRotated);
            display.teleport(at);
            display.setTransformation(transform(.55F, (float) (localAngle + planeAngle + Math.PI / 2D),
                    (float) (tick * .12D + slot), (float) (Math.sin(localAngle) * .45D)));
        }
    }

    private void scheduleNextPhase() {
        int cadence = isBurst() ? config.burstCooldownTicks() : config.attackCooldownTicks();
        nextAttackTick = tick + cadence;
        if (random.nextDouble() < config.gatewaySpecialChance()) {
            // The network appears only for this transient special phase, then the service removes it.
            gatewayPhase.run();
            return;
        }
        int capacity = Math.min(config.basicActorCap() - flights.size(), countEligibleOrbitWeapons());
        if (capacity <= 0) return;
        Map<BasicWeaponPattern, List<Integer>> selection = patternSelector.selectOrbitSlots(random, capacity, compatibleOrbitSlots());
        for (Map.Entry<BasicWeaponPattern, List<Integer>> entry : selection.entrySet()) {
            for (int slot : entry.getValue()) {
                if (!state.reserve(slot)) continue;
                flights.add(new WeaponFlight(slot, entry.getKey(), config.weaponThrowTelegraphTicks()));
            }
        }
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
        return state.randomOrbitingSlot(eligible, random);
    }

    private boolean isBurst() {
        return session.dummy() != null && session.dummy().health() / PrototypeBossDummy.MAX_HEALTH <= config.burstHealthThreshold();
    }

    private int randomOrbitingSlot(BasicWeaponPattern pattern) {
        if (pattern == BasicWeaponPattern.SHIELD_ORBIT) return -1;
        List<Integer> eligible = new ArrayList<>();
        for (int slot = 0; slot < state.slotCount(); slot++) {
            if (state.slotStatus(slot) == GatewayBossState.SlotStatus.ORBITING && weaponFor(slot) == familyFor(pattern)) eligible.add(slot);
        }
        return state.randomOrbitingSlot(eligible, random);
    }

    private Map<BasicWeaponPattern, List<Integer>> compatibleOrbitSlots() {
        Map<BasicWeaponPattern, List<Integer>> slots = new java.util.EnumMap<>(BasicWeaponPattern.class);
        for (BasicWeaponPattern pattern : BasicWeaponPattern.values()) slots.put(pattern, new ArrayList<>());
        for (int slot = 0; slot < state.slotCount(); slot++) {
            if (state.slotStatus(slot) != GatewayBossState.SlotStatus.ORBITING) continue;
            for (BasicWeaponPattern pattern : BasicWeaponPattern.values()) {
                if (pattern != BasicWeaponPattern.SHIELD_ORBIT && familyFor(pattern) == weaponFor(slot)) slots.get(pattern).add(slot);
            }
        }
        return slots;
    }

    private int countEligibleOrbitWeapons() {
        int count = 0;
        for (int slot = 0; slot < state.slotCount(); slot++)
            if (state.slotStatus(slot) == GatewayBossState.SlotStatus.ORBITING && weaponFor(slot) != WeaponKind.SHIELD) count++;
        return count;
    }

    private WeaponKind familyFor(BasicWeaponPattern pattern) {
        return switch (pattern) {
            case MACE_MELEE, MACE_DROP -> WeaponKind.MACE;
            case SPEAR_MELEE, SPEAR_LUNGE, TRIDENT_THROWER -> WeaponKind.SPEAR;
            case AXE_MELEE -> WeaponKind.AXE;
            case HOE_MELEE -> WeaponKind.HOE;
            case SHIELD_ORBIT -> WeaponKind.SHIELD;
        };
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
        private final BasicWeaponPattern pattern;
        private int telegraphTicks;
        private int age;
        private boolean launched;
        private long nextStrikeAge;
        private Vex hiddenDriver;
        private VexWeaponActor sharedActor;
        private Vector lockedDirection;

        private WeaponFlight(int slot, BasicWeaponPattern pattern, int telegraphTicks) {
            this.slot = slot; this.pattern = pattern; this.telegraphTicks = telegraphTicks;
        }
        boolean tick() {
            ItemDisplay display = orbitWeapons.get(slot);
            if (!display.isValid()) return true;
            if (!launched) {
                if (--telegraphTicks > 0) {
                    double orbit = tick * .20D + slot;
                    display.teleport(core.clone().add(Math.cos(orbit) * 1.35D, .75D + Math.sin(orbit) * .3D, Math.sin(orbit) * 1.35D));
                    display.setTransformation(transform(.78F, (float) orbit, (float) (tick * .24D), .45F));
                    if (telegraphTicks % 4 == 0) core.getWorld().spawnParticle(Particle.END_ROD, display.getLocation(), 4, .18, .18, .18, .01);
                    return false;
                }
                launched = true; state.launch(slot);
                Player owner = plugin.getServer().getPlayer(session.ownerId());
                if (owner == null) return true;
                if (requiresDriver()) {
                    if (usesSharedVexActor()) {
                        sharedActor = VexWeaponActor.start(plugin, owner, display, pattern,
                                new VexWeaponActor.Stats(Math.min(config.basicActorLifetimeTicks(), config.driverLifetimeTicks()),
                                        config.maceDamage(), config.axeDamage(), config.hoeDamage()),
                                session.entities()::add, session.tasks()::add, () -> { });
                        hiddenDriver = sharedActor.driver();
                    } else hiddenDriver = display.getWorld().spawn(display.getLocation(), Vex.class, vex -> {
                        vex.setPersistent(false); vex.setInvisible(true); vex.setInvulnerable(true); vex.setSilent(true); vex.setCollidable(false);
                        vex.getEquipment().clear(); vex.setTarget(owner);
                    });
                    if (!state.addDriver(hiddenDriver.getUniqueId(), config.maxActiveDrivers())) {
                        if (sharedActor != null) sharedActor.finish(); else hiddenDriver.remove(); hiddenDriver = null; return true;
                    }
                    session.entities().add(hiddenDriver);
                }
                if (pattern == BasicWeaponPattern.MACE_DROP)
                    display.teleport(owner.getLocation().clone().add(0, 8, 0));
                if (pattern == BasicWeaponPattern.SPEAR_LUNGE)
                    lockedDirection = owner.getEyeLocation().toVector().subtract(display.getLocation().toVector()).normalize();
                core.getWorld().playSound(display.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, .85F);
            }
            age++;
            Player owner = plugin.getServer().getPlayer(session.ownerId());
            if (owner == null || !owner.isOnline()) return true;
            if (pattern == BasicWeaponPattern.MACE_DROP) return tickDrop(display, owner);
            if (pattern == BasicWeaponPattern.SPEAR_LUNGE) return tickLunge(display, owner);
            if (sharedActor != null) {
                if (sharedActor.complete()) { state.removeDriver(hiddenDriver.getUniqueId()); return true; }
                return false;
            }
            if (pattern == BasicWeaponPattern.TRIDENT_THROWER) return tickTridentThrower(display, owner);
            if (hiddenDriver == null || !hiddenDriver.isValid()) return true;
            // The invisible Vex is strictly a movement driver. The display is the visible attack.
            display.teleport(hiddenDriver.getLocation().add(0, .35, 0));
            display.setTransformation(transform(.88F, (float) (age * .28D), (float) (age * .18D), (float) (age * .33D)));
            display.getWorld().spawnParticle(pattern == BasicWeaponPattern.AXE_MELEE ? Particle.CRIT : Particle.END_ROD,
                    display.getLocation(), 5, .12, .12, .12, .01);
            if (age >= nextStrikeAge && owner.getWorld().equals(display.getWorld())) {
                double radius = radiusFor(pattern);
                if (owner.getLocation().distanceSquared(display.getLocation()) <= radius * radius) {
                    owner.damage(damageFor(pattern));
                    nextStrikeAge = age + attackCadence(pattern);
                    display.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, owner.getLocation().add(0, 1, 0), 12, .35, .5, .35, .1);
                }
            }
            if (age >= Math.min(config.basicActorLifetimeTicks(), config.driverLifetimeTicks())) {
                display.getWorld().spawnParticle(Particle.END_ROD, display.getLocation(), 18, .25, .25, .25, .05);
                removeDriver();
                return true;
            }
            return false;
        }
        private boolean tickDrop(ItemDisplay display, Player owner) {
            Location before = display.getLocation();
            Vector step = new Vector(0, -.42D, 0);
            RayTraceResult block = before.getWorld().rayTraceBlocks(before, step.clone().normalize(), step.length(), FluidCollisionMode.NEVER, true);
            if (block != null) display.teleport(block.getHitPosition().toLocation(before.getWorld()));
            else display.teleport(before.add(step));
            display.setTransformation(transform(.95F, age * .32F, 0, 0));
            if (block != null || display.getBoundingBox().expand(.25).overlaps(owner.getBoundingBox())) {
                if (owner.getLocation().distanceSquared(display.getLocation()) <= 9) owner.damage(config.maceDamage());
                display.getWorld().spawnParticle(Particle.EXPLOSION, display.getLocation(), 2, .25, .1, .25, .01);
                return true;
            }
            return age >= config.basicActorLifetimeTicks();
        }
        private boolean tickLunge(ItemDisplay display, Player owner) {
            display.teleport(display.getLocation().add(lockedDirection.clone().multiply(config.weaponThrowSpeed())));
            display.setTransformation(transform(.92F, age * .34F, 0, .3F));
            if (owner.getLocation().distanceSquared(display.getLocation()) <= 2.25D) { owner.damage(damageFor(pattern)); return true; }
            return age >= 36;
        }
        private boolean tickTridentThrower(ItemDisplay display, Player owner) {
            if (hiddenDriver == null || !hiddenDriver.isValid()) return true;
            if (age < 28) {
                // Retreat is a short scripted setup only; after the throw the actual Vex AI resumes.
                hiddenDriver.setTarget(null);
                if (age < 14) hiddenDriver.setVelocity(hiddenDriver.getLocation().toVector().subtract(owner.getLocation().toVector()).normalize().multiply(.16D).setY(.06D));
                else hiddenDriver.setVelocity(new Vector());
            } else if (age == 28) hiddenDriver.setTarget(owner);
            display.teleport(hiddenDriver.getLocation().add(0, .35, 0));
            if (age == 20) {
                Vector aim = owner.getEyeLocation().toVector().subtract(display.getLocation().toVector()).normalize();
                Trident trident = display.getWorld().spawn(display.getLocation(), Trident.class, projectile -> {
                    projectile.setVelocity(aim.multiply(config.weaponThrowSpeed() * 2.8D)); projectile.setPersistent(false);
                });
                session.entities().add(trident);
            }
            if (age >= Math.min(config.basicActorLifetimeTicks(), config.driverLifetimeTicks())) { removeDriver(); return true; }
            return false;
        }
        private boolean requiresDriver() { return pattern != BasicWeaponPattern.MACE_DROP && pattern != BasicWeaponPattern.SPEAR_LUNGE; }
        private boolean usesSharedVexActor() {
            return pattern == BasicWeaponPattern.MACE_MELEE || pattern == BasicWeaponPattern.SPEAR_MELEE
                    || pattern == BasicWeaponPattern.AXE_MELEE || pattern == BasicWeaponPattern.HOE_MELEE
                    || pattern == BasicWeaponPattern.TRIDENT_THROWER;
        }
        private double radiusFor(BasicWeaponPattern weapon) {
            return switch (weapon) { case MACE_MELEE -> 1.25D; case SPEAR_MELEE -> 1.45D; case AXE_MELEE -> 2.0D; case HOE_MELEE -> 2.3D; default -> 1.2D; };
        }
        private void removeDriver() {
            if (hiddenDriver != null) { state.removeDriver(hiddenDriver.getUniqueId()); if (sharedActor != null) sharedActor.finish(); else hiddenDriver.remove(); hiddenDriver = null; }
        }
        private double damageFor(BasicWeaponPattern weapon) {
            return switch (weapon) {
                case MACE_MELEE, MACE_DROP -> config.maceDamage();
                case SPEAR_MELEE, SPEAR_LUNGE, TRIDENT_THROWER -> config.maceDamage() * .75D;
                case AXE_MELEE -> config.axeDamage(); case HOE_MELEE -> config.hoeDamage();
                case SHIELD_ORBIT -> 0.0D;
            };
        }
        private int attackCadence(BasicWeaponPattern weapon) {
            return switch (weapon) { case MACE_MELEE -> 30; case SPEAR_MELEE -> 18; case AXE_MELEE -> 24; case HOE_MELEE -> 16; default -> 20; };
        }
    }

}
