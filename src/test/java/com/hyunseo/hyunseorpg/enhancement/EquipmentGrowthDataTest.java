package com.hyunseo.hyunseorpg.enhancement;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the configured growth path for every supported vanilla equipment family. */
final class EquipmentGrowthDataTest {
    @Test
    void everyNormalVanillaEquipmentFamilyHasAnEnhancementPath() {
        YamlConfiguration growth = load();
        for (Material material : List.of(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD,
                Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.MACE,
                Material.BOW, Material.CROSSBOW, Material.TRIDENT,
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE,
                Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL,
                Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE,
                Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE,
                Material.DIAMOND_HOE, Material.NETHERITE_HOE,
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
                Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                Material.TURTLE_HELMET)) {
            assertTrue(findEnhancementProfile(growth, material) != null,
                    () -> material + " is missing an enhancement profile");
        }
    }



    @Test
    void stageTwoCapsAreCategoryBasedAndIndependentOfLegacyTierCeilings() {
        YamlConfiguration growth = load();
        assertEquals(40, growth.getInt("enhancement.max-level"));
        assertEquals(30, growth.getInt("enhancement.caps.vanilla"));
        assertEquals(40, growth.getInt("enhancement.caps.elemental"));
        assertFalse(growth.isConfigurationSection("tiers.definitions"));
    }









    private ConfigurationSection findEnhancementProfile(YamlConfiguration growth, Material material) {
        ConfigurationSection profiles = growth.getConfigurationSection("enhancement.profiles");
        assertNotNull(profiles);
        for (String profile : profiles.getKeys(false)) {
            if (growth.getStringList("enhancement.profiles." + profile + ".materials")
                    .stream().anyMatch(value -> material.name().equalsIgnoreCase(value))) {
                return profiles.getConfigurationSection(profile);
            }
        }
        return null;
    }

    private YamlConfiguration load() {
        Path path = Path.of(System.getProperty("user.dir"), "src", "main", "resources", "equipment-growth.yml");
        assertTrue(path.toFile().isFile(), "equipment-growth.yml");
        return YamlConfiguration.loadConfiguration(path.toFile());
    }
}
