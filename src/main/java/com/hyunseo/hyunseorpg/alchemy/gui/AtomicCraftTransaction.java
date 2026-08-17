package com.hyunseo.hyunseorpg.alchemy.gui;

public interface AtomicCraftTransaction<S, O> {
    Validation validate(S session);
    boolean canFitOutput(S session, O output);
    CommitResult commit(S session, O output);
    void rollback(S session);
    TransactionState state(S session);
    ReturnResult returnUncommitted(S session);
    enum Validation { VALID, SESSION_LOCKED, RECIPE_NOT_FOUND, MATERIALS_INVALID, OUTPUT_OVERFLOW }
    enum CommitResult { SUCCESS, FAILED, ALREADY_COMMITTED, OUTPUT_NOT_DELIVERED }
    enum ReturnResult { RETURNED, NOTHING_TO_RETURN, FAILED, ALREADY_RETURNED }
    enum TransactionState { OPEN, VALIDATED, COMMITTING, COMMITTED, ROLLED_BACK, RETURNED }
}
