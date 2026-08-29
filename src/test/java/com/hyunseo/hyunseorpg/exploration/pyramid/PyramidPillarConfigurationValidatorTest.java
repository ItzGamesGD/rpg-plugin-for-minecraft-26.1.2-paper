package com.hyunseo.hyunseorpg.exploration.pyramid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PyramidPillarConfigurationValidatorTest {
    private static final List<PushPillarDefinition> DEFAULT = List.of(
            pillar("sun", "-3,-3", "-1,-1", "-3,-3", "-2,-3", "-2,-2", "-1,-2", "-1,-1"),
            pillar("moon", "3,-3", "1,-1", "3,-3", "2,-3", "2,-2", "1,-2", "1,-1"),
            pillar("emerald", "-3,3", "-1,1", "-3,3", "-2,3", "-2,2", "-1,2", "-1,1"),
            pillar("amethyst", "3,3", "1,1", "3,3", "2,3", "2,2", "1,2", "1,1"));

    @Test
    void defaultPatternIsDisjointReachableAndInsideUsableRoom() {
        var result = PyramidPillarConfigurationValidator.validate(DEFAULT);
        assertTrue(result.isValid(), result.errors().toString());
        assertEquals(4, DEFAULT.size());
        Set<PyramidGridPoint> targets = new HashSet<>();
        for (var definition : DEFAULT) {
            assertTrue(definition.allowedCells().contains(definition.initialPosition()));
            assertTrue(definition.allowedCells().contains(definition.targetPosition()));
            assertTrue(targets.add(definition.targetPosition()));
            assertTrue(definition.allowedCells().stream()
                    .allMatch(PyramidPillarConfigurationValidator::insideUsableRoom));
            assertTrue(cardinalPath(definition).size() > 1);
        }
    }

    @Test
    void everySolveOrderReachesAllSolvedWithoutCrossPillarSoftlock() {
        int[] order = {0, 1, 2, 3};
        do {
            PushPillarBoard board = new PushPillarBoard(DEFAULT, 0);
            long tick = 0;
            for (int index : order) {
                var definition = DEFAULT.get(index);
                var path = cardinalPath(definition);
                for (int i = 1; i < path.size(); i++) {
                    var direction = direction(path.get(i - 1), path.get(i));
                    var moved = board.tryMove(definition.id(), direction, tick++);
                    assertTrue(moved.moved(), definition.id() + " " + moved.rejection());
                }
                assertEquals(definition.targetPosition(), board.currentPosition(definition.id()).orElseThrow());
            }
            assertTrue(board.allSolved());
        } while (nextPermutation(order));
    }

    @Test
    void overlappingPathsAreRejectedWithConflictingPillarDiagnostic() {
        var overlap = List.of(
                pillar("a", "0,0", "1,0", "0,0", "1,0"),
                pillar("b", "0,1", "1,1", "0,1", "1,1", "1,0"),
                pillar("c", "-3,-3", "-2,-3", "-3,-3", "-2,-3"));
        var result = PyramidPillarConfigurationValidator.validate(overlap);
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(error ->
                error.contains("overlapping pillar paths") && error.contains("a") && error.contains("b")));
    }

    @Test
    void invalidPathDefinitionsAreRejected() {
        var invalid = List.of(
                pillar("a", "4,0", "3,0", "4,0", "3,0"),
                pillar("b", "-3,0", "-2,0", "-3,0", "-2,0"),
                pillar("c", "0,3", "0,2", "0,3", "0,2"));
        var result = PyramidPillarConfigurationValidator.validate(invalid);
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("outside usable room")));
    }

    private static PushPillarDefinition pillar(String id, String initial, String target, String... allowed) {
        return new PushPillarDefinition(id, id, "sand", point(initial), point(target),
                java.util.Arrays.stream(allowed).map(PyramidPillarConfigurationValidatorTest::point).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)));
    }

    private static PyramidGridPoint point(String value) {
        String[] parts = value.split(",");
        return new PyramidGridPoint(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private static List<PyramidGridPoint> cardinalPath(PushPillarDefinition definition) {
        Map<PyramidGridPoint, PyramidGridPoint> previous = new HashMap<>();
        ArrayDeque<PyramidGridPoint> queue = new ArrayDeque<>();
        queue.add(definition.initialPosition());
        previous.put(definition.initialPosition(), null);
        while (!queue.isEmpty()) {
            PyramidGridPoint current = queue.remove();
            if (current.equals(definition.targetPosition())) break;
            for (PyramidGridDirection direction : PyramidGridDirection.values()) {
                PyramidGridPoint next = current.translate(direction);
                if (definition.allowedCells().contains(next) && !previous.containsKey(next)) {
                    previous.put(next, current);
                    queue.add(next);
                }
            }
        }
        List<PyramidGridPoint> path = new ArrayList<>();
        for (PyramidGridPoint current = definition.targetPosition(); current != null; current = previous.get(current)) {
            path.add(current);
        }
        java.util.Collections.reverse(path);
        return path;
    }

    private static PyramidGridDirection direction(PyramidGridPoint from, PyramidGridPoint to) {
        int dx = to.x() - from.x();
        int dz = to.z() - from.z();
        if (dx == 1 && dz == 0) return PyramidGridDirection.EAST;
        if (dx == -1 && dz == 0) return PyramidGridDirection.WEST;
        if (dx == 0 && dz == 1) return PyramidGridDirection.SOUTH;
        if (dx == 0 && dz == -1) return PyramidGridDirection.NORTH;
        throw new IllegalArgumentException("non-cardinal step");
    }

    private static boolean nextPermutation(int[] values) {
        int i = values.length - 2;
        while (i >= 0 && values[i] >= values[i + 1]) i--;
        if (i < 0) return false;
        int j = values.length - 1;
        while (values[j] <= values[i]) j--;
        int tmp = values[i]; values[i] = values[j]; values[j] = tmp;
        for (int left = i + 1, right = values.length - 1; left < right; left++, right--) {
            tmp = values[left]; values[left] = values[right]; values[right] = tmp;
        }
        return true;
    }
}
