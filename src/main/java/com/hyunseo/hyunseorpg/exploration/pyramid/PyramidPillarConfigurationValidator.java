package com.hyunseo.hyunseorpg.exploration.pyramid;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic validation for the configured Desert Pyramid pillar graph.
 *
 * <p>The usable room grid is the inclusive -3..3 range on both axes. Every
 * pillar owns an isolated set of cells; overlap is rejected before a runtime
 * board can be activated.</p>
 */
public final class PyramidPillarConfigurationValidator {
    public static final int MIN_PILLERS = 3;
    public static final int MAX_PILLERS = 4;
    public static final int USABLE_MIN = -3;
    public static final int USABLE_MAX = 3;

    private PyramidPillarConfigurationValidator() {}

    public static ValidationResult validate(Collection<PushPillarDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        List<PushPillarDefinition> pillars = List.copyOf(definitions);
        var errors = new java.util.ArrayList<String>();

        if (pillars.size() < MIN_PILLERS) {
            errors.add("pyramid pillar count must be at least " + MIN_PILLERS);
        }
        if (pillars.size() > MAX_PILLERS) {
            errors.add("pyramid pillar count must be at most " + MAX_PILLERS);
        }

        Set<String> ids = new LinkedHashSet<>();
        Set<PyramidGridPoint> targets = new LinkedHashSet<>();
        for (PushPillarDefinition pillar : pillars) {
            if (!ids.add(pillar.id())) {
                errors.add("duplicate pillar id: " + pillar.id());
            }
            if (pillar.initialPosition().equals(pillar.targetPosition())) {
                errors.add("pillar " + pillar.id() + " initial and target must differ");
            }
            if (!pillar.allowedCells().contains(pillar.initialPosition())) {
                errors.add("pillar " + pillar.id() + " initial is not allowed: " + pillar.initialPosition());
            }
            if (!pillar.allowedCells().contains(pillar.targetPosition())) {
                errors.add("pillar " + pillar.id() + " target is not allowed: " + pillar.targetPosition());
            }
            if (!targets.add(pillar.targetPosition())) {
                errors.add("duplicate target cell: " + pillar.targetPosition());
            }
            for (PyramidGridPoint cell : pillar.allowedCells()) {
                if (!insideUsableRoom(cell)) {
                    errors.add("pillar " + pillar.id() + " cell outside usable room: " + cell);
                }
            }
            if (!isCardinallyReachable(pillar)) {
                errors.add("pillar " + pillar.id() + " target is not cardinally reachable from initial");
            }
        }

        for (int i = 0; i < pillars.size(); i++) {
            for (int j = i + 1; j < pillars.size(); j++) {
                var left = pillars.get(i);
                var right = pillars.get(j);
                Set<PyramidGridPoint> overlap = new LinkedHashSet<>(left.allowedCells());
                overlap.retainAll(right.allowedCells());
                if (!overlap.isEmpty()) {
                    errors.add("overlapping pillar paths: " + left.id() + " and " + right.id()
                            + " share " + overlap);
                }
            }
        }
        return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    public static void requireValid(Collection<PushPillarDefinition> definitions) {
        ValidationResult result = validate(definitions);
        if (!result.valid()) {
            throw new IllegalArgumentException(String.join("; ", result.errors()));
        }
    }

    public static boolean insideUsableRoom(PyramidGridPoint point) {
        return point != null
                && point.x() >= USABLE_MIN && point.x() <= USABLE_MAX
                && point.z() >= USABLE_MIN && point.z() <= USABLE_MAX;
    }

    private static boolean isCardinallyReachable(PushPillarDefinition pillar) {
        var seen = new LinkedHashSet<PyramidGridPoint>();
        var queue = new ArrayDeque<PyramidGridPoint>();
        seen.add(pillar.initialPosition());
        queue.add(pillar.initialPosition());
        while (!queue.isEmpty()) {
            PyramidGridPoint current = queue.remove();
            if (current.equals(pillar.targetPosition())) return true;
            for (PyramidGridDirection direction : PyramidGridDirection.values()) {
                var next = current.translate(direction);
                if (pillar.allowedCells().contains(next) && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public ValidationResult {
            errors = List.copyOf(errors == null ? List.of() : errors);
        }

        public boolean isValid() {
            return valid;
        }
    }
}
