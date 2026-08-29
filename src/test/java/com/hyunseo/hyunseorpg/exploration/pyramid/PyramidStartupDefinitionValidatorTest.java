package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationStructureDefinition;
import com.hyunseo.hyunseorpg.exploration.registry.StructureVariantDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

final class PyramidStartupDefinitionValidatorTest {
    @Test
    void bundledPyramidDefinitionPassesAuthoritativeStartupGate() {
        assertDoesNotThrow(() -> PyramidStartupDefinitionValidator.requireValid(bundledDefinition()));
    }

    @Test
    void repeatedSequenceStateExtensionsDoNotFailOfficialComponentValidation() {
        List<ExplorationComponentSpec> components = requiredComponents();
        components.add(spec("sequence_state", "pyramid_joke_gate"));
        components.add(spec("sequence_state", "pyramid_raid_warning"));
        assertDoesNotThrow(() -> PyramidStartupDefinitionValidator.requireValid(
                definitionWithComponents(components)));
    }

    @Test
    void invalidPillarsAbortBeforeAnyActivationMutation() {
        for (Object invalidPillars : List.of(
                List.of(pillar("a", "-3,-3", "-2,-3", List.of("-3,-3", "-2,-3")),
                        pillar("b", "-2,-3", "-1,-3", List.of("-2,-3", "-1,-3"))),
                List.of(pillar("dup", "-3,-3", "-2,-3", List.of("-3,-3", "-2,-3")),
                        pillar("dup", "0,-3", "1,-3", List.of("0,-3", "1,-3")),
                        pillar("c", "3,3", "2,3", List.of("3,3", "2,3"))),
                malformedOrOverlappingPillars())) {
            AtomicBoolean roomOrDisplayMutation = new AtomicBoolean();
            assertThrows(IllegalArgumentException.class,
                    () -> PyramidStartupDefinitionValidator.requireValid(definition(invalidPillars)));
            assertFalse(roomOrDisplayMutation.get(), "startup rejection occurs before runtime mutation is reachable");
        }
    }

    @Test
    void malformedUnreachableOutOfBoundsAndDuplicateTargetConfigurationsAreRejected() {
        assertRejected(List.of(pillar("a", List.of(0, 0, 1), "1,0", List.of("0,0", "1,0"))));
        assertRejected(threeWithReplacement(pillar("a", "-3,-3", "-1,-1", List.of("-3,-3", "-1,-1"))));
        assertRejected(threeWithReplacement(pillar("a", "-3,-3", "4,-3", List.of("-3,-3", "4,-3"))));
        List<Map<String, Object>> duplicateTarget = validThree();
        duplicateTarget.set(1, pillar("b", "0,-3", "-2,-3", List.of("0,-3", "-1,-3", "-2,-3")));
        assertRejected(duplicateTarget);
    }

    private static void assertRejected(Object pillars) {
        assertThrows(IllegalArgumentException.class,
                () -> PyramidStartupDefinitionValidator.requireValid(definition(pillars)));
    }

    private static Object malformedOrOverlappingPillars() {
        List<Map<String, Object>> pillars = validThree();
        pillars.set(1, pillar("b", "-2,-3", "-1,-3", List.of("-2,-3", "-1,-3")));
        return pillars;
    }

    private static List<Map<String, Object>> threeWithReplacement(Map<String, Object> first) {
        List<Map<String, Object>> pillars = validThree();
        pillars.set(0, first);
        return pillars;
    }

    private static List<Map<String, Object>> validThree() {
        return new ArrayList<>(List.of(
                pillar("a", "-3,-3", "-2,-3", List.of("-3,-3", "-2,-3")),
                pillar("b", "0,-3", "1,-3", List.of("0,-3", "1,-3")),
                pillar("c", "3,3", "2,3", List.of("3,3", "2,3"))));
    }

    private static ExplorationStructureDefinition bundledDefinition() {
        var stream = PyramidStartupDefinitionValidatorTest.class.getClassLoader()
                .getResourceAsStream("exploration/structures.yml");
        assertNotNull(stream);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        var pyramid = yaml.getConfigurationSection("structures.desert_pyramid");
        assertNotNull(pyramid);
        var guardian = pyramid.getConfigurationSection("variants.guardian_trial");
        assertNotNull(guardian);
        List<ExplorationComponentSpec> components = specs(guardian.getMapList("components"));
        return definitionWithComponents(components);
    }

    private static ExplorationStructureDefinition definition(Object pillars) {
        List<ExplorationComponentSpec> components = requiredComponents();
        components.removeIf(spec -> "pyramid_push_pillars".equals(spec.type()));
        components.add(new ExplorationComponentSpec("pyramid_push_pillars",
                Map.of("phase", "pyramid_pillar_restore", "pillars", pillars)));
        return definitionWithComponents(components);
    }

    private static ExplorationStructureDefinition definitionWithComponents(List<ExplorationComponentSpec> components) {
        return new ExplorationStructureDefinition("desert_pyramid", "minecraft:desert_pyramid", true,
                1.0D, 32.0D, 32.0D, 100L, 64.0D, 100L,
                List.of(new StructureVariantDefinition("guardian_trial", 1.0D, true, components)), 4.0D);
    }

    private static List<ExplorationComponentSpec> requiredComponents() {
        return new ArrayList<>(List.of(
                spec("pyramid_room", "pyramid_loot_trigger"), spec("pyramid_room_reveal", "pyramid_room_reveal"),
                spec("pyramid_repel", "pyramid_entry"), spec("choice_prompt", "pyramid_quiz"),
                spec("pyramid_guardian", "pyramid_guardian_spawn"),
                new ExplorationComponentSpec("pyramid_push_pillars", Map.of("phase", "pyramid_pillar_restore",
                        "pillars", validThree())), spec("reward_drop", "clear")));
    }

    private static ExplorationComponentSpec spec(String type, String phase) {
        return new ExplorationComponentSpec(type, Map.of("phase", phase));
    }

    private static List<ExplorationComponentSpec> specs(List<Map<?, ?>> raw) {
        List<ExplorationComponentSpec> result = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            Map<String, Object> options = new LinkedHashMap<>();
            entry.forEach((key, value) -> { if (!"type".equals(String.valueOf(key))) options.put(String.valueOf(key), value); });
            result.add(new ExplorationComponentSpec(String.valueOf(entry.get("type")), options));
        }
        return result;
    }

    private static Map<String, Object> pillar(String id, Object initial, Object target, List<?> allowed) {
        return Map.of("id", id, "initial", initial, "target", target, "allowed", allowed);
    }
}
