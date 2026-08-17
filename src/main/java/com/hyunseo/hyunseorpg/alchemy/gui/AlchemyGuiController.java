package com.hyunseo.hyunseorpg.alchemy.gui;

import java.util.UUID;

public interface AlchemyGuiController<S extends AlchemyGuiSession> {
    S open(UUID playerId);
    void close(UUID playerId, CloseReason reason);
    ClickResult handleClick(S session, ClickAction action);
    void returnUncommitted(S session);
    enum CloseReason { PLAYER_CLOSE, LOGOUT, KICK, SERVER_SHUTDOWN, REPLACED }
    enum ClickAction { NORMAL, SHIFT, NUMBER_KEY, DRAG, DOUBLE_CLICK, COLLECT_TO_CURSOR, HOPPER }
    enum ClickResult { ACCEPTED, BLOCKED, SESSION_CLOSED, TRANSACTION_LOCKED }
}
