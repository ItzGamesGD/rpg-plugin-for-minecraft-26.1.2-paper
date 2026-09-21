package com.hyunseo.hyunseorpg.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StageSixRemovalArchitectureTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void farmingAndCookingRuntimeAndResourcesAreAbsent() throws Exception {
        assertFalse(Files.exists(MAIN.resolve("java/com/hyunseo/hyunseorpg/farming")));
        assertFalse(Files.exists(MAIN.resolve("resources/farming")));
        String plugin = Files.readString(MAIN.resolve("java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java"));
        assertFalse(plugin.contains("hyunseorpg.farming"));
        assertFalse(plugin.contains("reloadFarming"));

        for (String relative : Set.of(
                "java/com/hyunseo/hyunseorpg/core/config/ConfigService.java",
                "java/com/hyunseo/hyunseorpg/core/config/ConfigDoctor.java",
                "java/com/hyunseo/hyunseorpg/core/config/ConfigMigrationService.java",
                "java/com/hyunseo/hyunseorpg/player/PlayerRPGData.java",
                "java/com/hyunseo/hyunseorpg/player/YamlPlayerDataRepository.java")) {
            String source = Files.readString(MAIN.resolve(relative)).toLowerCase(java.util.Locale.ROOT);
            assertFalse(source.contains("farming"), relative);
            assertFalse(source.contains("cooking"), relative);
        }
    }

    @Test
    void vanillaBrewingFrontendReplacesBlanketBlockerAndCustomGui() throws Exception {
        Path alchemy = MAIN.resolve("java/com/hyunseo/hyunseorpg/alchemy");
        assertFalse(Files.exists(alchemy.resolve("AlchemyVanillaBypassListener.java")));
        String listener = Files.readString(alchemy.resolve("brewing/BrewingStandAlchemyListener.java"));
        assertTrue(listener.contains("AlchemyRecipeRegistry"));
        assertTrue(listener.contains("PotionFactory"));
        assertTrue(listener.contains("Status.UNMATCHED) return"));
        assertFalse(listener.contains("Bukkit.createInventory"));
    }

    @Test
    void pluginOwnedServicesHaveDeclaredFieldsIncludingGenericItemDelivery() throws Exception {
        String plugin = Files.readString(MAIN.resolve("java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java"));
        String declarations = plugin.substring(0, plugin.indexOf("public void onEnable()"));
        Set<String> declared = matches(declarations,
                Pattern.compile("(?m)^\\s*private\\s+(?:final\\s+)?[\\w.$<>?, ]+\\s+(\\w+)\\s*(?:[;=])"));
        Set<String> referenced = matches(plugin, Pattern.compile("\\bthis\\.(\\w+)"));

        assertTrue(declared.contains("inventoryDeliveryService"));
        assertTrue(declared.containsAll(referenced), () -> "undeclared plugin fields: " + difference(referenced, declared));
    }

    private Set<String> matches(String source, Pattern pattern) {
        Set<String> values = new HashSet<>();
        pattern.matcher(source).results().forEach(match -> values.add(match.group(1)));
        return values;
    }

    private Set<String> difference(Set<String> values, Set<String> excluded) {
        Set<String> result = new HashSet<>(values);
        result.removeAll(excluded);
        return result;
    }
}
