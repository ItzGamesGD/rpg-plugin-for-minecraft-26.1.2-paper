package com.hyunseo.hyunseorpg.exploration.pyramid;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Runtime-only logical state for Desert Pyramid push pillars.
 *
 * <p>This is intentionally not a generic grid-puzzle engine. Bukkit display,
 * contact region and particle presentation are separate adapters around this
 * deterministic, room-local state. Calls are synchronized so competing player
 * move callbacks can commit at most one transition at a time.</p>
 */
public final class PushPillarBoard {
    private final Map<String, PillarState> pillarsById;
    private final Map<PyramidGridPoint, String> occupancy;
    private final Set<String> solvedIds = new LinkedHashSet<>();
    private final long cooldownTicks;

    public PushPillarBoard(Collection<PushPillarDefinition> definitions, long cooldownTicks) {
        this(definitions, cooldownTicks, Map.of());
    }

    /** Reconstructs only from durable logical positions; display coordinates are never read. */
    public PushPillarBoard(Collection<PushPillarDefinition> definitions, long cooldownTicks,
                           Map<String, PyramidGridPoint> restoredPositions) {
        Objects.requireNonNull(definitions, "definitions");
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("at least one pillar is required");
        }
        if (definitions.size() > 4) {
            throw new IllegalArgumentException("pyramid puzzle supports at most four pillars");
        }
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("cooldownTicks must be non-negative");
        }

        this.cooldownTicks = cooldownTicks;
        this.pillarsById = new LinkedHashMap<>();
        this.occupancy = new LinkedHashMap<>();
        Map<String, PyramidGridPoint> restored = restoredPositions == null ? Map.of() : restoredPositions;

        Set<PyramidGridPoint> targets = new LinkedHashSet<>();
        for (PushPillarDefinition definition : definitions) {
            PyramidGridPoint position = restored.getOrDefault(definition.id(), definition.initialPosition());
            if (!definition.allowedCells().contains(position)) {
                throw new IllegalArgumentException("restored pillar position is outside its legal path: " + definition.id());
            }
            if (pillarsById.putIfAbsent(definition.id(), new PillarState(definition, position)) != null) {
                throw new IllegalArgumentException("duplicate pillar id: " + definition.id());
            }
            if (occupancy.putIfAbsent(position, definition.id()) != null) {
                throw new IllegalArgumentException("duplicate pillar cell: " + position);
            }
            if (!targets.add(definition.targetPosition())) {
                throw new IllegalArgumentException("duplicate target cell: " + definition.targetPosition());
            }
        }
        for (PillarState state : pillarsById.values()) {
            if (state.currentPosition.equals(state.definition.targetPosition())) {
                state.solved = true;
                solvedIds.add(state.definition.id());
            }
        }
    }

    /**
     * Attempts one atomic cardinal transition. A caller must pass the current
     * server tick; moves inside the configured short cooldown are rejected,
     * preventing two adjacent callbacks from advancing a pillar twice.
     */
    public synchronized MoveResult tryMove(String pillarId, PyramidGridDirection direction, long serverTick) {
        Objects.requireNonNull(pillarId, "pillarId");
        Objects.requireNonNull(direction, "direction");

        PillarState pillar = pillarsById.get(pillarId);
        if (pillar == null) {
            return MoveResult.unknownPillar(pillarId);
        }
        if (pillar.solved) {
            return MoveResult.rejected(pillarId, pillar.currentPosition, Rejection.SOLVED);
        }
        if (serverTick < pillar.nextAllowedMoveTick) {
            return MoveResult.rejected(pillarId, pillar.currentPosition, Rejection.COOLDOWN);
        }

        PyramidGridPoint destination = pillar.currentPosition.translate(direction);
        if (!pillar.definition.allowedCells().contains(destination)) {
            return MoveResult.rejected(pillarId, pillar.currentPosition, Rejection.NOT_ALLOWED);
        }
        if (occupancy.containsKey(destination)) {
            return MoveResult.rejected(pillarId, pillar.currentPosition, Rejection.OCCUPIED);
        }

        occupancy.remove(pillar.currentPosition);
        occupancy.put(destination, pillarId);
        pillar.currentPosition = destination;
        pillar.nextAllowedMoveTick = safeNextTick(serverTick, cooldownTicks);

        boolean solvedNow = destination.equals(pillar.definition.targetPosition());
        if (solvedNow) {
            pillar.solved = true;
            solvedIds.add(pillarId);
        }
        return MoveResult.moved(pillarId, destination, solvedNow, solvedIds.size() == pillarsById.size());
    }

    /**
     * Returns whether this exact next move would irrevocably solve the board.
     * The caller may durably reserve completion before applying the final move.
     */
    public synchronized boolean wouldCompleteMove(String pillarId, PyramidGridDirection direction, long serverTick) {
        PillarState pillar = pillarsById.get(pillarId);
        if (pillar == null || pillar.solved || serverTick < pillar.nextAllowedMoveTick) return false;
        PyramidGridPoint destination = pillar.currentPosition.translate(direction);
        return !occupancy.containsKey(destination)
                && pillar.definition.allowedCells().contains(destination)
                && destination.equals(pillar.definition.targetPosition())
                && solvedIds.size() + 1 == pillarsById.size();
    }

    public synchronized Optional<PyramidGridPoint> currentPosition(String pillarId) {
        PillarState state = pillarsById.get(pillarId);
        return state == null ? Optional.empty() : Optional.of(state.currentPosition);
    }

    public synchronized boolean isSolved(String pillarId) {
        PillarState state = pillarsById.get(pillarId);
        return state != null && state.solved;
    }

    public synchronized int solvedCount() {
        return solvedIds.size();
    }

    public synchronized Set<String> solvedPillarIds() {
        return Set.copyOf(solvedIds);
    }

    public synchronized boolean allSolved() {
        return solvedIds.size() == pillarsById.size();
    }

    private static long safeNextTick(long tick, long cooldown) {
        if (cooldown == 0 || tick > Long.MAX_VALUE - cooldown) {
            return tick;
        }
        return tick + cooldown;
    }

    private static final class PillarState {
        private final PushPillarDefinition definition;
        private PyramidGridPoint currentPosition;
        private boolean solved;
        private long nextAllowedMoveTick = Long.MIN_VALUE;

        private PillarState(PushPillarDefinition definition, PyramidGridPoint position) {
            this.definition = definition;
            this.currentPosition = position;
        }
    }

    public enum Rejection {
        UNKNOWN_PILLAR,
        SOLVED,
        COOLDOWN,
        NOT_ALLOWED,
        OCCUPIED
    }

    public record MoveResult(
        String pillarId,
        PyramidGridPoint position,
        boolean moved,
        boolean solvedNow,
        boolean allSolved,
        Rejection rejection
    ) {
        private static MoveResult moved(String pillarId, PyramidGridPoint position, boolean solvedNow, boolean allSolved) {
            return new MoveResult(pillarId, position, true, solvedNow, allSolved, null);
        }

        private static MoveResult rejected(String pillarId, PyramidGridPoint position, Rejection rejection) {
            return new MoveResult(pillarId, position, false, false, false, rejection);
        }

        private static MoveResult unknownPillar(String pillarId) {
            return rejected(pillarId, null, Rejection.UNKNOWN_PILLAR);
        }
    }
}
