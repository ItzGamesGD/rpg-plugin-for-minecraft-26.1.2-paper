package com.hyunseo.hyunseorpg.exploration.pyramid;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void malformedEntriesAndFieldsAreNeverDropped() {
        assertFieldFailure(List.of("not-a-map"), "pillar", 0, "<missing>");
        assertFieldFailure(List.of(Map.of("initial", "0,0", "target", "1,0", "allowed", List.of("0,0", "1,0"))),
                "id", 0, "<missing>");
        assertFieldFailure(List.of(pillar("bad-initial", "x,0", "1,0", List.of("0,0", "1,0"))),
                "initial", 0, "bad-initial");
        assertFieldFailure(List.of(pillar("bad-target", "0,0", "1,x", List.of("0,0", "1,0"))),
                "target", 0, "bad-target");
        assertFieldFailure(List.of(Map.of("id", "bad-list", "initial", "0,0", "target", "1,0",
                        "allowed", "0,0")), "allowed", 0, "bad-list");
        assertFieldFailure(List.of(pillar("bad-cell", "0,0", "1,0", List.of("0,0", "broken"))),
                "allowed[1]", 0, "bad-cell");
    }

    @Test
    void malformedFourthEntryCannotCollapseToThreeValidPillars() {
        List<Object> configured = new java.util.ArrayList<>(List.of(
                pillar("a", "-3,-3", "-2,-3", List.of("-3,-3", "-2,-3")),
                pillar("b", "0,-3", "1,-3", List.of("0,-3", "1,-3")),
                pillar("c", "3,3", "2,3", List.of("3,3", "2,3"))));
        configured.add(Map.of("id", "d", "initial", "broken", "target", "0,1", "allowed", List.of("0,0", "0,1")));
        assertFieldFailure(configured, "initial", 3, "d");
    }

    @Test
    void coordinateListsRequireExactlyTwoElementsAndPreserveFieldDiagnostics() {
        assertFieldFailure(List.of(pillar("bad-initial", List.of(0, 0, 1), "1,0",
                List.of("0,0", "1,0"))), "initial", 0, "bad-initial");
        assertFieldFailure(List.of(pillar("bad-target", "0,0", List.of(1, 0, 1),
                List.of("0,0", "1,0"))), "target", 0, "bad-target");
        assertFieldFailure(List.of(pillar("bad-allowed", "0,0", "1,0",
                List.of(List.of(0, 0, 1), "1,0"))), "allowed[0]", 0, "bad-allowed");
        assertFieldFailure(List.of(pillar("short", List.of(0), "1,0",
                List.of("0,0", "1,0"))), "initial", 0, "short");
        assertFieldFailure(List.of(pillar("string-extra", "0,0,1", "1,0",
                List.of("0,0", "1,0"))), "initial", 0, "string-extra");

        var parsed = PyramidPillarDefinitionParser.parse(List.of(
                pillar("valid", List.of(0, 0), "1,0", List.of(List.of(0, 0), "1,0"))));
        assertEquals(new PyramidGridPoint(0, 0), parsed.getFirst().initialPosition());
        assertEquals(new PyramidGridPoint(1, 0), parsed.getFirst().targetPosition());
    }

    private static void assertFieldFailure(Object raw, String field, int index, String id) {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> PyramidPillarDefinitionParser.parse(raw, "structure=desert_pyramid, variant=audit"));
        assertTrue(failure.getMessage().contains("variant=audit"));
        assertTrue(failure.getMessage().contains("pillarIndex=" + index));
        assertTrue(failure.getMessage().contains("pillarId=" + id));
        assertTrue(failure.getMessage().contains("field=" + field));
    }

    private static Map<String, Object> pillar(String id, Object initial, Object target, List<?> allowed) {
        return Map.of("id", id, "initial", initial, "target", target, "allowed", allowed);
    }
}
