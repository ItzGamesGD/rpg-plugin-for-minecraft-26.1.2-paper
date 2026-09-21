package com.hyunseo.hyunseorpg.alchemy.brewing;

import com.hyunseo.hyunseorpg.alchemy.potion.PotionFactory;
import com.hyunseo.hyunseorpg.alchemy.recipe.AlchemyRecipeRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Adapts canonical one-step recipes to the vanilla Brewing Stand.
 * Unmatched states are never cancelled or mutated, leaving Minecraft authoritative.
 */
public final class BrewingStandAlchemyListener implements Listener {
    private final AlchemyRecipeRegistry recipes;
    private final PotionFactory potions;
    private final BrewingInputResolver inputs;

    public BrewingStandAlchemyListener(AlchemyRecipeRegistry recipes, PotionFactory potions,
                                       BrewingInputResolver inputs) {
        this.recipes = recipes;
        this.potions = potions;
        this.inputs = inputs;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        String ingredientId = inputs.resolveIngredient(event.getContents().getIngredient());
        if (ingredientId.isBlank()) return;

        List<String> bases = java.util.stream.IntStream.range(0, event.getResults().size())
                .mapToObj(slot -> inputs.resolveBase(event.getContents().getItem(slot))).toList();
        BrewingTransactionPlanner<ItemStack> planner = new BrewingTransactionPlanner<>(recipes, (slot, recipe) -> {
            int amount = event.getContents().getItem(slot) == null
                    ? 1 : Math.max(1, event.getContents().getItem(slot).getAmount());
            return potions.create(recipe.resultPotionId(), amount);
        });
        BrewingTransactionPlanner.Plan<ItemStack> plan = planner.plan(bases, ingredientId, event.getResults());
        if (plan.status() == BrewingTransactionPlanner.Status.UNMATCHED) return;
        if (plan.status() == BrewingTransactionPlanner.Status.INVALID) {
            // A matched custom transition is all-or-nothing. Cancelling here lets
            // Bukkit retain every input and prevents vanilla fallback duplication.
            event.setCancelled(true);
            return;
        }

        // BrewEvent owns ingredient consumption. By committing only the complete
        // result list and not cancelling, the vanilla stand consumes it exactly once.
        for (int slot = 0; slot < plan.outputs().size(); slot++) {
            event.getResults().set(slot, plan.outputs().get(slot));
        }
    }
}
