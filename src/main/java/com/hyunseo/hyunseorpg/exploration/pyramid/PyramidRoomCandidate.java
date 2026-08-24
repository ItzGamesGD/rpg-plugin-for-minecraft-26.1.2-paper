package com.hyunseo.hyunseorpg.exploration.pyramid;

import java.util.Objects;

/**
 * A preflight candidate in the fixed Pyramid ordering. World scanning is
 * performed by the Bukkit adapter; this value only keeps the resolved result
 * deterministic across runtime recreation.
 */
public record PyramidRoomCandidate(
    Slot slot,
    PyramidBlockPosition origin,
    PyramidRoomOrientation orientation
) {
    public PyramidRoomCandidate {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(orientation, "orientation");
    }

    public enum Slot {
        CENTER,
        NORTH,
        SOUTH,
        EAST,
        WEST
    }
}
