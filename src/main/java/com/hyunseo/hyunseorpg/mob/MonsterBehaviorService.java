package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/** Common behavior handlers for registered non-boss RPG monsters. */
public final class MonsterBehaviorService implements Listener {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final MobService mobService;
    private final Set<UUID> tracked = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> nextAction = new ConcurrentHashMap<>();
    private final Map<UUID, Long> nextPathRecalculation = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> currentTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Location> navigationTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> phases = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> grounded = new ConcurrentHashMap<>();
    private final Map<UUID, Long> dashUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Vector> dashDirections = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> dashHits = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> projectileSources = new ConcurrentHashMap<>();
    private final Map<UUID, String> projectileTypes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> projectileExpiry = new ConcurrentHashMap<>();
    private final Map<UUID, DripstoneAttack> dripstoneAttacks = new ConcurrentHashMap<>();
    private final Set<UUID> splitChildren = ConcurrentHashMap.newKeySet();
    private final Set<UUID> approvedExplosions = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BulwarkState> bulwarks = new ConcurrentHashMap<>();
    private final Map<UUID, ShamanState> shamans = new ConcurrentHashMap<>();
    private final Map<UUID, MudPool> mudPools = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> mireMinionOwners = new ConcurrentHashMap<>();
    private final org.bukkit.NamespacedKey rapidArrowKey;
    private final org.bukkit.NamespacedKey rapidSourceKey;
    private final org.bukkit.NamespacedKey swappingProjectileKey;
    private final org.bukkit.NamespacedKey mireProjectileKey;
    private final org.bukkit.NamespacedKey dripstoneAttackKey;
    private final Set<org.bukkit.scheduler.BukkitTask> tasks = ConcurrentHashMap.newKeySet();
    private org.bukkit.scheduler.BukkitTask tickTask;
    private long ticks;

    private static final int FAKE_APPROACH = 0;
    private static final int FAKE_FAKE_EXPLOSION = 1;
    private static final int FAKE_FLEE = 2;
    private static final int FAKE_REAPPROACH = 3;
    private static final int FAKE_REAL_EXPLOSION = 4;
    private enum BulwarkPhase { NORMAL, TELEGRAPH, LUNGE, RECOVERY }
    private static final class BulwarkState { BulwarkPhase phase = BulwarkPhase.NORMAL; long until; Vector direction; final Set<UUID> hits = ConcurrentHashMap.newKeySet(); }
    private enum ShamanAction { IDLE, PROJECTILE_TELEGRAPH, POOL_TELEGRAPH, SUMMON_TELEGRAPH, RECLAIM }
    private static final class ShamanState { ShamanAction action = ShamanAction.IDLE; long until; long nextSummonAt; Location snapshot; boolean reclaimUsed; String lastAction = ""; }
    private static final class MudPool { final UUID owner; final Location center; final double radius; final long expiresAt; MudPool(UUID owner, Location center, double radius, long expiresAt) { this.owner=owner; this.center=center; this.radius=radius; this.expiresAt=expiresAt; } }

    public MonsterBehaviorService(JavaPlugin plugin, ConfigService configService, MobService mobService) {
        this.plugin = plugin;
        this.configService = configService;
        this.mobService = mobService;
        this.rapidArrowKey = new org.bukkit.NamespacedKey(plugin, "rapid_shooter_arrow");
        this.rapidSourceKey = new org.bukkit.NamespacedKey(plugin, "rapid_shooter_source");
        this.swappingProjectileKey = new org.bukkit.NamespacedKey(plugin, "swapping_witch_projectile");
        this.mireProjectileKey = new org.bukkit.NamespacedKey(plugin, "mire_shaman_projectile");
        this.dripstoneAttackKey = new org.bukkit.NamespacedKey(plugin, "mining_giant_dripstone");
    }

    private static final class DripstoneAttack {
        private final UUID displayId;
        private final UUID sourceId;
        private final Set<UUID> hitTargets = ConcurrentHashMap.newKeySet();
        private final Vector velocity;
        private final long expiresAt;
        private final double collisionRadius;
        private final double damage;
        private final double knockback;

        private DripstoneAttack(UUID displayId, UUID sourceId, Vector velocity, long expiresAt,
                                double collisionRadius, double damage, double knockback) {
            this.displayId = displayId;
            this.sourceId = sourceId;
            this.velocity = velocity;
            this.expiresAt = expiresAt;
            this.collisionRadius = collisionRadius;
            this.damage = damage;
            this.knockback = knockback;
        }
    }

