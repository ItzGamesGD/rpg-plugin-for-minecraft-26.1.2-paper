package com.hyunseo.hyunseorpg.exploration.runtime;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;

/** Ephemeral state only. Persistent lifecycle lives in StructureRecord. */
public final class ExplorationRuntime {
    private final UUID structureId;
    private final String variantId;
    private final long activatedAtTick;
    private final RuntimeObjectTracker tracker = new RuntimeObjectTracker();
    private final ExplorationSequenceState sequence = new ExplorationSequenceState();
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Set<UUID> objectiveEntities = new LinkedHashSet<>();
    private boolean objectiveMode;
    private Long lootTriggerExitAtTick;
    private Long combatAbandonExitAtTick;
    private UUID choiceOwner;
    private String choicePromptId = "";
    private Set<String> allowedChoices = Set.of();
    private String defaultChoice = "flee";
    private long choiceExpiresAtTick = -1L;
    private String selectedChoice = "";
    private UUID looter;
    private long lootTakenAtTick = -1L;
    private boolean lootExitPrompted;
    private boolean raidStarted;
    private Location raidOrigin;
    private int objectiveSpawnCount;
    private final Set<UUID> confirmedDeadObjectives = new LinkedHashSet<>();
    private List<String> raidWavePoolIds = List.of();
    private int raidWaveIndex = -1;
    private long nextRaidWaveDelayTicks = 30L;
    private Long nextRaidWaveAtTick;
    private UUID raidTarget;
    private UUID entryActor;
    private double entryDeltaX;
    private double entryDeltaZ;

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
    /** Runtime-only state for generic sequence waits, flags, counters and task ownership. */
    public ExplorationSequenceState sequence() { return sequence; }
    public synchronized void addParticipant(UUID playerId) { if (playerId != null) participants.add(playerId); }
    public synchronized Set<UUID> participants() { return Set.copyOf(participants); }
    public synchronized void trackObjectives(java.util.Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return;
        objectiveMode = true;
        objectiveEntities.addAll(ids);
        objectiveSpawnCount += ids.size();
    }
    public synchronized boolean objectiveMode() { return objectiveMode; }
    public synchronized Set<UUID> objectiveEntities() { return Set.copyOf(objectiveEntities); }
    public synchronized void removeObjective(UUID id) { confirmObjectiveDeath(id); }
    public synchronized boolean confirmObjectiveDeath(UUID id) {
        if (id == null || !objectiveEntities.remove(id)) return false;
        confirmedDeadObjectives.add(id);
        return true;
    }
    public synchronized boolean objectivesCleared() {
        return objectiveMode && objectiveSpawnCount > 0
                && confirmedDeadObjectives.size() >= objectiveSpawnCount;
    }
    public synchronized int objectiveSpawnCount() { return objectiveSpawnCount; }
    public synchronized int confirmedDeadObjectiveCount() { return confirmedDeadObjectives.size(); }
    public synchronized void configureRaidWaveSequence(Collection<String> poolIds) {
        configureRaidWaveSequence(poolIds, 30L);
    }
    public synchronized void configureRaidWaveSequence(Collection<String> poolIds, long delayTicks) {
        List<String> normalized = new ArrayList<>();
        if (poolIds != null) {
            for (String poolId : poolIds) {
                if (poolId != null && !poolId.isBlank()) {
                    normalized.add(poolId.trim().toLowerCase(java.util.Locale.ROOT));
                }
            }
        }
        raidWavePoolIds = List.copyOf(normalized);
        raidWaveIndex = raidWavePoolIds.isEmpty() ? -1 : 0;
        nextRaidWaveDelayTicks = Math.max(0L, delayTicks);
        nextRaidWaveAtTick = null;
    }
    public synchronized boolean hasNextRaidWave() {
        return raidWaveIndex >= 0 && raidWaveIndex + 1 < raidWavePoolIds.size();
    }
    public synchronized boolean advanceRaidWave() {
        if (!hasNextRaidWave()) return false;
        raidWaveIndex++;
        nextRaidWaveAtTick = null;
        return true;
    }
    public synchronized String currentRaidWavePoolId() {
        return raidWaveIndex < 0 || raidWaveIndex >= raidWavePoolIds.size()
                ? "" : raidWavePoolIds.get(raidWaveIndex);
    }
    public synchronized int raidWaveNumber() { return raidWaveIndex + 1; }
    public synchronized int raidWaveCount() { return raidWavePoolIds.size(); }
    public synchronized boolean scheduleNextRaidWave(long currentTick) {
        if (!hasNextRaidWave() || nextRaidWaveAtTick != null) return false;
        nextRaidWaveAtTick = Math.max(0L, currentTick) + nextRaidWaveDelayTicks;
        return true;
    }
    public synchronized boolean nextRaidWaveDue(long currentTick) {
        return nextRaidWaveAtTick != null && currentTick >= nextRaidWaveAtTick;
    }
    public synchronized Long nextRaidWaveAtTick() { return nextRaidWaveAtTick; }
    public synchronized void setRaidTarget(UUID playerId) { raidTarget = playerId; }
    public synchronized UUID raidTarget() { return raidTarget; }
    /** Actual exterior boundary crosser; deliberately distinct from proximity participants. */
    public synchronized void markPyramidEntry(UUID playerId, double deltaX, double deltaZ) {
        if (playerId == null || entryActor != null) return;
        entryActor = playerId;
        entryDeltaX = deltaX;
        entryDeltaZ = deltaZ;
    }
    public synchronized UUID entryActor() { return entryActor; }
    public synchronized void restorePyramidEntryActor(UUID playerId) { if (entryActor == null) entryActor = playerId; }
    public synchronized double entryDeltaX() { return entryDeltaX; }
    public synchronized double entryDeltaZ() { return entryDeltaZ; }
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
    /** Releases a failed Pyramid choice without making the runtime terminal. */
    public synchronized void releaseChoiceForRetry() {
        choiceOwner = null;
        choicePromptId = "";
        allowedChoices = Set.of();
        selectedChoice = "";
        choiceExpiresAtTick = -1L;
    }
    public synchronized void markLootTaken(UUID playerId, long tick) {
        if (playerId == null) return;
        looter = playerId;
        lootTakenAtTick = Math.max(0L, tick);
    }
    public synchronized boolean lootTaken() { return looter != null; }
    public synchronized UUID looter() { return looter; }
    public synchronized long lootTakenAtTick() { return lootTakenAtTick; }
    public synchronized boolean lootExitPrompted() { return lootExitPrompted; }
    public synchronized void markLootExitPrompted() { lootExitPrompted = true; }
    public synchronized boolean raidStarted() { return raidStarted; }
    public synchronized void markRaidStarted() { raidStarted = true; }
    public synchronized Location raidOrigin() { return raidOrigin == null ? null : raidOrigin.clone(); }
    public synchronized void snapshotRaidOrigin(Location origin) {
        if (raidOrigin == null && origin != null && origin.getWorld() != null) raidOrigin = origin.clone();
    }
    public synchronized boolean choose(UUID playerId, String choice) {
        String normalized = normalizeChoice(choice);
        if (!choicePending() || !choiceOwner.equals(playerId) || !allowedChoices.contains(normalized)) return false;
        selectedChoice = normalized;
        return true;
    }
    public synchronized Long lootTriggerExitAtTick() { return lootTriggerExitAtTick; }
    public synchronized void markLootTriggerExit(long tick) { lootTriggerExitAtTick = tick; }
    public synchronized void clearLootTriggerExit() { lootTriggerExitAtTick = null; }
    public synchronized Long combatAbandonExitAtTick() { return combatAbandonExitAtTick; }
    public synchronized void markCombatAbandonExit(long tick) { combatAbandonExitAtTick = tick; }
    public synchronized void clearCombatAbandonExit() { combatAbandonExitAtTick = null; }

    private static String normalizeChoice(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
