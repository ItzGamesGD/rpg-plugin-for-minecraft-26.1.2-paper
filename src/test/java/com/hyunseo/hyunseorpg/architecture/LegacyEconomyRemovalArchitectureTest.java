package com.hyunseo.hyunseorpg.architecture;

import com.hyunseo.hyunseorpg.enhancement.VanillaAnvilEnhancementListener;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyEconomyRemovalArchitectureTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void legacyEconomyBossAuthorityAndNavigationShellAreRemoved() throws Exception {
        for (String file : List.of(
                "java/com/hyunseo/hyunseorpg/economy/CoinService.java",
                "java/com/hyunseo/hyunseorpg/activity/ActivityCoinRewardService.java",
                "java/com/hyunseo/hyunseorpg/progression/MagicStoneFragmentService.java",
                "java/com/hyunseo/hyunseorpg/boss/BossRewardService.java",
                "java/com/hyunseo/hyunseorpg/boss/BossSessionManager.java",
                "java/com/hyunseo/hyunseorpg/ui/RPGMenuService.java",
                "java/com/hyunseo/hyunseorpg/enhancement/EquipmentGrowthGuiService.java",
                "resources/shops.yml", "resources/progression-loop.yml", "resources/bosses.yml")) {
            assertFalse(Files.exists(MAIN.resolve(file)), file);
        }
        String plugin = Files.readString(MAIN.resolve("java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java"));
        assertFalse(plugin.contains("BossSession"));
        assertFalse(plugin.contains("RPGMenu"));
        assertFalse(plugin.contains("EquipmentGrowthGui"));
    }

    @Test
    void upgradeStoneRemainsPhysicalWhileCustomCraftingConfigIsRetired() throws Exception {
        String items = Files.readString(MAIN.resolve("resources/items.yml"));
        assertTrue(items.contains("basic_upgrade_stone:"));
        assertFalse(items.contains("basic_upgrade_fragment:"));
        assertFalse(Files.exists(MAIN.resolve("resources/crafting.yml")));
        assertEquals(1, VanillaAnvilEnhancementListener.stoneCost());
    }

    @Test
    void stageOneThroughThreeFoundationsRemain() {
        assertTrue(Files.exists(MAIN.resolve("java/com/hyunseo/hyunseorpg/enchant/nativeapi/HyunseoRPGPluginBootstrap.java")));
        assertTrue(Files.exists(MAIN.resolve("resources/datapack/pack.mcmeta")));
        assertTrue(Files.exists(MAIN.resolve("java/com/hyunseo/hyunseorpg/enhancement/VanillaAnvilEnhancementListener.java")));
        assertTrue(Files.exists(MAIN.resolve("java/com/hyunseo/hyunseorpg/enhancement/VanillaAnvilPolicyListener.java")));
        assertFalse(Files.exists(MAIN.resolve("java/com/hyunseo/hyunseorpg/enhancement/EquipmentPromotionService.java")));
        assertFalse(Files.exists(MAIN.resolve("java/com/hyunseo/hyunseorpg/farming")));
    }

    @Test
    void removedConfigsAreNotManaged() throws Exception {
        String config = Files.readString(MAIN.resolve("java/com/hyunseo/hyunseorpg/core/config/ConfigService.java"));
        String migration = Files.readString(MAIN.resolve("java/com/hyunseo/hyunseorpg/core/config/ConfigMigrationService.java"));
        for (String removed : List.of("shops.yml", "progression-loop.yml", "equipment-support.yml", "bosses.yml")) {
            assertFalse(config.contains("loadManagedConfig(\"" + removed + "\")"), removed);
            assertFalse(managedFilesDeclaration(migration).contains("\"" + removed + "\""), removed);
        }
    }

    private String managedFilesDeclaration(String source) {
        int start = source.indexOf("MANAGED_FILES = List.of(");
        int end = source.indexOf(");", start);
        return source.substring(start, end);
    }
}
