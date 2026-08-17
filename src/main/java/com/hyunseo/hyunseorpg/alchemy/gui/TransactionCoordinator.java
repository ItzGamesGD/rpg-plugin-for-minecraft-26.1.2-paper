package com.hyunseo.hyunseorpg.alchemy.gui;

public interface TransactionCoordinator<S, O> {
    Result execute(S session, O output);
    Result close(S session, CloseReason reason);
    enum Result { SUCCESS, BLOCKED, OUTPUT_CAPACITY_FAILED, INPUT_STATE_CHANGED, CONCURRENT_TRANSACTION, INPUT_RETURNED, RETURN_FAILED, ALREADY_FINALIZED }
    enum CloseReason { PLAYER_CLOSE, LOGOUT, KICK, SERVER_SHUTDOWN, GUI_REPLACED }
}
