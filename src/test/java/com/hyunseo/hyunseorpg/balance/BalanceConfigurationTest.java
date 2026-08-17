package com.hyunseo.hyunseorpg.balance;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the non-final balance defaults and the canonical magic-stone recipe. */
final class BalanceConfigurationTest {
    @Test
    void xpCurveRemainsNeutralUntilPlaytestTimingExists() {
        YamlConfiguration exp = load("exp.yml");
        assertEquals(100, exp.getInt("base-level.max-level"));
        assertEquals(1.0D, exp.getDouble("base-level.required-exp-multiplier"), 0.000001D);
    }

    @Test
    void canonicalMagicStoneRecipeUsesNineFragments() {
        YamlConfiguration crafting = load("crafting.yml");
        assertEquals(9, crafting.getInt("crafting-recipes.magic_stone_from_fragments.inputs.magic_stone_fragment"));
        assertEquals("magic_stone", crafting.getString(
                "crafting-recipes.magic_stone_from_fragments.output.item-id"));
        assertEquals(1, crafting.getInt(
                "crafting-recipes.magic_stone_from_fragments.output.amount"));
        assertTrue(crafting.getStringList("crafting.categories.materials")
                .contains("magic_stone_from_fragments"));
        assertEquals(19, crafting.getInt("crafting.layout.materials.magic_stone_from_fragments"));
    }

    private YamlConfiguration load(String fileName) {
        Path path = Path.of(System.getProperty("user.dir"), "src", "main", "resources", fileName);
        return YamlConfiguration.loadConfiguration(path.toFile());
    }
}
