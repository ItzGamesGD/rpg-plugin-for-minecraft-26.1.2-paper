package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NativeMigrationMutationArchitectureTest {
    @Test
    void migrationRemovesOnlyRetiredSourcesAndPreservesUnmappableData() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/enchant/EnchantService.java"));
        String migration = source.substring(source.indexOf("public boolean migrateLegacyEnchantments"),
                source.indexOf("private Enchantment nativeEnchant"));
        assertTrue(migration.contains("setLevel(meta, target"));
        assertTrue(migration.contains("migrateRetiredNativeEnchantments"));
        assertFalse(migration.contains("getKeys().forEach"));
        assertTrue(migration.contains("remove(equippedKey)"));
        assertTrue(migration.contains("Preserving incompatible legacy enchant"));
    }
}
