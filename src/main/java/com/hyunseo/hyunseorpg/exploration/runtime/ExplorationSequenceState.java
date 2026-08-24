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
    private final Map<String, BukkitTask> pendingTasks = new LinkedHashMap<>();
    private long physicalMoveEpoch;

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

    /** Reserves a logical action. A duplicate callback for the same id receives false. */
    public synchronized boolean beginAction(String actionId) {
        return completedActions.add(requiredKey(actionId));
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

    /** Idempotent lifecycle cleanup. Scheduled callbacks are deliberately not persistent. */
    public synchronized void cancelPendingTasks() {
        for (BukkitTask task : pendingTasks.values()) {
            if (task != null) task.cancel();
        }
        pendingTasks.clear();
    }

    private static String requiredKey(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) throw new IllegalArgumentException("sequence key is required");
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
