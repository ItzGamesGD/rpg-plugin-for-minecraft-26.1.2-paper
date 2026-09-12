package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayBossConfigTest {
    @Test void bundledBossConfigurationContainsEveryRuntimeControl() {
        YamlConfiguration yaml = loadBosses();
        String root = "gateway-boss.";
        assertTrue(yaml.getDouble(root + "attack-range") >= 20.0D);
        assertTrue(yaml.getInt(root + "weapon-throw-telegraph-ticks") >= 10);
        assertTrue(yaml.getInt(root + "parry-telegraph-ticks") >= 15);
        assertTrue(yaml.getDouble(root + "parry-return-speed") < yaml.getDouble(root + "weapon-throw-speed"));
        assertTrue(yaml.getInt(root + "max-active-summons") >= yaml.getInt(root + "vex-summon-count"));
        assertNotNull(yaml.getConfigurationSection(root + "phase-health-thresholds"));
    }

    @Test void invalidValuesAreBoundedToReadableAndSafeLimits() {
        GatewayBossConfig config = GatewayBossConfig.bounded(1, 99, 9, 99, 9, 1,
                -1, -1, -1, 1, 9, 1, 99, 99, 9999, 9, 1, 1, 1);
        assertEquals(20.0D, config.attackRange());
        assertEquals(12.0D, config.orbitRadius());
        assertEquals(12, config.maxOrbitWeapons());
        assertEquals(10, config.weaponThrowTelegraphTicks());
        assertEquals(15, config.parryTelegraphTicks());
        assertEquals(16, config.maxActiveSummons());
        assertEquals(1200, config.summonLifetimeTicks());
    }

    private YamlConfiguration loadBosses() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("bosses.yml")) {
            if (stream == null) throw new AssertionError("Missing bosses.yml");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
