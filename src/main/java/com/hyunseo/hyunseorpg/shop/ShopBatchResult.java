package com.hyunseo.hyunseorpg.shop;

import java.util.Map;

/** Result of one atomic Dialog submission. */
public record ShopBatchResult(ShopTransactionReason reason, Map<String, Integer> amounts, long coins) {
    public static ShopBatchResult failed(ShopTransactionReason reason) {
        return new ShopBatchResult(reason, Map.of(), 0L);
    }

    public boolean success() {
        return reason == ShopTransactionReason.SUCCESS;
    }
}
