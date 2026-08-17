package com.hyunseo.hyunseorpg.player;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class PlayerDataService {
    private final JavaPlugin plugin;
    private final PlayerDataRepository repository;
    private final PlayerDataCache cache;
    private final Set<UUID> failedLoads = new HashSet<>();
    private final Set<UUID> dirtyPlayers = new HashSet<>();
    private org.bukkit.scheduler.BukkitTask autosaveTask;

    public PlayerDataService(JavaPlugin plugin, PlayerDataRepository repository, PlayerDataCache cache) {
        this.plugin = plugin;
        this.repository = repository;
        this.cache = cache;
    }

    public PlayerRPGData loadPlayer(Player player) {
        return loadPlayer(player.getUniqueId());
    }

    public PlayerRPGData loadPlayer(UUID uuid) {
        try {
            PlayerRPGData data = repository.load(uuid);
            cache.put(data);
            markMigrationDirty(data);
            failedLoads.remove(uuid);
            return data;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load RPG data for " + uuid, exception);
            PlayerRPGData fallbackData = new PlayerRPGData(uuid);
            cache.put(fallbackData);
            failedLoads.add(uuid);
            return fallbackData;
        }
    }

    public PlayerRPGData getOrLoad(Player player) {
        return getOrLoad(player.getUniqueId());
    }

    public PlayerRPGData getOrLoad(UUID uuid) {
        return cache.get(uuid).orElseGet(() -> loadPlayer(uuid));
    }

    public boolean isLoaded(UUID uuid) {
        return uuid != null && cache.get(uuid).isPresent();
    }

    public Optional<PlayerRPGData> getCached(UUID uuid) {
        return cache.get(uuid);
    }

    public void savePlayer(Player player) {
        if (player != null) savePlayer(player.getUniqueId());
    }

    public synchronized void savePlayer(UUID uuid) {
        if (uuid != null && cache.get(uuid).isPresent()) dirtyPlayers.add(uuid);
    }

    public boolean savePlayerNow(UUID uuid) {
        if (uuid == null) return false;
        boolean saved = cache.get(uuid).map(this::saveData).orElse(false);
        if (saved) {
            synchronized (this) { dirtyPlayers.remove(uuid); }
        }
        return saved;
    }

    public synchronized boolean isDirty(UUID uuid) {
        return uuid != null && dirtyPlayers.contains(uuid);
    }

    /** Reloads a player from disk only when the in-memory snapshot has no unsaved changes. */
    public ReloadResult reloadPlayerIfClean(UUID uuid) {
        if (uuid == null) return ReloadResult.INVALID;
        synchronized (this) {
            if (dirtyPlayers.contains(uuid)) return ReloadResult.DIRTY;
        }
        try {
            PlayerRPGData data = repository.load(uuid);
            cache.put(data);
            markMigrationDirty(data);
            failedLoads.remove(uuid);
            return ReloadResult.RELOADED;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload RPG data for " + uuid, exception);
            return ReloadResult.FAILED;
        }
    }

    /** Coalesces frequent state changes; logout and shutdown still use synchronous flushes. */
    public synchronized void startAutosave() {
        if (autosaveTask != null) return;
        long seconds = Math.max(1L, plugin.getConfig().getLong("persistence.autosave-seconds", 15L));
        long maxBatch = Math.max(1L, plugin.getConfig().getLong("persistence.max-dirty-players-per-batch", 50L));
        autosaveTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> flushDirty((int) Math.min(Integer.MAX_VALUE, maxBatch)),
                seconds * 20L, seconds * 20L);
    }

    public synchronized void stopAutosave() {
        if (autosaveTask != null) autosaveTask.cancel();
        autosaveTask = null;
    }

    public void saveAndUnload(Player player) {
        UUID uuid = player.getUniqueId();
        cache.remove(uuid).ifPresent(data -> {
            dirtyPlayers.remove(uuid);
            saveData(data);
        });
        failedLoads.remove(uuid);
    }

    public void saveAll() {
        for (PlayerRPGData data : cache.values()) {
            saveData(data);
        }
        synchronized (this) { dirtyPlayers.clear(); }
    }

    public void saveAllAndClear() {
        saveAll();
        cache.clear();
        failedLoads.clear();
    }

    private boolean saveData(PlayerRPGData data) {
        if (failedLoads.contains(data.getUuid())) {
            plugin.getLogger().warning("Skipped saving RPG data for " + data.getUuid() + " because loading failed earlier.");
            return false;
        }

        try {
            repository.save(data);
            data.setFarmingDataMigrationRequired(false);
            data.setAlchemyDataMigrationRequired(false);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save RPG data for " + data.getUuid(), exception);
            return false;
        }
    }

    private void flushDirty(int maxBatch) {
        java.util.List<UUID> batch;
        synchronized (this) {
            batch = dirtyPlayers.stream().limit(Math.max(1, maxBatch)).toList();
            dirtyPlayers.removeAll(batch);
        }
        for (UUID uuid : batch) {
            cache.get(uuid).ifPresent(data -> {
                if (!saveData(data) && plugin.getConfig().getBoolean("persistence.retry-on-failure", true)) {
                    synchronized (this) { dirtyPlayers.add(uuid); }
                }
            });
        }
    }

    private synchronized void markMigrationDirty(PlayerRPGData data) {
        if (data.isFarmingDataMigrationRequired() || data.isAlchemyDataMigrationRequired()) {
            dirtyPlayers.add(data.getUuid());
        }
    }

    public enum ReloadResult {
        RELOADED,
        DIRTY,
        FAILED,
        INVALID
    }
}
