package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentLocationsTest {
    @Test
    void terrestrialSafetyStillRejectsWater() {
        TestWorld world = new TestWorld();
        world.set(0, 64, 0, Material.WATER);
        world.set(0, 65, 0, Material.WATER);
        world.set(0, 63, 0, Material.STONE);

        assertFalse(ComponentLocations.isEntitySafe(world.block(0, 64, 0)));
    }

    @Test
    void aquaticSafetyAcceptsTwoWaterBlocks() {
        TestWorld world = new TestWorld();
        world.set(0, 64, 0, Material.WATER);
        world.set(0, 65, 0, Material.WATER);

        assertTrue(ComponentLocations.isAquaticEntitySafe(world.block(0, 64, 0)));
    }

    @Test
    void aquaticSafetyRejectsLavaAndSolidBodyBlocks() {
        TestWorld world = new TestWorld();
        world.set(0, 64, 0, Material.LAVA);
        world.set(0, 65, 0, Material.LAVA);
        world.set(1, 64, 0, Material.WATER);
        world.set(1, 65, 0, Material.STONE);

        assertFalse(ComponentLocations.isAquaticEntitySafe(world.block(0, 64, 0)));
        assertFalse(ComponentLocations.isAquaticEntitySafe(world.block(1, 64, 0)));
    }

    @Test
    void aquaticSearchFindsNearestWaterAndStaysInsideBounds() {
        TestWorld testWorld = new TestWorld();
        World world = testWorld.world();
        StructureBounds bounds = new StructureBounds(0, 60, 0, 2, 66, 2);
        testWorld.set(2, 64, 2, Material.WATER);
        testWorld.set(2, 65, 2, Material.WATER);
        testWorld.set(3, 64, 2, Material.WATER);
        testWorld.set(3, 65, 2, Material.WATER);

        Location result = ComponentLocations.safeSpawnLocation(
                world, bounds, new Location(world, 0.5, 64, 0.5), true).orElseThrow();

        assertEquals(2, result.getBlockX());
        assertEquals(64, result.getBlockY());
        assertEquals(2, result.getBlockZ());
    }

    @Test
    void aquaticSearchFailsClosedWithoutValidWater() {
        TestWorld testWorld = new TestWorld();
        World world = testWorld.world();

        assertTrue(ComponentLocations.safeSpawnLocation(world,
                new StructureBounds(0, 60, 0, 2, 66, 2),
                new Location(world, 1.5, 64, 1.5), true).isEmpty());
    }

    private static final class TestWorld {
        private final Map<Position, Material> materials = new HashMap<>();
        private final World world = proxy(World.class, (proxy, method, args) -> switch (method.getName()) {
            case "getBlockAt" -> block((int) args[0], (int) args[1], (int) args[2]);
            case "getMinHeight" -> -64;
            case "getMaxHeight" -> 320;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> defaultValue(method.getReturnType());
        });

        World world() { return world; }
        void set(int x, int y, int z, Material material) { materials.put(new Position(x, y, z), material); }

        Block block(int x, int y, int z) {
            return proxy(Block.class, (proxy, method, args) -> switch (method.getName()) {
                case "getType" -> materials.getOrDefault(new Position(x, y, z), Material.AIR);
                case "isLiquid" -> {
                    Material material = materials.getOrDefault(new Position(x, y, z), Material.AIR);
                    yield material == Material.WATER || material == Material.LAVA;
                }
                case "isPassable" -> {
                    Material material = materials.getOrDefault(new Position(x, y, z), Material.AIR);
                    yield material == Material.AIR || material == Material.WATER || material == Material.LAVA;
                }
                case "getRelative" -> block(x + (int) args[0], y + (int) args[1], z + (int) args[2]);
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0D;
        if (type == float.class) return 0.0F;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }

    private record Position(int x, int y, int z) { }
}
