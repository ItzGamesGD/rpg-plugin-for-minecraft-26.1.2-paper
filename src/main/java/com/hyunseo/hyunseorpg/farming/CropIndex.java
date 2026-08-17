package com.hyunseo.hyunseorpg.farming;

import org.bukkit.Chunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory index of loaded crops, keyed by canonical base position and chunk. */
public final class CropIndex {
    private final Map<ChunkKey, Map<CropPosition, CropInstance>> byChunk = new LinkedHashMap<>();

    public synchronized boolean register(CropInstance instance) {
        ChunkKey key = ChunkKey.of(instance.position());
        Map<CropPosition, CropInstance> crops = byChunk.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
        if (crops.containsKey(instance.position())) return false;
        crops.put(instance.position(), instance);
        return true;
    }

    public synchronized Optional<CropInstance> get(CropPosition position) {
        Map<CropPosition, CropInstance> crops = byChunk.get(ChunkKey.of(position));
        return crops == null ? Optional.empty() : Optional.ofNullable(crops.get(position));
    }

    public synchronized Optional<CropInstance> findByBlock(CropPosition position) {
        return get(position);
    }

    public synchronized Optional<CropInstance> remove(CropPosition position) {
        ChunkKey key = ChunkKey.of(position);
        Map<CropPosition, CropInstance> crops = byChunk.get(key);
        if (crops == null) return Optional.empty();
        CropInstance removed = crops.remove(position);
        if (crops.isEmpty()) byChunk.remove(key);
        return Optional.ofNullable(removed);
    }

    public synchronized List<CropInstance> getChunk(UUID worldId, int chunkX, int chunkZ) {
        Map<CropPosition, CropInstance> crops = byChunk.get(new ChunkKey(worldId, chunkX, chunkZ));
        return crops == null ? List.of() : List.copyOf(crops.values());
    }

    public synchronized List<CropInstance> allLoaded() {
        List<CropInstance> result = new ArrayList<>();
        byChunk.values().forEach(crops -> result.addAll(crops.values()));
        return List.copyOf(result);
    }

    public synchronized List<CropInstance> removeChunk(UUID worldId, int chunkX, int chunkZ) {
        Map<CropPosition, CropInstance> removed = byChunk.remove(new ChunkKey(worldId, chunkX, chunkZ));
        return removed == null ? List.of() : List.copyOf(removed.values());
    }

    public synchronized int loadedChunkCount() {
        return byChunk.size();
    }

    public synchronized boolean isChunkLoaded(UUID worldId, int chunkX, int chunkZ) {
        return byChunk.containsKey(new ChunkKey(worldId, chunkX, chunkZ));
    }

    public synchronized List<ChunkKey> loadedChunks() {
        return List.copyOf(byChunk.keySet());
    }

    public synchronized int cropCount() {
        return byChunk.values().stream().mapToInt(Map::size).sum();
    }

    public static ChunkKey chunkKey(Chunk chunk) {
        return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    public record ChunkKey(UUID worldId, int x, int z) {
        public static ChunkKey of(CropPosition position) {
            return new ChunkKey(position.worldId(), position.chunkX(), position.chunkZ());
        }
    }
}
