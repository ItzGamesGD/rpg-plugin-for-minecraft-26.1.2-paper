package com.hyunseo.hyunseorpg.crafting;

import org.bukkit.entity.Player;

/** Compatibility facade retained for legacy call sites; all work is delegated to the canonical transaction. */
public final class CraftingService {
    private final CraftingTransactionService transaction;

    public CraftingService(CraftingTransactionService transaction) {
        this.transaction = transaction;
    }

    public Result craft(Player player, String recipeId) {
        return Result.from(transaction.craft(player, recipeId, false));
    }

    public enum Result {
        SUCCESS, UNKNOWN, INVALID, MISSING_INGREDIENTS, INSUFFICIENT_ABUNDANCE_POINTS, NO_SPACE;

        private static Result from(CraftingTransactionService.Result result) {
            return switch (result.status()) {
                case SUCCESS -> SUCCESS;
                case UNKNOWN_RECIPE -> UNKNOWN;
                case INVALID_OUTPUT -> INVALID;
                case MISSING_INGREDIENTS -> MISSING_INGREDIENTS;
                case INSUFFICIENT_ABUNDANCE_POINTS -> INSUFFICIENT_ABUNDANCE_POINTS;
                default -> NO_SPACE;
            };
        }
    }
}
