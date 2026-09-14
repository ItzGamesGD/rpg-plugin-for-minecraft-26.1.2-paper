package com.hyunseo.hyunseorpg.enhancement;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VanillaAnvilEnhancementPolicyTest {
    @Test
    void classificationAllowsVanillaAndElementalButRejectsDedicatedAndInvalidEquipment() {
        assertEquals(EnhancementClass.VANILLA,
                EnhancementClassificationService.classify(true, false, false, false));
        // Enchantment presence is intentionally absent from classification: enchanted vanilla is still vanilla.
        assertEquals(EnhancementClass.VANILLA,
                EnhancementClassificationService.classify(true, false, false, false));
        assertEquals(EnhancementClass.ELEMENTAL,
                EnhancementClassificationService.classify(true, false, true, true));
        assertEquals(EnhancementClass.SPECIAL,
                EnhancementClassificationService.classify(true, false, true, false));
        assertEquals(EnhancementClass.ENDGAME,
                EnhancementClassificationService.classify(true, true, true, true));
        assertEquals(EnhancementClass.UNSUPPORTED,
                EnhancementClassificationService.classify(false, false, false, false));
    }

    @Test
    void categoryCapsRejectOverCapTransitions() {
        assertEquals(30, EnhancementClass.VANILLA.defaultCap());
        assertEquals(40, EnhancementClass.ELEMENTAL.defaultCap());
        assertEquals(30, EnhancementCapPolicy.nextLevel(29, 30).orElseThrow());
        assertTrue(EnhancementCapPolicy.nextLevel(30, 30).isEmpty());
        assertEquals(40, EnhancementCapPolicy.nextLevel(39, 40).orElseThrow());
        assertTrue(EnhancementCapPolicy.nextLevel(40, 40).isEmpty());
        assertTrue(EnhancementCapPolicy.nextLevel(0, 0).isEmpty());
    }

    @Test
    void capsAndCostsUseSmallVanillaLevelDeductions() {
        var config = load();
        assertEquals(30, config.getInt("enhancement.caps.vanilla"));
        assertEquals(40, config.getInt("enhancement.caps.elemental"));
        assertEquals(1, config.getInt("enhancement.xp-level-cost.default"));
        assertEquals(1, config.getInt("enhancement.xp-level-cost.levels.1"));
        assertEquals(2, config.getInt("enhancement.xp-level-cost.levels.6"));
        assertEquals(5, config.getInt("enhancement.xp-level-cost.levels.31"));
        assertFalse(config.contains("enhancement.initial-coin-cost"));
        assertFalse(config.contains("enhancement.coin-cost-curve"));
        assertFalse(config.contains("enhancement.fail-bonus"));
        assertFalse(config.contains("enhancement.start-chance"));
    }

    @Test
    void elementalBoundaryIsExplicitAndExcludesDedicatedWeapons() {
        var config = load();
        var elemental = config.getStringList("enhancement.elemental-item-ids");
        assertTrue(elemental.containsAll(java.util.List.of("burning_sword", "flowing_water_sword",
                "wind_cutting_sword", "earth_special_sword", "ice_special_sword", "dark_energy_sword",
                "burning_bow", "wind_archers_bow", "earth_heavy_bow", "freezing_bow", "dark_energy_bow")));
        assertFalse(elemental.contains("thanatos_mace"));
        assertFalse(elemental.contains("solaris"));
        assertFalse(elemental.contains("thunder_gods_axe"));
        assertFalse(elemental.contains("poseidon_spear"));
        assertFalse(elemental.contains("moonlit_afterglow"));
    }

    @Test
    void physicalStoneCostAndSuccessAreFixed() {
        assertEquals(1, VanillaAnvilEnhancementListener.STONE_COST);
        assertTrue(load().getString("enhancement.required-stone-item-id", "").equals("basic_upgrade_stone"));
    }

    private YamlConfiguration load() {
        return YamlConfiguration.loadConfiguration(Path.of("src/main/resources/equipment-growth.yml").toFile());
    }
}
