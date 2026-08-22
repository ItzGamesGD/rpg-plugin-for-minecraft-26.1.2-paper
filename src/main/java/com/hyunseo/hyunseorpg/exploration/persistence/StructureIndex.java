package com.hyunseo.hyunseorpg.exploration.persistence;

import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** In-memory spatial index. Records are indexed across every chunk touched by their bounds. */
public final class StructureIndex {
    private final Map<UUID, StructureRecord> byId = new LinkedHashMap<>();
    private final Map<ChunkKey, Set<UUID>> byChunk = new LinkedHashMap<>();

    public synchronized boolean register(StructureRecord record) {
        if (byId.containsKey(record.structureId())) return false;
        byId.put(record.structureId(), record);
        index(record);
        return true;
    }

    public synchronized void upsert(StructureRecord record) {
        StructureRecord old = byId.put(record.structureId(), record);
        if (old != null) deindex(old);
        index(record);
    }

    public synchronized Optional<StructureRecord> get(UUID id) { return Optional.ofNullable(byId.get(id)); }

    public synchronized List<StructureRecord> getWorld(UUID worldId) {
        return byId.values().stream().filter(record -> record.worldId().equals(worldId)).toList();
    }

    public synchronized List<StructureRecord> getChunk(UUID worldId, int chunkX, int chunkZ) {
        Set<UUID> ids = byChunk.get(new ChunkKey(worldId, chunkX, chunkZ));
        if (ids == null) return List.of();
        List<StructureRecord> result = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            StructureRecord record = byId.get(id);
            if (record != null) result.add(record);
        }
        return List.copyOf(result);
    }

    public synchronized List<StructureRecord> nearby(UUID worldId, double x, double z, double radius) {
        int minChunkX = ((int) Math.floor(x - radius)) >> 4;
        int maxChunkX = ((int) Math.floor(x + radius)) >> 4;
        int minChunkZ = ((int) Math.floor(z - radius)) >> 4;
        int maxChunkZ = ((int) Math.floor(z + radius)) >> 4;
        Set<UUID> ids = new LinkedHashSet<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                Set<UUID> chunk = byChunk.get(new ChunkKey(worldId, cx, cz));
                if (chunk != null) ids.addAll(chunk);
            }
        }
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    public synchronized int size() { return byId.size(); }

    private void index(StructureRecord record) {
        for (int cx = record.bounds().minChunkX(); cx <= record.bounds().maxChunkX(); cx++) {
            for (int cz = record.bounds().minChunkZ(); cz <= record.bounds().maxChunkZ(); cz++) {
                byChunk.computeIfAbsent(new ChunkKey(record.worldId(), cx, cz), ignored -> new LinkedHashSet<>())
                        .add(record.structureId());
            }
        }
    }

    private void deindex(StructureRecord record) {
        for (int cx = record.bounds().minChunkX(); cx <= record.bounds().maxChunkX(); cx++) {
            for (int cz = record.bounds().minChunkZ(); cz <= record.bounds().maxChunkZ(); cz++) {
                ChunkKey key = new ChunkKey(record.worldId(), cx, cz);
                Set<UUID> ids = byChunk.get(key);
                if (ids == null) continue;
                ids.remove(record.structureId());
                if (ids.isEmpty()) byChunk.remove(key);
            }
        }
    }

    private record ChunkKey(UUID worldId, int x, int z) { }
}
