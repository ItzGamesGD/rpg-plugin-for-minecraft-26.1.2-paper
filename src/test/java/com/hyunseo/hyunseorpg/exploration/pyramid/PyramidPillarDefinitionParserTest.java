package com.hyunseo.hyunseorpg.exploration.pyramid;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PyramidPillarDefinitionParserTest {
    @Test
    void startupParserProducesTheDefinitionValidatedBeforeRuntimeMutation() {
        List<PushPillarDefinition> definitions = PyramidPillarDefinitionParser.parse(List.of(
                pillar("a", "-3,-3", "-2,-3", List.of("-3,-3", "-2,-3")),
                pillar("b", "0,-3", "1,-3", List.of("0,-3", "1,-3")),
                pillar("c", "3,3", "2,3", List.of("3,3", "2,3"))));

        PyramidPillarConfigurationValidator.requireValid(definitions);
        assertEquals(List.of("a", "b", "c"), definitions.stream().map(PushPillarDefinition::id).toList());
    }

    @Test
    void invalidParsedConfigurationFailsStartupValidation() {
        List<PushPillarDefinition> definitions = PyramidPillarDefinitionParser.parse(List.of(
                pillar("a", "-3,-3", "-2,-3", List.of("-3,-3", "-2,-3")),
                pillar("b", "-2,-3", "-1,-3", List.of("-2,-3", "-1,-3"))));

        assertThrows(IllegalArgumentException.class,
                () -> PyramidPillarConfigurationValidator.requireValid(definitions));
    }

    private static Map<String, Object> pillar(String id, String initial, String target, List<String> allowed) {
        return Map.of("id", id, "initial", initial, "target", target, "allowed", allowed);
    }
}
