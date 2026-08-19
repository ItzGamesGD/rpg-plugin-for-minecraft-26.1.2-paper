package com.hyunseo.hyunseorpg.exploration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class ExplorationConfigDataTest {
    @Test
    void packageShipsDisabledAndBalancePending() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("exploration/structures.yml")) {
            assertNotNull(stream);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            assertFalse(yaml.getBoolean("enabled", true));
            assertEquals(0.0D, yaml.getDouble("structures.swamp_hut.selection-chance", 1.0D));
            assertTrue(yaml.getBoolean("structures.swamp_hut.balance-pending", false));
            assertTrue(yaml.getBoolean(
                    "structures.swamp_hut.variants.elite_witch_prototype.components.0.objective", false));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void everyRegisteredStructureHasAStableNamespacedKeyAndSafeChance() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("exploration/structures.yml")) {
            assertNotNull(stream);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            ConfigurationSection structures = yaml.getConfigurationSection("structures");
            assertNotNull(structures);

            Set<String> keys = new HashSet<>();
            for (String id : structures.getKeys(false)) {
                String key = structures.getString(id + ".minecraft-key", "");
                double chance = structures.getDouble(id + ".selection-chance", -1.0D);
                assertTrue(key.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"), id + " key is not namespaced");
                assertTrue(keys.add(key), "duplicate minecraft key: " + key);
                assertTrue(chance >= 0.0D && chance <= 1.0D, id + " chance is outside 0..1");
            }
            assertEquals(9, keys.size());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
