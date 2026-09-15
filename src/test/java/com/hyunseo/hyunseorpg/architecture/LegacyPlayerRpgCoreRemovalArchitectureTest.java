package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
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
    private String productionSource() throws IOException {
        try (var paths = Files.walk(PRODUCTION)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .map(path -> { try { return Files.readString(path); } catch (IOException e) { throw new RuntimeException(e); } })
                    .reduce("", (left, right) -> left + "\n" + right);
        }
    }
}
