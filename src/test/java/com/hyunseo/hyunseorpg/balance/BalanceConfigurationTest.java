package com.hyunseo.hyunseorpg.balance;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Guards the non-final balance defaults after the custom crafting surface is retired. */
final class BalanceConfigurationTest {
    @Test
    void xpCurveRemainsNeutralUntilPlaytestTimingExists() {
        YamlConfiguration exp = load("exp.yml");
        assertEquals(100, exp.getInt("base-level.max-level"));
        assertEquals(1.0D, exp.getDouble("base-level.required-exp-multiplier"), 0.000001D);
    }

    @Test
    void customCraftingConfigurationIsRetired() {
        Path crafting = Path.of(System.getProperty("user.dir"), "src", "main", "resources", "crafting.yml");
        assertFalse(java.nio.file.Files.exists(crafting));
    }

    private YamlConfiguration load(String fileName) {
        Path path = Path.of(System.getProperty("user.dir"), "src", "main", "resources", fileName);
        return YamlConfiguration.loadConfiguration(path.toFile());
    }
}
