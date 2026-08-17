package com.hyunseo.hyunseorpg.balance;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the approved source defaults before they are migrated to a live server. */
final class RuntimeEconomyPolicyTest {
    @Test
    void activityCoinPolicyIsExplicit() {
        YamlConfiguration progression = load("progression-loop.yml");
        assertEquals(50, progression.getInt("activity-coins.activities.MINING.coins"));
        assertEquals(0, progression.getInt("activity-coins.activities.FARMING.coins"));
        assertEquals(0, progression.getInt("activity-coins.activities.HUNTING.coins"));
        assertFalse(progression.isSet("activity-coins.activities.BUILDING"));
    }

    @Test
    void allActiveEnchantBooksUseTheFixedMagicStonePrice() {
        YamlConfiguration shops = load("shops.yml");
        ConfigurationSection items = shops.getConfigurationSection("shops.enchant.items");
        assertNotNull(items);
        assertEquals(32, items.getKeys(false).size());
        for (String product : items.getKeys(false)) {
            String root = "shops.enchant.items." + product;
            assertTrue(shops.getString(root + ".item.id", "").startsWith("enchant_book_"));
            assertEquals(9, shops.getInt(root + ".buy-price"));
            assertEquals("magic_stone", shops.getString(root + ".currency-item-id"));
            assertTrue(shops.getBoolean(root + ".purchasable"));
            assertFalse(shops.getBoolean(root + ".sellable"));
        }
    }

    @Test
    void growthMaterialsAndSupportTicketsCannotBeBought() {
        YamlConfiguration shops = load("shops.yml");
        assertFalse(shops.getBoolean("shops.general.items.basic_upgrade_stone.purchasable"));
        assertFalse(shops.getBoolean("shops.general.items.basic_upgrade_stone.sellable"));
        assertFalse(shops.getBoolean("shops.equipment_support.items.enchant_extraction_ticket.purchasable"));
        assertFalse(shops.getBoolean("shops.equipment_support.items.promotion_option_reroll_ticket.purchasable"));
    }

    @Test
    void supportRecipesUseCanonicalCosts() {
        YamlConfiguration crafting = load("crafting.yml");
        assertEquals(4, crafting.getInt("crafting-recipes.enchant_extraction_ticket.inputs.magic_stone"));
        assertEquals(4, crafting.getInt("crafting-recipes.enchant_extraction_ticket.inputs.basic_promotion_stone"));
        assertEquals(2, crafting.getInt("crafting-recipes.promotion_option_reroll_ticket.inputs.magic_stone"));
        assertEquals(1, crafting.getInt("crafting-recipes.promotion_option_reroll_ticket.inputs.basic_promotion_stone"));
    }

    private YamlConfiguration load(String fileName) {
        Path path = Path.of(System.getProperty("user.dir"), "src", "main", "resources", fileName);
        return YamlConfiguration.loadConfiguration(path.toFile());
    }
}
