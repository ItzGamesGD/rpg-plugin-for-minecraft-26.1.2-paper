package com.hyunseo.hyunseorpg.exploration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class ExplorationConfigDataTest {
    @Test
    void packageShipsActivatesPyramidWhileKeepingOtherContentSafe() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("exploration/structures.yml")) {
            assertNotNull(stream);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            assertTrue(yaml.getBoolean("enabled", false));
            assertTrue(yaml.getBoolean("structures.desert_pyramid.enabled", false));
            assertEquals(1.0D, yaml.getDouble("structures.desert_pyramid.selection-chance", 0.0D));
            assertEquals(0.0D, yaml.getDouble("structures.swamp_hut.selection-chance", 1.0D));
            assertTrue(yaml.getBoolean("structures.swamp_hut.balance-pending", false));

            List<Map<?, ?>> components = yaml.getMapList(
                    "structures.swamp_hut.variants.elite_witch_prototype.components");
            assertEquals(1, components.size());
            assertEquals(Boolean.TRUE, components.get(0).get("objective"));
            assertEquals("custom:mire_shaman", components.get(0).get("mob-id"));
            assertEquals("WITCH", components.get(0).get("cleanup-unmanaged-type"));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void pyramidPillarComponentUsesPillarOnlyRecoveryPhase() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("exploration/structures.yml")) {
            assertNotNull(stream);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            List<Map<?, ?>> components = yaml.getMapList(
                    "structures.desert_pyramid.variants.guardian_trial.components");
            Map<?, ?> pillars = components.stream()
                    .filter(component -> "pyramid_push_pillars".equals(component.get("type")))
                    .findFirst().orElseThrow();
            assertEquals("pyramid_pillar_restore", pillars.get("phase"));
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

    @Test
    void outpostRaidPoolsHaveExplicitBoundedHeavyRules() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("exploration/structures.yml")) {
            assertNotNull(stream);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            assertEquals(3, yaml.getInt("raid-pools.outpost_t1_wave_1.total-max-spawns"));
            assertEquals(4, yaml.getInt("raid-pools.outpost_t1_wave_2.total-max-spawns"));
            assertEquals(4, yaml.getInt("raid-pools.outpost_t2_wave_1.total-max-spawns"));
            assertEquals(5, yaml.getInt("raid-pools.outpost_t2_wave_3.total-max-spawns"));
            assertEquals(1, yaml.getInt("raid-pools.outpost_t2_wave_3.guaranteed.golden_bulwark"));
            assertEquals(5, yaml.getInt("raid-pools.outpost_t3_wave_1.total-max-spawns"));
            assertEquals(6, yaml.getInt("raid-pools.outpost_t3_wave_4.total-max-spawns"));
            assertEquals(2, yaml.getInt("raid-pools.outpost_t3_wave_4.heavy-max-spawns"));
            assertEquals(1, yaml.getInt("raid-pools.outpost_t3_wave_4.guaranteed.golden_bulwark"));
            assertEquals(1, yaml.getInt("raid-pools.outpost_t3_wave_4.mobs.ravager_rider.max"));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
