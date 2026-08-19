package com.hyunseo.hyunseorpg.exploration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

final class ExplorationConfigDataTest {
    @Test
    void packageShipsDisabledAndBalancePending() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("exploration/structures.yml")) {
            assertNotNull(stream);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            assertFalse(yaml.getBoolean("enabled", true));
            assertEquals(0.0D, yaml.getDouble("structures.swamp_hut.selection-chance", 1.0D));
            assertTrue(yaml.getBoolean("structures.swamp_hut.balance-pending", false));
            assertTrue(yaml.getBoolean("structures.swamp_hut.variants.elite_witch_prototype.components.0.objective", false));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
