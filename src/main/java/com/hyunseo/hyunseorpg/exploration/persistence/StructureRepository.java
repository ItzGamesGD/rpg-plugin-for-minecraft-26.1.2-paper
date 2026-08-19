package com.hyunseo.hyunseorpg.exploration.persistence;

import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Authoritative gateway: index mutation and persistence stay coupled. */
public final class StructureRepository {
    private final StructureStorage storage;
    private final StructureIndex index;
    private final Set<UUID> loadedWorlds = new HashSet<>();

    public StructureRepository(StructureStorage storage, StructureIndex index) {
        this.storage = storage;
        this.index = index;
    }

    public synchronized void ensureWorldLoaded(UUID worldId) throws IOException {
        if (loadedWorlds.contains(worldId)) return;

        // Mark the world loaded only after the complete snapshot is read and indexed.
        // A failed load must remain retryable after the underlying file is repaired.
        List<StructureRecord> records = storage.loadWorld(worldId);
        for (StructureRecord record : records) index.upsert(record);
        loadedWorlds.add(worldId);
    }

    public synchronized Optional<StructureRecord> get(UUID id) { return index.get(id); }
    public StructureIndex index() { return index; }

    public synchronized boolean createIfAbsent(StructureRecord record) throws IOException {
        ensureWorldLoaded(record.worldId());
        if (!index.register(record)) return false;
        saveWorld(record.worldId());
        return true;
    }

    public synchronized void save(StructureRecord record) throws IOException {
        ensureWorldLoaded(record.worldId());
        index.upsert(record);
        saveWorld(record.worldId());
    }

    public synchronized void flushAll() throws IOException {
        for (UUID worldId : Set.copyOf(loadedWorlds)) saveWorld(worldId);
    }

    private void saveWorld(UUID worldId) throws IOException {
        List<StructureRecord> records = index.getWorld(worldId);
        storage.saveWorld(worldId, records);
    }
}
