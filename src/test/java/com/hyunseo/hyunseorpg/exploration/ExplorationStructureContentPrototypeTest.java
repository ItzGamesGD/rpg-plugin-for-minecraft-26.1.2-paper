package com.hyunseo.hyunseorpg.exploration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class ExplorationStructureContentPrototypeTest {

    @Test
    void outpostScoutWaveIsDefinedButSafeByDefault() {
        InputStream resource = getClass().getClassLoader()
                .getResourceAsStream("exploration/structures.yml");

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(resource)
        );

        assertFalse(yaml.getBoolean("enabled", true));
        assertFalse(yaml.getBoolean("structures.pillager_outpost.enabled", true));
        assertEquals(0.0D, yaml.getDouble("structures.pillager_outpost.selection-chance"), 0.000001D);

        String variant = "structures.pillager_outpost.variants.scout_wave_prototype";
        assertTrue(yaml.getBoolean(variant + ".enabled", false));
        assertTrue(yaml.getBoolean(variant + ".prototype", false));
        assertEquals(1.0D, yaml.getDouble(variant + ".weight"), 0.000001D);

        String component = variant + ".components.0";
        assertEquals("scripted_spawn", yaml.getString(component + ".type"));
        assertEquals("vanilla:pillager", yaml.getString(component + ".mob-id"));
        assertEquals(2, yaml.getInt(component + ".count"));
        assertTrue(yaml.getBoolean(component + ".objective", false));
        assertEquals(0, yaml.getInt(component + ".dx"));
        assertEquals(1, yaml.getInt(component + ".dy"));
        assertEquals(0, yaml.getInt(component + ".dz"));
    }
}
