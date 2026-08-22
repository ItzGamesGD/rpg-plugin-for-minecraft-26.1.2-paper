package com.hyunseo.hyunseorpg.exploration.runtime;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Ephemeral state only. Persistent lifecycle lives in StructureRecord. */
public final class ExplorationRuntime {
    private final UUID structureId;
    private final String variantId;
    private final RuntimeObjectTracker tracker = new RuntimeObjectTracker();
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Set<UUID> objectiveEntities = new LinkedHashSet<>();
    private boolean objectiveMode;
    private Long physicalExitAtTick;

    public ExplorationRuntime(UUID structureId, String variantId) {
        this.structureId = structureId;
        this.variantId = variantId;
    }

    public UUID structureId() { return structureId; }
    public String variantId() { return variantId; }
    public RuntimeObjectTracker tracker() { return tracker; }
    public synchronized void addParticipant(UUID playerId) { if (playerId != null) participants.add(playerId); }
    public synchronized Set<UUID> participants() { return Set.copyOf(participants); }
    public synchronized void trackObjectives(java.util.Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return;
        objectiveMode = true;
        objectiveEntities.addAll(ids);
    }
    public synchronized boolean objectiveMode() { return objectiveMode; }
    public synchronized Set<UUID> objectiveEntities() { return Set.copyOf(objectiveEntities); }
    public synchronized void removeObjective(UUID id) { objectiveEntities.remove(id); }
    public synchronized Long physicalExitAtTick() { return physicalExitAtTick; }
    public synchronized void markPhysicalExit(long tick) { physicalExitAtTick = tick; }
    public synchronized void clearPhysicalExit() { physicalExitAtTick = null; }
}
