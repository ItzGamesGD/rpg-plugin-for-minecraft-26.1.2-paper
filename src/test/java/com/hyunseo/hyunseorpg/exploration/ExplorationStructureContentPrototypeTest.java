package com.hyunseo.hyunseorpg.exploration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
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

        List<Map<?, ?>> components = yaml.getMapList(variant + ".components");
        assertEquals(1, components.size());
        Map<?, ?> component = components.get(0);
        assertEquals("scripted_spawn", component.get("type"));
        assertEquals("vanilla:pillager", component.get("mob-id"));
        assertEquals(2, ((Number) component.get("count")).intValue());
        assertEquals(Boolean.TRUE, component.get("objective"));
        assertEquals(0, ((Number) component.get("dx")).intValue());
        assertEquals(1, ((Number) component.get("dy")).intValue());
        assertEquals(0, ((Number) component.get("dz")).intValue());
    }
}
