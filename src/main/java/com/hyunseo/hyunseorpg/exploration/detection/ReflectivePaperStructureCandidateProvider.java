package com.hyunseo.hyunseorpg.exploration.detection;

import com.hyunseo.hyunseorpg.exploration.model.StructureAnchor;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureCandidate;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.NamespacedKey;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;

/**
 * Paper 26.1.2 structure scanner. The target API is compile-time available, so using the typed
 * contract avoids implementation-class reflection failures on live Paper servers.
 */
public final class ReflectivePaperStructureCandidateProvider implements StructureCandidateProvider {
    private final JavaPlugin plugin;
    private volatile boolean failureLogged;

    public ReflectivePaperStructureCandidateProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<StructureCandidate> scanChunk(World world, int chunkX, int chunkZ, Set<String> minecraftKeys) {
        if (minecraftKeys.isEmpty()) return List.of();
        try {
            Collection<GeneratedStructure> collection = world.getStructures(chunkX, chunkZ);
            if (collection == null || collection.isEmpty()) return List.of();

            List<StructureCandidate> result = new ArrayList<>();
            for (GeneratedStructure generated : collection) {
                String minecraftKey = structureKey(generated);
                if (minecraftKey == null || !minecraftKeys.contains(minecraftKey)) continue;
                StructureCandidate candidate = candidate(world, minecraftKey, generated);
                if (candidate != null) result.add(candidate);
            }
            return List.copyOf(result);
        } catch (RuntimeException exception) {
            if (!failureLogged) {
                failureLogged = true;
                plugin.getLogger().log(Level.WARNING,
                        "Exploration structure scan failed for this chunk; detection will retry on the next chunk load.",
                        exception);
            }
            return List.of();
        }
    }

    private String structureKey(GeneratedStructure generated) {
        Structure structure = generated.getStructure();
        if (structure == null) return null;
        NamespacedKey key = structure.getKey();
        if (key == null) return null;
        return key.toString().trim().toLowerCase(java.util.Locale.ROOT);
    }

    private StructureCandidate candidate(World world, String minecraftKey, GeneratedStructure generated) {
        org.bukkit.util.BoundingBox box = generated.getBoundingBox();
        if (box == null) return null;
        int minX = floor(box.getMinX());
        int minY = floor(box.getMinY());
        int minZ = floor(box.getMinZ());
        int maxX = ceil(box.getMaxX());
        int maxY = ceil(box.getMaxY());
        int maxZ = ceil(box.getMaxZ());
        StructureBounds bounds = new StructureBounds(minX, minY, minZ, maxX, maxY, maxZ);
        StructureAnchor anchor = new StructureAnchor(world.getUID(), bounds.centerX(), bounds.centerY(), bounds.centerZ());
        return new StructureCandidate(world.getUID(), minecraftKey, bounds, anchor);
    }

    private static int floor(double value) { return (int) Math.floor(value); }
    private static int ceil(double value) { return (int) Math.ceil(value); }
}
