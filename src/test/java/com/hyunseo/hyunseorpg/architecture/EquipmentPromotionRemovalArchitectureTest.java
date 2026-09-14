package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EquipmentPromotionRemovalArchitectureTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void equipmentPromotionRuntimeTypesAreRemovedAndNotBootstrapped() throws IOException {
        Path enhancement = ROOT.resolve("src/main/java/com/hyunseo/hyunseorpg/enhancement");
        for (String removed : List.of("EquipmentPromotionService.java", "OptionRollService.java", "PromotionInventoryHolder.java")) {
            assertFalse(Files.exists(enhancement.resolve(removed)), removed);
        }
        String plugin = Files.readString(ROOT.resolve("src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java"));
        assertFalse(plugin.contains("EquipmentPromotionService"));
        assertFalse(plugin.contains("PromotionInventoryHolder"));
    }

    @Test
    void promotionItemsRecipesAndEquipmentConfigAreRetired() throws IOException {
        String growth = resource("equipment-growth.yml");
        String items = resource("items.yml");
        String crafting = resource("crafting.yml");
        String support = resource("equipment-support.yml");
        assertFalse(growth.contains("\npromotion:"));
        assertFalse(growth.contains("max-promotion-stage"));
        assertFalse(items.contains("basic_promotion_stone:"));
        assertFalse(items.contains("promotion_option_reroll_ticket:"));
        assertFalse(crafting.contains("basic_promotion_stone"));
        assertFalse(crafting.contains("promotion_option_reroll_ticket"));
        assertFalse(support.contains("promotion-option-reroll:"));
    }

    @Test
    void enhancementAndNativeEnchantFoundationsRemainPresent() throws IOException {
        String growth = resource("equipment-growth.yml");
        assertTrue(growth.contains("vanilla: 30"));
        assertTrue(growth.contains("elemental: 40"));
        assertTrue(growth.contains("required-stone-item-id: basic_upgrade_stone"));
        assertTrue(Files.exists(ROOT.resolve("src/main/resources/datapack/pack.mcmeta")));
        assertTrue(Files.exists(ROOT.resolve("src/main/java/com/hyunseo/hyunseorpg/enchant/nativeapi/HyunseoRPGPluginBootstrap.java")));
    }

    private String resource(String name) throws IOException {
        return Files.readString(ROOT.resolve("src/main/resources").resolve(name));
    }
}
