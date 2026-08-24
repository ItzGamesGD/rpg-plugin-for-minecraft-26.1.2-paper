package com.hyunseo.hyunseorpg.exploration.pyramid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Deterministically chooses one already-calculated Pyramid room candidate.
 *
 * <p>The supplied predicate is where the Bukkit-only, bounded shell and
 * build-height scan belongs. This class never loads chunks or mutates blocks,
 * so preflight can fail safely before chest loot starts an event.</p>
 */
public final class PyramidRoomPreflight {
    private PyramidRoomPreflight() {
    }

    public static Optional<PyramidRoomCandidate> firstUsable(
        Collection<PyramidRoomCandidate> candidates,
        Predicate<PyramidRoomCandidate> usable
    ) {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(usable, "usable");

        List<PyramidRoomCandidate> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator.comparingInt(candidate -> candidate.slot().ordinal()));
        return ordered.stream().filter(usable).findFirst();
    }
}
