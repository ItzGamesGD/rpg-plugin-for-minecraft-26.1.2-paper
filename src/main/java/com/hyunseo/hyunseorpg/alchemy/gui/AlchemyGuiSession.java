package com.hyunseo.hyunseorpg.alchemy.gui;

import java.util.UUID;

public interface AlchemyGuiSession {
    UUID playerId();
    SessionState state();
    void lock();
    void unlock();
    void close();
    enum SessionState { OPEN, LOCKED, CLOSED }
}
