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
    void upgradeStoneRecipeUsesMinecraftResources() {
        YamlConfiguration crafting = load("crafting.yml");
        assertEquals(1, crafting.getInt("crafting-recipes.basic_upgrade_stone.inputs.vanilla:DIAMOND"));
        assertEquals(4, crafting.getInt("crafting-recipes.basic_upgrade_stone.inputs.vanilla:REDSTONE"));
        assertEquals(4, crafting.getInt("crafting-recipes.basic_upgrade_stone.inputs.vanilla:LAPIS_LAZULI"));
        assertEquals("basic_upgrade_stone", crafting.getString("crafting-recipes.basic_upgrade_stone.output.item-id"));
    }

    private YamlConfiguration load(String fileName) {
        Path path = Path.of(System.getProperty("user.dir"), "src", "main", "resources", fileName);
        return YamlConfiguration.loadConfiguration(path.toFile());
    }
}
