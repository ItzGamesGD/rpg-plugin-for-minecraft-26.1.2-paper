package com.hyunseo.hyunseorpg.exploration.detection;

import com.hyunseo.hyunseorpg.exploration.model.StructureCandidate;
import org.bukkit.World;

import java.util.List;
import java.util.Set;

/** Adapter boundary around version-sensitive Paper/Bukkit structure APIs. */
public interface StructureCandidateProvider {
    List<StructureCandidate> scanChunk(World world, int chunkX, int chunkZ, Set<String> minecraftKeys);
}
