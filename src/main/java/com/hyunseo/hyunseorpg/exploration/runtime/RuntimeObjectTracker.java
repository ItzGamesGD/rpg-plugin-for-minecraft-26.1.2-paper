package com.hyunseo.hyunseorpg.exploration.runtime;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/** Owns all temporary runtime objects so cleanup is centralized and idempotent. */
public final class RuntimeObjectTracker {
    private final Deque<Runnable> cleanup = new ArrayDeque<>();
    private boolean closed;

    public synchronized void trackEntity(UUID entityId) {
        if (entityId == null || closed) return;
        cleanup.push(() -> {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) entity.remove();
        });
    }

    public synchronized void track(Runnable action) {
        if (action == null || closed) return;
        cleanup.push(action);
    }

    public synchronized void cleanup() {
        if (closed) return;
        closed = true;
        RuntimeException first = null;
        while (!cleanup.isEmpty()) {
            try { cleanup.pop().run(); }
            catch (RuntimeException exception) { if (first == null) first = exception; }
        }
        if (first != null) throw first;
    }

    public synchronized boolean isClosed() { return closed; }
}
