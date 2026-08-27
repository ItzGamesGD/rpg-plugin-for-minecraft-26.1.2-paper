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
        if (index.get(record.structureId()).isPresent()) return false;
        List<StructureRecord> next = new java.util.ArrayList<>(index.getWorld(record.worldId()));
        next.add(record);
        // Persist the candidate snapshot before publishing it to the in-memory index.
        // A failed write therefore cannot leave a ghost registration.
        storage.saveWorld(record.worldId(), List.copyOf(next));
        index.register(record);
        return true;
    }

    public synchronized void save(StructureRecord record) throws IOException {
        ensureWorldLoaded(record.worldId());
        List<StructureRecord> next = new java.util.ArrayList<>(index.getWorld(record.worldId()));
        boolean replaced = false;
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).structureId().equals(record.structureId())) {
                next.set(i, record);
                replaced = true;
                break;
            }
        }
        if (!replaced) next.add(record);
        // Publish only after the durable snapshot succeeds. This prevents a failed
        // pending-intent write from being mistaken for durable evidence on retry.
        storage.saveWorld(record.worldId(), List.copyOf(next));
        index.upsert(record);
    }

    public synchronized void flushAll() throws IOException {
        for (UUID worldId : Set.copyOf(loadedWorlds)) saveWorld(worldId);
    }

    private void saveWorld(UUID worldId) throws IOException {
        List<StructureRecord> records = index.getWorld(worldId);
        storage.saveWorld(worldId, records);
    }
}
