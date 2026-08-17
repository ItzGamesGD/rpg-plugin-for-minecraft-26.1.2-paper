package com.hyunseo.hyunseorpg.shop;

public record ShopTransactionResult(ShopTransactionReason reason, int amount, long coins) {
    public static ShopTransactionResult failed(ShopTransactionReason reason) {
        return new ShopTransactionResult(reason, 0, 0L);
    }

    public static ShopTransactionResult success(int amount, long coins) {
        return new ShopTransactionResult(ShopTransactionReason.SUCCESS, amount, coins);
    }

    public boolean success() {
        return reason == ShopTransactionReason.SUCCESS;
    }
}
