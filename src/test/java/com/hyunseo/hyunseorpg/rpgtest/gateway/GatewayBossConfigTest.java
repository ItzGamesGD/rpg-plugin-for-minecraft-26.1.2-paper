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
        assertTrue(yaml.getDouble(root + "orbit.ring-plane-speed") > 0.0D);
        assertTrue(yaml.getInt(root + "basic.detach-telegraph-ticks") >= 10);
        assertTrue(yaml.getDouble(root + "basic.weapon-throw-speed") < 1.0D);
        assertTrue(yaml.getDouble(root + "gateway.phase-spacing") >= 8.0D);
        assertTrue(yaml.getDouble(root + "payload.speed-multiplier") > 0.0D);
        assertTrue(yaml.getInt(root + "payload.local-caps.end-crystal-bomb") > 0);
        assertTrue(yaml.getDouble(root + "reflection.interaction.arrow") > 0.0D);
        assertTrue(yaml.getDouble(root + "damage.reflected.end-crystal-bomb") > 0.0D);
        assertTrue(yaml.getInt(root + "internal-ai-driver.max-active") > 0);
        assertTrue(yaml.getInt(root + "orbit.max-weapons") - yaml.getInt(root + "orbit.max-weapons") / 5 >= yaml.getInt(root + "basic.actor-total-cap"));
        assertTrue(yaml.getInt(root + "internal-ai-driver.max-active") >= yaml.getInt(root + "basic.actor-total-cap"));
        assertTrue(!yaml.contains(root + "vex-summon-count"));
        assertNotNull(yaml.getConfigurationSection(root + "phase-health-thresholds"));
    }

    @Test void invalidValuesAreBoundedToReadableAndSafeLimits() {
        GatewayBossConfig config = GatewayBossConfig.bounded(1, 99, 9, 99, 9, 9, 1,
                -1, -1, -1, 99, 9999, 99, 9999, 1, 9, 1, 1, 1);
        assertEquals(28.0D, config.attackRange());
        assertEquals(6.0D, config.orbitRadius());
        assertEquals(40, config.maxOrbitWeapons());
        assertEquals(10, config.weaponThrowTelegraphTicks());
        assertEquals(1200, config.basicActorLifetimeTicks());
        assertEquals(24, config.maxActiveDrivers());
        assertEquals(1200, config.driverLifetimeTicks());
        assertEquals(12.0D, config.phaseSpacing());
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
