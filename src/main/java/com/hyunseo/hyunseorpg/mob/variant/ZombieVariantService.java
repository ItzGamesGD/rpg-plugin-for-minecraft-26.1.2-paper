package com.hyunseo.hyunseorpg.mob.variant;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.mob.MobTagService;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Optional;

public final class ZombieVariantService implements Listener {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final MobTagService mobTagService;
    private final ZombieVariantSelector selector;
    private final BombZombieService bombService;
    private final LeapZombieService leapService;
    private final NamespacedKey variantKey;
    private final NamespacedKey initializedKey;
    private final NamespacedKey processedKey;
    private final NamespacedKey spawnReasonKey;

    public ZombieVariantService(JavaPlugin plugin, ConfigService configService, MobTagService mobTagService) {
        this.plugin = plugin;
        this.configService = configService;
        this.mobTagService = mobTagService;
        this.selector = new ZombieVariantSelector(configService);
        this.bombService = new BombZombieService(plugin, configService);
        this.leapService = new LeapZombieService(plugin, configService);
        this.variantKey = new NamespacedKey(plugin, "zombie_variant");
        this.initializedKey = new NamespacedKey(plugin, "zombie_variant_initialized");
        this.processedKey = new NamespacedKey(plugin, "bomb_death_processed");
        this.spawnReasonKey = new NamespacedKey(plugin, "zombie_spawn_reason");
    }

    public void start() {
        leapService.start();
    }

    /** Called by the existing RPG natural-spawn pipeline after level scaling is applied. */
    public void initializeNaturalSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)
                || !mobTagService.isRpgMob(zombie)
                || !selector.shouldConsider(zombie.getType(), event.getSpawnReason(), zombie.getWorld())
                || isInitialized(zombie)) {
            return;
        }

        setInitialized(zombie, event.getSpawnReason().name());
        setVariant(zombie, selector.select());
        applyVariant(zombie);
    }

    public Optional<ZombieVariant> getVariant(LivingEntity entity) {
        if (entity == null) return Optional.empty();
        String raw = entity.getPersistentDataContainer().get(variantKey, PersistentDataType.STRING);
        return raw == null ? Optional.empty() : Optional.of(ZombieVariant.fromStored(raw));
    }

    public boolean isInitialized(LivingEntity entity) {
        return entity != null && entity.getPersistentDataContainer().has(initializedKey, PersistentDataType.BYTE);
    }

    public String getSpawnReason(LivingEntity entity) {
        if (entity == null) return "";
        return entity.getPersistentDataContainer().getOrDefault(spawnReasonKey, PersistentDataType.STRING, "");
    }

    public int getMobLevel(LivingEntity entity) {
        return entity == null ? 1 : mobTagService.getMobLevel(entity);
    }

    public boolean isBombDeathProcessed(LivingEntity entity) {
        return entity != null && entity.getPersistentDataContainer().has(processedKey, PersistentDataType.BYTE);
    }

    public long getLeapCooldownRemainingMillis(LivingEntity entity) {
        return entity == null ? 0L : leapService.getCooldownRemainingMillis(entity.getUniqueId());
    }

    public boolean isLeaping(LivingEntity entity) {
        return entity != null && leapService.isLeaping(entity.getUniqueId());
    }

    public ZombieVariant rollForDebug() {
        return selector.select();
    }

    public boolean forceVariant(LivingEntity entity, ZombieVariant variant) {
        if (!(entity instanceof Zombie) || variant == null) {
            return false;
        }
        setInitialized(entity, "DEBUG");
        setVariant(entity, variant);
        applyVariant(entity);
        return true;
    }

    public void registerExisting(LivingEntity entity) {
        if (!(entity instanceof Zombie) || !isInitialized(entity)) {
            return;
        }
        applyVariant(entity);
    }

    public void cancelAll() {
        bombService.cancelAll();
        leapService.cancelAll();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        ZombieVariant variant = getVariant(entity).orElse(ZombieVariant.NORMAL);
        if (variant == ZombieVariant.BOMB && !isBombDeathProcessed(entity)) {
            entity.getPersistentDataContainer().set(processedKey, PersistentDataType.BYTE, (byte) 1);
            bombService.scheduleExplosion(entity.getLocation(), mobTagService.getMobLevel(entity));
        }
        if (variant == ZombieVariant.LEAP) {
            leapService.unregister(entity);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        leapService.handleDamage(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof LivingEntity living) {
                registerExisting(living);
            }
        }
    }

    private void applyVariant(LivingEntity entity) {
        ZombieVariant variant = getVariant(entity).orElse(ZombieVariant.NORMAL);
        if (variant != ZombieVariant.BOMB) {
            bombService.removeVisualHelmet(entity);
        }
        if (variant != ZombieVariant.LEAP) {
            leapService.unregister(entity);
        } else {
            leapService.register(entity);
        }
        if (variant == ZombieVariant.BOMB) {
            bombService.apply(entity);
        }
    }

    private void setInitialized(LivingEntity entity, String reason) {
        entity.getPersistentDataContainer().set(initializedKey, PersistentDataType.BYTE, (byte) 1);
        String normalizedReason = reason == null ? "UNKNOWN" : reason.trim().toUpperCase(Locale.ROOT);
        entity.getPersistentDataContainer().set(spawnReasonKey, PersistentDataType.STRING, normalizedReason);
    }

    private void setVariant(LivingEntity entity, ZombieVariant variant) {
        entity.getPersistentDataContainer().set(variantKey, PersistentDataType.STRING, variant.name());
    }
}
