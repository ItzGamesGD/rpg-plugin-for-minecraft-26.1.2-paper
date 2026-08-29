package com.hyunseo.hyunseorpg.exploration.pyramid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void keepsTheCanonicalCandidateOrderForProtectedCenterFallback() {
        assertEquals(List.of(PyramidRoomCandidate.Slot.CENTER, PyramidRoomCandidate.Slot.NORTH,
                        PyramidRoomCandidate.Slot.SOUTH, PyramidRoomCandidate.Slot.EAST,
                        PyramidRoomCandidate.Slot.WEST),
                List.of(PyramidRoomCandidate.Slot.values()));
    }

    @Test
    void fallsBackFromUnsafeCenterToNorth() {
        PyramidRoomCandidate center = candidate(PyramidRoomCandidate.Slot.CENTER);
        PyramidRoomCandidate north = candidate(PyramidRoomCandidate.Slot.NORTH);

        assertEquals(PyramidRoomCandidate.Slot.NORTH,
                PyramidRoomPreflight.firstUsable(List.of(north, center),
                        value -> value.slot() != PyramidRoomCandidate.Slot.CENTER).orElseThrow().slot());
    }

    @Test
    void skipsMultipleUnsafeSlotsBeforeSelectingLaterSafeSlot() {
        PyramidRoomCandidate center = candidate(PyramidRoomCandidate.Slot.CENTER);
        PyramidRoomCandidate north = candidate(PyramidRoomCandidate.Slot.NORTH);
        PyramidRoomCandidate south = candidate(PyramidRoomCandidate.Slot.SOUTH);

        assertEquals(PyramidRoomCandidate.Slot.SOUTH,
                PyramidRoomPreflight.firstUsable(List.of(south, north, center),
                        value -> value.slot() == PyramidRoomCandidate.Slot.SOUTH).orElseThrow().slot());
    }

    @Test
    void failsClosedWhenEveryCandidateIsUnsafe() {
        assertTrue(PyramidRoomPreflight.firstUsable(List.of(
                        candidate(PyramidRoomCandidate.Slot.CENTER),
                        candidate(PyramidRoomCandidate.Slot.NORTH),
                        candidate(PyramidRoomCandidate.Slot.SOUTH),
                        candidate(PyramidRoomCandidate.Slot.EAST),
                        candidate(PyramidRoomCandidate.Slot.WEST)),
                value -> false).isEmpty());
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
