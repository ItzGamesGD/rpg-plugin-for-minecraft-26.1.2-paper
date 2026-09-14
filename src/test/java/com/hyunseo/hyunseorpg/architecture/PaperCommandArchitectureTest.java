package com.hyunseo.hyunseorpg.architecture;

import com.hyunseo.hyunseorpg.command.paper.PaperCommandCatalog;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PaperCommandArchitectureTest {
    @Test
    void productionPluginDoesNotUseJavaPluginGetCommandAndCatalogIsComplete() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java"));
        assertFalse(source.contains("getCommand("));
        Set<String> expected = Set.of("rpg", "effectlist", "rpgtest", "crafting", "weaponinfo", "rpgstat",
                "rpgstatbalance", "stats", "skillstats", "weaponstats", "rpglevel", "rpgcooldown",
                "rpgmob", "rpgquest", "shop", "shopadmin", "specialequipment");
        assertEquals(expected, PaperCommandCatalog.ALL.stream().map(spec -> spec.name()).collect(Collectors.toSet()));
        String descriptor = Files.readString(Path.of("src/main/resources/paper-plugin.yml"));
        assertFalse(descriptor.contains("\ncommands:"), "Paper commands must use LifecycleEvents.COMMANDS");
        assertTrue(descriptor.contains("required: false"), "MythicMobs must remain optional");
        assertFalse(Files.exists(Path.of("src/main/resources/plugin.yml")), "do not ship competing descriptors");
    }
}
