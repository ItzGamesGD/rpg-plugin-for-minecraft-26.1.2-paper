package com.hyunseo.hyunseorpg.rpgtest.gateway;

import com.hyunseo.hyunseorpg.rpgtest.basic.BasicWeaponPattern;
import com.hyunseo.hyunseorpg.rpgtest.basic.BasicWeaponPatternSelector;
import com.hyunseo.hyunseorpg.rpgtest.basic.BasicWeaponRuntime;
import com.hyunseo.hyunseorpg.rpgtest.orbital.OrbitalWeaponCoreRuntime;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Trident;
import org.bukkit.entity.WindCharge;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** RPGTEST-only owner of gateway/basic prototype entities and tasks. */
public final class GatewayPrototypeService implements Listener {
    public static final int DEFAULT_GATEWAY_COUNT = 7;
    public static final int GATEWAY_TOTAL_CAP = 16;
    /** Caps attack-pattern AI actors. Projectiles emitted by an actor do not consume this cap. */
    public static final int BASIC_ACTOR_CAP = 14;
    public static final double PROJECTILE_SPEED_MULTIPLIER = .90;
    public static final long GATEWAY_RECOVERY_TICKS = 8;
    private static final Map<GatewayPayloadType, Integer> LOCAL_CAPS = Map.of(
            GatewayPayloadType.SONIC_BOOM, 2, GatewayPayloadType.BEAM, 2,
            GatewayPayloadType.DRAGON_BREATH, 2, GatewayPayloadType.FLAME_STREAM, 3,
            GatewayPayloadType.END_CRYSTAL_BOMB, 3);

    private final Plugin plugin;
    private final ConfigService config;
    private final GatewayPlacement placement = new GatewayPlacement();
    private final BasicWeaponPatternSelector basicSelector = new BasicWeaponPatternSelector();
    private final Map<UUID, GatewaySession> sessions = new HashMap<>();
    private final Map<UUID, RoutedProjectile> reflections = new HashMap<>();
    private final Map<UUID, RoutedProjectile> projectiles = new HashMap<>();
    private final Map<UUID, BasicWeaponRuntime> basics = new HashMap<>();
    private final Map<UUID, OrbitalWeaponCoreRuntime> orbitals = new HashMap<>();
    private final Map<UUID, GatewayBossRuntime> bossBattles = new HashMap<>();
    private final Random random = new Random();

    public GatewayPrototypeService(Plugin plugin, ConfigService config) { this.plugin = plugin; this.config = config; }
    public String place(Player player, boolean debug) { return startSession(player, debug, false); }
    public String pairing(Player player) { return startSession(player, true, false); }

    /** Starts the complete gateway-owned boss loop, not the legacy one-shot payload test. */
    public String bossBattle(Player player, boolean debug) {
        cleanup(player.getUniqueId());
        Location snapshot = player.getLocation().clone().add(0, 1, 0);
        GatewayBossConfig bossConfig = GatewayBossConfig.from(config);
        Location boss = bossPoint(player, bossConfig.phaseSpacing());
        GatewaySession session = new GatewaySession(player, snapshot, boss, List.of());
        ArmorStand stand = boss.getWorld().spawn(boss, ArmorStand.class, entity -> {
            entity.setGravity(false); entity.setPersistent(false); entity.setInvulnerable(true); entity.setVisible(true);
        });
        session.entities().add(stand); session.setDummy(new PrototypeBossDummy(stand));
        sessions.put(player.getUniqueId(), session);
        GatewayBossRuntime runtime = new GatewayBossRuntime(plugin, session, bossConfig,
                () -> cleanup(player.getUniqueId()), () -> triggerGatewayPhase(session, player, debug));
        bossBattles.put(player.getUniqueId(), runtime);
        runtime.start();
        return "Gateway Boss engaged: orbital/basic/gateway phases active; range="
                + bossConfig.attackRange();
    }

