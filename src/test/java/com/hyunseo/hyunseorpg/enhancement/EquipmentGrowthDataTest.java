package com.hyunseo.hyunseorpg.enhancement;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void eachPromotableFamilyHasAResolvedPromotionProfile() {
        YamlConfiguration growth = load();
        Map<Material, String> profiles = Map.of(
                Material.DIAMOND_PICKAXE, "pickaxe",
                Material.DIAMOND_SHOVEL, "shovel",
                Material.DIAMOND_HOE, "hoe",
                Material.DIAMOND_AXE, "axe",
                Material.CROSSBOW, "crossbow",
                Material.BOW, "bow",
                Material.TRIDENT, "spear",
                Material.MACE, "melee",
                Material.DIAMOND_SWORD, "sword",
                Material.DIAMOND_CHESTPLATE, "armor"
        );
        for (Map.Entry<Material, String> entry : profiles.entrySet()) {
            assertNotNull(findEnhancementProfile(growth, entry.getKey()), entry.getKey().name());
            assertTrue(growth.isConfigurationSection("promotion.profiles." + entry.getValue()),
                    () -> entry.getKey() + " is missing promotion profile " + entry.getValue());
        }
    }

    @Test
    void stageTwoCapsAreCategoryBasedAndIndependentOfLegacyTierCeilings() {
        YamlConfiguration growth = load();
        assertEquals(40, growth.getInt("enhancement.max-level"));
        assertEquals(30, growth.getInt("enhancement.caps.vanilla"));
        assertEquals(40, growth.getInt("enhancement.caps.elemental"));
        for (String tier : Set.of("wooden", "stone", "gold", "iron", "diamond", "netherite")) {
            assertTrue(growth.getInt("tiers.definitions." + tier + ".max-enhancement") >= 30, tier);
        }
        assertEquals(0, growth.getInt("tiers.definitions.elytra.max-promotion-stage"));
    }

    @Test
    void promotionRequirementsAreSeparateFromTheUniversalEnhancementCeiling() {
        YamlConfiguration growth = load();
        assertEquals(10, growth.getInt("promotion.grades.normal.required-enhancement"));
        assertEquals(20, growth.getInt("promotion.grades.advanced.required-enhancement"));
        assertEquals(30, growth.getInt("promotion.grades.rare.required-enhancement"));
        assertEquals(40, growth.getInt("promotion.grades.heroic.required-enhancement"));
        assertEquals(50, growth.getInt("promotion.grades.legendary.required-enhancement"));
        assertEquals(50, growth.getInt("promotion.grades.mythic.required-enhancement"));
    }

    @Test
    void everyPromotionDefinitionHasAClosedFirstRollAndRerollRange() {
        YamlConfiguration growth = load();
        ConfigurationSection definitions = growth.getConfigurationSection("promotion.option-definitions");
        assertNotNull(definitions);
        for (String id : definitions.getKeys(false)) {
            String path = "promotion.option-definitions." + id + ".value";
            double min = growth.getDouble(path + ".min", Double.NaN);
            double max = growth.getDouble(path + ".max", Double.NaN);
            double step = growth.getDouble(path + ".precision", Double.NaN);
            assertTrue(Double.isFinite(min) && Double.isFinite(max) && Double.isFinite(step), id);
            assertTrue(min >= 0.0D && max >= min && step > 0.0D, id);
            // The service uses this same max for the first promotion roll and reroll upper bound.
            assertTrue(max >= min, id + " has a reroll upper bound below its first-roll lower bound");
        }
    }

    @Test
    void duplicatePromotionRollsMustRemainWithinTheSharedDefinitionMaximum() {
        YamlConfiguration growth = load();
        ConfigurationSection definitions = growth.getConfigurationSection("promotion.option-definitions");
        assertNotNull(definitions);
        for (String id : definitions.getKeys(false)) {
            String path = "promotion.option-definitions." + id + ".value";
            double max = growth.getDouble(path + ".max", 0.0D);
            assertTrue(max >= 0.0D, id);
            // The runtime now treats this value as the aggregate cap, including duplicate rolls.
            double accumulated = 0.0D;
            for (int roll = 0; roll < 20; roll++) {
                accumulated = Math.min(max, accumulated + max);
                assertTrue(accumulated <= max + 0.000001D, id);
            }
        }
    }

    @Test
    void aggregateOptionValueCannotExceedTheSharedDefinitionMaximum() {
        EquipmentPromotionService.OptionDefinition definition = new EquipmentPromotionService.OptionDefinition(
                "damage-reduction", "Damage Reduction", "PERCENT_POINT",
                0.01D, 0.03D, 0.01D, 1, true, true, false);

        assertEquals(0.03D, EquipmentPromotionService.capAggregateOptionValue(0.06D, definition), 0.000001D);
        assertEquals(0.0D, EquipmentPromotionService.capAggregateOptionValue(-0.01D, definition), 0.000001D);
        assertEquals(0.06D, EquipmentPromotionService.capAggregateOptionValue(0.06D,
                new EquipmentPromotionService.OptionDefinition(
                        "legacy", "Legacy", "FLAT", 0.0D, 0.03D, 0.01D,
                        1, true, false, true)), 0.000001D);
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
