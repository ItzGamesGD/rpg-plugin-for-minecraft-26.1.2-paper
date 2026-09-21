package com.hyunseo.hyunseorpg.alchemy.brewing;

import com.hyunseo.hyunseorpg.alchemy.material.AlchemyMaterialResolver;
import com.hyunseo.hyunseorpg.alchemy.potion.PaperPotionPdcContract;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionFactory;
import com.hyunseo.hyunseorpg.alchemy.recipe.AlchemyRecipeDefinition;
import com.hyunseo.hyunseorpg.alchemy.recipe.AlchemyRecipeRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

/**
 * Uses the real brewing inventory as the sole alchemy frontend. Recipes are
 * staged vanilla-style: one base potion in each bottle slot plus the ingredient
 * slot performs one registry-defined transition. A later catalyst is simply a
 * second transition; no hidden slots or menu state exist.
 */
public final class VanillaBrewingStandAlchemyListener implements Listener {
    private final JavaPlugin plugin;
    private final AlchemyRecipeRegistry recipes;
    private final AlchemyMaterialResolver materials;
    private final PaperPotionPdcContract pdc;
    private final PotionFactory potions;

    public VanillaBrewingStandAlchemyListener(JavaPlugin plugin, AlchemyRecipeRegistry recipes,
                                               AlchemyMaterialResolver materials,
                                               PaperPotionPdcContract pdc, PotionFactory potions) {
        this.plugin = plugin;
        this.recipes = recipes;
        this.materials = materials;
        this.pdc = pdc;
        this.potions = potions;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getType() == InventoryType.BREWING)
            schedule((BrewerInventory) event.getView().getTopInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getType() == InventoryType.BREWING)
            schedule((BrewerInventory) event.getView().getTopInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        if (event.getDestination() instanceof BrewerInventory brewing) schedule(brewing);
        if (event.getSource() instanceof BrewerInventory brewing) schedule(brewing);
    }

    private void schedule(BrewerInventory inventory) {
        Bukkit.getScheduler().runTask(plugin, () -> execute(inventory));
    }

    /** Mutates only after every output has been constructed successfully. */
    boolean execute(BrewerInventory inventory) {
        ItemStack ingredient = inventory.getIngredient();
        if (ingredient == null || ingredient.getType().isAir()) return false;
        List<ItemStack> inputs = java.util.Arrays.asList(inventory.getItem(0), inventory.getItem(1), inventory.getItem(2));
        Optional<BrewingTransaction.Plan<ItemStack>> planned = BrewingTransaction.plan(inputs, ingredient,
                this::transition);
        if (planned.isEmpty()) return false;
        BrewingTransaction.Plan<ItemStack> plan = planned.orElseThrow();
        for (int slot = 0; slot < 3; slot++) inventory.setItem(slot, plan.outputs().get(slot));
        ItemStack remaining = ingredient.clone();
        remaining.setAmount(remaining.getAmount() - plan.ingredientConsumption());
        inventory.setIngredient(remaining.getAmount() == 0 ? null : remaining);
        return true;
    }

    private Optional<ItemStack> transition(ItemStack input, ItemStack ingredient) {
        String base = potionId(input);
        if (base.isBlank()) return Optional.empty();
        for (AlchemyRecipeDefinition recipe : recipes.all().values()) {
            if (recipe.enabled() && recipe.basePotionId().equals(base)
                    && materials.matches(ingredient, recipe.ingredientId())) {
                // Re-resolve through the canonical registry API rather than treating
                // the listener's iteration as recipe authority.
                if (recipes.findTransition(base, recipe.ingredientId()).isEmpty()) return Optional.empty();
                return potions.create(recipe.resultPotionId(), input.getAmount());
            }
        }
        return Optional.empty();
    }

    private String potionId(ItemStack input) {
        String custom = pdc.readPotionId(input);
        if (!custom.isBlank()) return custom;
        return input.getType() == Material.POTION ? "vanilla:potion" : "";
    }
}
