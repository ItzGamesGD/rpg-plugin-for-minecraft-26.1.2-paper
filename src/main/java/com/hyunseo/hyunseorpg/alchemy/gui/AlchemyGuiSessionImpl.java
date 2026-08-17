package com.hyunseo.hyunseorpg.alchemy.gui;

import java.util.Objects;
import java.util.UUID;

public final class AlchemyGuiSessionImpl implements AlchemyGuiSession {
    private final UUID playerId;
    private final UUID sessionId = UUID.randomUUID();
    private SessionState state = SessionState.OPEN;

    public AlchemyGuiSessionImpl(UUID playerId) { this.playerId = Objects.requireNonNull(playerId, "playerId"); }
    public UUID sessionId() { return sessionId; }
    @Override public UUID playerId() { return playerId; }
    @Override public synchronized SessionState state() { return state; }
    @Override public synchronized void lock() { if (state == SessionState.OPEN) state = SessionState.LOCKED; }
    @Override public synchronized void unlock() { if (state == SessionState.LOCKED) state = SessionState.OPEN; }
    @Override public synchronized void close() { state = SessionState.CLOSED; }
}
