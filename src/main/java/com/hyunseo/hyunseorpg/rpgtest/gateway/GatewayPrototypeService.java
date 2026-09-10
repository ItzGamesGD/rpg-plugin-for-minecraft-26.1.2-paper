package com.hyunseo.hyunseorpg.rpgtest.gateway;

import com.hyunseo.hyunseorpg.rpgtest.basic.BasicWeaponPattern;
import com.hyunseo.hyunseorpg.rpgtest.basic.BasicWeaponPatternSelector;
import com.hyunseo.hyunseorpg.rpgtest.basic.BasicWeaponRuntime;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
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
    public static final int BASIC_TOTAL_CAP = 14;
    public static final double PROJECTILE_SPEED_MULTIPLIER = 0.90;
    public static final long GATEWAY_RECOVERY_TICKS = 8L;

    private static final Map<GatewayPayloadType, Integer> LOCAL_CAPS = Map.of(
            GatewayPayloadType.SONIC_BOOM, 2,
            GatewayPayloadType.BEAM, 2,
            GatewayPayloadType.DRAGON_BREATH, 2,
            GatewayPayloadType.FLAME_STREAM, 3,
            GatewayPayloadType.END_CRYSTAL_BOMB, 3);

    private final Plugin plugin;
    private final GatewayPlacement placement = new GatewayPlacement();
    private final BasicWeaponPatternSelector basicSelector = new BasicWeaponPatternSelector();
    private final Map<UUID, GatewaySession> sessions = new HashMap<>();
    private final Map<UUID, ProjectileRuntime> projectilesByHitbox = new HashMap<>();
    private final Map<UUID, BasicWeaponRuntime> basics = new HashMap<>();
    private final Random random = new Random();

    public GatewayPrototypeService(Plugin plugin) { this.plugin = plugin; }

    public String place(Player player, boolean debug) { return startSession(player, debug, false); }
    public String pairing(Player player) { return startSession(player, true, false); }

    public String randomCycle(Player player, boolean debug) {
        String result = startSession(player, debug, true);
        GatewaySession session = sessions.get(player.getUniqueId());
        if (session == null) return result;
        GatewayPayloadScheduler scheduler = new GatewayPayloadScheduler(GATEWAY_TOTAL_CAP, LOCAL_CAPS);
        List<GatewayPayloadType> pool = new ArrayList<>(List.of(GatewayPayloadType.values()));
        long delay = 15L;
        for (int attempt = 0; attempt < GATEWAY_TOTAL_CAP * 3 && scheduler.total() < GATEWAY_TOTAL_CAP; attempt++) {
            GatewayPayloadType type = pool.get(random.nextInt(pool.size()));
            GatewayPair pair = session.pairs().get(random.nextInt(session.pairs().size()));
            long active = type.sustained() ? 50L : 2L;
            if (!scheduler.reserve(pair.id(), type, delay, active, GATEWAY_RECOVERY_TICKS)) { delay += 3; continue; }
            var task = plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> { if (sessions.get(player.getUniqueId()) == session) fire(session, pair, type, debug); }, delay);
            session.tasks().add(task);
            delay += 7L;
        }
        return result + "; scheduled=" + scheduler.total() + "/" + GATEWAY_TOTAL_CAP;
    }

    public String payload(Player player, GatewayPayloadType type, boolean debug) {
        startSession(player, debug, true);
        GatewaySession session = sessions.get(player.getUniqueId());
        fire(session, session.pairs().getFirst(), type, debug);
        return "payload=" + type + ", reflectable=" + type.reflectable();
    }

    public String reflection(Player player) { return payload(player, GatewayPayloadType.ARROW, true); }

    public String basicRandom(Player player) {
        cleanup(player.getUniqueId());
        BasicWeaponRuntime runtime = new BasicWeaponRuntime(plugin);
        basics.put(player.getUniqueId(), runtime);
        Map<BasicWeaponPattern, Integer> selected = basicSelector.select(random, BASIC_TOTAL_CAP);
        runtime.start(player, bossPoint(player), selected);
        return "basic entities=" + selected.values().stream().mapToInt(Integer::intValue).sum()
                + "/" + BASIC_TOTAL_CAP + " patterns=" + selected;
    }

    public String basicPattern(Player player, BasicWeaponPattern pattern, int count) {
        cleanup(player.getUniqueId());
        int safeCount = Math.max(1, Math.min(BASIC_TOTAL_CAP, count));
        BasicWeaponRuntime runtime = new BasicWeaponRuntime(plugin);
        basics.put(player.getUniqueId(), runtime);
        runtime.start(player, bossPoint(player), Map.of(pattern, safeCount));
        return "basic pattern=" + pattern + " entities=" + safeCount;
    }

    private String startSession(Player player, boolean debug, boolean withDummy) {
        cleanup(player.getUniqueId());
        Location snapshot = player.getLocation().clone().add(0, 1, 0);
        Location boss = bossPoint(player);
        if (!validSpace(snapshot, boss)) return "Gateway cast cancelled: insufficient safe space.";
        List<Location> launchers = placement.launcherLocations(snapshot, DEFAULT_GATEWAY_COUNT, 7.0, 4.5);
        List<Location> returns = placement.returnLocations(boss, DEFAULT_GATEWAY_COUNT);
        List<GatewayPair> pairs = new ArrayList<>();
        List<Entity> visuals = new ArrayList<>();
        for (int index = 0; index < DEFAULT_GATEWAY_COUNT; index++) {
            BlockDisplay launcher = gatewayDisplay(launchers.get(index), 2.0f);
            BlockDisplay returning = gatewayDisplay(returns.get(index), 1.0f);
            visuals.add(launcher); visuals.add(returning);
            pairs.add(new GatewayPair(index + 1, launchers.get(index), returns.get(index),
                    placement.snapshotForward(launchers.get(index), snapshot), launcher.getUniqueId(), returning.getUniqueId()));
        }
        GatewaySession session = new GatewaySession(player, snapshot, boss, pairs);
        session.entities().addAll(visuals);
        if (withDummy) {
            ArmorStand dummy = boss.getWorld().spawn(boss, ArmorStand.class, stand -> {
                stand.setCustomName("§5Gateway boss dummy"); stand.setCustomNameVisible(true);
                stand.setGravity(false); stand.setPersistent(false); stand.setInvulnerable(false);
            });
            session.entities().add(dummy);
        }
        sessions.put(player.getUniqueId(), session);
        if (debug) drawDebug(session);
        return "gateways=" + DEFAULT_GATEWAY_COUNT + ", P0=" + format(snapshot) + ", debug=" + debug;
    }

    private BlockDisplay gatewayDisplay(Location center, float scale) {
        return center.getWorld().spawn(center, BlockDisplay.class, display -> {
            display.setBlock(Material.END_GATEWAY.createBlockData());
            display.setPersistent(false);
            Transformation t = display.getTransformation();
            t.getTranslation().set(-scale / 2f, -scale / 2f, -scale / 2f);
            t.getScale().set(new Vector3f(scale));
            display.setTransformation(t);
        });
    }

    private void drawDebug(GatewaySession session) {
        session.snapshot().getWorld().spawnParticle(Particle.HAPPY_VILLAGER, session.snapshot(), 20, .2, .2, .2, 0);
        for (GatewayPair pair : session.pairs()) {
            Location point = pair.launcher();
            for (int i = 0; i < 8; i++) point = point.clone().add(pair.snapshotForward().multiply(.45));
            pair.launcher().getWorld().spawnParticle(Particle.END_ROD, point, 4, .05, .05, .05, 0);
            pair.returnGateway().getWorld().spawnParticle(Particle.WITCH, pair.returnGateway(), pair.id(), .15, .15, .15, 0);
        }
    }

    private void fire(GatewaySession session, GatewayPair pair, GatewayPayloadType type, boolean debug) {
        if (type.sustained() || type == GatewayPayloadType.SONIC_BOOM) { sustained(session, pair, type, debug); return; }
        Material material = switch (type) {
            case ARROW -> Material.ARROW; case TRIDENT -> Material.TRIDENT;
            case BLAZE_SMALL_FIREBALL -> Material.FIRE_CHARGE; case GHAST_FIREBALL -> Material.FIRE_CHARGE;
            case WITHER_SKULL -> Material.WITHER_SKELETON_SKULL; case SHULKER_BULLET -> Material.SHULKER_SHELL;
            case WIND_CHARGE -> Material.WIND_CHARGE; case END_CRYSTAL_BOMB -> Material.END_CRYSTAL;
            default -> throw new IllegalArgumentException("Not a projectile: " + type);
        };
        ItemDisplay visual = pair.launcher().getWorld().spawn(pair.launcher(), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material)); display.setPersistent(false);
            Transformation t = display.getTransformation(); t.getScale().set(new Vector3f(type == GatewayPayloadType.END_CRYSTAL_BOMB ? .55f : .8f));
            display.setTransformation(t);
        });
        Interaction hitbox = pair.launcher().getWorld().spawn(pair.launcher(), Interaction.class, interaction -> {
            float size = type == GatewayPayloadType.BLAZE_SMALL_FIREBALL ? 1.35f : 1.0f;
            interaction.setInteractionWidth(size); interaction.setInteractionHeight(size); interaction.setResponsive(true);
            interaction.setPersistent(false);
        });
        session.entities().add(visual); session.entities().add(hitbox);
        ReflectableProjectileState state = new ReflectableProjectileState(visual.getUniqueId(), pair.id(), type);
        ProjectileRuntime runtime = new ProjectileRuntime(session, pair, state, visual, hitbox, debug);
        projectilesByHitbox.put(hitbox.getUniqueId(), runtime);
        BukkitTask task = runtime.runTaskTimer(plugin, 1L, 1L);
        session.tasks().add(task);
    }

    private void sustained(GatewaySession session, GatewayPair pair, GatewayPayloadType type, boolean debug) {
        Particle particle = switch (type) {
            case DRAGON_BREATH -> Particle.DRAGON_BREATH; case FLAME_STREAM -> Particle.FLAME;
            case SONIC_BOOM -> Particle.SONIC_BOOM; default -> Particle.END_ROD;
        };
        long duration = type == GatewayPayloadType.SONIC_BOOM ? 12 : 50;
        BukkitRunnable runnable = new BukkitRunnable() {
            int age;
            @Override public void run() {
                if (age++ >= duration || !sessions.containsValue(session)) { cancel(); return; }
                Vector forward = pair.snapshotForward();
                for (double distance = .5; distance <= 10; distance += .6)
                    pair.launcher().getWorld().spawnParticle(particle, pair.launcher().clone().add(forward.clone().multiply(distance)), 1, 0, 0, 0, 0);
            }
        };
        BukkitTask task = runnable.runTaskTimer(plugin, type == GatewayPayloadType.SONIC_BOOM ? 10L : 1L, 1L);
        session.tasks().add(task);
    }

    @EventHandler(ignoreCancelled = true)
    public void onReflect(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        ProjectileRuntime runtime = projectilesByHitbox.get(event.getEntity().getUniqueId());
        if (runtime == null) return;
        event.setCancelled(true);
        runtime.state.reflect(); // attack/look direction intentionally never read
        runtime.visual.getWorld().playSound(runtime.visual.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.4f);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { cleanup(event.getPlayer().getUniqueId()); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { cleanup(event.getPlayer().getUniqueId()); }

    public void cleanup(UUID owner) {
        GatewaySession session = sessions.remove(owner);
        if (session != null) session.cleanup();
        BasicWeaponRuntime basic = basics.remove(owner);
        if (basic != null) basic.cleanup();
        projectilesByHitbox.entrySet().removeIf(entry -> entry.getValue().session.ownerId().equals(owner));
    }

    public void shutdown() {
        List.copyOf(sessions.keySet()).forEach(this::cleanup);
        List.copyOf(basics.keySet()).forEach(this::cleanup);
    }

    private boolean validSpace(Location snapshot, Location boss) {
        return snapshot.getWorld().equals(boss.getWorld()) && snapshot.getY() + 7 < snapshot.getWorld().getMaxHeight();
    }

    private Location bossPoint(Player player) {
        Vector forward = player.getLocation().getDirection().setY(0);
        if (forward.lengthSquared() < .01) forward.setZ(1);
        return player.getLocation().clone().add(forward.normalize().multiply(10)).add(0, 1, 0);
    }

    private String format(Location location) { return "%.1f,%.1f,%.1f".formatted(location.getX(), location.getY(), location.getZ()); }

    private final class ProjectileRuntime extends BukkitRunnable {
        private final GatewaySession session; private final GatewayPair pair; private final ReflectableProjectileState state;
        private final ItemDisplay visual; private final Interaction hitbox; private final boolean debug;
        private int age; private int fuse;
        private ProjectileRuntime(GatewaySession session, GatewayPair pair, ReflectableProjectileState state,
                                  ItemDisplay visual, Interaction hitbox, boolean debug) {
            this.session = session; this.pair = pair; this.state = state; this.visual = visual; this.hitbox = hitbox; this.debug = debug;
        }
        @Override public void run() {
            if (!visual.isValid() || !sessions.containsValue(session) || age++ > 240) { finish(); return; }
            Location destination;
            double speed;
            switch (state.phase()) {
                case OUTBOUND -> { destination = session.snapshot(); speed = baseSpeed(state.type()) * PROJECTILE_SPEED_MULTIPLIER; }
                case RETURNING_TO_SOURCE -> { destination = pair.launcher(); speed = .48; }
                case RETURNING_TO_BOSS -> { destination = session.bossTarget(); speed = .52; }
                case FUSE -> { if (++fuse >= 18) explode(false); return; }
                default -> { finish(); return; }
            }
            Vector delta = destination.toVector().subtract(visual.getLocation().toVector());
            if (delta.lengthSquared() <= speed * speed * 2.0) {
                if (state.phase() == ReflectableProjectileState.Phase.RETURNING_TO_SOURCE) {
                    state.enterSourceGateway(); visual.teleport(pair.returnGateway()); hitbox.teleport(pair.returnGateway()); return;
                }
                if (state.phase() == ReflectableProjectileState.Phase.RETURNING_TO_BOSS) {
                    if (state.type() == GatewayPayloadType.END_CRYSTAL_BOMB) explode(true); else hitBoss(); return;
                }
                if (state.type() == GatewayPayloadType.END_CRYSTAL_BOMB) { state.beginFuse(); return; }
            }
            Vector movement = delta.normalize().multiply(speed);
            if (state.type() == GatewayPayloadType.SHULKER_BULLET && state.phase() == ReflectableProjectileState.Phase.OUTBOUND) {
                Player owner = plugin.getServer().getPlayer(session.ownerId());
                if (owner != null) movement = owner.getEyeLocation().toVector().subtract(visual.getLocation().toVector()).normalize().multiply(speed);
            }
            visual.teleport(visual.getLocation().add(movement)); hitbox.teleport(visual.getLocation());
            if (debug && age % 5 == 0) visual.getWorld().spawnParticle(state.phase() == ReflectableProjectileState.Phase.OUTBOUND ? Particle.CRIT : Particle.ENCHANTED_HIT, visual.getLocation(), 2, .05, .05, .05, 0);
        }
        private void hitBoss() {
            session.bossTarget().getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, session.bossTarget(), 12, .3, .5, .3, 0);
            session.bossTarget().getWorld().playSound(session.bossTarget(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, .7f); finish();
        }
        private void explode(boolean bossArrival) {
            visual.getWorld().spawnParticle(Particle.EXPLOSION, visual.getLocation(), bossArrival ? 4 : 2);
            visual.getWorld().playSound(visual.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f); finish();
        }
        private void finish() {
            state.finish(); projectilesByHitbox.remove(hitbox.getUniqueId()); visual.remove(); hitbox.remove(); cancel();
        }
        private double baseSpeed(GatewayPayloadType type) {
            return switch (type) {
                case ARROW -> .62; case BLAZE_SMALL_FIREBALL -> .55; case TRIDENT -> .48;
                case WIND_CHARGE -> .44; case SHULKER_BULLET -> .35; case END_CRYSTAL_BOMB -> .31;
                case GHAST_FIREBALL -> .28; case WITHER_SKULL -> .30; default -> .4;
            };
        }
    }
}
