package com.hyunseo.hyunseorpg.alchemy.gui;

/** Executes the scaffold commit order and leaves recovery decisions to the transaction. */
public final class AtomicTransactionCoordinator<S, O> implements TransactionCoordinator<S, O> {
    private final AtomicCraftTransaction<S, O> transaction;

    public AtomicTransactionCoordinator(AtomicCraftTransaction<S, O> transaction) {
        this.transaction = transaction;
    }

    @Override public Result execute(S session, O output) {
        AtomicCraftTransaction.Validation validation = transaction.validate(session);
        if (validation == AtomicCraftTransaction.Validation.SESSION_LOCKED) return Result.CONCURRENT_TRANSACTION;
        if (validation != AtomicCraftTransaction.Validation.VALID) {
            return validation == AtomicCraftTransaction.Validation.OUTPUT_OVERFLOW
                    ? Result.OUTPUT_CAPACITY_FAILED : Result.BLOCKED;
        }
        if (!transaction.canFitOutput(session, output)) return Result.OUTPUT_CAPACITY_FAILED;
        AtomicCraftTransaction.CommitResult result = transaction.commit(session, output);
        return switch (result) {
            case SUCCESS -> Result.SUCCESS;
            case ALREADY_COMMITTED -> Result.ALREADY_FINALIZED;
            case OUTPUT_NOT_DELIVERED, FAILED -> Result.RETURN_FAILED;
        };
    }

    @Override public Result close(S session, CloseReason reason) {
        AtomicCraftTransaction.ReturnResult result = transaction.returnUncommitted(session);
        return switch (result) {
            case RETURNED, NOTHING_TO_RETURN -> Result.INPUT_RETURNED;
            case ALREADY_RETURNED -> Result.ALREADY_FINALIZED;
            case FAILED -> Result.RETURN_FAILED;
        };
    }
}
