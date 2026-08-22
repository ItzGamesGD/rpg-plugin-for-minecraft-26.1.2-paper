package com.hyunseo.hyunseorpg.mob;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OutpostRaiderDefinitionTest {
    @Test
    void firstOutpostRosterUsesVanillaPillagerFamilyAndExplicitSafetyPolicies() {
        YamlConfiguration mobs = loadMobs();
        List<String> ids = List.of("shield_raider", "crossbow_raider", "charger_raider",
                "banner_raider", "spike_evoker", "ravager_rider");
        for (String id : ids) {
            String path = "custom-mobs." + id;
            assertTrue(mobs.isConfigurationSection(path), "missing outpost mob: " + id);
            assertFalse(mobs.getString(path + ".drop-table", "missing").equals("missing"));
        }
        assertEquals("VINDICATOR", mobs.getString("custom-mobs.shield_raider.vanilla-type"));
        assertEquals("PILLAGER", mobs.getString("custom-mobs.crossbow_raider.vanilla-type"));
        assertEquals("EVOKER", mobs.getString("custom-mobs.spike_evoker.vanilla-type"));
        assertFalse(mobs.getBoolean("custom-mobs.spike_evoker.behavior.vex-summon", true));
        assertFalse(mobs.getBoolean("custom-mobs.spike_evoker.behavior.totem-drop", true));
        assertEquals("RAVAGER", mobs.getString("custom-mobs.ravager_rider.vanilla-type"));
    }

    private YamlConfiguration loadMobs() {
        var stream = getClass().getClassLoader().getResourceAsStream("mobs.yml");
        assertNotNull(stream);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
