package com.hyunseo.hyunseorpg.special.moonlit;

import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/** Runtime for Moonlit Afterglow. All Bukkit entity access remains on the server thread. */
public final class MoonlitAfterglowListener implements Listener {
    public static final String ID = "moonlit_afterglow";
    private static final String YUGWANG = ID + ":yugwang";
    private static final String MOON_FLASH = ID + ":moon_flash";
    private static final String MOON_SHADOW = ID + ":moon_shadow";
    private final ConfigService configService;
    private final SpecialEquipmentService specials;
    private final CombatService combat;
    private final CooldownService cooldowns;
    private final Map<UUID, Long> mitigationUntilTick = new HashMap<>();
    private final Map<UUID, RuntimeState> states = new HashMap<>();
    private final Map<UUID, MovementInput> movementInputs = new HashMap<>();
    private final Map<UUID, Long> finalInputSuppressionTicks = new HashMap<>();
    private final Map<UUID, Set<BukkitTask>> slashTasks = new HashMap<>();
    private long tick;
    private final BukkitTask clockTask;

    public MoonlitAfterglowListener(ConfigService configService, SpecialEquipmentService specials,
                                    CombatService combat, CooldownService cooldowns) {
        this.configService = configService;
        this.specials = specials;
        this.combat = combat;
        this.cooldowns = cooldowns;
        this.clockTask = Bukkit.getScheduler().runTaskTimer(configService.getPlugin(), () -> {
            tick++;
            finalInputSuppressionTicks.entrySet().removeIf(entry -> entry.getValue() < tick);
            mitigationUntilTick.entrySet().removeIf(entry -> entry.getValue() <= tick);
        }, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (combat.isInternalDamage() || !(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity target) || !holding(player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;
        if (finalInputSuppressionTicks.getOrDefault(player.getUniqueId(), -1L) >= tick) return;
        RuntimeState runtime = states.get(player.getUniqueId());
        if (runtime != null) {
            if (runtime.machine.phase() == MoonShadowState.Phase.WAITING_FOR_FINAL_TRIGGER) triggerFinal(player);
            return;
        }
        MoonlitAfterglowConfig config = config();
        sweep(player.getLocation().add(0, 1, 0));
        combat.applyAdditionalMeleeDamage(player, player.getInventory().getItemInMainHand(), target, config.passiveDamage());
        mitigationUntilTick.put(player.getUniqueId(), tick + config.mitigationTicks());
        if (!cooldowns.isOnCooldown(player.getUniqueId(), YUGWANG)) {
            cooldowns.startCooldown(player.getUniqueId(), YUGWANG, millis(config.yugwangCooldownSeconds()));
            // Collision-clamped teleport was selected over velocity: it is deterministic and cannot leave momentum.
            moveLinear(player, yugwangDirection(player), config.yugwangDistance());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIncomingDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        long until = mitigationUntilTick.getOrDefault(player.getUniqueId(), -1L);
        if (tick >= until) { mitigationUntilTick.remove(player.getUniqueId()); return; }
        event.setDamage(event.getDamage() * (1.0 - config().damageReduction()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !holding(event.getPlayer())) return;
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            UUID playerId = event.getPlayer().getUniqueId();
            if (triggerFinal(event.getPlayer()) || finalInputSuppressionTicks.getOrDefault(playerId, -1L) >= tick
                    || states.containsKey(playerId)) event.setCancelled(true);
            return;
        }
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (states.containsKey(player.getUniqueId()) || cooldowns.isOnCooldown(player.getUniqueId(), MOON_FLASH)) {
            event.setCancelled(true);
            return;
        }
        MoonlitAfterglowConfig config = config();
        cooldowns.startCooldown(player.getUniqueId(), MOON_FLASH, millis(config.moonFlashCooldownSeconds()));
        Location origin = player.getLocation().clone();
        Location destination = moveLinear(player, player.getEyeLocation().getDirection(), config.moonFlashDistance());
        if (destination.distanceSquared(origin) > .01) trailSlash(player, origin, destination, config.moonFlashDamage(), config.slashSpeed());
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!holding(player)) return;
        event.setCancelled(true); // F is owned by this weapon even when activation is rejected.
        if (states.containsKey(player.getUniqueId())
                || cooldowns.isOnCooldown(player.getUniqueId(), MOON_SHADOW)) return;
        LivingEntity target = acquireTarget(player);
        if (target == null) return;
        startMoonShadow(player, target);
    }

    private void startMoonShadow(Player player, LivingEntity target) {
        MoonlitAfterglowConfig config = config();
        cooldowns.startCooldown(player.getUniqueId(), MOON_SHADOW, millis(config.moonShadowCooldownSeconds()));
        MoonShadowState machine = new MoonShadowState(config.teleportCount());
        RuntimeState runtime = new RuntimeState(player.getWorld().getUID(), target.getUniqueId(), machine,
                player.getInventory().getItemInMainHand());
        states.put(player.getUniqueId(), runtime);
        runtime.task = Bukkit.getScheduler().runTaskTimer(configService.getPlugin(), () -> {
            try {
                moonShadowIteration(player, runtime, config);
            } catch (RuntimeException exception) {
                configService.getPlugin().getLogger().log(Level.SEVERE,
                        "Unexpected Moon Shadow iteration failure; cleaning state for player "
                                + player.getUniqueId() + " at attempt " + machine.attempts(), exception);
                cleanupMoonShadow(player.getUniqueId());
                return;
            }
            if (machine.phase() == MoonShadowState.Phase.WAITING_FOR_FINAL_TRIGGER) {
                runtime.task.cancel();
                runtime.task = Bukkit.getScheduler().runTaskLater(configService.getPlugin(),
                        () -> cleanupMoonShadow(player.getUniqueId()), config.finalTimeoutTicks());
            }
        }, 0L, config.cadenceTicks());
    }

    private void moonShadowIteration(Player player, RuntimeState runtime, MoonlitAfterglowConfig config) {
        LivingEntity target = validTarget(player, runtime);
        if (target == null || !holding(player)) { cleanupMoonShadow(player.getUniqueId()); return; }
        Location targetNow = target.getLocation().clone();
        Location chosen = null;
        for (int candidate = 0; candidate < config.candidateRetries(); candidate++) {
            MoonlitAfterglowMath.Point offset = MoonlitAfterglowMath.sphericalOffset(
                    ThreadLocalRandom.current(), config.minimumRadius(), config.maximumRadius());
            Location test = targetNow.clone().add(offset.x(), offset.y(), offset.z());
            if (safeDestination(player.getWorld(), test)) { chosen = test; break; }
        }
        if (chosen != null) {
            face(chosen, targetNow);
            if (!player.teleport(chosen, PlayerTeleportEvent.TeleportCause.PLUGIN)) chosen = null;
        }
        if (chosen == null) {
            // Expected movement failure: consume this bounded attempt without placing a fake slash.
            runtime.machine.attempt(null);
            return;
        }
        runtime.machine.attempt(snapshot(chosen, targetNow));
        sweep(chosen.clone().add(0, 1, 0));
    }

    private boolean triggerFinal(Player player) {
        RuntimeState runtime = states.get(player.getUniqueId());
        if (runtime == null || runtime.machine.phase() != MoonShadowState.Phase.WAITING_FOR_FINAL_TRIGGER) return false;
        List<MoonShadowState.SlashSnapshot> slashes = runtime.machine.trigger();
        if (runtime.task != null) runtime.task.cancel();
        sweep(player.getLocation().add(0, 1, 0));
        finalInputSuppressionTicks.put(player.getUniqueId(), tick);
        for (MoonShadowState.SlashSnapshot slash : slashes) releaseSlash(player, runtime.sourceItem, slash);
        runtime.machine.finish();
        states.remove(player.getUniqueId());
        return true;
    }

    private void releaseSlash(Player owner, ItemStack sourceItem, MoonShadowState.SlashSnapshot snapshot) {
        Location from = location(owner.getWorld(), snapshot.origin());
        Location to = location(owner.getWorld(), snapshot.target());
        trailSlash(owner, sourceItem, from, to, config().shadowSlashDamage(), config().slashSpeed());
    }

    private void trailSlash(Player owner, Location from, Location to, double damage, double speed) {
        trailSlash(owner, owner.getInventory().getItemInMainHand().clone(), from, to, damage, speed);
    }

    private void trailSlash(Player owner, ItemStack sourceItem, Location from, Location to, double damage, double speed) {
        Vector delta = to.toVector().subtract(from.toVector());
        double length = delta.length();
        if (length < .001) return;
        Vector step = delta.normalize().multiply(Math.max(.1, speed));
        Set<UUID> hit = new HashSet<>();
        final Location[] cursor = {from.clone()};
        final double[] travelled = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(configService.getPlugin(), () -> {
            try {
                if (!owner.isOnline() || owner.isDead() || owner.getWorld() != cursor[0].getWorld()) {
                    cancelSlashTask(owner.getUniqueId(), task[0]);
                    return;
                }
                cursor[0].getWorld().spawnParticle(Particle.SWEEP_ATTACK, cursor[0], 1);
                for (Entity entity : cursor[0].getWorld().getNearbyEntities(cursor[0], 1, 1, 1)) {
                    if (entity instanceof LivingEntity living && entity != owner && hit.add(entity.getUniqueId()))
                        combat.applyMultiHitDamage(owner, sourceItem, living, damage);
                }
                cursor[0].add(step); travelled[0] += step.length();
                if (travelled[0] >= length) cancelSlashTask(owner.getUniqueId(), task[0]);
            } catch (RuntimeException exception) {
                configService.getPlugin().getLogger().log(Level.SEVERE,
                        "Unexpected delayed Moonlit slash failure; cancelling task for player "
                                + owner.getUniqueId(), exception);
                cancelSlashTask(owner.getUniqueId(), task[0]);
            }
        }, 1L, 1L);
        slashTasks.computeIfAbsent(owner.getUniqueId(), ignored -> new HashSet<>()).add(task[0]);
    }

    private Location moveLinear(Player player, Vector rawDirection, double distance) {
        Location start = player.getLocation().clone();
        Vector direction = rawDirection.clone();
        if (direction.lengthSquared() < 1.0e-8) return start;
        direction.normalize();
        Location safe = start.clone();
        for (double travelled = .2; travelled <= Math.max(0, distance) + 1.0e-8; travelled += .2) {
            Location candidate = start.clone().add(direction.clone().multiply(Math.min(travelled, distance)));
            if (!safeDestination(player.getWorld(), candidate)) break;
            safe = candidate;
        }
        Vector previousVelocity = player.getVelocity().clone();
        if (safe.distanceSquared(start) > .001 && player.teleport(safe, PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            player.setVelocity(previousVelocity); // preserve jump/fall/knockback; introduce no dash momentum.
            return safe;
        }
        return start;
    }

    static boolean safeDestination(World world, Location feet) {
        if (feet.getY() < world.getMinHeight() || feet.getY() + 1.8 > world.getMaxHeight()) return false;
        if (!world.isChunkLoaded(feet.getBlockX() >> 4, feet.getBlockZ() >> 4)) return false;
        BoundingBox playerBox = new BoundingBox(feet.getX() - .3, feet.getY(), feet.getZ() - .3,
                feet.getX() + .3, feet.getY() + 1.8, feet.getZ() + .3);
        int minX = (int) Math.floor(playerBox.getMinX()), maxX = (int) Math.floor(playerBox.getMaxX());
        int minY = (int) Math.floor(playerBox.getMinY()), maxY = (int) Math.floor(playerBox.getMaxY());
        int minZ = (int) Math.floor(playerBox.getMinZ()), maxZ = (int) Math.floor(playerBox.getMaxZ());
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            Block block = world.getBlockAt(x, y, z);
            if (!block.isPassable() && block.getBoundingBox().overlaps(playerBox)) return false;
        }
        return true;
    }

    private LivingEntity acquireTarget(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        RayTraceResult result = player.getWorld().rayTraceEntities(eye, direction, 32, .5,
                entity -> entity instanceof LivingEntity && entity != player);
        if (result == null || !(result.getHitEntity() instanceof LivingEntity living)) return null;
        RayTraceResult block = player.getWorld().rayTraceBlocks(eye, direction, 32, FluidCollisionMode.NEVER, true);
        return block == null || block.getHitPosition().distanceSquared(eye.toVector())
                > result.getHitPosition().distanceSquared(eye.toVector()) ? living : null;
    }
    private LivingEntity validTarget(Player player, RuntimeState runtime) {
        if (!player.isOnline() || player.isDead() || !player.getWorld().getUID().equals(runtime.worldId)) return null;
        Entity entity = Bukkit.getEntity(runtime.targetId);
        return entity instanceof LivingEntity living && living.isValid() && !living.isDead()
                && living.getWorld().equals(player.getWorld()) ? living : null;
    }
    private boolean holding(Player player) { return ID.equals(specials.getSpecialId(player.getInventory().getItemInMainHand())); }
    @EventHandler public void onInput(PlayerInputEvent event) {
        Input input = event.getInput();
        movementInputs.put(event.getPlayer().getUniqueId(),
                new MovementInput(input.isForward(), input.isBackward(), input.isLeft(), input.isRight()));
    }
    private Vector yugwangDirection(Player player) {
        Vector facing = player.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 1.0e-8) return new Vector();
        facing.normalize();
        MovementInput input = movementInputs.getOrDefault(player.getUniqueId(), MovementInput.STATIONARY);
        MoonlitAfterglowMath.Point selected = MoonlitAfterglowMath.movementDirection(
                input.forward, input.backward, input.left, input.right,
                new MoonlitAfterglowMath.Point(facing.getX(), 0, facing.getZ()),
                new MoonlitAfterglowMath.Point(-facing.getZ(), 0, facing.getX()));
        return selected == null ? facing : new Vector(selected.x(), 0, selected.z());
    }
    private void sweep(Location location) { location.getWorld().spawnParticle(Particle.SWEEP_ATTACK, location, 1); }
    private void face(Location from, Location target) { from.setDirection(target.toVector().subtract(from.toVector())); }
    private MoonShadowState.SlashSnapshot snapshot(Location origin, Location target) {
        MoonlitAfterglowMath.Point o = point(origin), t = point(target);
        MoonlitAfterglowMath.Point d = new MoonlitAfterglowMath.Point(t.x()-o.x(), t.y()-o.y(), t.z()-o.z());
        return new MoonShadowState.SlashSnapshot(o, t, d);
    }
    private MoonlitAfterglowMath.Point point(Location l) { return new MoonlitAfterglowMath.Point(l.getX(), l.getY(), l.getZ()); }
    private Location location(World world, MoonlitAfterglowMath.Point p) { return new Location(world, p.x(), p.y(), p.z()); }
    private long millis(double seconds) { return Math.max(0, Math.round(seconds * 1000)); }
    private MoonlitAfterglowConfig config() { return MoonlitAfterglowConfig.from(configService); }

    private void cleanupMoonShadow(UUID playerId) {
        RuntimeState removed = states.remove(playerId);
        if (removed != null && removed.task != null) removed.task.cancel();
    }
    private void cancelSlashTask(UUID playerId, BukkitTask task) {
        task.cancel();
        Set<BukkitTask> tasks = slashTasks.get(playerId);
        if (tasks != null) { tasks.remove(task); if (tasks.isEmpty()) slashTasks.remove(playerId); }
    }
    private void cancelSlashTasks(UUID playerId) {
        Set<BukkitTask> tasks = slashTasks.remove(playerId);
        if (tasks != null) List.copyOf(tasks).forEach(BukkitTask::cancel);
    }
    private void cleanupOwner(UUID playerId) {
        cleanupMoonShadow(playerId);
        mitigationUntilTick.remove(playerId);
        cancelSlashTasks(playerId);
        movementInputs.remove(playerId);
        finalInputSuppressionTicks.remove(playerId);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { cleanupOwner(event.getPlayer().getUniqueId()); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { cleanupOwner(event.getPlayer().getUniqueId()); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { cleanupOwner(event.getPlayer().getUniqueId()); }
    @EventHandler public void onHeld(PlayerItemHeldEvent event) { cleanupMoonShadow(event.getPlayer().getUniqueId()); }
    @EventHandler public void onDrop(PlayerDropItemEvent event) {
        if (ID.equals(specials.getSpecialId(event.getItemDrop().getItemStack()))) cleanupMoonShadow(event.getPlayer().getUniqueId());
    }
    @EventHandler public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && states.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().runTask(configService.getPlugin(), () -> {
                if (!holding(player)) cleanupMoonShadow(player.getUniqueId());
            });
        }
    }
    @EventHandler public void onTargetDeath(EntityDeathEvent event) {
        UUID targetId = event.getEntity().getUniqueId();
        states.entrySet().stream().filter(entry -> entry.getValue().targetId.equals(targetId))
                .map(Map.Entry::getKey).toList().forEach(this::cleanupMoonShadow);
    }
    public void shutdown() {
        clockTask.cancel();
        for (RuntimeState state : states.values()) if (state.task != null) state.task.cancel();
        List<BukkitTask> releasedTasks = slashTasks.values().stream().flatMap(Set::stream).toList();
        states.clear(); slashTasks.clear(); movementInputs.clear(); finalInputSuppressionTicks.clear(); mitigationUntilTick.clear();
        releasedTasks.forEach(BukkitTask::cancel);
    }
    private static final class RuntimeState {
        private final UUID worldId, targetId;
        private final MoonShadowState machine;
        private final ItemStack sourceItem;
        private BukkitTask task;
        private RuntimeState(UUID worldId, UUID targetId, MoonShadowState machine, ItemStack sourceItem) {
            this.worldId = worldId;
            this.targetId = targetId;
            this.machine = machine;
            // In-memory damage metadata only; never inserted into an inventory or assigned a new instance UUID.
            this.sourceItem = sourceItem.clone();
        }
    }
    private record MovementInput(boolean forward, boolean backward, boolean left, boolean right) {
        private static final MovementInput STATIONARY = new MovementInput(false, false, false, false);
    }
}
