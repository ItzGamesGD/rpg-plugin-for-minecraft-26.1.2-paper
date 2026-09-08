package com.hyunseo.hyunseorpg.special.thanatos;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ThanatosFloorSelectorTest {
    private record Cell(String id, boolean floor, boolean clear) { }

    @Test
    void selectsFirstSafeFloorInsideOnlyTheProvidedLocalBand() {
        List<Cell> band = List.of(new Cell("air", false, true), new Cell("container", false, false),
                new Cell("air-2", false, true), new Cell("stone", true, false));
        assertEquals("stone", ThanatosFloorSelector.nearestLocalFloor(band, Cell::floor, Cell::clear).id());
    }

    @Test
    void refusesFunctionalOrCoveredCandidates() {
        List<Cell> covered = List.of(new Cell("solid-cover", false, false), new Cell("stone", true, false));
        assertNull(ThanatosFloorSelector.nearestLocalFloor(covered, Cell::floor, Cell::clear));
        assertNull(ThanatosFloorSelector.nearestLocalFloor(
                List.of(new Cell("air", false, true), new Cell("chest", false, false)), Cell::floor, Cell::clear));
    }
}
