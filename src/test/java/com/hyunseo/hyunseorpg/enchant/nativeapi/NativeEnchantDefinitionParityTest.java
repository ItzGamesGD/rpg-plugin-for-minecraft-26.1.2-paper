package com.hyunseo.hyunseorpg.enchant.nativeapi;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NativeEnchantDefinitionParityTest {
    @Test
    void activeYamlAndBootstrapDefinitionsHaveExactIdAndLevelParity() throws Exception {
        var resource = getClass().getClassLoader().getResourceAsStream("enchants.yml");
        assertNotNull(resource);
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
        var section = yaml.getConfigurationSection("enchants");
        assertNotNull(section);
        Map<String, Integer> active = new HashMap<>();
        Set<String> aliases = new HashSet<>();
        for (String id : section.getKeys(false)) {
            String path = "enchants." + id;
            if (!yaml.getBoolean(path + ".enabled", true)) continue;
            active.put(id, Math.max(1, yaml.getInt(path + ".max-level", 1)));
            for (String alias : yaml.getStringList(path + ".legacy-aliases")) {
                assertTrue(aliases.add(alias), "duplicate alias: " + alias);
                assertFalse(section.contains(alias), "alias shadows canonical YAML id: " + alias);
                assertFalse(NativeEnchantDefinitions.BY_ID.containsKey(alias), "alias shadows native id: " + alias);
            }
        }
        assertEquals(active.keySet(), NativeEnchantDefinitions.BY_ID.keySet(), "missing or orphan native definition");
        active.forEach((id, level) -> assertEquals(level,
                NativeEnchantDefinitions.BY_ID.get(id).maxLevel(), id + " max-level drift"));
    }
}
