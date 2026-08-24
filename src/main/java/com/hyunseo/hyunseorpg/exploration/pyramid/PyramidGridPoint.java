package com.hyunseo.hyunseorpg.exploration.pyramid;

import java.util.Objects;

/**
 * A room-local cell for the desert-pyramid push-pillar puzzle.
 *
 * <p>It intentionally has no Bukkit World or Location reference. The content
 * integration resolves this cell through the selected room origin/orientation,
 * keeping the puzzle definition reusable across deterministic room candidates.</p>
 */
public record PyramidGridPoint(int x, int z) {

    public PyramidGridPoint {
        // Keep a canonical value object and make accidental null direction use
        // fail at the call site rather than producing an invalid transition.
    }

    public PyramidGridPoint translate(PyramidGridDirection direction) {
        Objects.requireNonNull(direction, "direction");
        return new PyramidGridPoint(x + direction.x(), z + direction.z());
    }
}
