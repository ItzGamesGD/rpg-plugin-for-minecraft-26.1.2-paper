package com.hyunseo.hyunseorpg.exploration.pyramid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PushPillarBoardTest {

    @Test
    void eachAcceptedPushMovesExactlyOneCellAndCooldownBlocksAdjacentCallback() {
        PushPillarBoard board = new PushPillarBoard(List.of(
            pillar("sun", new PyramidGridPoint(0, 0), new PyramidGridPoint(2, 0),
                new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0), new PyramidGridPoint(2, 0))
        ), 2);

        PushPillarBoard.MoveResult first = board.tryMove("sun", PyramidGridDirection.EAST, 100);
        assertTrue(first.moved());
        assertEquals(new PyramidGridPoint(1, 0), first.position());
        assertEquals(new PyramidGridPoint(1, 0), board.currentPosition("sun").orElseThrow());

        PushPillarBoard.MoveResult duplicate = board.tryMove("sun", PyramidGridDirection.EAST, 101);
        assertFalse(duplicate.moved());
        assertEquals(PushPillarBoard.Rejection.COOLDOWN, duplicate.rejection());
        assertEquals(new PyramidGridPoint(1, 0), board.currentPosition("sun").orElseThrow());

        PushPillarBoard.MoveResult second = board.tryMove("sun", PyramidGridDirection.EAST, 102);
        assertTrue(second.moved());
        assertTrue(second.solvedNow());
        assertTrue(second.allSolved());
    }

    @Test
    void rejectsPathsOutsideDefinitionAndOccupiedDestination() {
        PushPillarBoard board = new PushPillarBoard(List.of(
            pillar("sun", new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0),
                new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0)),
            pillar("moon", new PyramidGridPoint(1, 0), new PyramidGridPoint(1, 1),
                new PyramidGridPoint(1, 0), new PyramidGridPoint(1, 1))
        ), 0);

        PushPillarBoard.MoveResult occupied = board.tryMove("sun", PyramidGridDirection.EAST, 10);
        assertFalse(occupied.moved());
        assertEquals(PushPillarBoard.Rejection.OCCUPIED, occupied.rejection());

        PushPillarBoard.MoveResult outside = board.tryMove("sun", PyramidGridDirection.NORTH, 11);
        assertFalse(outside.moved());
        assertEquals(PushPillarBoard.Rejection.NOT_ALLOWED, outside.rejection());
    }

    @Test
    void solvedPillarCannotIncreaseSolvedCountTwice() {
        PushPillarBoard board = new PushPillarBoard(List.of(
            pillar("sun", new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0),
                new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0))
        ), 0);

        assertTrue(board.tryMove("sun", PyramidGridDirection.EAST, 1).solvedNow());
        assertEquals(1, board.solvedCount());

        PushPillarBoard.MoveResult retry = board.tryMove("sun", PyramidGridDirection.WEST, 2);
        assertFalse(retry.moved());
        assertEquals(PushPillarBoard.Rejection.SOLVED, retry.rejection());
        assertEquals(1, board.solvedCount());
    }

    @Test
    void finalMoveCanBeDetectedBeforeItMutatesTheSolvedBoard() {
        PushPillarBoard board = new PushPillarBoard(List.of(
                pillar("sun", new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0),
                        new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0))
        ), 0);

        assertTrue(board.wouldCompleteMove("sun", PyramidGridDirection.EAST, 1));
        assertFalse(board.allSolved());
        assertTrue(board.tryMove("sun", PyramidGridDirection.EAST, 1).allSolved());
    }

    @Test
    void invalidDefinitionsFailBeforeAnyRuntimeBoardIsCreated() {
        assertThrows(IllegalArgumentException.class, () -> new PushPillarDefinition(
            "sun", "S", "yellow", new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0),
            Set.of(new PyramidGridPoint(0, 0))
        ));

        PushPillarDefinition one = pillar("sun", new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0),
            new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0));
        PushPillarDefinition two = pillar("moon", new PyramidGridPoint(2, 0), new PyramidGridPoint(1, 0),
            new PyramidGridPoint(2, 0), new PyramidGridPoint(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new PushPillarBoard(List.of(one, two), 0));
    }

    @Test
    void reloadRestoresDurableLogicalPositionAndNeverLeavesLegalPath() {
        PushPillarDefinition definition = pillar("sun", new PyramidGridPoint(0, 0), new PyramidGridPoint(2, 0),
                new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0), new PyramidGridPoint(2, 0));
        PushPillarBoard restored = new PushPillarBoard(List.of(definition), 0,
                java.util.Map.of("sun", new PyramidGridPoint(1, 0)));
        assertEquals(new PyramidGridPoint(1, 0), restored.currentPosition("sun").orElseThrow());
        assertTrue(restored.tryMove("sun", PyramidGridDirection.EAST, 1).allSolved());
    }

    @Test
    void failedLogicalCommitCanRollbackWithoutChangingSolvedCount() {
        PushPillarBoard board = new PushPillarBoard(List.of(
                pillar("sun", new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0),
                        new PyramidGridPoint(0, 0), new PyramidGridPoint(1, 0))), 0);
        PyramidGridPoint previous = board.currentPosition("sun").orElseThrow();
        assertTrue(board.tryMove("sun", PyramidGridDirection.EAST, 1).moved());
        assertTrue(board.rollbackMove("sun", previous));
        assertEquals(previous, board.currentPosition("sun").orElseThrow());
        assertEquals(0, board.solvedCount());
    }

    private static PushPillarDefinition pillar(
        String id,
        PyramidGridPoint initial,
        PyramidGridPoint target,
        PyramidGridPoint... cells
    ) {
        return new PushPillarDefinition(id, id.toUpperCase(), "sand", initial, target, Set.of(cells));
    }
}