    /** Temporary special phase only; normal boss combat never owns Gateway visuals. */
    private void triggerGatewayPhase(GatewaySession session, Player player, boolean debug) {
        if (sessions.get(player.getUniqueId()) != session || !player.isOnline()) return;
        Location phaseSnapshot = player.getLocation().clone().add(0, 1, 0);
        if (!session.gatewayPhase().begin(phaseSnapshot)) return;
        int gatewayCount = Math.max(6, Math.min(16, config.getBossesInt("gateway-boss.gateway.count", DEFAULT_GATEWAY_COUNT)));
        double radialMin = Math.max(4.0D, Math.min(20.0D, config.getBossesDouble("gateway-boss.gateway.radial-min", 8.0D)));
        double radialMax = Math.max(8.0D, Math.min(24.0D, config.getBossesDouble("gateway-boss.gateway.radial-max", 14.0D)));
        if (radialMax < radialMin) radialMax = radialMin;
        double upperHeight = Math.max(2.0D, Math.min(16.0D, config.getBossesDouble("gateway-boss.gateway.upper-height", 5.0D)));
        double minSpacing = Math.max(2.0D, Math.min(8.0D, config.getBossesDouble("gateway-boss.gateway.minimum-spacing", GatewayPlacement.MIN_SPACING)));
        int payloadCap = Math.max(1, Math.min(48, config.getBossesInt("gateway-boss.gateway.global-payload-cap", GATEWAY_TOTAL_CAP)));
        List<Location> launchers = placement.launcherLocations(phaseSnapshot,
                gatewayCount, radialMin, radialMax, upperHeight, minSpacing, random, this::gatewaySpaceClear);
        if (launchers.size() != gatewayCount) { session.gatewayPhase().close(); return; }
        List<Location> returns = placement.returnLocations(session.bossTarget(), gatewayCount);
        if (returns.stream().anyMatch(location -> !returnSpaceClear(location))) { session.gatewayPhase().close(); return; }
        List<Entity> phaseVisuals = new ArrayList<>();
        List<GatewayPair> pairs = new ArrayList<>();
        for (int i = 0; i < launchers.size(); i++) {
            BlockDisplay launcher = gatewayDisplay(launchers.get(i), 2), returning = gatewayDisplay(returns.get(i), 1);
            phaseVisuals.add(launcher); phaseVisuals.add(returning); session.entities().add(launcher); session.entities().add(returning);
            pairs.add(new GatewayPair(i + 1, launchers.get(i), returns.get(i),
                    placement.snapshotForward(launchers.get(i), phaseSnapshot), launcher.getUniqueId(), returning.getUniqueId()));
        }
        session.gatewayPhase().deploy();
        GatewayPayloadScheduler scheduler = new GatewayPayloadScheduler(payloadCap, LOCAL_CAPS);
        long delay = 16;
        for (int attempt = 0; attempt < payloadCap * 4 && scheduler.total() < payloadCap; attempt++) {
            GatewayPayloadType type = GatewayPayloadType.values()[random.nextInt(GatewayPayloadType.values().length)];
            GatewayPair pair = pairs.get(random.nextInt(pairs.size()));
            long active = type.sustained() ? 50 : type == GatewayPayloadType.SONIC_BOOM ? 20 : 2;
            if (!scheduler.reserve(pair.id(), type, delay, active, GATEWAY_RECOVERY_TICKS)) { delay += 3; continue; }
            session.tasks().add(plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> { if (sessions.get(player.getUniqueId()) == session) fire(session, pair, type, debug); }, delay));
            delay += 7;
        }
        session.tasks().add(plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            session.gatewayPhase().resolve();
            phaseVisuals.forEach(Entity::remove);
            session.gatewayPhase().close();
        }, delay + 80));
    }

    public String randomCycle(Player player, boolean debug) {
        String result = startSession(player, debug, true);
        GatewaySession session = sessions.get(player.getUniqueId());
        if (session == null) return result;
        GatewayPayloadScheduler scheduler = new GatewayPayloadScheduler(GATEWAY_TOTAL_CAP, LOCAL_CAPS);
        long delay = 15;
        for (int attempt = 0; attempt < GATEWAY_TOTAL_CAP * 4 && scheduler.total() < GATEWAY_TOTAL_CAP; attempt++) {
            GatewayPayloadType type = GatewayPayloadType.values()[random.nextInt(GatewayPayloadType.values().length)];
            GatewayPair pair = session.pairs().get(random.nextInt(session.pairs().size()));
            long active = type.sustained() ? 50 : type == GatewayPayloadType.SONIC_BOOM ? 20 : 2;
            if (!scheduler.reserve(pair.id(), type, delay, active, GATEWAY_RECOVERY_TICKS)) { delay += 3; continue; }
            var task = plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> { if (sessions.get(player.getUniqueId()) == session) fire(session, pair, type, debug); }, delay);
            session.tasks().add(task); delay += 7;
        }
        return result + "; scheduled=" + scheduler.total() + "/" + GATEWAY_TOTAL_CAP;
    }

    public String payload(Player player, GatewayPayloadType type, boolean debug) {
        String start = startSession(player, debug, true);
        GatewaySession session = sessions.get(player.getUniqueId());
        if (session == null) return start;
        fire(session, session.pairs().getFirst(), type, debug);
        return "payload=" + type + ", reflectable=" + type.reflectable();
    }
    public String reflection(Player player) { return payload(player, GatewayPayloadType.ARROW, true); }

    public String basicRandom(Player player) {
        cleanup(player.getUniqueId());
        BasicWeaponRuntime runtime = new BasicWeaponRuntime(plugin); basics.put(player.getUniqueId(), runtime);
        Map<BasicWeaponPattern, Integer> selected = basicSelector.select(random, BASIC_ACTOR_CAP);
        runtime.start(player, bossPoint(player), selected);
        return "basic actors=" + selected.values().stream().mapToInt(Integer::intValue).sum() + "/" + BASIC_ACTOR_CAP + " patterns=" + selected;
    }
    public String basicPattern(Player player, BasicWeaponPattern pattern, int count) {
        cleanup(player.getUniqueId());
        int safe = Math.max(1, Math.min(BASIC_ACTOR_CAP, count));
        BasicWeaponRuntime runtime = new BasicWeaponRuntime(plugin); basics.put(player.getUniqueId(), runtime);
        runtime.start(player, bossPoint(player), Map.of(pattern, safe));
        return "basic pattern=" + pattern + " actors=" + safe;
    }
    public String orbitalCore(Player player, int rings) {
        cleanup(player.getUniqueId());
        OrbitalWeaponCoreRuntime runtime = new OrbitalWeaponCoreRuntime(plugin); orbitals.put(player.getUniqueId(), runtime);
        runtime.start(bossPoint(player), rings); return "orbital core rings=" + Math.max(1, Math.min(4, rings));
    }

    private String startSession(Player player, boolean debug, boolean dummy) {
        cleanup(player.getUniqueId());
        Location snapshot = player.getLocation().clone().add(0, 1, 0), boss = bossPoint(player);
        List<Location> launchers = placement.launcherLocations(snapshot, DEFAULT_GATEWAY_COUNT, 7, 4.5, random, this::gatewaySpaceClear);
        if (launchers.size() != DEFAULT_GATEWAY_COUNT) return "Gateway cast cancelled: insufficient clear positions.";
        List<Location> returns = placement.returnLocations(boss, DEFAULT_GATEWAY_COUNT);
        if (returns.stream().anyMatch(location -> !returnSpaceClear(location))) return "Gateway cast cancelled: return area obstructed.";
        List<GatewayPair> pairs = new ArrayList<>(); List<Entity> visuals = new ArrayList<>();
        for (int i = 0; i < DEFAULT_GATEWAY_COUNT; i++) {
            BlockDisplay launcher = gatewayDisplay(launchers.get(i), 2), returning = gatewayDisplay(returns.get(i), 1);
            visuals.add(launcher); visuals.add(returning);
            pairs.add(new GatewayPair(i + 1, launchers.get(i), returns.get(i), placement.snapshotForward(launchers.get(i), snapshot), launcher.getUniqueId(), returning.getUniqueId()));
        }
        GatewaySession session = new GatewaySession(player, snapshot, boss, pairs); session.entities().addAll(visuals);
        if (dummy) {
            ArmorStand stand = boss.getWorld().spawn(boss, ArmorStand.class, entity -> {
                entity.setGravity(false); entity.setPersistent(false); entity.setInvulnerable(true); entity.setVisible(true);
            });
            session.entities().add(stand); session.setDummy(new PrototypeBossDummy(stand));
        }
        sessions.put(player.getUniqueId(), session); if (debug) drawDebug(session);
        return "gateways=" + DEFAULT_GATEWAY_COUNT + ", P0=" + format(snapshot) + ", debug=" + debug;
    }

    private boolean gatewaySpaceClear(Location center) {
        if (center.getY() - 1 < center.getWorld().getMinHeight() || center.getY() + 2 >= center.getWorld().getMaxHeight()) return false;
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++)
            if (center.clone().add(x, y, z).getBlock().getType().isSolid()) return false;
        return true;
    }
    private boolean returnSpaceClear(Location center) {
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) for (int z = -1; z <= 1; z++)
            if (center.clone().add(x, y, z).getBlock().getType().isSolid()) return false;
        return true;
    }
    private BlockDisplay gatewayDisplay(Location center, float scale) {
        return center.getWorld().spawn(center, BlockDisplay.class, display -> {
            display.setBlock(Material.END_GATEWAY.createBlockData()); display.setPersistent(false);
            Transformation t = display.getTransformation(); t.getTranslation().set(-scale/2, -scale/2, -scale/2); t.getScale().set(new Vector3f(scale)); display.setTransformation(t);
        });
    }
    private void drawDebug(GatewaySession session) {
        session.snapshot().getWorld().spawnParticle(Particle.HAPPY_VILLAGER, session.snapshot(), 20, .2, .2, .2, 0);
        for (GatewayPair pair : session.pairs()) {
            for (double d=.5; d<=3; d+=.5) pair.launcher().getWorld().spawnParticle(Particle.END_ROD, pair.launcher().clone().add(pair.snapshotForward().multiply(d)), 1, 0,0,0,0);
            pair.returnGateway().getWorld().spawnParticle(Particle.WITCH, pair.returnGateway(), pair.id(), .15,.15,.15,0);
        }
    }

    private void fire(GatewaySession session, GatewayPair pair, GatewayPayloadType type, boolean debug) {
        if (type == GatewayPayloadType.END_CRYSTAL_BOMB) { fireCrystal(session, pair, debug); return; }
        if (!type.reflectable()) { fireVolume(session, pair, type); return; }
        Projectile projectile = spawnNativeProjectile(session, pair, type);
        Interaction reflection = projectile.getWorld().spawn(projectile.getLocation(), Interaction.class, hitbox -> {
            float size = reflectionSize(type); hitbox.setInteractionWidth(size); hitbox.setInteractionHeight(size); hitbox.setResponsive(true); hitbox.setPersistent(false);
        });
        session.entities().add(projectile); session.entities().add(reflection);
        ReflectableProjectileState state = new ReflectableProjectileState(projectile.getUniqueId(), pair.id(), type);
        RoutedProjectile routed = new RoutedProjectile(session, pair, state, projectile, reflection, debug);
        reflections.put(reflection.getUniqueId(), routed); projectiles.put(projectile.getUniqueId(), routed);
        routed.runTaskTimer(plugin, 1, 1);
    }

    private Projectile spawnNativeProjectile(GatewaySession session, GatewayPair pair, GatewayPayloadType type) {
        Location origin = pair.launcher().clone().add(pair.snapshotForward().multiply(1.2));
        Vector velocity = pair.snapshotForward().multiply(baseSpeed(type) * PROJECTILE_SPEED_MULTIPLIER);
        Projectile projectile = switch (type) {
            case ARROW -> origin.getWorld().spawn(origin, org.bukkit.entity.Arrow.class);
            case TRIDENT -> origin.getWorld().spawn(origin, Trident.class);
            case BLAZE_SMALL_FIREBALL -> origin.getWorld().spawn(origin, SmallFireball.class);
            case GHAST_FIREBALL -> origin.getWorld().spawn(origin, Fireball.class);
            case WITHER_SKULL -> origin.getWorld().spawn(origin, WitherSkull.class);
            case SHULKER_BULLET -> origin.getWorld().spawn(origin, ShulkerBullet.class);
            case WIND_CHARGE -> origin.getWorld().spawn(origin, WindCharge.class);
            default -> throw new IllegalArgumentException("No native projectile for " + type);
        };
        projectile.setShooter(null); projectile.setPersistent(false); projectile.setVelocity(velocity);
        if (projectile instanceof AbstractArrow arrow) { arrow.setDamage(type == GatewayPayloadType.ARROW ? 4 : 6); arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED); }
        if (projectile instanceof Fireball fireball) { fireball.setIsIncendiary(false); fireball.setYield(0); }
        if (projectile instanceof WitherSkull skull) { skull.setIsIncendiary(false); skull.setYield(0); }
        if (projectile instanceof ShulkerBullet bullet) {
            Player owner = plugin.getServer().getPlayer(session.ownerId()); if (owner != null) bullet.setTarget(owner);
        }
        return projectile;
    }

    private void fireCrystal(GatewaySession session, GatewayPair pair, boolean debug) {
        ItemDisplay crystal = pair.launcher().getWorld().spawn(pair.launcher(), ItemDisplay.class, item -> {
            item.setItemStack(new ItemStack(Material.END_CRYSTAL)); item.setPersistent(false);
            Transformation t=item.getTransformation(); t.getScale().set(new Vector3f(.55f)); item.setTransformation(t);
        });
        Interaction reflection = crystal.getWorld().spawn(crystal.getLocation(), Interaction.class, box -> {
            box.setInteractionWidth(1.15f); box.setInteractionHeight(1.15f); box.setResponsive(true); box.setPersistent(false);
        });
        session.entities().add(crystal); session.entities().add(reflection);
        CrystalRuntime runtime = new CrystalRuntime(session, pair, new ReflectableProjectileState(crystal.getUniqueId(), pair.id(), GatewayPayloadType.END_CRYSTAL_BOMB), crystal, reflection, debug);
        reflections.put(reflection.getUniqueId(), runtime); runtime.runTaskTimer(plugin, 1, 1);
    }

    private void fireVolume(GatewaySession session, GatewayPair pair, GatewayPayloadType type) {
        Particle particle = switch(type) { case SONIC_BOOM -> Particle.SONIC_BOOM; case DRAGON_BREATH -> Particle.DRAGON_BREATH; case FLAME_STREAM -> Particle.FLAME; default -> Particle.END_ROD; };
        int duration = type == GatewayPayloadType.SONIC_BOOM ? 20 : 50, telegraph = type == GatewayPayloadType.SONIC_BOOM || type == GatewayPayloadType.BEAM ? 12 : 4;
        BukkitRunnable task = new BukkitRunnable() {
            int age;
            @Override public void run() {
                if (++age > duration || !sessions.containsValue(session)) { cancel(); return; }
                Vector forward = pair.snapshotForward();
                Location origin = pair.launcher();
                RayTraceResult obstruction = origin.getWorld().rayTraceBlocks(origin, forward, 11, FluidCollisionMode.NEVER, true);
                Location endpoint = obstruction == null ? origin.clone().add(forward.clone().multiply(11))
                        : obstruction.getHitPosition().toLocation(origin.getWorld());
                double length = origin.distance(endpoint);
                for (double d=.5; d<=length; d+=.5) origin.getWorld().spawnParticle(particle, origin.clone().add(forward.clone().multiply(d)), 1,0,0,0,0);
                if (shouldDamageVolume(type, age, telegraph)) {
                    Player owner = plugin.getServer().getPlayer(session.ownerId());
                    if (owner != null && pointToSegmentDistance(owner.getEyeLocation().toVector(), origin.toVector(), endpoint.toVector()) <= volumeRadius(type))
                        owner.damage(volumeDamage(type));
                }
            }
        };
        session.tasks().add(task.runTaskTimer(plugin, 1, 1));
    }

    @EventHandler(ignoreCancelled = true) public void onReflect(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Vex) {
            for (GatewayBossRuntime battle : bossBattles.values()) {
                if (battle.ownsHiddenDriver(event.getDamager())) { event.setCancelled(true); return; }
            }
            for (BasicWeaponRuntime basic : basics.values()) {
                if (basic.ownsHiddenDriver(event.getDamager())) { event.setCancelled(true); return; }
            }
        }
        if (event.getDamager() instanceof Player) {
            for (GatewayBossRuntime battle : new ArrayList<>(bossBattles.values())) {
                if (battle.ownsDummy(event.getEntity())) {
                    event.setCancelled(true);
                    battle.playerStruckBoss(event.getFinalDamage());
                    return;
                }
            }
        }
        if (!(event.getDamager() instanceof Player)) return;
        RoutedProjectile runtime = reflections.get(event.getEntity().getUniqueId());
        if (runtime == null || runtime.state.phase() != ReflectableProjectileState.Phase.OUTBOUND) return;
        event.setCancelled(true); runtime.reflect();
    }
    @EventHandler public void onProjectileHit(ProjectileHitEvent event) {
        RoutedProjectile runtime = projectiles.get(event.getEntity().getUniqueId());
        if (runtime == null) return;
        boolean hitBoss = runtime.session.dummy() != null
                && event.getHitEntity() == runtime.session.dummy().entity();
        ReflectableProjectileState.CollisionResult result = runtime.state.collide(hitBoss);
        if (result == ReflectableProjectileState.CollisionResult.BOSS_HIT) {
            runtime.hitBoss(reflectedDamage(runtime.state.type()));
        }
        if (result != ReflectableProjectileState.CollisionResult.IGNORED) runtime.finish();
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { cleanup(event.getPlayer().getUniqueId()); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { cleanup(event.getPlayer().getUniqueId()); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { cleanup(event.getEntity().getUniqueId()); }

    public void cleanup(UUID owner) {
        GatewayBossRuntime battle=bossBattles.remove(owner); if (battle!=null) battle.dispose();
        GatewaySession session=sessions.remove(owner); if(session!=null) session.cleanup();
        BasicWeaponRuntime basic=basics.remove(owner); if(basic!=null) basic.cleanup();
        OrbitalWeaponCoreRuntime orbital=orbitals.remove(owner); if(orbital!=null) orbital.cleanup();
        reflections.entrySet().removeIf(entry -> entry.getValue().session.ownerId().equals(owner));
        projectiles.entrySet().removeIf(entry -> entry.getValue().session.ownerId().equals(owner));
    }
    public void shutdown() { new ArrayList<>(sessions.keySet()).forEach(this::cleanup); new ArrayList<>(bossBattles.keySet()).forEach(this::cleanup); new ArrayList<>(basics.keySet()).forEach(this::cleanup); new ArrayList<>(orbitals.keySet()).forEach(this::cleanup); }
    private Location bossPoint(Player player) { return bossPoint(player, 10.0D); }
    private Location bossPoint(Player player, double spacing) { Vector forward=player.getLocation().getDirection().setY(0); if(forward.lengthSquared()<.01)forward.setZ(1); return player.getLocation().clone().add(forward.normalize().multiply(spacing)).add(0,1,0); }
    private String format(Location l) { return "%.1f,%.1f,%.1f".formatted(l.getX(),l.getY(),l.getZ()); }
    private float reflectionSize(GatewayPayloadType type) { return type == GatewayPayloadType.BLAZE_SMALL_FIREBALL ? 1.35f : type == GatewayPayloadType.ARROW ? .9f : 1.1f; }
    private double baseSpeed(GatewayPayloadType type) { return switch(type) { case ARROW->1.5; case BLAZE_SMALL_FIREBALL->1.1; case TRIDENT->1.25; case WIND_CHARGE->.9; case SHULKER_BULLET->.55; case GHAST_FIREBALL->.65; case WITHER_SKULL->.7; default->.5; }; }
    private double reflectedDamage(GatewayPayloadType type) { return switch(type) { case ARROW->6; case TRIDENT->10; case BLAZE_SMALL_FIREBALL->7; case GHAST_FIREBALL->13; case WITHER_SKULL->11; case SHULKER_BULLET->8; case WIND_CHARGE->8; case END_CRYSTAL_BOMB->22; default->0; }; }
    private double volumeRadius(GatewayPayloadType type) { return switch(type) { case SONIC_BOOM, BEAM->.75; case FLAME_STREAM->1.2; default->1.5; }; }
    private double volumeDamage(GatewayPayloadType type) { return switch(type) { case SONIC_BOOM->8; case BEAM->7; case FLAME_STREAM->3; default->4; }; }
    static boolean shouldDamageVolume(GatewayPayloadType type, int age, int telegraph) {
        if (type == GatewayPayloadType.SONIC_BOOM) return age == telegraph;
        return type.sustained() && age >= telegraph && (age - telegraph) % 8 == 0;
    }
    static double pointToSegmentDistance(Vector p, Vector a, Vector b) {
        Vector ab=b.clone().subtract(a);
        if (ab.lengthSquared() < 1.0E-10) return p.distance(a);
        double t=Math.max(0,Math.min(1,p.clone().subtract(a).dot(ab)/ab.lengthSquared()));
        return p.distance(a.clone().add(ab.multiply(t)));
    }

    private class RoutedProjectile extends BukkitRunnable {
        final GatewaySession session; final GatewayPair pair; final ReflectableProjectileState state; final Entity projectile; final Interaction reflection; final boolean debug;
        int age;
        RoutedProjectile(GatewaySession s, GatewayPair p, ReflectableProjectileState st, Entity e, Interaction r, boolean d) {session=s;pair=p;state=st;projectile=e;reflection=r;debug=d;}
        void reflect() {
            state.reflect();
            if(projectile instanceof ShulkerBullet bullet) bullet.setTarget(null);
            if(projectile instanceof Projectile nativeProjectile) nativeProjectile.setShooter(null);
            aimOnce(pair.launcher(), .75);
            projectile.getWorld().playSound(projectile.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE,1,1.4f);
        }
        @Override public void run() {
            if(!projectile.isValid()||!reflection.isValid()||!sessions.containsValue(session)||age++>300){finish();return;}
            reflection.teleport(projectile.getLocation());
            if(state.phase()==ReflectableProjectileState.Phase.OUTBOUND)return;
            if(state.phase()==ReflectableProjectileState.Phase.RETURNING_TO_SOURCE) {
                Vector delta=pair.launcher().toVector().subtract(projectile.getLocation().toVector());
                if(delta.lengthSquared()<.8) {
                    state.enterSourceGateway();
                    projectile.teleport(pair.returnGateway());
                    reflection.teleport(pair.returnGateway());
                    aimOnce(session.bossTarget(), .85);
                    return;
                }
            }
            if(debug&&age%4==0)projectile.getWorld().spawnParticle(Particle.ENCHANTED_HIT,projectile.getLocation(),2,.05,.05,.05,0);
        }
        void aimOnce(Location destination, double speed) {
            Vector delta=destination.toVector().subtract(projectile.getLocation().toVector());
            if(delta.lengthSquared()>1.0E-8) projectile.setVelocity(delta.normalize().multiply(speed));
        }
        void hitBoss(double damage){if(session.dummy()!=null&&session.dummy().damage(damage)){session.bossTarget().getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,session.bossTarget(),12,.3,.5,.3,0);}}
        void finish(){state.finish();reflections.remove(reflection.getUniqueId());projectiles.remove(projectile.getUniqueId());projectile.remove();reflection.remove();cancel();}
    }

    private final class CrystalRuntime extends RoutedProjectile {
        int fuse;
        Vector outboundVelocity;
        Vector routeVelocity;
        CrystalRuntime(GatewaySession s, GatewayPair p, ReflectableProjectileState st, ItemDisplay e, Interaction r, boolean d){super(s,p,st,e,r,d);}
        @Override void reflect() {
            state.reflect();
            outboundVelocity = null;
            routeVelocity = direction(projectile.getLocation(), pair.launcher(), .7);
            projectile.getWorld().playSound(projectile.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE,1,1.4f);
        }
        @Override public void run(){
            if(!projectile.isValid()||!sessions.containsValue(session)||age++>300){finish();return;}
            reflection.teleport(projectile.getLocation());
            if(state.phase()==ReflectableProjectileState.Phase.FUSE){if(++fuse>=18)explode(false);return;}
            if(state.phase()==ReflectableProjectileState.Phase.OUTBOUND){
                if(outboundVelocity==null) outboundVelocity=pair.snapshotForward().multiply(.32);
                outboundVelocity.setY(outboundVelocity.getY()-.012);
                double speed=outboundVelocity.length();
                RayTraceResult hit=projectile.getWorld().rayTraceBlocks(projectile.getLocation(),outboundVelocity.clone().normalize(),speed, FluidCollisionMode.NEVER,true);
                if(hit!=null){projectile.teleport(hit.getHitPosition().toLocation(projectile.getWorld()));state.beginFuse();return;}
                RayTraceResult entity=projectile.getWorld().rayTraceEntities(projectile.getLocation(),outboundVelocity.clone().normalize(),speed,.45,
                        candidate -> candidate != projectile && candidate != reflection
                                && !(candidate instanceof BlockDisplay) && !(candidate instanceof ItemDisplay)
                                && !(candidate instanceof Interaction));
                if(entity!=null&&entity.getHitEntity()!=null){
                    if(entity.getHitEntity() instanceof Player owner&&owner.getUniqueId().equals(session.ownerId()))owner.damage(5);
                    state.beginFuse();return;
                }
                projectile.teleport(projectile.getLocation().add(outboundVelocity));
                return;
            }
            if(state.phase()==ReflectableProjectileState.Phase.RETURNING_TO_SOURCE
                    && projectile.getLocation().distanceSquared(pair.launcher())<.8) {
                state.enterSourceGateway();
                projectile.teleport(pair.returnGateway());
                reflection.teleport(pair.returnGateway());
                routeVelocity=direction(pair.returnGateway(),session.bossTarget(),.7);
                return;
            }
            if(routeVelocity==null || routeVelocity.lengthSquared()<1.0E-8){finish();return;}
            Location from=projectile.getLocation();
            double speed=routeVelocity.length();
            Vector direction=routeVelocity.clone().normalize();
            RayTraceResult block=from.getWorld().rayTraceBlocks(from,direction,speed,FluidCollisionMode.NEVER,true);
            if(block!=null){finish();return;}
            RayTraceResult entity=from.getWorld().rayTraceEntities(from,direction,speed,.45,
                    candidate -> candidate != projectile && candidate != reflection
                            && !(candidate instanceof BlockDisplay) && !(candidate instanceof ItemDisplay)
                            && !(candidate instanceof Interaction));
            if(entity!=null && entity.getHitEntity()!=null){
                boolean boss=session.dummy()!=null && entity.getHitEntity()==session.dummy().entity();
                if(state.collide(boss)==ReflectableProjectileState.CollisionResult.BOSS_HIT) explode(true);
                else finish();
                return;
            }
            projectile.teleport(from.clone().add(routeVelocity));
            reflection.teleport(projectile.getLocation());
        }
        private Vector direction(Location from,Location to,double speed){Vector delta=to.toVector().subtract(from.toVector());return delta.lengthSquared()<1.0E-8?new Vector():delta.normalize().multiply(speed);}
        private void explode(boolean reflected){Location at=projectile.getLocation();at.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER,at,1);at.getWorld().playSound(at,Sound.ENTITY_GENERIC_EXPLODE,1.2f,.8f);if(reflected)hitBoss(reflectedDamage(state.type()));else{Player owner=plugin.getServer().getPlayer(session.ownerId());if(owner!=null&&owner.getLocation().distanceSquared(at)<=16)owner.damage(9);}finish();}
    }
}