    public void start() {
        if (tickTask != null) return;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (tickTask != null) tickTask.cancel();
        tickTask = null;
        tasks.forEach(org.bukkit.scheduler.BukkitTask::cancel);
        tasks.clear();
        tracked.clear();
        nextAction.clear();
        nextPathRecalculation.clear();
        currentTargets.clear();
        navigationTargets.clear();
        phases.clear();
        grounded.clear();
        dashUntil.clear();
        dashDirections.clear();
        dashHits.clear();
        projectileSources.clear();
        projectileTypes.clear();
        projectileExpiry.clear();
        dripstoneAttacks.values().forEach(attack -> {
            Entity entity = Bukkit.getEntity(attack.displayId);
            if (entity != null) entity.remove();
        });
        dripstoneAttacks.clear();
        splitChildren.clear();
        approvedExplosions.clear();
        mudPools.clear();
        bulwarks.clear();
        shamans.clear();
        mireMinionOwners.keySet().forEach(id -> { Entity minion = Bukkit.getEntity(id); if (minion != null) minion.remove(); });
        mireMinionOwners.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> register(event.getEntity()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ItemDisplay display
                    && display.getPersistentDataContainer().getOrDefault(
                    dripstoneAttackKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1
                    && !dripstoneAttacks.containsKey(display.getUniqueId())) {
                display.remove();
                continue;
            }
            if (entity instanceof LivingEntity living) register(living);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String behavior = behaviorId(entity);
        mireMinionOwners.remove(entity.getUniqueId());
        removeDripstonesForSource(entity.getUniqueId());
        removeMireOwned(entity.getUniqueId());
        unregister(entity.getUniqueId());
        if (!"splitting_creeper".equals(behavior) || splitChildren.contains(entity.getUniqueId())) return;

        var section = behaviorSection(entity);
        int count = Math.max(1, Math.min(8, section == null ? 3 : section.getInt("child-count", 3)));
        int offset = section == null ? -2 : section.getInt("child-level-offset", -2);
        int level = Math.max(1, mobService.getMobTagService().getMobLevel(entity) + offset);
        for (int index = 0; index < count; index++) {
            Location location = entity.getLocation().clone().add((index - 1) * 0.9D, 0.2D, 0.0D);
            mobService.spawnCustomMob(location, "splitting_creeper", level).ifPresent(child -> {
                splitChildren.add(child.getUniqueId());
                child.getPersistentDataContainer().set(
                        new org.bukkit.NamespacedKey(plugin, "split_child"), PersistentDataType.BYTE, (byte) 1);
                AttributeInstance scale = child.getAttribute(Attribute.SCALE);
                if (scale != null) scale.setBaseValue(0.65D);
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity victim && "golden_bulwark".equals(behaviorId(victim))) {
            LivingEntity attacker = resolveDamageAttacker(event.getDamager());
            if (attacker != null) {
                Vector forward = victim.getLocation().getDirection().setY(0.0D);
                Vector toAttacker = attacker.getLocation().toVector().subtract(victim.getLocation().toVector()).setY(0.0D);
                if (forward.lengthSquared() > 0.001D && toAttacker.lengthSquared() > 0.001D) {
                    double halfAngle = Math.toRadians(behaviorSectionValue(victim, "frontal-half-angle", 60.0D));
                    if (forward.normalize().dot(toAttacker.normalize()) >= Math.cos(halfAngle)) {
                        event.setDamage(event.getDamage() * behaviorSectionValue(victim, "frontal-damage-multiplier", 0.35D));
                    }
                }
            }
        }
        if (event.getDamager() instanceof ThrownPotion potion
                && potion.getShooter() instanceof LivingEntity shooter
                && shooter.getUniqueId().equals(event.getEntity().getUniqueId())
                && "swapping_witch".equals(behaviorId(shooter))) {
            event.setCancelled(true);
            return;
        }
        if (event.getDamager() instanceof AreaEffectCloud cloud
                && cloud.getSource() instanceof LivingEntity shooter
                && shooter.getUniqueId().equals(event.getEntity().getUniqueId())
                && "swapping_witch".equals(behaviorId(shooter))) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getDamager() instanceof LivingEntity source)
                || !(event.getEntity() instanceof Player player)) return;
        if ("stone_armored_zombie".equals(behaviorId(source))) {
            var section = behaviorSection(source);
            int duration = section == null ? 40 : Math.max(1, section.getInt("bind-duration-ticks", 40));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 1));
        }
    }

    private LivingEntity resolveDamageAttacker(Entity damager) {
        if (damager instanceof LivingEntity living) return living;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) return living;
        return null;
    }

    @EventHandler(ignoreCancelled = false)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        String type = projectileTypes.remove(projectile.getUniqueId());
        UUID sourceId = projectileSources.remove(projectile.getUniqueId());
        projectileExpiry.remove(projectile.getUniqueId());
        if (type == null) return;
        Entity sourceEntity = sourceId == null ? null : Bukkit.getEntity(sourceId);
        if (!(sourceEntity instanceof LivingEntity source)) {
            projectile.remove();
            return;
        }
        if (event.getHitEntity() != null && event.getHitEntity().getUniqueId().equals(source.getUniqueId())) {
            // A swapping projectile must never affect the witch that created it.
            projectile.remove();
            return;
        }
        if ("swapping_witch".equals(type) && event.getHitEntity() instanceof Player player) {
            trySwap(source, player, projectile.getLocation());
        } else if ("rapid_arrow".equals(type) && event.getHitEntity() instanceof Player player) {
            var section = behaviorSection(source);
            double damage = section == null ? 2.0D : section.getDouble("damage-per-arrow",
                    section.getDouble("projectile-damage", 2.0D));
            boolean bypassHurtImmunity = (section == null || section.getBoolean("bypass-hurt-immunity", true))
                    && projectile.getPersistentDataContainer()
                    .getOrDefault(rapidArrowKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
            if (bypassHurtImmunity) {
                // This exception is deliberately scoped to rapid_shooter arrows.
                player.setNoDamageTicks(0);
            }
            player.damage(Math.max(0.0D, damage), source);
            if (bypassHurtImmunity) {
                player.setNoDamageTicks(0);
            }
        }
        if ("mire_toxic".equals(type) && event.getHitEntity() instanceof Player player) {
            player.damage(behaviorSectionValue(source, "projectile.damage", 2.5D), source);
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) behaviorSectionValue(source, "projectile.poison-ticks", 35), 0));
        }
        projectile.getWorld().spawnParticle(Particle.ENTITY_EFFECT, projectile.getLocation(), 14, .25, .08, .25, 0.0D, org.bukkit.Color.LIME);
        projectile.remove();
    }

    @EventHandler(ignoreCancelled = false)
    public void onPotionSplash(PotionSplashEvent event) {
        if (!(event.getPotion().getShooter() instanceof LivingEntity shooter)) return;
        if ("mire_shaman".equals(behaviorId(shooter))) {
            event.setCancelled(true);
            return;
        }
        if (!"swapping_witch".equals(behaviorId(shooter))) return;
        event.setIntensity(shooter, 0.0F);
    }

    @EventHandler(ignoreCancelled = false)
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event) {
        if (!(event.getEntity().getSource() instanceof LivingEntity shooter)
                || !"swapping_witch".equals(behaviorId(shooter))) return;
        event.getAffectedEntities().remove(shooter);
    }

    @EventHandler(ignoreCancelled = false)
    public void onCreeperExplosionPrime(ExplosionPrimeEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        String behavior = behaviorId(creeper);
        if ("fake_explosion_creeper".equals(behavior)) {
            if (!approvedExplosions.remove(creeper.getUniqueId())) {
                event.setCancelled(true);
                creeper.setIgnited(false);
                return;
            }
        } else if (!"splitting_creeper".equals(behavior)) {
            return;
        }
        var section = behaviorSection(creeper);
        event.setRadius(section == null ? 3.0F : (float) section.getDouble("explode-radius", 3.0D));
        event.setFire(false);
    }

    @EventHandler(ignoreCancelled = false)
    public void onCreeperExplosion(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        String behavior = behaviorId(creeper);
        if (behavior.isBlank()) return;
        if ("fake_explosion_creeper".equals(behavior)
                && phases.getOrDefault(creeper.getUniqueId(), FAKE_APPROACH) != FAKE_REAL_EXPLOSION) {
            event.setCancelled(true);
            return;
        }
        event.blockList().clear();
    }

    private void register(LivingEntity entity) {
        String behavior = behaviorId(entity);
        if (behavior.isBlank()) return;
        if (tracked.add(entity.getUniqueId())) {
            nextAction.put(entity.getUniqueId(), ticks + 20L);
            if (usesPathfinderGroundController(behavior)) configurePathfinder(entity);
            if ("fake_explosion_creeper".equals(behavior)) phases.putIfAbsent(entity.getUniqueId(), FAKE_APPROACH);
            equip(entity, behavior);
        }
    }

    private void equip(LivingEntity entity, String behavior) {
        if (!(entity instanceof org.bukkit.entity.Mob mob)) return;
        if ("mining_giant".equals(behavior)) {
            mob.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));
            mob.getEquipment().setItemInMainHandDropChance(0.0F);
        }
        if ("stone_armored_zombie".equals(behavior)) {
            AttributeInstance knockback = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (knockback != null) knockback.setBaseValue(
                    behaviorSection(entity) == null ? 0.8D : behaviorSection(entity).getDouble("knockback-resistance", 0.8D));
        }
    }

    private void tick() {
        ticks++;
        tickDripstoneAttacks();
        tickSwappingProjectiles();
        // Existing custom mobs are discovered on spawn/chunk load. The periodic scan is
        // only a recovery path for entities loaded before this service started.
        if (ticks % 20L == 0L) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getPlayers().isEmpty()) continue;
                for (LivingEntity entity : world.getLivingEntities()) register(entity);
            }
        }
        for (UUID id : new ArrayList<>(tracked)) {
            Entity raw = Bukkit.getEntity(id);
            if (!(raw instanceof LivingEntity entity) || entity.isDead() || !entity.isValid()) {
                unregister(id);
                continue;
            }
            String behavior = behaviorId(entity);
            if (usesPathfinderGroundController(behavior)) {
                configurePathfinder(entity);
            }
            Player target = trackedTargetOrNearest(entity, behaviorSectionValue(entity, "recognition-range", 24.0D));
            if (target == null) {
                currentTargets.remove(id);
                if (entity instanceof Mob mob && "charging_zombie".equals(behavior)) mob.setTarget(null);
                stopPathfinding(entity);
            } else {
                currentTargets.put(id, target.getUniqueId());
                if (entity instanceof Mob mob && "charging_zombie".equals(behavior)
                        && !dashUntil.containsKey(id)) mob.setTarget(target);
            }
            switch (behavior) {
                case "mining_giant" -> tickMiningGiant(entity, target);
                case "lava_cube" -> tickLavaCube(entity, target);
                case "fake_explosion_creeper" -> tickFakeCreeper(entity, target);
                case "swapping_witch" -> tickSwappingWitch(entity, target);
                case "charging_zombie" -> tickChargingZombie(entity, target);
                case "rapid_shooter" -> tickRapidShooter(entity, target);
                case "golden_bulwark" -> tickGoldenBulwark(entity, target);
                case "mire_shaman" -> tickMireShaman(entity, target);
                default -> { }
            }
        }
        tickMudPools();
    }

    private void tickGoldenBulwark(LivingEntity source, Player target) {
        BulwarkState state = bulwarks.computeIfAbsent(source.getUniqueId(), ignored -> new BulwarkState());
        var section = behaviorSection(source); if (section == null) return;
        if (state.phase == BulwarkPhase.NORMAL && target != null && ready(source) && source.getLocation().distanceSquared(target.getLocation()) <= Math.pow(section.getDouble("bash.prepare-range", 3.5D), 2)) {
            Vector d = target.getLocation().toVector().subtract(source.getLocation().toVector()).setY(0); if (d.lengthSquared() > .01D) { state.direction=d.normalize(); state.phase=BulwarkPhase.TELEGRAPH; state.until=ticks+section.getLong("bash.telegraph-ticks",12L); source.getWorld().playSound(source.getLocation(), Sound.ITEM_SHIELD_BLOCK, .8F, .8F); }
        } else if (state.phase == BulwarkPhase.TELEGRAPH) {
            source.getWorld().spawnParticle(Particle.DUST, source.getLocation().add(state.direction.clone().multiply(.7)).add(0,.9,0), 4, .15,.2,.15, new Particle.DustOptions(org.bukkit.Color.YELLOW, 1));
            if (ticks >= state.until) { source.setVelocity(state.direction.clone().multiply(section.getDouble("bash.lunge-horizontal", .65D)).setY(section.getDouble("bash.lunge-vertical", .35D))); state.phase=BulwarkPhase.LUNGE; state.until=ticks+section.getLong("bash.lunge-ticks",6L); }
        } else if (state.phase == BulwarkPhase.LUNGE) {
            for (Player player : playersNear(source.getLocation(), section.getDouble("bash.hit-range",1.8D))) { Vector to=player.getLocation().toVector().subtract(source.getLocation().toVector()).setY(0); if (!state.hits.contains(player.getUniqueId()) && to.lengthSquared()>.01 && state.direction.dot(to.normalize()) >= Math.cos(Math.toRadians(section.getDouble("bash.hit-half-angle",45)))) { state.hits.add(player.getUniqueId()); player.setVelocity(to.normalize().multiply(section.getDouble("bash.knockback-horizontal",1.05D)).setY(section.getDouble("bash.knockback-vertical",.85D))); double damage=section.getDouble("bash.damage",0D); if(damage>0) player.damage(damage,source); } }
            if(ticks>=state.until){state.phase=BulwarkPhase.RECOVERY;state.until=ticks+section.getLong("bash.recovery-ticks",18L);}
        } else if(state.phase==BulwarkPhase.RECOVERY && ticks>=state.until){ state.phase=BulwarkPhase.NORMAL; state.hits.clear(); nextAction.put(source.getUniqueId(),ticks+section.getLong("bash.cooldown-ticks",70L)); }
    }

    private void tickMireShaman(LivingEntity source, Player target) {
        ShamanState state=shamans.computeIfAbsent(source.getUniqueId(), ignored->new ShamanState()); var section=behaviorSection(source); if(section==null) return;
        if(state.action!=ShamanAction.IDLE){ renderShamanTelegraph(source,state,section); if(ticks<state.until) return; executeShaman(source,state,section); return; }
        if(target==null || !ready(source)) return;
        int pools=(int)mudPools.values().stream().filter(p->p.owner.equals(source.getUniqueId())).count();
        int minions=activeMireMinionCount(source.getUniqueId());
        AttributeInstance maximumHealth = source.getAttribute(Attribute.MAX_HEALTH);
        double healthRatio = maximumHealth == null || maximumHealth.getValue() <= 0.0D
                ? 1.0D : source.getHealth() / maximumHealth.getValue();
        if(MireShamanPolicy.canReclaim(state.reclaimUsed, healthRatio, pools,
                section.getDouble("reclaim.health-threshold",.5D))){state.action=ShamanAction.RECLAIM;state.until=ticks+10;return;}
        double distance=source.getLocation().distance(target.getLocation());
        if(pools<section.getInt("pool.max-active",3) && distance>=6 && distance<=12 && !"POOL".equals(state.lastAction)){state.action=ShamanAction.POOL_TELEGRAPH;state.snapshot=target.getLocation().clone();state.until=ticks+section.getLong("pool.telegraph-ticks",20);state.lastAction="POOL";return;}
        if(minions<section.getInt("minion.max-active",2) && ticks >= state.nextSummonAt && !"SUMMON".equals(state.lastAction)){state.action=ShamanAction.SUMMON_TELEGRAPH;state.until=ticks+12;state.lastAction="SUMMON";return;}
        if(distance>=8 && distance<=14){state.action=ShamanAction.PROJECTILE_TELEGRAPH;state.snapshot=target.getEyeLocation().clone();state.until=ticks+section.getLong("projectile.telegraph-ticks",24);state.lastAction="PROJECTILE";}
    }

    private void executeShaman(LivingEntity source, ShamanState state, org.bukkit.configuration.ConfigurationSection section) {
        if(state.action==ShamanAction.PROJECTILE_TELEGRAPH && state.snapshot != null){
            Snowball p=source.launchProjectile(Snowball.class);
            Vector d=state.snapshot.toVector().subtract(source.getEyeLocation().toVector());
            if (d.lengthSquared() > 0.001D) p.setVelocity(d.normalize().multiply(section.getDouble("projectile.speed",.55D)));
            p.getPersistentDataContainer().set(mireProjectileKey,PersistentDataType.BYTE,(byte)1);
            projectileSources.put(p.getUniqueId(),source.getUniqueId());projectileTypes.put(p.getUniqueId(),"mire_toxic");projectileExpiry.put(p.getUniqueId(),ticks+80);
        }
        else if(state.action==ShamanAction.POOL_TELEGRAPH){ Location c=state.snapshot; mudPools.put(UUID.randomUUID(),new MudPool(source.getUniqueId(),c,section.getDouble("pool.radius",2.75D),ticks+section.getLong("pool.duration-ticks",120))); }
        else if(state.action==ShamanAction.SUMMON_TELEGRAPH){
            int count = MireShamanPolicy.summonCount(activeMireMinionCount(source.getUniqueId()),
                    section.getInt("minion.count",2), section.getInt("minion.max-active",2));
            for(int i=0;i<count;i++){ org.bukkit.entity.Slime slime=source.getWorld().spawn(source.getLocation(),org.bukkit.entity.Slime.class);slime.setSize(1);mireMinionOwners.put(slime.getUniqueId(),source.getUniqueId()); }
            state.nextSummonAt=ticks+section.getLong("minion.cooldown-ticks",200L);
        }
        else if(state.action==ShamanAction.RECLAIM){ int count=(int)mudPools.values().stream().filter(p->p.owner.equals(source.getUniqueId())).count(); mudPools.entrySet().removeIf(e->e.getValue().owner.equals(source.getUniqueId())); source.setHealth(Math.min(source.getAttribute(Attribute.MAX_HEALTH).getValue(),source.getHealth()+source.getAttribute(Attribute.MAX_HEALTH).getValue()*section.getDouble("reclaim.heal-ratio",.125D))); state.reclaimUsed=true; }
        state.action=ShamanAction.IDLE;nextAction.put(source.getUniqueId(),ticks+20);
    }
    private void renderShamanTelegraph(LivingEntity source, ShamanState state, org.bukkit.configuration.ConfigurationSection section) {
        if (ticks % 2L != 0L) return;
        if (state.action == ShamanAction.PROJECTILE_TELEGRAPH && state.snapshot != null) {
            Vector direction = state.snapshot.toVector().subtract(source.getEyeLocation().toVector());
            if (direction.lengthSquared() > 0.001D) {
                Location marker = source.getEyeLocation().add(direction.normalize().multiply(0.55D));
                source.getWorld().spawnParticle(Particle.DUST, marker, 3, .12D, .12D, .12D, new Particle.DustOptions(org.bukkit.Color.LIME, 1.0F));
            }
        }
        if (state.action == ShamanAction.POOL_TELEGRAPH && state.snapshot != null) {
            double radius = section.getDouble("pool.radius", 2.75D);
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2.0D * i / 8.0D;
                Location marker = state.snapshot.clone().add(Math.cos(angle) * radius, .06D, Math.sin(angle) * radius);
                source.getWorld().spawnParticle(Particle.DUST, marker, 1, 0.0D, 0.0D, 0.0D, new Particle.DustOptions(org.bukkit.Color.OLIVE, 1.0F));
            }
        }
    }
    private void tickMudPools(){ mudPools.entrySet().removeIf(e->e.getValue().expiresAt<=ticks); for(MudPool p:mudPools.values()){ p.center.getWorld().spawnParticle(Particle.DUST,p.center,3,p.radius*.5,.03,p.radius*.5,new Particle.DustOptions(org.bukkit.Color.OLIVE,1)); for(Player player:playersNear(p.center,p.radius)) if(player.getLocation().distanceSquared(p.center)<=p.radius*p.radius){player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,20,0));player.addPotionEffect(new PotionEffect(PotionEffectType.POISON,20,0));} } }
    private int activeMireMinionCount(UUID owner) {
        mireMinionOwners.entrySet().removeIf(entry -> {
            Entity minion = Bukkit.getEntity(entry.getKey());
            return minion == null || !minion.isValid() || minion.isDead();
        });
        return (int) mireMinionOwners.values().stream().filter(owner::equals).count();
    }
    private void removeMireOwned(UUID owner){ mudPools.entrySet().removeIf(e->e.getValue().owner.equals(owner)); mireMinionOwners.entrySet().removeIf(e->{if(e.getValue().equals(owner)){Entity minion=Bukkit.getEntity(e.getKey());if(minion!=null)minion.remove();return true;}return false;});bulwarks.remove(owner);shamans.remove(owner); }

    private void tickMiningGiant(LivingEntity source, Player target) {
        if (target == null || !ready(source)) return;
        var section = behaviorSection(source);
        if (section == null) return;
        int count = Math.max(1, Math.min(8, section.getInt("rock-count", 1)));
        double radius = Math.max(0.0D, section.getDouble("rock-spawn-radius", 1.5D));
        double height = Math.max(1.0D, section.getDouble("rock-height", 7.0D));
        double prediction = Math.max(0.0D, section.getDouble("rock-prediction-factor", 0.35D));
        Location predicted = target.getLocation().clone().add(target.getVelocity().clone().multiply(prediction));
        long interval = Math.max(0L, section.getLong("rock-spawn-interval-ticks", 2L));
        for (int index = 0; index < count; index++) {
            Location impact = predicted.clone().add(
                    (Math.random() - 0.5D) * radius * 2.0D,
                    0.0D,
                    (Math.random() - 0.5D) * radius * 2.0D);
            spawnDripstone(source, impact.add(0.0D, height, 0.0D), index * interval, section);
        }
        nextAction.put(source.getUniqueId(), ticks + Math.max(1L, section.getLong("rock-cooldown-ticks", 100L)));
    }

    private void spawnDripstone(LivingEntity source, Location location, long delay,
                                org.bukkit.configuration.ConfigurationSection section) {
        Runnable spawn = () -> {
            if (!source.isValid() || source.isDead() || location.getWorld() == null) return;
            ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class);
            display.setItemStack(new ItemStack(Material.POINTED_DRIPSTONE));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            float scale = (float) Math.max(0.25D, section.getDouble("rock-scale", 1.6D));
            display.setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
            display.getPersistentDataContainer().set(dripstoneAttackKey, PersistentDataType.BYTE, (byte) 1);
            display.getPersistentDataContainer().set(rapidSourceKey, PersistentDataType.STRING,
                    source.getUniqueId().toString());
            display.setInterpolationDuration(1);
            double speed = Math.max(0.05D, section.getDouble("rock-speed", 0.35D));
            double collisionRadius = Math.max(0.2D, section.getDouble("rock-collision-radius", 1.0D));
            double damage = Math.max(0.0D, section.getDouble("rock-damage", 6.0D));
            double knockback = Math.max(0.0D, section.getDouble("rock-knockback", 0.35D));
            long lifetime = Math.max(20L, section.getLong("rock-lifetime-ticks", 80L));
            dripstoneAttacks.put(display.getUniqueId(), new DripstoneAttack(
                    display.getUniqueId(), source.getUniqueId(), new Vector(0.0D, -speed, 0.0D),
                    ticks + lifetime, collisionRadius, damage, knockback));
        };
        if (delay <= 0L) {
            spawn.run();
            return;
        }
        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, spawn, delay);
        tasks.add(task);
        Bukkit.getScheduler().runTaskLater(plugin, () -> tasks.remove(task), delay + 1L);
    }

    private void tickDripstoneAttacks() {
        for (DripstoneAttack attack : new ArrayList<>(dripstoneAttacks.values())) {
            Entity raw = Bukkit.getEntity(attack.displayId);
            if (!(raw instanceof ItemDisplay display) || !display.isValid() || display.isDead()
                    || ticks >= attack.expiresAt) {
                removeDripstone(attack);
                continue;
            }
            Location next = display.getLocation().clone().add(attack.velocity);
            Entity source = Bukkit.getEntity(attack.sourceId);
            boolean impact = next.getY() <= next.getWorld().getMinHeight()
                    || !next.getBlock().isPassable();
            for (Player player : playersNear(next, attack.collisionRadius)) {
                if (!attack.hitTargets.add(player.getUniqueId())) continue;
                if (source instanceof LivingEntity living) player.damage(attack.damage, living);
                else player.damage(attack.damage);
                Vector push = player.getLocation().toVector().subtract(next.toVector()).setY(0.15D);
                if (push.lengthSquared() > 0.01D) player.setVelocity(player.getVelocity().add(
                        push.normalize().multiply(attack.knockback)));
                impact = true;
            }
            if (impact) {
                next.getWorld().spawnParticle(Particle.BLOCK, next, 18,
                        0.5D, 0.2D, 0.5D, Material.POINTED_DRIPSTONE.createBlockData());
                next.getWorld().playSound(next, Sound.BLOCK_STONE_BREAK, 0.8F, 1.1F);
                removeDripstone(attack);
                continue;
            }
            display.teleport(next);
            attack.velocity.setY(Math.max(-1.2D, attack.velocity.getY()
                    - Math.max(0.0D, behaviorSectionValueFromSource(attack.sourceId, "rock-gravity", 0.035D))));
        }
    }

    private double behaviorSectionValueFromSource(UUID sourceId, String key, double fallback) {
        Entity source = Bukkit.getEntity(sourceId);
        return source instanceof LivingEntity living ? behaviorSectionValue(living, key, fallback) : fallback;
    }

    private void removeDripstonesForSource(UUID sourceId) {
        dripstoneAttacks.values().stream()
                .filter(attack -> attack.sourceId.equals(sourceId))
                .toList()
                .forEach(this::removeDripstone);
    }

    private void removeDripstone(DripstoneAttack attack) {
        dripstoneAttacks.remove(attack.displayId);
        Entity display = Bukkit.getEntity(attack.displayId);
        if (display != null) display.remove();
    }

    private void tickSwappingProjectiles() {
        for (UUID projectileId : new ArrayList<>(projectileTypes.keySet())) {
            if (!"swapping_witch".equals(projectileTypes.get(projectileId))) continue;
            Entity raw = Bukkit.getEntity(projectileId);
            if (!(raw instanceof Projectile projectile) || !projectile.isValid() || projectile.isDead()
                    || ticks >= projectileExpiry.getOrDefault(projectileId, Long.MAX_VALUE)) {
                removeTrackedProjectile(projectileId);
                continue;
            }
            Entity sourceEntity = Bukkit.getEntity(projectileSources.get(projectileId));
            if (!(sourceEntity instanceof LivingEntity source)) {
                removeTrackedProjectile(projectileId);
                continue;
            }
            double radius = behaviorSectionValue(source, "virtual-hitbox-radius", 1.1D);
            for (Player player : playersNear(projectile.getLocation(), radius)) {
                if (!hasClearLine(projectile.getLocation(), player.getEyeLocation())) continue;
                if (trySwap(source, player, projectile.getLocation())) {
                    removeTrackedProjectile(projectileId);
                    break;
                }
            }
        }
    }

    private boolean hasClearLine(Location start, Location target) {
        if (start.getWorld() == null || target.getWorld() != start.getWorld()) return false;
        Vector direction = target.toVector().subtract(start.toVector());
        double distance = direction.length();
        if (distance <= 0.01D) return true;
        return start.getWorld().rayTraceBlocks(start, direction.normalize(), distance,
                FluidCollisionMode.NEVER, false) == null;
    }

    private boolean trySwap(LivingEntity source, Player player, Location impact) {
        var section = behaviorSection(source);
        double minDistance = section == null ? 3.0D : section.getDouble("min-swap-distance", 3.0D);
        double maxDistance = section == null ? 32.0D : section.getDouble("max-swap-distance", 32.0D);
        double distance = source.getLocation().distance(player.getLocation());
        if (distance < minDistance || distance > maxDistance || !hasClearLine(impact, player.getEyeLocation())) return false;
        Location sourceLocation = source.getLocation().clone();
        Location playerLocation = player.getLocation().clone();
        source.teleport(playerLocation);
        player.teleport(sourceLocation);
        player.getWorld().spawnParticle(Particle.PORTAL, playerLocation, 20, 0.4D, 0.7D, 0.4D, 0.1D);
        return true;
    }

    private void removeTrackedProjectile(UUID projectileId) {
        projectileSources.remove(projectileId);
        projectileTypes.remove(projectileId);
        projectileExpiry.remove(projectileId);
        Entity projectile = Bukkit.getEntity(projectileId);
        if (projectile != null) projectile.remove();
    }

    private void tickLavaCube(LivingEntity source, Player target) {
        if (!(source instanceof MagmaCube cube) || target == null) return;
        boolean onGround = source.isOnGround();
        boolean wasGround = grounded.getOrDefault(source.getUniqueId(), false);
        grounded.put(source.getUniqueId(), onGround);
        if (onGround && !wasGround) createLavaField(source);
        if (!onGround || !ready(source) || source.getLocation().distanceSquared(target.getLocation()) < 9.0D) return;
        var section = behaviorSection(source);
        if (section == null) return;
        Vector direction = target.getLocation().toVector().subtract(source.getLocation().toVector()).setY(0.0D);
        if (direction.lengthSquared() < 0.01D) return;
        double jump = section.getDouble("jump-strength", 0.7D);
        source.setVelocity(direction.normalize().multiply(0.35D).setY(jump));
        nextAction.put(source.getUniqueId(), ticks + Math.max(1L, section.getLong("jump-cooldown-ticks", 50L)));
    }

    private void createLavaField(LivingEntity source) {
        var section = behaviorSection(source);
        int duration = section == null ? 60 : Math.max(1, section.getInt("lava-duration-ticks", 60));
        double radius = section == null ? 2.5D : section.getDouble("lava-radius", 2.5D);
        Location center = source.getLocation().clone();
        final org.bukkit.scheduler.BukkitTask[] holder = new org.bukkit.scheduler.BukkitTask[1];
        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int elapsed;
            @Override public void run() {
                if (elapsed++ >= duration || center.getWorld() == null) {
                    if (holder[0] != null) {
                        holder[0].cancel();
                        tasks.remove(holder[0]);
                    }
                    return;
                }
                center.getWorld().spawnParticle(Particle.FLAME, center, 12, radius * 0.5D, 0.1D, radius * 0.5D, 0.02D);
                for (Player player : playersNear(center, radius)) player.damage(1.0D, source);
            }
        }, 0L, 5L);
        holder[0] = task;
        tasks.add(task);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { task.cancel(); tasks.remove(task); }, duration + 2L);
    }

    private void tickFakeCreeper(LivingEntity source, Player target) {
        if (!(source instanceof Creeper creeper) || target == null) return;
        var section = behaviorSection(source);
        if (section == null) return;
        UUID id = source.getUniqueId();
        int phase = phases.getOrDefault(id, FAKE_APPROACH);
        double radius = section.getDouble("fake-radius", 3.0D);
        double distance = source.getLocation().distance(target.getLocation());
        if (phase == FAKE_APPROACH) {
            if (distance <= radius) {
                phases.put(id, FAKE_FAKE_EXPLOSION);
                stopPathfinding(source);
                source.getWorld().spawnParticle(Particle.SMOKE, source.getLocation(), 40, 0.6D, 0.7D, 0.6D, 0.05D);
                source.getWorld().playSound(source.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.0F, 0.8F);
                nextAction.put(id, ticks + Math.max(1L, section.getLong("fake-duration-ticks", 20L)));
            } else {
                pathfindTo(source, target, section.getDouble("approach-speed", 0.20D));
            }
        } else if (phase == FAKE_FAKE_EXPLOSION) {
            stopPathfinding(source);
            if (ready(source)) phases.put(id, FAKE_FLEE);
        } else if (phase == FAKE_FLEE) {
            double fleeDistance = section.getDouble("flee-distance", 9.0D);
            if (distance < fleeDistance) {
                getOrCreateFleeDestination(source, target, fleeDistance)
                        .ifPresentOrElse(location -> pathfindTo(source, location,
                                        section.getDouble("flee-speed", 0.32D)),
                                () -> stopPathfinding(source));
            } else {
                stopPathfinding(source);
                phases.put(id, FAKE_REAPPROACH);
                nextAction.put(id, ticks + 5L);
            }
        } else if (phase == FAKE_REAPPROACH) {
            if (distance <= radius && ready(source)) {
                phases.put(id, FAKE_REAL_EXPLOSION);
                creeper.setExplosionRadius((int) section.getDouble("explode-radius", 3.0D));
                approvedExplosions.add(id);
                creeper.setIgnited(true);
            } else {
                pathfindTo(source, target, section.getDouble("approach-speed", 0.20D));
            }
        } else if (phase == FAKE_REAL_EXPLOSION) {
            stopPathfinding(source);
        }
    }

    private void tickSwappingWitch(LivingEntity source, Player target) {
        if (target == null || !ready(source)) return;
        var section = behaviorSection(source);
        double speed = section == null ? 1.25D : Math.max(0.2D, section.getDouble("projectile-speed", 1.25D));
        double distance = source.getLocation().distance(target.getLocation());
        double predictionStrength = section == null ? 0.45D : Math.max(0.0D,
                section.getDouble("prediction-strength", 0.45D));
        double travelTicks = Math.min(20.0D, distance / speed);
        Location aim = target.getLocation().clone().add(target.getVelocity().clone()
                .multiply(travelTicks * predictionStrength));
        double aimError = section == null ? 0.02D : Math.max(0.0D, section.getDouble("aim-error", 0.02D));
        aim.add((Math.random() - 0.5D) * aimError, (Math.random() - 0.5D) * aimError,
                (Math.random() - 0.5D) * aimError);
        Vector direction = aim.toVector().subtract(source.getEyeLocation().toVector()).normalize();
        Snowball projectile = source.getWorld().spawn(source.getEyeLocation(), Snowball.class);
        projectile.setVelocity(direction.multiply(speed));
        projectile.setGravity(section == null || section.getBoolean("projectile-gravity", false));
        projectile.getPersistentDataContainer().set(swappingProjectileKey, PersistentDataType.BYTE, (byte) 1);
        projectile.getPersistentDataContainer().set(rapidSourceKey, PersistentDataType.STRING,
                source.getUniqueId().toString());
        projectileSources.put(projectile.getUniqueId(), source.getUniqueId());
        projectileTypes.put(projectile.getUniqueId(), "swapping_witch");
        projectileExpiry.put(projectile.getUniqueId(), ticks + behaviorSectionLong(source, "projectile-lifetime-ticks", 50L));
        nextAction.put(source.getUniqueId(), ticks + behaviorSectionLong(source, "attack-cooldown-ticks", 100L));
    }

    private void tickChargingZombie(LivingEntity source, Player target) {
        if (!(source instanceof Mob mob)) return;
        if (target == null) {
            if (dashUntil.containsKey(source.getUniqueId())) clearDash(source);
            mob.setTarget(null);
            return;
        }
        if (dashUntil.containsKey(source.getUniqueId())) {
            mob.setTarget(null);
            if (ticks >= dashUntil.get(source.getUniqueId()) || source.getLocation().getBlock().getType().isSolid()) {
                stun(source);
                source.setVelocity(new Vector());
                clearDash(source);
                mob.setAI(true);
                mob.setTarget(target);
                return;
            }
            source.setVelocity(dashDirections.get(source.getUniqueId()).clone()
                    .multiply(behaviorSectionValue(source, "charge-speed", 1.15D)));
            for (Player player : playersNear(source.getLocation(), 1.5D)) {
                if (dashHits.computeIfAbsent(source.getUniqueId(), ignored -> new HashSet<>()).add(player.getUniqueId())) {
                    player.damage(behaviorSectionValue(source, "charge-damage", 8.0D), source);
                }
            }
            return;
        }
        double distance = source.getLocation().distance(target.getLocation());
        double minDistance = behaviorSectionValue(source, "charge-min-distance", 6.0D);
        double maxDistance = behaviorSectionValue(source, "charge-max-distance", 18.0D);
        mob.setTarget(target);
        if (distance > maxDistance) return;
        if (distance < minDistance || !ready(source)) return;
        mob.setTarget(null);
        stopPathfinding(source);
        Vector direction = target.getLocation().add(0.0D, 0.5D, 0.0D).toVector()
                .subtract(source.getLocation().toVector()).normalize();
        dashDirections.put(source.getUniqueId(), direction);
        dashUntil.put(source.getUniqueId(), ticks + behaviorSectionLong(source, "charge-duration-ticks", 18L));
        dashHits.put(source.getUniqueId(), new HashSet<>());
        nextAction.put(source.getUniqueId(), ticks + 60L);
    }

    private void tickRapidShooter(LivingEntity source, Player target) {
        if (target == null || !ready(source)) return;
        double speed = behaviorSectionValue(source, "projectile-speed", 1.6D);
        var section = behaviorSection(source);
        int projectileCount = section == null ? 3 : Math.max(1, Math.min(8, section.getInt("projectile-count", 3)));
        long interval = section == null ? 2L : Math.max(1L, section.getLong("projectile-interval-ticks", 2L));
        double spread = section == null ? 0.06D : Math.max(0.0D, section.getDouble("spread", 0.06D));
        Location targetLocation = target.getLocation().add(0.0D, 1.0D, 0.0D).clone();
        for (int index = 0; index < projectileCount; index++) {
            int shotIndex = index;
            scheduleRapidArrow(source, targetLocation, speed, spread, shotIndex, projectileCount, interval * index);
        }
        nextAction.put(source.getUniqueId(), ticks + behaviorSectionLong(source, "attack-cooldown-ticks", 80L)
                + interval * projectileCount);
    }

    private void scheduleRapidArrow(LivingEntity source, Location targetLocation, double speed, double spread,
                                     int index, int count, long delay) {
        final org.bukkit.scheduler.BukkitTask[] holder = new org.bukkit.scheduler.BukkitTask[1];
        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!source.isValid() || source.isDead() || source.getWorld() != targetLocation.getWorld()) return;
            Arrow arrow = source.getWorld().spawn(source.getEyeLocation(), Arrow.class);
            Vector direction = targetLocation.toVector().subtract(arrow.getLocation().toVector()).normalize();
            double center = (count - 1) / 2.0D;
            double offset = (index - center) * spread;
            direction.add(new Vector(offset, 0.0D, offset * 0.35D)).normalize();
            arrow.setVelocity(direction.multiply(speed));
            arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            arrow.getPersistentDataContainer().set(rapidArrowKey, PersistentDataType.BYTE, (byte) 1);
            arrow.getPersistentDataContainer().set(rapidSourceKey, PersistentDataType.STRING,
                    source.getUniqueId().toString());
            projectileSources.put(arrow.getUniqueId(), source.getUniqueId());
            projectileTypes.put(arrow.getUniqueId(), "rapid_arrow");
            long lifetime = Math.max(20L, Math.round(
                    behaviorSectionValue(source, "max-range", 32.0D) / Math.max(0.1D, speed) * 2.0D));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (arrow.isValid()) arrow.remove();
                projectileSources.remove(arrow.getUniqueId());
                projectileTypes.remove(arrow.getUniqueId());
            }, lifetime);
        }, delay);
        holder[0] = task;
        tasks.add(task);
        Bukkit.getScheduler().runTaskLater(plugin, () -> tasks.remove(holder[0]), delay + 1L);
    }

    private void clearDash(LivingEntity source) {
        UUID id = source.getUniqueId();
        dashUntil.remove(id);
        dashDirections.remove(id);
        dashHits.remove(id);
        stopPathfinding(source);
    }

    private void faceTarget(LivingEntity source, Location target) {
        Vector direction = target.toVector().subtract(source.getLocation().toVector());
        if (direction.lengthSquared() < 0.01D) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        float pitch = (float) Math.toDegrees(-Math.atan2(direction.getY(),
                Math.sqrt(direction.getX() * direction.getX() + direction.getZ() * direction.getZ())));
        source.setRotation(yaw, Math.max(-90.0F, Math.min(90.0F, pitch)));
    }

    private void stun(LivingEntity source) {
        int duration = behaviorSection(source) == null ? 40 : behaviorSection(source).getInt("stun-duration-ticks", 40);
        source.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Math.max(1, duration), 10));
        source.getWorld().spawnParticle(Particle.CRIT, source.getLocation().add(0.0D, 1.0D, 0.0D), 12, 0.3D, 0.3D, 0.3D, 0.1D);
    }

    private void configurePathfinder(LivingEntity source) {
        if (!(source instanceof org.bukkit.entity.Mob mob)) return;
        source.setGravity(true);
        mob.setAI(true);
        mob.setAware(true);
        if (!"charging_zombie".equals(behaviorId(source))) mob.setTarget(null);
        Pathfinder pathfinder = mob.getPathfinder();
        pathfinder.setCanFloat(false);
    }

    private boolean pathfindTo(LivingEntity source, LivingEntity target, double speed) {
        return pathfindTo(source, target.getLocation(), speed);
    }

    private boolean pathfindTo(LivingEntity source, Location destination, double speed) {
        if (!(source instanceof org.bukkit.entity.Mob mob) || destination.getWorld() == null
                || source.getWorld() != destination.getWorld()) return false;
        Pathfinder pathfinder = mob.getPathfinder();
        pathfinder.setCanFloat(false);
        long interval = Math.max(5L, Math.min(10L,
                behaviorSectionLong(source, "path-recalc-interval-ticks", 8L)));
        UUID id = source.getUniqueId();
        long next = nextPathRecalculation.getOrDefault(id, 0L);
        if (ticks < next) return pathfinder.hasPath();
        nextPathRecalculation.put(id, ticks + interval);
        return pathfinder.moveTo(destination, Math.max(0.05D, speed));
    }

    private void stopPathfinding(LivingEntity source) {
        if (source instanceof org.bukkit.entity.Mob mob) {
            mob.getPathfinder().stopPathfinding();
        }
        nextPathRecalculation.remove(source.getUniqueId());
        navigationTargets.remove(source.getUniqueId());
    }

    private java.util.Optional<Location> getOrCreateFleeDestination(LivingEntity source, Player target,
                                                                      double distance) {
        UUID id = source.getUniqueId();
        Location existing = navigationTargets.get(id);
        if (existing != null && existing.getWorld() == source.getWorld()
                && source.getLocation().distanceSquared(existing) > 2.25D) {
            return java.util.Optional.of(existing);
        }
        java.util.Optional<Location> next = findSafeFleeLocation(source, target, distance);
        next.ifPresent(location -> navigationTargets.put(id, location));
        return next;
    }

    private java.util.Optional<Location> findSafeFleeLocation(LivingEntity source, Player target, double distance) {
        if (source.getWorld() == null || target.getWorld() != source.getWorld()) return java.util.Optional.empty();
        Vector away = source.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0.0D);
        if (away.lengthSquared() < 0.01D) {
            away = target.getLocation().getDirection().setY(0.0D).multiply(-1.0D);
        }
        if (away.lengthSquared() < 0.01D) return java.util.Optional.empty();
        away.normalize();
        Vector side = new Vector(-away.getZ(), 0.0D, away.getX());
        double[] distances = {distance, distance * 0.75D, distance * 1.25D};
        double[] sideOffsets = {0.0D, 1.0D, -1.0D, 2.0D, -2.0D};
        for (double candidateDistance : distances) {
            for (double sideOffset : sideOffsets) {
                Location candidate = source.getLocation().clone()
                        .add(away.clone().multiply(candidateDistance))
                        .add(side.clone().multiply(sideOffset));
                int blockX = candidate.getBlockX();
                int blockZ = candidate.getBlockZ();
                Block ground = source.getWorld().getHighestBlockAt(blockX, blockZ);
                if (!ground.getType().isSolid() || ground.isLiquid()) continue;
                Location feet = new Location(source.getWorld(), blockX + 0.5D, ground.getY() + 1.0D,
                        blockZ + 0.5D, source.getLocation().getYaw(), source.getLocation().getPitch());
                if (feet.getBlock().isPassable() && feet.clone().add(0.0D, 1.0D, 0.0D).getBlock().isPassable()) {
                    if (source instanceof Mob mob && mob.getPathfinder().findPath(feet) == null) continue;
                    return java.util.Optional.of(feet);
                }
            }
        }
        return java.util.Optional.empty();
    }

    private boolean ready(LivingEntity entity) {
        return ticks >= nextAction.getOrDefault(entity.getUniqueId(), 0L);
    }

    private boolean usesPathfinderGroundController(String behavior) {
        return "fake_explosion_creeper".equals(behavior)
                || "charging_zombie".equals(behavior);
    }

    private Player nearestPlayer(LivingEntity source, double range) {
        Player nearest = null;
        double closest = range * range;
        for (Entity entity : source.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof Player player) || player.isDead() || !player.isValid()) continue;
            double distance = source.getLocation().distanceSquared(player.getLocation());
            if (distance < closest) { closest = distance; nearest = player; }
        }
        return nearest;
    }

    private Player trackedTargetOrNearest(LivingEntity source, double range) {
        UUID targetId = currentTargets.get(source.getUniqueId());
        Entity tracked = targetId == null ? null : Bukkit.getEntity(targetId);
        double maxDistanceSquared = range * range;
        if (tracked instanceof Player player && player.isValid() && !player.isDead()
                && player.getWorld() == source.getWorld()
                && player.getLocation().distanceSquared(source.getLocation()) <= maxDistanceSquared) {
            return player;
        }
        currentTargets.remove(source.getUniqueId());
        return nearestPlayer(source, range);
    }

    private List<Player> playersNear(Location location, double radius) {
        return location.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
                .filter(Player.class::isInstance).map(Player.class::cast).filter(player -> !player.isDead()).toList();
    }

    private String behaviorId(LivingEntity entity) {
        var section = mobSection(entity);
        if (section == null) return "";
        String configured = section.getString("behavior-id", "").trim().toLowerCase(Locale.ROOT);
        if (!configured.isBlank()) return configured;
        String id = mobService.getMobId(entity).trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "mining_giant", "lava_cube", "stone_armored_zombie", "golden_bulwark", "mire_shaman",
                    "fake_explosion_creeper", "swapping_witch", "charging_zombie", "rapid_shooter",
                    "splitting_creeper" -> id;
            default -> "";
        };
    }

    private org.bukkit.configuration.ConfigurationSection behaviorSection(LivingEntity entity) {
        var section = mobSection(entity);
        if (section == null) return null;
        var behavior = section.getConfigurationSection("behavior");
        return behavior == null ? section : behavior;
    }

    private org.bukkit.configuration.ConfigurationSection mobSection(LivingEntity entity) {
        String id = mobService.getMobId(entity);
        return id.isBlank() ? null : mobService.getMobRegistry().getSection(id).orElse(null);
    }

    private double behaviorSectionValue(LivingEntity entity, String key, double fallback) {
        var section = behaviorSection(entity);
        return section == null ? fallback : section.getDouble(key, fallback);
    }

    private long behaviorSectionLong(LivingEntity entity, String key, long fallback) {
        var section = behaviorSection(entity);
        return section == null ? fallback : section.getLong(key, fallback);
    }

    public List<String> debugInfo(LivingEntity entity) {
        String behavior = behaviorId(entity);
        if (behavior.isBlank()) return List.of();
        String target = "none";
        UUID targetId = currentTargets.get(entity.getUniqueId());
        Entity targetEntity = targetId == null ? null : Bukkit.getEntity(targetId);
        if (targetEntity instanceof Player player) target = player.getName();
        boolean hasPath = entity instanceof org.bukkit.entity.Mob mob && mob.getPathfinder().hasPath();
        long cooldown = Math.max(0L, nextAction.getOrDefault(entity.getUniqueId(), 0L) - ticks);
        int phase = phases.getOrDefault(entity.getUniqueId(), FAKE_APPROACH);
        return List.of(
                "mobId=" + mobService.getMobId(entity),
                "behaviorId=" + behavior,
                "phase=" + phaseName(behavior, phase),
                "ai=" + entity.hasAI(),
                "gravity=" + entity.hasGravity(),
                "velocity=" + formatVector(entity.getVelocity()),
                "target=" + target,
                "pathfinder.hasPath=" + hasPath,
                "cooldown-ticks=" + cooldown
        );
    }

    private String phaseName(String behavior, int phase) {
        if (!"fake_explosion_creeper".equals(behavior)) return String.valueOf(phase);
        return switch (phase) {
            case FAKE_APPROACH -> "PHASE_APPROACH";
            case FAKE_FAKE_EXPLOSION -> "PHASE_FAKE";
            case FAKE_FLEE -> "PHASE_FLEE";
            case FAKE_REAPPROACH -> "PHASE_REAPPROACH";
            case FAKE_REAL_EXPLOSION -> "PHASE_REAL_EXPLOSION";
            default -> "UNKNOWN";
        };
    }

    private String formatVector(Vector vector) {
        return String.format(Locale.ROOT, "%.2f,%.2f,%.2f", vector.getX(), vector.getY(), vector.getZ());
    }

    private void unregister(UUID id) {
        tracked.remove(id);
        nextAction.remove(id);
        nextPathRecalculation.remove(id);
        currentTargets.remove(id);
        navigationTargets.remove(id);
        phases.remove(id);
        grounded.remove(id);
        dashUntil.remove(id);
        dashDirections.remove(id);
        dashHits.remove(id);
        approvedExplosions.remove(id);
        removeMireOwned(id);
        projectileExpiry.entrySet().removeIf(entry -> entry.getKey().equals(id));
    }
}
