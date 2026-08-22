package com.hyunseo.hyunseorpg.exploration.detection;

import com.hyunseo.hyunseorpg.exploration.model.StructureAnchor;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import com.hyunseo.hyunseorpg.exploration.model.StructureCandidate;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Paper structure scanner isolated behind reflection because the structure API is an integration seam.
 * It uses World#getStructures(chunkX, chunkZ), then reads GeneratedStructure#getStructure/getBoundingBox.
 * A missing API disables scanning safely instead of crashing the plugin.
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
            Method worldGetStructures = findWorldGetStructures(world.getClass());
            if (worldGetStructures == null) throw new NoSuchMethodException("World#getStructures(int,int)");

            Object raw = worldGetStructures.invoke(world, chunkX, chunkZ);
            if (!(raw instanceof Collection<?> collection)) return List.of();

            List<StructureCandidate> result = new ArrayList<>();
            for (Object generated : collection) {
                String minecraftKey = structureKey(generated);
                if (minecraftKey == null || !minecraftKeys.contains(minecraftKey)) continue;
                StructureCandidate candidate = candidate(world, minecraftKey, generated);
                if (candidate != null) result.add(candidate);
            }
            return List.copyOf(result);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!failureLogged) {
                failureLogged = true;
                plugin.getLogger().log(Level.WARNING,
                        "Exploration structure scan failed for this chunk; detection will retry on the next chunk load.",
                        exception);
            }
            return List.of();
        }
    }

    private String structureKey(Object generated) throws ReflectiveOperationException {
        Object structure = generated.getClass().getMethod("getStructure").invoke(generated);
        if (structure == null) return null;
        Object key = structure.getClass().getMethod("getKey").invoke(structure);
        if (key == null) return null;
        return key.toString().trim().toLowerCase(java.util.Locale.ROOT);
    }

    private StructureCandidate candidate(World world, String minecraftKey, Object generated) throws ReflectiveOperationException {
        Method getBoundingBox = generated.getClass().getMethod("getBoundingBox");
        Object box = getBoundingBox.invoke(generated);
        if (box == null) return null;
        int minX = floor(invokeDouble(box, "getMinX"));
        int minY = floor(invokeDouble(box, "getMinY"));
        int minZ = floor(invokeDouble(box, "getMinZ"));
        int maxX = ceil(invokeDouble(box, "getMaxX"));
        int maxY = ceil(invokeDouble(box, "getMaxY"));
        int maxZ = ceil(invokeDouble(box, "getMaxZ"));
        StructureBounds bounds = new StructureBounds(minX, minY, minZ, maxX, maxY, maxZ);
        StructureAnchor anchor = new StructureAnchor(world.getUID(), bounds.centerX(), bounds.centerY(), bounds.centerZ());
        return new StructureCandidate(world.getUID(), minecraftKey, bounds, anchor);
    }

    private static Method findWorldGetStructures(Class<?> worldClass) {
        for (Method method : worldClass.getMethods()) {
            if (!method.getName().equals("getStructures") || method.getParameterCount() != 2) continue;
            Class<?>[] parameters = method.getParameterTypes();
            if ((parameters[0] == int.class || parameters[0] == Integer.class)
                    && (parameters[1] == int.class || parameters[1] == Integer.class)) return method;
        }
        return null;
    }

    private static double invokeDouble(Object target, String method) throws ReflectiveOperationException {
        Object value = target.getClass().getMethod(method).invoke(target);
        if (!(value instanceof Number number)) throw new IllegalStateException(method + " did not return a number");
        return number.doubleValue();
    }

    private static int floor(double value) { return (int) Math.floor(value); }
    private static int ceil(double value) { return (int) Math.ceil(value); }
}
