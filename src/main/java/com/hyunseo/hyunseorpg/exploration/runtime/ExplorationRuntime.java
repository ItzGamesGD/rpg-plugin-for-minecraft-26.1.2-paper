package com.hyunseo.hyunseorpg.exploration.runtime;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Ephemeral state only. Persistent lifecycle lives in StructureRecord. */
public final class ExplorationRuntime {
    private final UUID structureId;
    private final String variantId;
    private final long activatedAtTick;
    private final RuntimeObjectTracker tracker = new RuntimeObjectTracker();
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Set<UUID> objectiveEntities = new LinkedHashSet<>();
    private boolean objectiveMode;
    private Long physicalExitAtTick;
    private UUID choiceOwner;
    private String choicePromptId = "";
    private Set<String> allowedChoices = Set.of();
    private String defaultChoice = "flee";
    private long choiceExpiresAtTick = -1L;
    private String selectedChoice = "";

    public ExplorationRuntime(UUID structureId, String variantId) {
        this(structureId, variantId, 0L);
    }

    public ExplorationRuntime(UUID structureId, String variantId, long activatedAtTick) {
        this.structureId = structureId;
        this.variantId = variantId;
        this.activatedAtTick = Math.max(0L, activatedAtTick);
    }

    public UUID structureId() { return structureId; }
    public String variantId() { return variantId; }
    public long activatedAtTick() { return activatedAtTick; }
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
    public synchronized boolean beginChoice(UUID owner, String promptId, Set<String> choices,
                                            String fallback, long expiresAtTick) {
        if (owner == null || choicePending() || !selectedChoice.isBlank()) return false;
        Set<String> normalized = new LinkedHashSet<>();
        if (choices != null) {
            for (String choice : choices) {
                String value = normalizeChoice(choice);
                if (!value.isBlank()) normalized.add(value);
            }
        }
        if (normalized.isEmpty()) return false;
        String normalizedFallback = normalizeChoice(fallback);
        if (!normalized.contains(normalizedFallback)) normalizedFallback = normalized.contains("flee") ? "flee" : normalized.iterator().next();
        choiceOwner = owner;
        choicePromptId = promptId == null ? "" : promptId.trim();
        allowedChoices = Set.copyOf(normalized);
        defaultChoice = normalizedFallback;
        choiceExpiresAtTick = Math.max(0L, expiresAtTick);
        return true;
    }
    public synchronized boolean choicePending() { return choiceOwner != null && selectedChoice.isBlank(); }
    public synchronized boolean choiceExpired(long currentTick) {
        return choicePending() && choiceExpiresAtTick >= 0L && currentTick >= choiceExpiresAtTick;
    }
    public synchronized UUID choiceOwner() { return choiceOwner; }
    public synchronized String choicePromptId() { return choicePromptId; }
    public synchronized String defaultChoice() { return defaultChoice; }
    public synchronized Set<String> allowedChoices() { return allowedChoices; }
    public synchronized String selectedChoice() { return selectedChoice; }
    public synchronized boolean choose(UUID playerId, String choice) {
        String normalized = normalizeChoice(choice);
        if (!choicePending() || !choiceOwner.equals(playerId) || !allowedChoices.contains(normalized)) return false;
        selectedChoice = normalized;
        return true;
    }
    public synchronized Long physicalExitAtTick() { return physicalExitAtTick; }
    public synchronized void markPhysicalExit(long tick) { physicalExitAtTick = tick; }
    public synchronized void clearPhysicalExit() { physicalExitAtTick = null; }

    private static String normalizeChoice(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
