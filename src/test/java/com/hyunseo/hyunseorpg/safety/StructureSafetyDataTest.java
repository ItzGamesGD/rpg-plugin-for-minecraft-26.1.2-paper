package com.hyunseo.hyunseorpg.safety;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression guards for the first economy safety migration. */
final class StructureSafetyDataTest {
    @Test
    void customCraftingAndLegacyRecipeSurfaceIsRetired() {
        Path crafting = Path.of(System.getProperty("user.dir"), "src", "main", "resources", "crafting.yml");
        assertFalse(Files.exists(crafting));
    }

    @Test
    void specialEquipmentUsesTheSingleGrowthPolicy() {
        YamlConfiguration special = load("special-equipment.yml");
        var items = special.getConfigurationSection("special-equipment.items");
        assertTrue(items != null && !items.getKeys(false).isEmpty());
        for (String id : items.getKeys(false)) {
            String root = "special-equipment.items." + id + ".growth";
            assertFalse(special.isSet(root + ".enhancement-enabled"), id);
            assertFalse(special.isSet(root + ".promotion-enabled"), id);
            assertTrue(special.getBoolean(root + ".unbreakable", false), id);
            assertFalse(special.isConfigurationSection("special-equipment.items." + id + ".promotion"), id);
        }
    }

    @Test
    void legacyCurrencyFragmentsAreAbsentFromMobDrops() {
        String mythic = read("mythic-mobs.yml");
        assertFalse(mythic.contains("item-id: upgrade_stone_fragment"));
        assertFalse(mythic.contains("item-id: basic_upgrade_fragment"));
    }

    private YamlConfiguration load(String name) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
            if (stream == null) throw new IllegalStateException("Missing test resource: " + name);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private String read(String name) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
            if (stream == null) throw new IllegalStateException("Missing test resource: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
