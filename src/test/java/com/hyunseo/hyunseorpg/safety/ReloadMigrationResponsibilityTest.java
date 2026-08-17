package com.hyunseo.hyunseorpg.safety;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadMigrationResponsibilityTest {
    @Test
    void ordinaryReloadDoesNotApplyItemMigration() throws Exception {
        Path source = Path.of("src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java");
        String text = Files.readString(source, StandardCharsets.UTF_8);
        assertFalse(text.contains("configMigrationService.migrate(\"items\", false)"));
        assertTrue(text.contains("registerDetailed(\"all\""));
        assertTrue(text.contains("migrate(\"configs\", true)"));
        assertFalse(text.contains("throw new IllegalStateException(\"Unable to load canonical crafting layout\")"));
    }
}
