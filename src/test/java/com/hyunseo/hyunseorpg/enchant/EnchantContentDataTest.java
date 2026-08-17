package com.hyunseo.hyunseorpg.enchant;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EnchantContentDataTest {
    @Test
    void everyEnchantDefinitionReferencesAnExistingCustomBook() {
        YamlConfiguration enchants = load("enchants.yml");
        YamlConfiguration items = load("items.yml");
        ConfigurationSection definitions = enchants.getConfigurationSection("enchants");
        ConfigurationSection itemDefinitions = items.getConfigurationSection("items");

        assertNotNull(definitions);
        assertNotNull(itemDefinitions);
        assertFalse(definitions.getKeys(false).isEmpty());

        for (String enchantId : definitions.getKeys(false)) {
            ConfigurationSection definition = definitions.getConfigurationSection(enchantId);
            assertNotNull(definition, enchantId);
            assertFalse(definition.getString("display-name", "").isBlank(), enchantId);
            assertTrue(definition.getInt("max-level", 1) >= 1, enchantId);

            String bookId = definition.getString("book-item-id", "");
            assertFalse(bookId.isBlank(), enchantId);
            assertTrue(itemDefinitions.contains(bookId), () -> enchantId + " references missing book " + bookId);

            if ("content".equalsIgnoreCase(definition.getString("handler-id", ""))) {
                assertNotNull(definition.getConfigurationSection("settings"), enchantId + " needs YAML-owned settings");
            }
        }
    }

    @Test
    void retiredEnchantDefinitionsAreNotRegistered() {
        YamlConfiguration enchants = load("enchants.yml");
        for (String retired : List.of(
                "spear_charge",
                "spear_throw",
                "lancer",
                "paladins_blessing",
                "holy_counter",
                "homing_arrow",
                "hunters_mark")) {
            assertFalse(enchants.contains("enchants." + retired), retired);
            assertFalse(enchants.contains("enchant-lore." + retired), retired);
        }
    }

    @Test
    void canonicalEnchantCatalogueContainsRecoveredDefinitions() {
        YamlConfiguration enchants = load("enchants.yml");
        YamlConfiguration items = load("items.yml");
        assertEquals(5, enchants.getInt("config-version"));
        for (String id : List.of("fire_arrow_rain", "unbreaking", "mining_bonus_drop",
                "area_excavation", "chain_logging")) {
            assertTrue(enchants.isConfigurationSection("enchants." + id), id);
        }
        for (String id : List.of("enchant_book_fire_arrow_rain", "enchant_book_mining_bonus_drop",
                "enchant_book_area_mining_pickaxe", "enchant_book_chain_logging")) {
            assertTrue(items.isConfigurationSection("items." + id), id);
        }
        assertFalse(enchants.isConfigurationSection("enchants.blade_throw"));
        assertFalse(enchants.isConfigurationSection("enchants.area_mining_pickaxe"));
        assertFalse(enchants.isConfigurationSection("enchants.durability_save_pickaxe"));
        assertEquals("ELYTRA_BOOST", String.valueOf(
                enchants.getMapList("enchants.precision_flight.triggers").getFirst().get("type")));
        assertEquals(100, enchants.getInt("enchants.crossbow_barrage.settings.maximum-lifetime-ticks"));
    }

    @Test
    void everyEnchantHasCurrentCentralLore() {
        YamlConfiguration enchants = load("enchants.yml");
        ConfigurationSection definitions = enchants.getConfigurationSection("enchants");
        assertNotNull(definitions);
        assertEquals(3, enchants.getInt("enchant-lore-version"));
        for (String enchantId : definitions.getKeys(false)) {
            List<String> lines = enchants.getStringList("enchant-lore." + enchantId + ".description");
            assertFalse(lines.isEmpty(), enchantId + " is missing central lore");
            assertTrue(lines.stream().noneMatch(line -> line.contains("?")), enchantId + " has legacy broken lore");
        }
    }

    @Test
    void legacyItemsFixtureReceivesEveryReferencedEnchantBookAndCanBeReloaded() throws Exception {
        YamlConfiguration embeddedItems = load("items.yml");
        YamlConfiguration embeddedEnchants = load("enchants.yml");
        YamlConfiguration legacyItems = new YamlConfiguration();
        legacyItems.createSection("items");
        for (String id : embeddedItems.getConfigurationSection("items").getKeys(false)) {
            if (Set.of("enchant_book_mining_bonus_drop", "enchant_book_area_mining_pickaxe",
                    "enchant_book_chain_logging").contains(id)) continue;
            legacyItems.set("items." + id, embeddedItems.get("items." + id));
        }
        var migration = Class.forName("com.hyunseo.hyunseorpg.core.config.ConfigMigrationService")
                .getDeclaredMethod("migrateRequiredEnchantBooks", org.bukkit.configuration.file.FileConfiguration.class,
                        org.bukkit.configuration.file.FileConfiguration.class,
                        org.bukkit.configuration.file.FileConfiguration.class,
                        org.bukkit.configuration.file.FileConfiguration.class, List.class);
        migration.setAccessible(true);
        boolean changed = (boolean) migration.invoke(null, legacyItems, embeddedItems, embeddedEnchants,
                embeddedEnchants, new ArrayList<String>());
        assertTrue(changed);
        Path temp = Files.createTempFile("hyunseorpg-items-migration-", ".yml");
        legacyItems.save(temp.toFile());
        YamlConfiguration reloaded = YamlConfiguration.loadConfiguration(temp.toFile());
        for (String id : List.of("enchant_book_mining_bonus_drop", "enchant_book_area_mining_pickaxe",
                "enchant_book_chain_logging")) {
            assertTrue(reloaded.isConfigurationSection("items." + id), id);
            assertFalse(reloaded.getString("items." + id + ".material", "").isBlank(), id);
            assertFalse(reloaded.getString("items." + id + ".display-name", "").isBlank(), id);
            assertFalse(reloaded.getString("items." + id + ".category", "").isBlank(), id);
            assertTrue(reloaded.getInt("items." + id + ".custom-model-data", 0) > 0, id);
            assertFalse(reloaded.getStringList("items." + id + ".lore").isEmpty(), id);
        }

        var legacyClassifier = Class.forName("com.hyunseo.hyunseorpg.core.config.ConfigMigrationService")
                .getDeclaredMethod("isLegacyProfessionItem", ConfigurationSection.class, String.class);
        legacyClassifier.setAccessible(true);
        for (String id : List.of("enchant_book_mining_bonus_drop", "enchant_book_area_mining_pickaxe",
                "enchant_book_chain_logging")) {
            assertFalse((boolean) legacyClassifier.invoke(null,
                    reloaded.getConfigurationSection("items." + id), id),
                    id + " must not be removed as a legacy profession item");
        }
        Files.deleteIfExists(temp);
    }

    @Test
    void secondRuntimeRepairSettingsAreCanonical() {
        YamlConfiguration enchants = load("enchants.yml");
        assertEquals(.95D, enchants.getDouble("enchants.wind_arrow.settings.minimum-force"), .0001D);
        assertFalse(enchants.isSet("enchants.wind_arrow.settings.required-charge-ticks"));
        assertEquals(20, enchants.getInt("enchants.wind_arrow.settings.maximum-charge-ticks"));
        assertTrue(enchants.getInt("enchants.precision_flight.settings.durability-consume-interval-ticks") > 0);
        assertTrue(enchants.getInt("enchants.precision_flight.settings.recovery-interval-ticks-level-1") > 0);
        assertEquals(15, enchants.getInt("enchants.explosive_mace.settings.chain-delay-ticks"));
        assertEquals(20, enchants.getInt("enchants.explosive_mace.settings.maximum-centers"));
    }

    @Test
    void gatheringPromotionPoolsExcludeCombatOnlyOptions() {
        YamlConfiguration growth = load("equipment-growth.yml");
        for (String profile : List.of("pickaxe", "shovel", "hoe", "axe")) {
            Set<String> options = new LinkedHashSet<>();
            options.addAll(growth.getStringList("promotion.profiles." + profile + ".general-option-pool"));
            options.addAll(growth.getStringList("promotion.profiles." + profile + ".special-option-pool"));
            assertFalse(options.contains("skill-damage"), profile);
            assertFalse(options.contains("weapon-skill-damage"), profile);
            assertFalse(options.contains("bleed-chance"), profile);
        }
    }

    private YamlConfiguration load(String resource) {
        Path path = Path.of(System.getProperty("user.dir"), "src", "main", "resources", resource);
        assertTrue(path.toFile().isFile(), resource);
        return YamlConfiguration.loadConfiguration(path.toFile());
    }
}
