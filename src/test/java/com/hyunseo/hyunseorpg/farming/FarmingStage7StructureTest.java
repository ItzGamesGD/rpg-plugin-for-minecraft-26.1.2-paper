package com.hyunseo.hyunseorpg.farming;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FarmingStage7StructureTest {
    private static final List<String> CROPS = List.of("corn", "onion", "chili", "garlic");
    private static final List<String> QUALITIES = List.of("normal", "basic", "proficient", "advanced", "supreme");

    @Test
    void qualityDistributionIsOneHundredPercentOnlyForActiveQualityConfig() {
        YamlConfiguration quality = load("farming/quality.yml");
        assertEquals(100.0D, sum(quality.getConfigurationSection("base-distribution")), 0.000001D);

        YamlConfiguration promotion = load("farming/hoe_promotion.yml");
        assertFalse(promotion.isSet("farming-stage-mapping"));
        assertFalse(promotion.isSet("levels"));
    }

    @Test
    void hoeEnhancementContainsOnlyConfiguredDurabilityPerformance() {
        YamlConfiguration enhancement = load("farming/hoe_enhancement.yml");
        assertTrue(enhancement.isConfigurationSection("levels"));
        for (String level : enhancement.getConfigurationSection("levels").getKeys(false)) {
            assertTrue(enhancement.isSet("levels." + level + ".durability-save-chance"));
            assertTrue(enhancement.isSet("levels." + level + ".quality-sale-bonus"));
            assertFalse(enhancement.isSet("levels." + level + ".quality-density-shift"));
            assertFalse(enhancement.isSet("levels." + level + ".quality-score"));
            assertFalse(enhancement.isSet("levels." + level + ".abundance-point-multiplier"));
            assertFalse(enhancement.isSet("levels." + level + ".rare-seed-chance"));
        }
        assertTrue(enhancement.isSet("limits.maximum-quality-sale-bonus"));
    }

    @Test
    void everyCropQualityHasExactlyOneProcessingRecipeWithSameQuality() {
        YamlConfiguration crafting = load("crafting.yml");
        YamlConfiguration items = load("items.yml");
        Set<String> recipeIds = crafting.getConfigurationSection("crafting-recipes").getKeys(false);
        int count = 0;
        for (String crop : CROPS) {
            for (String quality : QUALITIES) {
                String recipe = "process_" + crop + "_" + quality;
                String path = "crafting-recipes." + recipe;
                assertTrue(recipeIds.contains(recipe), recipe);
                assertEquals("materials", crafting.getString(path + ".category"));
                assertTrue(crafting.isSet("crafting.layout.materials." + recipe),
                        "layout missing: " + recipe);
                String input = crafting.getConfigurationSection(path + ".inputs").getKeys(false).stream()
                        .findFirst().orElse("");
                String output = crafting.getString(path + ".output.item-id", "");
                if (quality.equals("normal")) {
                    assertEquals("crop_" + crop, input);
                } else {
                    assertTrue(input.endsWith("_quality_" + quality), input);
                }
                assertEquals(quality, suffix(output));
                assertEquals(20, crafting.getConfigurationSection(path + ".inputs").getInt(input));
                assertTrue(items.isConfigurationSection("items." + output), output);
                assertFalse(output.contains("cooking"));
                count++;
            }
        }
        assertEquals(20, count);
    }

    @Test
    void processedProductsAreSellableAndDoNotUseCookingTags() {
        YamlConfiguration shops = load("shops.yml");
        YamlConfiguration items = load("items.yml");
        for (String crop : CROPS) {
            for (String quality : QUALITIES) {
                String id = "processed_" + processedName(crop) + "_" + quality;
                assertTrue(shops.isConfigurationSection("shops.farming.items." + id), id);
                assertTrue(shops.getBoolean("shops.farming.items." + id + ".sellable"));
                assertTrue(items.getStringList("items." + id + ".tags").contains("farming-processed"));
                assertFalse(items.getStringList("items." + id + ".tags").stream()
                        .anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains("cooking")));
            }
        }
    }

    @Test
    void farmingSeedsArePurchasableOnlyThroughTheGatedFarmingShop() {
        YamlConfiguration shops = load("shops.yml");
        for (String crop : CROPS) {
            String path = "shops.farming.items.seed_" + crop;
            assertTrue(shops.isConfigurationSection(path), path);
            assertTrue(shops.getBoolean(path + ".purchasable"));
            assertEquals(crop, shops.getString(path + ".required-farming-crop"));
            assertFalse(shops.getBoolean(path + ".sellable"));
        }
    }

    @Test
    void processingContractKeepsCraftingYamlAsTheSingleExecutionSource() {
        YamlConfiguration processing = load("farming/processing.yml");
        assertEquals("crafting.yml", processing.getString("authoritative-source"));
        assertEquals("processing", processing.getString("recipe-category"));
    }

    @Test
    void hoePromotionContainsFixedItemLocalPassivesAndNoPlayerStageMapping() {
        YamlConfiguration promotion = load("farming/hoe_promotion.yml");
        assertFalse(promotion.isSet("farming-stage-mapping"));
        assertFalse(promotion.isSet("levels"));
        assertTrue(promotion.isConfigurationSection("limits"));
        ConfigurationSection tiers = promotion.getConfigurationSection("tiers");
        assertNotNull(tiers);
        assertTrue(tiers.getKeys(false).size() >= 2);
        for (String tier : tiers.getKeys(false)) {
            ConfigurationSection stars = tiers.getConfigurationSection(tier);
            assertNotNull(stars);
            for (String star : stars.getKeys(false)) {
                String path = "tiers." + tier + "." + star;
                assertTrue(promotion.isSet(path + ".quality-density-shift"));
                assertTrue(promotion.isSet(path + ".abundance-point-multiplier"));
                assertTrue(promotion.isSet(path + ".rare-seed-chance"));
            }
        }
    }

    @Test
    void craftingMenuCategoriesAndProcessingLayoutsAreDataDriven() {
        YamlConfiguration crafting = load("crafting.yml");
        ConfigurationSection categories = crafting.getConfigurationSection("crafting.menu-categories");
        ConfigurationSection layouts = crafting.getConfigurationSection("crafting.layout");
        assertNotNull(categories);
        assertNotNull(layouts);

        for (String category : categories.getKeys(false)) {
            assertTrue(categories.isSet(category + ".display-name"), category);
            assertTrue(categories.isSet(category + ".icon"), category);
            assertTrue(categories.isSet(category + ".slot"), category);
            assertTrue(layouts.isConfigurationSection(category), category);
        }

        ConfigurationSection materials = layouts.getConfigurationSection("materials");
        assertNotNull(materials);
        for (String crop : CROPS) {
            for (String quality : QUALITIES) {
                assertTrue(materials.isSet("process_" + crop + "_" + quality),
                        "processing recipe is not reachable from materials layout");
            }
        }
    }

    private double sum(ConfigurationSection section) {
        assertNotNull(section);
        return section.getKeys(false).stream().mapToDouble(section::getDouble).sum();
    }

    private String processedName(String crop) {
        return switch (crop) {
            case "corn" -> "corn_starch";
            case "onion" -> "onion_concentrate";
            case "chili" -> "chili_extract";
            case "garlic" -> "garlic_concentrate";
            default -> throw new IllegalArgumentException(crop);
        };
    }

    private String suffix(String id) {
        int index = id.lastIndexOf('_');
        return index < 0 ? "" : id.substring(index + 1);
    }

    private YamlConfiguration load(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
