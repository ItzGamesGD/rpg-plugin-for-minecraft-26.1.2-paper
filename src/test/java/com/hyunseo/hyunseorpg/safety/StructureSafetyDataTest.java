package com.hyunseo.hyunseorpg.safety;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression guards for the first economy safety migration. */
final class StructureSafetyDataTest {
    private static final Set<String> RETIRED_RECIPES = Set.of(
            "fire_sword", "fire_bow", "water_sword", "wind_sword", "wind_bow",
            "earth_sword", "earth_bow", "earth_mace", "ice_sword", "ice_bow",
            "magic_sword", "magic_bow");

    @Test
    void buildingRewardsAndLegacyRecipesAreNotActive() {
        YamlConfiguration progression = load("progression-loop.yml");
        YamlConfiguration crafting = load("crafting.yml");

        assertFalse(progression.isConfigurationSection("activity-coins.activities.BUILDING"));
        assertFalse(crafting.getBoolean("craft2.enabled", false));
        for (String recipe : RETIRED_RECIPES) {
            assertFalse(crafting.isConfigurationSection("crafting-recipes." + recipe), recipe);
            assertFalse(crafting.getStringList("crafting.categories.equipment").contains(recipe), recipe);
        }
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
    void canonicalFragmentIsUsedByMobDrops() {
        String mythic = read("mythic-mobs.yml");
        assertFalse(mythic.contains("item-id: upgrade_stone_fragment"));
        assertTrue(mythic.contains("item-id: basic_upgrade_fragment"));
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
