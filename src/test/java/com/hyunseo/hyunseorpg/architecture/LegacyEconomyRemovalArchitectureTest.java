package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyEconomyRemovalArchitectureTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void currencyServicesAndActiveConfigsAreRemoved() {
        for (String file : List.of(
                "java/com/hyunseo/hyunseorpg/economy/CoinService.java",
                "java/com/hyunseo/hyunseorpg/economy/CoinDisplayTask.java",
                "java/com/hyunseo/hyunseorpg/activity/ActivityCoinRewardService.java",
                "java/com/hyunseo/hyunseorpg/progression/MagicStoneFragmentService.java",
                "resources/shops.yml", "resources/progression-loop.yml")) {
            assertFalse(Files.exists(MAIN.resolve(file)), file);
        }
    }

    @Test
    void upgradeStoneUsesPhysicalVanillaResourcesAndAnvilConsumesOne() throws Exception {
        String crafting = Files.readString(MAIN.resolve("resources/crafting.yml"));
        assertTrue(crafting.contains("vanilla:DIAMOND: 1"));
        assertTrue(crafting.contains("vanilla:REDSTONE: 4"));
        assertTrue(crafting.contains("vanilla:LAPIS_LAZULI: 4"));
        assertFalse(crafting.contains("basic_upgrade_fragment"));
        String listener = Files.readString(MAIN.resolve(
                "java/com/hyunseo/hyunseorpg/enhancement/VanillaAnvilEnhancementListener.java"));
        assertTrue(listener.contains("setRepairItemCountCost(1)"));
    }

    @Test
    void rewardsContainNoLegacyCurrencyAndBossesKeepXpAndUpgradeStones() throws Exception {
        String resources = Files.readString(MAIN.resolve("resources/bosses.yml"))
                + Files.readString(MAIN.resolve("resources/mobs.yml"))
                + Files.readString(MAIN.resolve("resources/mythic-mobs.yml"));
        assertFalse(resources.matches("(?s).*\\bcoins?:.*"));
        assertFalse(resources.contains("magic_stone"));
        assertFalse(resources.contains("basic_upgrade_fragment"));
        assertTrue(resources.contains("experience:"));
        assertTrue(resources.contains("basic_upgrade_stone:"));
    }

    @Test
    void legacyPlayerCurrencyFieldsAreIgnoredRatherThanPersisted() throws Exception {
        String data = Files.readString(MAIN.resolve("java/com/hyunseo/hyunseorpg/player/PlayerRPGData.java"));
        String repository = Files.readString(MAIN.resolve("java/com/hyunseo/hyunseorpg/player/YamlPlayerDataRepository.java"));
        assertFalse(data.contains("long coins"));
        assertFalse(repository.contains("yaml.set(\"coins\""));
        assertFalse(repository.contains("data.setCoins("));
    }

    @Test
    void nativeEnchantAndVanillaAnvilArchitecturesRemainPresent() {
        assertTrue(Files.exists(MAIN.resolve("java/com/hyunseo/hyunseorpg/enchant/nativeapi/HyunseoRPGPluginBootstrap.java")));
        assertTrue(Files.exists(MAIN.resolve("resources/datapack/pack.mcmeta")));
        assertTrue(Files.exists(MAIN.resolve("java/com/hyunseo/hyunseorpg/enhancement/VanillaAnvilEnhancementListener.java")));
        assertTrue(Files.exists(MAIN.resolve("java/com/hyunseo/hyunseorpg/enhancement/VanillaAnvilPolicyListener.java")));
    }
}
