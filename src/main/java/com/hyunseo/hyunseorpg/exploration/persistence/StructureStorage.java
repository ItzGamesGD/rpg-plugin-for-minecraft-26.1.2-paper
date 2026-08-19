package com.hyunseo.hyunseorpg.exploration.persistence;

import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface StructureStorage {
    List<StructureRecord> loadWorld(UUID worldId) throws IOException;
    void saveWorld(UUID worldId, List<StructureRecord> records) throws IOException;
}
