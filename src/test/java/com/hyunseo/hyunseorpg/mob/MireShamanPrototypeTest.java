package com.hyunseo.hyunseorpg.mob;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MireShamanPrototypeTest {
    @Test
    void mireShamanDefinitionUsesCanonicalNativeMobContract() {
        YamlConfiguration mobs = loadMobs();
        String path = "custom-mobs.mire_shaman";

        assertEquals("수렁 주술사", mobs.getString(path + ".display-name"));
        assertEquals("WITCH", mobs.getString(path + ".vanilla-type"));
        assertEquals("mire_shaman", mobs.getString(path + ".behavior-id"));
        assertFalse(mobs.isConfigurationSection("mire_shaman"));
        assertTrue(mobs.getInt(path + ".behavior.pool.max-active") > 0);
        assertTrue(mobs.getInt(path + ".behavior.minion.max-active") > 0);
        assertTrue(mobs.getDouble(path + ".behavior.reclaim.health-threshold") > 0.0D);
    }

    @Test
    void reclamationIsOwnedAndOncePerLife() {
        assertTrue(MireShamanPolicy.canReclaim(false, 0.50D, 1, 0.50D));
        assertFalse(MireShamanPolicy.canReclaim(true, 0.10D, 3, 0.50D));
        assertFalse(MireShamanPolicy.canReclaim(false, 0.10D, 0, 0.50D));
        assertFalse(MireShamanPolicy.canReclaim(false, 0.75D, 2, 0.50D));
    }

    @Test
    void minionSummonNeverExceedsConfiguredActiveCap() {
        assertEquals(2, MireShamanPolicy.summonCount(0, 2, 2));
        assertEquals(1, MireShamanPolicy.summonCount(1, 2, 2));
        assertEquals(0, MireShamanPolicy.summonCount(2, 2, 2));
    }

    @Test
    void vanillaWitchesAreBlockedButPluginCustomWitchesRemainPossible() {
        assertTrue(VanillaWitchSpawnBlockListener.shouldBlock(
                EntityType.WITCH, CreatureSpawnEvent.SpawnReason.NATURAL));
        assertTrue(VanillaWitchSpawnBlockListener.shouldBlock(
                EntityType.WITCH, CreatureSpawnEvent.SpawnReason.SPAWNER_EGG));
        assertTrue(VanillaWitchSpawnBlockListener.shouldBlock(
                EntityType.WITCH, CreatureSpawnEvent.SpawnReason.COMMAND));
        assertFalse(VanillaWitchSpawnBlockListener.shouldBlock(
                EntityType.WITCH, CreatureSpawnEvent.SpawnReason.CUSTOM));
        assertFalse(VanillaWitchSpawnBlockListener.shouldBlock(
                EntityType.ZOMBIE, CreatureSpawnEvent.SpawnReason.NATURAL));
    }

    private YamlConfiguration loadMobs() {
        var stream = getClass().getClassLoader().getResourceAsStream("mobs.yml");
        assertNotNull(stream);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
