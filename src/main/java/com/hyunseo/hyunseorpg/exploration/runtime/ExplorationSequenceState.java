package com.hyunseo.hyunseorpg.exploration.runtime;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.scheduler.BukkitTask;

/**
 * Small, runtime-owned sequence state. It intentionally stores only the values
 * needed by reusable exploration phases, never arbitrary objects or persisted tasks.
 */
public final class ExplorationSequenceState {
    private String currentPhase = "";
    private String lastTransitionReason = "";
    private final Set<String> flags = new LinkedHashSet<>();
    private final Map<String, Integer> counters = new LinkedHashMap<>();
    private final Set<String> completedActions = new LinkedHashSet<>();
    private final Set<String> inFlightActions = new LinkedHashSet<>();
    private final Map<String, BukkitTask> pendingTasks = new LinkedHashMap<>();
    private long physicalMoveEpoch;
    private PendingWait pendingWait;

    public synchronized String currentPhase() { return currentPhase; }
    public synchronized String lastTransitionReason() { return lastTransitionReason; }

    /** Returns false when this runtime has already reached the requested phase. */
    public synchronized boolean transitionTo(String phase, String reason) {
        String normalized = normalize(phase);
        if (normalized.isBlank() || normalized.equals(currentPhase)) return false;
        currentPhase = normalized;
        lastTransitionReason = reason == null ? "" : reason.trim();
        return true;
    }

    public synchronized boolean flag(String key) { return flags.contains(normalize(key)); }
    public synchronized boolean setFlag(String key) { return flags.add(requiredKey(key)); }
    public synchronized boolean clearFlag(String key) { return flags.remove(normalize(key)); }

    public synchronized int counter(String key) { return counters.getOrDefault(normalize(key), 0); }
    public synchronized int incrementCounter(String key) {
        String normalized = requiredKey(key);
        int next = counters.getOrDefault(normalized, 0) + 1;
        counters.put(normalized, next);
        return next;
    }
    public synchronized boolean counterAtLeast(String key, int threshold) {
        return counter(key) >= threshold;
    }

    /** Reserves an action without falsely marking it completed. */
    public synchronized boolean beginAction(String actionId) {
        String key = requiredKey(actionId);
        if (completedActions.contains(key) || !inFlightActions.add(key)) return false;
        return true;
    }

    public synchronized boolean completeAction(String actionId) {
        String key = requiredKey(actionId);
        if (!inFlightActions.remove(key)) return false;
        return completedActions.add(key);
    }

    public synchronized boolean releaseAction(String actionId) {
        return inFlightActions.remove(requiredKey(actionId));
    }

    /** Allows a completed external/staged action to be scheduled again after a retryable failure. */
    public synchronized boolean releaseActionForRetry(String actionId) {
        String key = requiredKey(actionId);
        boolean released = inFlightActions.remove(key);
        released |= completedActions.remove(key);
        return released;
    }

    public synchronized boolean actionInFlight(String actionId) {
        return inFlightActions.contains(normalize(actionId));
    }

    public synchronized void trackTask(String taskId, BukkitTask task) {
        String normalized = requiredKey(taskId);
        BukkitTask previous = pendingTasks.put(normalized, task);
        if (previous != null && previous != task) previous.cancel();
    }

    public synchronized void completeTask(String taskId) {
        pendingTasks.remove(normalize(taskId));
    }

    public synchronized int pendingTaskCount() { return pendingTasks.size(); }

    public synchronized void markPhysicalMove() { physicalMoveEpoch++; }
    public synchronized long physicalMoveEpoch() { return physicalMoveEpoch; }
    public synchronized boolean movedSince(long epoch) { return physicalMoveEpoch > epoch; }

    /**
     * Arms exactly one bounded wait for this runtime. Conditions are intentionally
     * limited to the common event primitives; no arbitrary predicate is stored.
     */
    public synchronized boolean armWait(String actionId, String condition, String key,
                                        int threshold, String nextPhase) {
        String normalizedAction = requiredKey(actionId);
        String normalizedCondition = requiredKey(condition);
        String normalizedPhase = requiredKey(nextPhase);
        if (pendingWait != null || completedActions.contains(normalizedAction)
                || !inFlightActions.add(normalizedAction)) return false;
        pendingWait = new PendingWait(normalizedAction, normalizedCondition, normalize(key),
                threshold, normalizedPhase, physicalMoveEpoch);
        return true;
    }

    public synchronized PendingWait pendingWait() { return pendingWait; }

    /** Returns and clears the currently armed wait; safe to call more than once. */
    public synchronized PendingWait completeWait(String actionId) {
        if (pendingWait == null || !pendingWait.actionId().equals(normalize(actionId))) return null;
        PendingWait completed = pendingWait;
        pendingWait = null;
        completeAction(completed.actionId());
        return completed;
    }

    /** Idempotent lifecycle cleanup. Scheduled callbacks are deliberately not persistent. */
    public synchronized void cancelPendingTasks() {
        for (BukkitTask task : pendingTasks.values()) {
            if (task != null) task.cancel();
        }
        pendingTasks.clear();
        pendingWait = null;
        inFlightActions.clear();
    }

    /** Immutable data for a runtime-owned wait; never serialized with StructureRecord. */
    public record PendingWait(String actionId, String condition, String key, int threshold,
                              String nextPhase, long movementEpoch) { }

    private static String requiredKey(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) throw new IllegalArgumentException("sequence key is required");
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
