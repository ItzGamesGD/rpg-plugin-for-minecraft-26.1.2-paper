package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class LegacyPlayerRpgCoreRemovalArchitectureTest {
    private static final Path PRODUCTION = Path.of("src/main/java");
    @Test
    void legacyCharacterSheetServicesAndCommandsStayRemoved() throws IOException {
        List<String> forbidden = List.of("ClassService", "ClassWeaponService", "ClassStatService", "StatService",
                "ManaService", "ManaBossBarService", "WeaponProficiencyService", "SkillRegistry", "SkillStatService");
        String source = productionSource();
        forbidden.forEach(symbol -> assertFalse(source.contains(symbol), symbol + " must not return to production"));
        assertFalse(Files.exists(Path.of("src/main/resources/classes.yml")));
        assertFalse(Files.exists(Path.of("src/main/resources/stats.yml")));
        assertFalse(Files.exists(Path.of("src/main/resources/skills.yml")));
    }
    @Test
    void playerPersistenceIgnoresLegacyCharacterSheetFields() throws IOException {
        String data = Files.readString(PRODUCTION.resolve("com/hyunseo/hyunseorpg/player/PlayerRPGData.java"));
        for (String field : List.of("selectedClass", "selectedProfession", "currentMana", "statPoints",
                "skillPoints", "skillLevels", "weaponProficiencyLevels")) assertFalse(data.contains(field));
        String repository = Files.readString(PRODUCTION.resolve("com/hyunseo/hyunseorpg/player/YamlPlayerDataRepository.java"));
        assertFalse(repository.contains("selectedClass"));
        assertFalse(repository.contains("currentMana"));
    }
    @Test
    void activeItemAndEnchantRuntimeDoesNotDependOnLegacyCore() throws IOException {
        String enchant = Files.readString(PRODUCTION.resolve("com/hyunseo/hyunseorpg/enchant/EquipmentEnchantContentService.java"));
        String special = Files.readString(PRODUCTION.resolve("com/hyunseo/hyunseorpg/special/SpecialEquipmentService.java"));
        for (String forbidden : List.of("ClassService", "ManaService", "SkillRegistry", "WeaponProficiencyService")) {
            assertFalse(enchant.contains(forbidden)); assertFalse(special.contains(forbidden));
        }
    }

    @Test
    void rpgLevelNeverOwnsMinecraftExperience() throws IOException {
        String levelService = Files.readString(PRODUCTION.resolve("com/hyunseo/hyunseorpg/exp/LevelService.java"));
        String levelCommand = Files.readString(PRODUCTION.resolve("com/hyunseo/hyunseorpg/command/RPGLevelAdminCommand.java"));
        String levelRuntime = levelService + levelCommand;
        for (String mutation : List.of("setLevel(", "setExp(", "setTotalExperience(", "setKeepLevel(",
                "setDroppedExp(", "setNewLevel(", "setNewExp(", "setNewTotalExp(")) {
            assertFalse(levelRuntime.contains(mutation), mutation + " must remain Minecraft-owned");
        }
        assertFalse(Files.exists(PRODUCTION.resolve("com/hyunseo/hyunseorpg/exp/LevelPlayerListener.java")));
        assertFalse(levelCommand.contains("refresh"), "the retired XP-bar synchronization command must stay absent");
        assertFalse(Files.readString(Path.of("src/main/resources/exp.yml")).contains("sync-vanilla-exp-bar"));
    }

    @Test
    void preNativeEnchantBlockerStaysRetired() throws IOException {
        assertFalse(Files.exists(PRODUCTION.resolve(
                "com/hyunseo/hyunseorpg/enhancement/VanillaEnchantBlockListener.java")));
        String config = Files.readString(Path.of("src/main/resources/config.yml"));
        assertFalse(config.contains("block-vanilla-enchanting"));
        assertFalse(config.contains("\nvanilla-enchants:"));
    }

    @Test
    void questAndLegacyExplorationRuntimeStayRetiredWhileOceanModelRemains() throws IOException {
        String plugin = Files.readString(PRODUCTION.resolve("com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java"));
        String command = Files.readString(PRODUCTION.resolve("com/hyunseo/hyunseorpg/command/RPGGiveCommand.java"));
        String migration = Files.readString(PRODUCTION.resolve("com/hyunseo/hyunseorpg/core/config/ConfigMigrationService.java"));
        for (String removed : List.of("QuestRegistry", "QuestService", "AutoQuestService",
                "QuestProgressListener", "reloadQuestsConfig", "questRegistry")) {
            assertFalse(plugin.contains(removed));
        }
        for (String removed : List.of("ExplorationModule", "explorationModule", "explorationModule::reload",
                "explorationModule.start()", "explorationModule.stop()")) assertFalse(plugin.contains(removed));
        for (String removed : List.of("setExplorationModule", "handleExploration", "\"exploration\"")) {
            assertFalse(command.contains(removed));
        }
        for (String removed : List.of("migrateExploration", "exploration/structures.yml", "\"exploration\"")) {
            assertFalse(migration.contains(removed));
        }

        Path exploration = PRODUCTION.resolve("com/hyunseo/hyunseorpg/exploration");
        try (var entries = Files.list(exploration)) {
            assertEquals(Set.of("ocean"), entries.map(path -> path.getFileName().toString()).collect(Collectors.toSet()));
        }
        assertFalse(Files.exists(exploration.resolve("pyramid")));
        assertFalse(Files.exists(exploration.resolve("raid")));
        assertFalse(Files.exists(exploration.resolve("runtime")));
        assertFalse(Files.exists(Path.of("src/main/resources/exploration")));
        for (String model : List.of("MonumentActionResult.java", "MonumentPhase.java", "OceanMonumentProgress.java")) {
            Path source = exploration.resolve("ocean").resolve(model);
            assertTrue(Files.exists(source));
            String content = Files.readString(source);
            String lower = content.toLowerCase(java.util.Locale.ROOT);
            for (String runtime : List.of("org.bukkit", "io.papermc", "explorationmodule", "listener",
                    "registry", "persistence", "javaplugin", "world", "entity", "quest")) {
                assertFalse(lower.contains(runtime), model + " must remain runtime-independent: " + runtime);
            }
        }
        assertTrue(Files.exists(Path.of("src/test/java/com/hyunseo/hyunseorpg/exploration/ocean/OceanMonumentProgressTest.java")));
    }

    @Test
    void playerDataHasNoActiveQuestApiAndRetiredQuestDomainStaysRemoved() throws IOException {
        Set<String> publicMethods = java.util.Arrays.stream(
                        com.hyunseo.hyunseorpg.player.PlayerRPGData.class.getMethods())
                .filter(method -> method.getDeclaringClass()
                        == com.hyunseo.hyunseorpg.player.PlayerRPGData.class)
                .map(java.lang.reflect.Method::getName)
                .collect(Collectors.toSet());
        assertTrue(publicMethods.stream().noneMatch(name -> name.toLowerCase().contains("quest")),
                "PlayerRPGData must not expose retired quest gameplay operations");

        Path questPackage = PRODUCTION.resolve("com/hyunseo/hyunseorpg/quest");
        for (String retired : List.of("AutoQuestData.java", "AutoQuestObjectiveData.java", "AutoQuestStatus.java",
                "AutoQuestType.java", "QuestData.java", "QuestObjective.java", "QuestObjectiveType.java",
                "QuestReward.java", "QuestRewardType.java", "QuestState.java",
                "availability/QuestTargetSource.java")) {
            assertFalse(Files.exists(questPackage.resolve(retired)), retired + " must remain retired");
        }

        String repository = Files.readString(
                PRODUCTION.resolve("com/hyunseo/hyunseorpg/player/YamlPlayerDataRepository.java"));
        for (String retired : List.of("AutoQuestData", "AutoQuestObjectiveData", "AutoQuestStatus",
                "AutoQuestType", "QuestTargetSource")) {
            assertFalse(repository.contains(retired), retired + " must not be interpreted during persistence");
        }
        assertTrue(repository.contains("LegacyQuestCompatibilityData.capture(yaml)"));
    }

    @Test
    void rpgLevelDataAndPersistenceRemainActive() throws IOException {
        String data = Files.readString(PRODUCTION.resolve("com/hyunseo/hyunseorpg/player/PlayerRPGData.java"));
        String repository = Files.readString(PRODUCTION.resolve("com/hyunseo/hyunseorpg/player/YamlPlayerDataRepository.java"));
        assertTrue(data.contains("private int baseLevel;"));
        assertTrue(data.contains("private long baseExp;"));
        assertTrue(repository.contains("yaml.set(\"baseLevel\""));
        assertTrue(repository.contains("yaml.set(\"baseExp\""));
    }
    private String productionSource() throws IOException {
        try (var paths = Files.walk(PRODUCTION)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .map(path -> { try { return Files.readString(path); } catch (IOException e) { throw new RuntimeException(e); } })
                    .reduce("", (left, right) -> left + "\n" + right);
        }
    }
}
