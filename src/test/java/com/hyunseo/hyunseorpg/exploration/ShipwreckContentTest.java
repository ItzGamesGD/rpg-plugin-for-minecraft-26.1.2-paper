package com.hyunseo.hyunseorpg.exploration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShipwreckContentTest {
    @Test
    void bundledShipwreckIsABoundedCombatOnlyEncounter() {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("exploration/structures.yml")) {
            assertNotNull(stream, "exploration/structures.yml is missing");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            String shipwreck = "structures.shipwreck";
            assertEquals("minecraft:shipwreck", yaml.getString(shipwreck + ".minecraft-key"));
            assertFalse("minecraft:desert_pyramid".equals(yaml.getString(shipwreck + ".minecraft-key")));
            assertTrue(yaml.getBoolean(shipwreck + ".enabled"));
            assertEquals(0.0D, yaml.getDouble(shipwreck + ".selection-chance"), 0.000001D);

            ConfigurationSection structures = yaml.getConfigurationSection("structures");
            assertNotNull(structures);
            long shipwreckMappings = structures.getKeys(false).stream()
                    .filter(id -> "minecraft:shipwreck".equals(yaml.getString("structures." + id + ".minecraft-key")))
                    .count();
            assertEquals(1L, shipwreckMappings, "minecraft:shipwreck must map uniquely");

            String variant = shipwreck + ".variants.riptide_ambush_prototype";
            assertTrue(yaml.isConfigurationSection(variant));
            assertTrue(yaml.getBoolean(variant + ".enabled"));
            List<Map<?, ?>> components = yaml.getMapList(variant + ".components");

            List<Map<?, ?>> spawns = components.stream()
                    .filter(component -> "scripted_spawn".equals(component.get("type")))
                    .toList();
            assertEquals(2, spawns.size());
            assertEquals(3, spawns.stream().mapToInt(ShipwreckContentTest::count).sum());
            assertEquals(1, countFor(spawns, "custom:riptide_drowned"));
            assertEquals(2, countFor(spawns, "vanilla:drowned"));
            assertTrue(spawns.stream().allMatch(spawn -> spawn.get("count") instanceof Number));
            assertTrue(spawns.stream().allMatch(spawn -> Boolean.TRUE.equals(spawn.get("objective"))));
            assertTrue(spawns.stream().allMatch(spawn -> Boolean.TRUE.equals(spawn.get("safe-spawn"))));

            List<Map<?, ?>> rewards = components.stream()
                    .filter(component -> "reward_drop".equals(component.get("type")))
                    .toList();
            assertEquals(1, rewards.size());
            Map<?, ?> reward = rewards.getFirst();
            assertEquals("clear", reward.get("phase"));
            assertEquals("participants", reward.get("recipient"));
            assertEquals("minecraft:nautilus_shell", reward.get("reward-id"));
            assertEquals(1, ((Number) reward.get("amount")).intValue());
            assertTrue(rewards.stream().noneMatch(candidate -> "looter".equals(candidate.get("recipient"))));

            assertTrue(components.stream().noneMatch(component ->
                    String.valueOf(component.get("type")).toLowerCase().contains("chest")));
            assertNull(yaml.getConfigurationSection(shipwreck + ".chest"));
            assertEquals("minecraft:desert_pyramid",
                    yaml.getString("structures.desert_pyramid.minecraft-key"));
            assertFalse(yaml.isConfigurationSection("structures.desert_pyramid.variants.riptide_ambush_prototype"));
        } catch (Exception exception) {
            throw new AssertionError("Unable to read bundled Shipwreck config", exception);
        }
    }

    private static int countFor(List<Map<?, ?>> spawns, String mobId) {
        return spawns.stream()
                .filter(spawn -> mobId.equals(spawn.get("mob-id")))
                .mapToInt(ShipwreckContentTest::count)
                .sum();
    }

    private static int count(Map<?, ?> component) {
        return ((Number) component.get("count")).intValue();
    }
}
