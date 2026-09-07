package com.hyunseo.hyunseorpg.special.thanatos;

import com.hyunseo.hyunseorpg.alchemy.EffectMovementLockService;
import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.skill.CooldownService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Thanatos combat runtime; timers and coordinates are authoritative, displays are presentation only. */
public final class ThanatosMaceListener implements Listener {
    public static final String ID = "thanatos_mace";
    private static final String OPPRESSION_COOLDOWN = "thanatos:oppression";
    private static final String SENTENCE_COOLDOWN = "thanatos:sentence";
    private static final String ULTIMATUM_COOLDOWN = "thanatos:ultimatum";

    private static final Set<Material> REPLACEABLE_FLOORS = EnumSet.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.PODZOL, Material.MYCELIUM,
            Material.ROOTED_DIRT, Material.MUD, Material.CLAY, Material.SAND, Material.RED_SAND, Material.GRAVEL,
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.ANDESITE,
            Material.DIORITE, Material.GRANITE, Material.TUFF, Material.NETHERRACK, Material.BLACKSTONE,
            Material.END_STONE, Material.SOUL_SOIL, Material.SOUL_SAND);

    private final JavaPlugin plugin;
    private final SpecialEquipmentService specials;
    private final EquipmentInstanceService instances;
    private final CombatService combat;
    private final CooldownService cooldowns;
    private final EffectMovementLockService movementLocks;
    private final ThanatosConfig config;
    private final ThanatosState state = new ThanatosState();
    private final ThanatosVfx vfx = new ThanatosVfx();
    private final Map<UUID, MortalRuntime> mortals = new HashMap<>();
    private final Map<UUID, UltimatumRuntime> ultimatums = new HashMap<>();
    private final Map<UUID, Ground> grounds = new HashMap<>();
    private final Map<BlockKey, Mutation> mutations = new HashMap<>();
    private final Map<BukkitTask, SentenceRuntime> sentenceTasks = new HashMap<>();
    private final Map<UUID, Long> pressureEnds = new HashMap<>();
    private final BukkitTask ticker;
    private long tick;

    public ThanatosMaceListener(JavaPlugin plugin, ConfigService configService, SpecialEquipmentService specials,
                                EquipmentInstanceService instances, CombatService combat, CooldownService cooldowns,
                                EffectMovementLockService movementLocks) {
        this.plugin = plugin;
        this.specials = specials;
        this.instances = instances;
        this.combat = combat;
        this.cooldowns = cooldowns;
        this.movementLocks = movementLocks;
        this.config = ThanatosConfig.from(configService);
        this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player owner)
                || !(event.getEntity() instanceof LivingEntity target)
                || !holding(owner) || combat.isInternalDamage()) return;

        applyMortal(owner, target);
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                target.getLocation().add(0.0D, 0.8D, 0.0D), 5, 0.2D, 0.25D, 0.2D, 0.01D);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_HEAVY_CORE_PLACE, 0.65F, 0.65F);

        if (event.getDamageSource().getDamageType().equals(DamageType.MACE_SMASH)
                && !cooldowns.isOnCooldown(owner.getUniqueId(), OPPRESSION_COOLDOWN)) {
            cooldowns.startCooldownTicks(owner.getUniqueId(), OPPRESSION_COOLDOWN, config.oppressionCooldownTicks());
            oppression(owner, target.getLocation(), config.oppressionRadius(), config.oppressionDamage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onF(PlayerSwapHandItemsEvent event) {
        Player owner = event.getPlayer();
        if (owner.isSneaking() || !holding(owner)) return;
        event.setCancelled(true);
        if (cooldowns.isOnCooldown(owner.getUniqueId(), SENTENCE_COOLDOWN)) return;

        LivingEntity target = selectTarget(owner);
        if (target == null) return;
        cooldowns.startCooldownTicks(owner.getUniqueId(), SENTENCE_COOLDOWN, config.sentenceCooldownTicks());

        // The impact center is locked at selection time; later movement cannot retarget it.
        Location center = target.getLocation().clone();
        ThanatosVfx.SentenceVisual visual = vfx.spawnSentence(center);
        BukkitTask[] holder = new BukkitTask[1];
        BukkitTask task = holder[0] = new BukkitRunnable() {
            private int age;

            @Override
            public void run() {
                age++;
                double progress = Math.min(1.0D, age / (double) config.sentenceFallTicks());
                vfx.updateSentence(visual, progress);
                if (age >= config.sentenceFallTicks()) {
                    SentenceRuntime runtime = sentenceTasks.get(holder[0]);
                    if (runtime != null) runtime.impacted = true;
                    vfx.impactSentence(visual);
                    cancel();
                    impactSentence(owner, center);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        sentenceTasks.put(task, new SentenceRuntime(owner.getUniqueId(), visual));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player owner = event.getPlayer();
        if (!holding(owner)) return;
        event.setCancelled(true);
        UUID playerId = owner.getUniqueId();
        if (cooldowns.isOnCooldown(playerId, ULTIMATUM_COOLDOWN) || state.hasUltimatum(playerId)) return;

        UUID instance = instances.ensure(owner.getInventory().getItemInMainHand());
        if (state.beginUltimatum(playerId, owner.getWorld().getUID(), tick, config.chargeTicks())) {
            ultimatums.put(playerId, new UltimatumRuntime(owner, instance, owner.getWorld().getUID()));
            cooldowns.startCooldownTicks(playerId, ULTIMATUM_COOLDOWN, config.ultimatumCooldownTicks());
        }
    }

    private void tick() {
        tick++;
        for (MortalRuntime mortal : new ArrayList<>(mortals.values())) tickMortal(mortal);
        for (UltimatumRuntime ultimatum : new ArrayList<>(ultimatums.values())) tickUltimatum(ultimatum);
        for (Ground ground : new ArrayList<>(grounds.values())) tickGround(ground);
        vfx.tick();
        tickPressure();
        for (Map.Entry<BukkitTask, SentenceRuntime> entry : new ArrayList<>(sentenceTasks.entrySet())) {
            if (entry.getKey().isCancelled()) {
                if (!entry.getValue().impacted) vfx.discard(entry.getValue().visual);
                sentenceTasks.remove(entry.getKey());
            }
        }
    }

    private void tickMortal(MortalRuntime mortal) {
        LivingEntity target = entity(mortal.target);
        if (target == null || target.isDead() || !target.getWorld().getUID().equals(mortal.world)) {
            cleanupMortal(mortal.target, false);
            return;
        }
        if (state.mortalPhase(mortal.target) == ThanatosState.MortalPhase.WAITING) {
            Location sword = target.getLocation().add(0.0D, target.getHeight() + 1.8D, 0.0D);
            double intensity = Math.min(1.0D, (tick - mortal.started) / (double) Math.max(1, config.mortalDelayTicks()));
            vfx.updateMortal(mortal.visual, target.getLocation(), intensity, false, sword);
            if (state.mortalDue(mortal.target, tick)
                    && state.beginMortalFall(mortal.target, tick, config.mortalFallTicks())) {
                mortal.fallStarted = tick;
                mortal.fallStart = sword;
            }
            return;
        }
        if (!state.mortalImpactDue(mortal.target, tick)) {
            double progress = Math.min(1.0D, (tick - mortal.fallStarted)
                    / (double) Math.max(1, config.mortalFallTicks()));
            Location destination = target.getLocation().add(0.0D, target.getHeight() * 0.55D, 0.0D);
            Location sword = mortal.fallStart.clone().add(
                    destination.toVector().subtract(mortal.fallStart.toVector()).multiply(progress));
            vfx.updateMortal(mortal.visual, target.getLocation(), 1.0D, true, sword);
            return;
        }
        if (!state.consumeMortalImpact(mortal.target, tick)) return;
        try {
            Location impact = target.getLocation().add(0.0D, target.getHeight() * 0.65D, 0.0D);
            Player owner = Bukkit.getPlayer(mortal.owner);
            if (owner != null && owner.isOnline()) combat.applySkillDamage(owner, target, config.mortalDamage());
            else target.damage(config.mortalDamage());
            vfx.executeMortal(mortal.visual, impact);
            impact.getWorld().spawnParticle(Particle.SQUID_INK, impact, 18, 0.3D, 0.4D, 0.3D, 0.04D);
            impact.getWorld().playSound(impact, Sound.BLOCK_ANVIL_LAND, 0.9F, 0.55F);
        } finally {
            cleanupMortal(mortal.target, true);
        }
    }

    private void applyMortal(Player owner, LivingEntity target) {
        if (!state.beginMortal(target.getUniqueId(), tick, config.mortalDelayTicks())) return;
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setCustomModelData(config.mortalModel());
        sword.setItemMeta(meta);
        ThanatosVfx.MortalVisual visual = vfx.spawnMortal(target.getLocation(), sword);
        mortals.put(target.getUniqueId(), new MortalRuntime(
                target.getUniqueId(), owner.getUniqueId(), target.getWorld().getUID(), visual, tick));
    }

    private void cleanupMortal(UUID targetId, boolean released) {
        MortalRuntime mortal = mortals.remove(targetId);
        state.endMortal(targetId);
        if (mortal != null && !released) vfx.discard(mortal.visual);
    }

    private void oppression(Player owner, Location center, double radius, double damage) {
        vfx.spawnOppression(center, radius, config.oppressionDurationTicks());
        center.getWorld().playSound(center, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.2F, 0.6F);
        center.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS,
                center.clone().add(0.0D, 3.0D, 0.0D), 35, radius * 0.5D, 1.0D, radius * 0.5D, 0.04D);
        for (LivingEntity target : hostiles(center, radius)) {
            if (damage > 0.0D) combat.applySkillDamage(owner, target, damage);
            applyPressure(target, config.oppressionDurationTicks(), center, null);
        }
    }

    private void applyPressure(LivingEntity target, int duration, Location center, Ground ground) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 5, false, false, true));
        movementLocks.lock(target, duration);
        pressureEnds.merge(target.getUniqueId(), tick + duration, Math::max);
        applyDownwardForce(target);
        if (ground != null) ground.targets.add(target.getUniqueId());
    }

    private void tickPressure() {
        for (Map.Entry<UUID, Long> entry : new HashMap<>(pressureEnds).entrySet()) {
            LivingEntity target = entity(entry.getKey());
            if (target == null || tick >= entry.getValue()) {
                pressureEnds.remove(entry.getKey());
                continue;
            }
            applyDownwardForce(target);
            if (tick % 3L == 0L) {
                Location above = target.getLocation().add(0.0D, target.getHeight() + 2.0D, 0.0D);
                target.getWorld().spawnParticle(Particle.SQUID_INK, above, 2, 0.18D, 0.25D, 0.18D, 0.01D);
                target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, above, 1, 0.0D, -0.8D, 0.0D, 0.12D);
            }
        }
    }

    private void applyDownwardForce(LivingEntity target) {
        target.setVelocity(target.getVelocity().setY(Math.min(target.getVelocity().getY(), -0.42D)));
    }

    private LivingEntity selectTarget(Player owner) {
        Location eye = owner.getEyeLocation();
        List<ThanatosTargeting.Candidate<LivingEntity>> candidates = new ArrayList<>();
        for (Entity entity : owner.getWorld().getNearbyEntities(owner.getLocation(), config.sentenceRange(),
                config.sentenceRange(), config.sentenceRange())) {
            if (!(entity instanceof LivingEntity living) || !hostile(owner, living)) continue;
            Vector direction = living.getBoundingBox().getCenter().subtract(eye.toVector());
            candidates.add(new ThanatosTargeting.Candidate<>(living, direction.getX(), direction.getY(), direction.getZ()));
        }
        Vector facing = eye.getDirection();
        List<LivingEntity> valid = ThanatosTargeting.forward(candidates, facing.getX(), facing.getY(), facing.getZ(),
                config.sentenceRange(), config.sentenceCosine());
        return ThanatosTargeting.random(valid, ThreadLocalRandom.current());
    }

    private void impactSentence(Player owner, Location center) {
        center.getWorld().playSound(center, Sound.BLOCK_HEAVY_CORE_PLACE, 2.0F, 0.45F);
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        Ground ground = createGround(center);
        for (LivingEntity target : hostiles(center, config.groundRadius())) {
            applyMortal(owner, target);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,
                    config.witherDurationTicks(), config.witherAmplifier()));
            applyPressure(target, config.groundDurationTicks(), center, ground);
        }
    }

    private Ground createGround(Location center) {
        Ground ground = new Ground(UUID.randomUUID(), center.clone(), tick + config.groundDurationTicks());
        grounds.put(ground.id, ground);
        int radius = (int) Math.ceil(config.groundRadius());
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > config.groundRadius() * config.groundRadius()) continue;
                Block block = localFloor(center.getWorld(), center.getBlockX() + x, center.getBlockY(), center.getBlockZ() + z);
                if (block == null) continue;
                BlockKey key = new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
                Mutation mutation = mutations.get(key);
                if (mutation == null) {
                    mutation = new Mutation(block.getBlockData().clone(), 1);
                    mutations.put(key, mutation);
                    block.setType(Material.SOUL_SAND, false);
                } else {
                    mutation.owners++;
                }
                ground.blocks.add(key);
            }
        }
        return ground;
    }

    private Block localFloor(World world, int x, int centerY, int z) {
        List<Block> band = new ArrayList<>();
        for (int y = Math.min(world.getMaxHeight() - 1, centerY + 2);
             y >= Math.max(world.getMinHeight(), centerY - 4); y--) {
            band.add(world.getBlockAt(x, y, z));
        }
        return ThanatosFloorSelector.nearestLocalFloor(band, this::replaceableFloor,
                block -> block.isPassable() && !block.isLiquid());
    }

    private boolean replaceableFloor(Block block) {
        return REPLACEABLE_FLOORS.contains(block.getType()) && block.getType().isSolid() && !block.isLiquid();
    }

    private void tickGround(Ground ground) {
        if (tick >= ground.ends) {
            cleanupGround(ground.id);
            return;
        }
        for (UUID targetId : new HashSet<>(ground.targets)) {
            LivingEntity target = entity(targetId);
            if (target == null || target.getWorld() != ground.center.getWorld()) {
                ground.targets.remove(targetId);
                continue;
            }
            Vector horizontal = target.getLocation().toVector().subtract(ground.center.toVector()).setY(0.0D);
            if (horizontal.length() > config.groundRadius() * 0.82D && !target.isInsideVehicle()) {
                Vector inward = horizontal.normalize().multiply(-0.22D);
                target.setVelocity(target.getVelocity().setX(inward.getX()).setZ(inward.getZ())
                        .setY(Math.min(target.getVelocity().getY(), -0.25D)));
            }
        }
    }

    private void cleanupGround(UUID groundId) {
        Ground ground = grounds.remove(groundId);
        if (ground == null) return;
        for (BlockKey key : ground.blocks) {
            Mutation mutation = mutations.get(key);
            if (mutation == null || --mutation.owners > 0) continue;
            mutations.remove(key);
            World world = Bukkit.getWorld(key.world);
            if (world == null) continue;
            Block block = world.getBlockAt(key.x, key.y, key.z);
            if (block.getType() == Material.SOUL_SAND) block.setBlockData(mutation.original, false);
        }
    }

    private void tickUltimatum(UltimatumRuntime ultimatum) {
        Player player = ultimatum.player;
        UUID playerId = player.getUniqueId();
        if (!valid(ultimatum)) {
            cancel(playerId);
            return;
        }
        ThanatosState.UltimatumPhase phase = state.phase(playerId);
        if (phase == ThanatosState.UltimatumPhase.CHARGING) {
            inwardParticles(player);
            if (state.chargeDue(playerId, tick)) {
                state.launch(playerId);
                player.getWorld().spawnParticle(Particle.GUST_EMITTER_SMALL, player.getLocation(), 2);
                player.setVelocity(player.getVelocity().setY(config.launchVelocity()));
            }
        } else if (phase == ThanatosState.UltimatumPhase.LAUNCHED && player.getVelocity().getY() <= 0.0D) {
            state.beginFall(playerId);
        } else if (phase == ThanatosState.UltimatumPhase.FALLING
                && state.land(playerId, player.getWorld().getUID(), player.isOnGround() || player.isInWater())) {
            ultimatums.remove(playerId);
            ultimatumImpact(player);
        }
    }

    private void inwardParticles(Player player) {
        for (int index = 0; index < 4; index++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0D);
            Location body = player.getLocation().add(0.0D, 1.0D, 0.0D);
            Location point = body.clone().add(Math.cos(angle) * 2.2D,
                    ThreadLocalRandom.current().nextDouble(-0.8D, 1.0D), Math.sin(angle) * 2.2D);
            Vector inward = body.toVector().subtract(point.toVector()).normalize();
            player.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, point, 1,
                    inward.getX(), inward.getY(), inward.getZ(), 0.18D);
            if (index == 0) player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1,
                    inward.getX(), inward.getY(), inward.getZ(), 0.12D);
        }
    }

    private void ultimatumImpact(Player owner) {
        Location center = owner.getLocation();
        center.getWorld().playSound(center, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 2.0F, 0.45F);
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 3);
        for (int index = 0; index < 120; index++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0D);
            double radius = ThreadLocalRandom.current().nextDouble(config.impactRadius());
            center.getWorld().spawnParticle(index % 3 == 0 ? Particle.SOUL : Particle.SQUID_INK,
                    center.clone().add(Math.cos(angle) * radius, 0.15D, Math.sin(angle) * radius), 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
        }
        for (LivingEntity target : hostiles(center, config.impactRadius())) {
            combat.applyUltimateDamage(owner, target, config.impactDamage());
            applyMortal(owner, target);
            applyPressure(target, config.oppressionDurationTicks(), center, null);
        }
    }

    private List<LivingEntity> hostiles(Location center, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, Math.max(4.0D, radius), radius)) {
            if (entity instanceof LivingEntity living && !(living instanceof ArmorStand)
                    && !(living instanceof Player) && !living.isDead()) result.add(living);
        }
        return result;
    }

    private boolean hostile(Player owner, LivingEntity target) {
        return target != owner && !(target instanceof ArmorStand) && !(target instanceof Player)
                && !target.isDead() && target.isValid();
    }

    private LivingEntity entity(UUID id) {
        Entity entity = Bukkit.getEntity(id);
        return entity instanceof LivingEntity living && living.isValid() ? living : null;
    }

    private boolean holding(Player player) {
        return specials.getSpecialId(player.getInventory().getItemInMainHand()).equals(ID);
    }

    private boolean valid(UltimatumRuntime ultimatum) {
        Player player = ultimatum.player;
        return player.isOnline() && !player.isDead() && holding(player)
                && instances.is(player.getInventory().getItemInMainHand(), ultimatum.instance)
                && player.getWorld().getUID().equals(ultimatum.world) && !player.isInsideVehicle();
    }

    private void cancel(UUID playerId) {
        state.cancelUltimatum(playerId);
        ultimatums.remove(playerId);
    }

    private void cancelPlayerEffects(UUID playerId) {
        cancel(playerId);
        for (Map.Entry<BukkitTask, SentenceRuntime> entry : new ArrayList<>(sentenceTasks.entrySet())) {
            if (!entry.getValue().owner.equals(playerId)) continue;
            entry.getKey().cancel();
            vfx.discard(entry.getValue().visual);
            sentenceTasks.remove(entry.getKey());
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { cancelPlayerEffects(event.getPlayer().getUniqueId()); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { cancelPlayerEffects(event.getPlayer().getUniqueId()); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { cancelPlayerEffects(event.getPlayer().getUniqueId()); }

    @EventHandler public void onHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!holding(event.getPlayer())) cancel(event.getPlayer().getUniqueId());
        });
    }

    @EventHandler public void onInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!holding(player)) cancel(player.getUniqueId());
        });
    }

    public void shutdown() {
        ticker.cancel();
        for (UUID targetId : new ArrayList<>(mortals.keySet())) cleanupMortal(targetId, false);
        for (UUID groundId : new ArrayList<>(grounds.keySet())) cleanupGround(groundId);
        for (Map.Entry<BukkitTask, SentenceRuntime> entry : sentenceTasks.entrySet()) {
            entry.getKey().cancel();
        }
        sentenceTasks.clear();
        vfx.shutdown();
        ultimatums.clear();
        pressureEnds.clear();
        state.clear();
    }

    private static final class MortalRuntime {
        private final UUID target;
        private final UUID owner;
        private final UUID world;
        private final ThanatosVfx.MortalVisual visual;
        private final long started;
        private long fallStarted;
        private Location fallStart;

        private MortalRuntime(UUID target, UUID owner, UUID world, ThanatosVfx.MortalVisual visual, long started) {
            this.target = target; this.owner = owner; this.world = world; this.visual = visual; this.started = started;
        }
    }

    private static final class SentenceRuntime {
        private final UUID owner;
        private final ThanatosVfx.SentenceVisual visual;
        private boolean impacted;
        private SentenceRuntime(UUID owner, ThanatosVfx.SentenceVisual visual) { this.owner = owner; this.visual = visual; }
    }

    private record UltimatumRuntime(Player player, UUID instance, UUID world) { }
    private record BlockKey(UUID world, int x, int y, int z) { }
    private static final class Ground {
        private final UUID id;
        private final Location center;
        private final long ends;
        private final Set<UUID> targets = new HashSet<>();
        private final Set<BlockKey> blocks = new HashSet<>();
        private Ground(UUID id, Location center, long ends) { this.id = id; this.center = center; this.ends = ends; }
    }
    private static final class Mutation {
        private final BlockData original;
        private int owners;
        private Mutation(BlockData original, int owners) { this.original = original; this.owners = owners; }
    }
}
