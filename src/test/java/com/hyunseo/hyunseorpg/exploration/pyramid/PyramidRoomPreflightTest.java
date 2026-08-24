package com.hyunseo.hyunseorpg.exploration.pyramid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PyramidRoomPreflightTest {

    @Test
    void selectsFirstUsableCandidateInFixedSlotOrder() {
        PyramidRoomCandidate west = candidate(PyramidRoomCandidate.Slot.WEST);
        PyramidRoomCandidate north = candidate(PyramidRoomCandidate.Slot.NORTH);
        PyramidRoomCandidate center = candidate(PyramidRoomCandidate.Slot.CENTER);

        PyramidRoomCandidate resolved = PyramidRoomPreflight.firstUsable(
            List.of(west, north, center),
            candidate -> candidate.slot() != PyramidRoomCandidate.Slot.CENTER
        ).orElseThrow();

        assertEquals(PyramidRoomCandidate.Slot.NORTH, resolved.slot());
    }

    @Test
    void roomLocalCoordinatesRotateWithoutDependingOnAbsoluteWorldPosition() {
        PyramidBlockPosition origin = new PyramidBlockPosition(100, 20, 200);

        assertEquals(new PyramidBlockPosition(102, 21, 203),
            PyramidRoomOrientation.NORTH.resolve(origin, 2, 1, 3));
        assertEquals(new PyramidBlockPosition(97, 21, 202),
            PyramidRoomOrientation.EAST.resolve(origin, 2, 1, 3));
        assertEquals(new PyramidBlockPosition(98, 21, 197),
            PyramidRoomOrientation.SOUTH.resolve(origin, 2, 1, 3));
        assertEquals(new PyramidBlockPosition(103, 21, 198),
            PyramidRoomOrientation.WEST.resolve(origin, 2, 1, 3));
    }

    private static PyramidRoomCandidate candidate(PyramidRoomCandidate.Slot slot) {
        return new PyramidRoomCandidate(slot, new PyramidBlockPosition(0, 0, 0), PyramidRoomOrientation.NORTH);
    }
}
