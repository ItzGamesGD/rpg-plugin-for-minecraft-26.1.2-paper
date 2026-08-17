package com.hyunseo.hyunseorpg.farming;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Storage abstraction for loaded crop chunk snapshots. */
public interface CropStorage extends AutoCloseable {
    CropChunkSnapshot load(UUID worldId, int chunkX, int chunkZ) throws IOException;

    void save(CropChunkSnapshot snapshot) throws IOException;

    @Override
    default void close() {
    }

    record CropChunkSnapshot(UUID worldId, int chunkX, int chunkZ, List<CropInstance> crops) {
        public CropChunkSnapshot {
            crops = List.copyOf(crops == null ? List.of() : crops);
        }
    }
}
