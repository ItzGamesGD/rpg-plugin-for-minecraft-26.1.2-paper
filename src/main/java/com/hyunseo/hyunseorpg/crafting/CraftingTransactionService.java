package com.hyunseo.hyunseorpg.crafting;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.item.VanillaStackingService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.BiPredicate;
import java.util.Optional;

/** Main-thread inventory transaction for every custom menu crafting action. */
public final class CraftingTransactionService {
    private final CraftingRecipeRegistry recipes;
    private final RPGItemService itemService;
    private final SoulboundItemService soulbound;
    private final VanillaStackingService vanillaStacking;
    private final BiConsumer<Player, ItemStack> deliveryObserver;
    private final Set<UUID> activeTransactions = new HashSet<>();
    private BiFunction<Player, CraftingRecipeData, ItemStack> outputFactory;
    private BiFunction<Player, CraftingRecipeData, Optional<ItemStack>> dynamicOutputResolver =
            (player, recipe) -> Optional.empty();
    private Predicate<String> maximumCraftingAllowed = recipeId -> true;
    private BiPredicate<Player, CraftingRecipeData> recipeAccessAllowed = (player, recipe) -> true;

    public CraftingTransactionService(CraftingRecipeRegistry recipes, RPGItemService itemService,
                                      SoulboundItemService soulbound,
                                      VanillaStackingService vanillaStacking,
                                      BiConsumer<Player, ItemStack> deliveryObserver) {
        this.recipes = recipes;
        this.itemService = itemService;
        this.soulbound = soulbound;
        this.vanillaStacking = vanillaStacking;
        this.deliveryObserver = deliveryObserver == null ? (player, item) -> { } : deliveryObserver;
        this.outputFactory = this::createBaseOutput;
    }

    public void setOutputFactory(BiFunction<Player, CraftingRecipeData, ItemStack> outputFactory) {
        this.outputFactory = outputFactory == null ? this::createBaseOutput : outputFactory;
    }

    public void setDynamicOutputResolver(BiFunction<Player, CraftingRecipeData, Optional<ItemStack>> resolver) {
        this.dynamicOutputResolver = resolver == null ? (player, recipe) -> Optional.empty() : resolver;
    }

    public void setMaximumCraftingAllowed(Predicate<String> predicate) {
        this.maximumCraftingAllowed = predicate == null ? recipeId -> true : predicate;
    }

    /**
     * Optional policy boundary for recipes with player-owned requirements.
     * Generic crafting remains open by default.
     */
    public void setRecipeAccessAllowed(BiPredicate<Player, CraftingRecipeData> predicate) {
        this.recipeAccessAllowed = predicate == null ? (player, recipe) -> true : predicate;
    }





    public Result craft(Player player, String recipeId, boolean maximum) {
        if (player == null || !player.isOnline()) return Result.failure(Status.PLAYER_OFFLINE);
        if (!activeTransactions.add(player.getUniqueId())) return Result.failure(Status.BUSY);
        try {
            CraftingRecipeData recipe = recipes.get(recipeId).orElse(null);
            if (recipe == null || !recipe.enabled()) return Result.failure(Status.UNKNOWN_RECIPE);
            if (!recipeAccessAllowed.test(player, recipe)) {
                return Result.failure(Status.REQUIREMENT_NOT_MET);
            }
            if (maximum && !maximumCraftingAllowed.test(recipe.id())) {
                return Result.failure(Status.BULK_DISABLED);
            }
            ItemStack output = createOutput(player, recipe);
            if (output == null || output.getType().isAir()) return Result.failure(Status.INVALID_OUTPUT);

            ItemStack[] initial = cloneStorage(player.getInventory().getStorageContents());
            Capacity capacity = capacity(recipe, output, initial, player);
            int requested = maximum ? capacity.maximum() : 1;
            if (requested < 1) {
                return Result.failure(capacity.materialMaximum() < 1
                        ? Status.MISSING_INGREDIENTS : Status.NO_SPACE, capacity);
            }
            if (!canFit(initial, output, requested)) return Result.failure(Status.NO_SPACE, capacity);

            ItemStack[] committed = cloneStorage(initial);
            consume(committed, recipe, requested, player);
            for (int count = 0; count < requested; count++) {
                if (!addExact(committed, output.clone())) {
                    return Result.failure(Status.NO_SPACE, capacity);
                }
            }
            BooleanSupplier applied = () -> {
                player.getInventory().setStorageContents(committed);
                player.updateInventory();
                for (int count = 0; count < requested; count++) deliveryObserver.accept(player, output.clone());
                return true;
            };
            try {
                if (!applied.getAsBoolean()) {
                    player.getInventory().setStorageContents(cloneStorage(initial));
                    player.updateInventory();
                    return Result.failure(Status.FAILED, capacity);
                }
            } catch (RuntimeException exception) {
                player.getInventory().setStorageContents(cloneStorage(initial));
                player.updateInventory();
                return Result.failure(Status.FAILED, capacity);
            }
            return Result.success(requested, capacity);
        } catch (RuntimeException exception) {
            return Result.failure(Status.FAILED);
        } finally {
            activeTransactions.remove(player.getUniqueId());
        }
    }

    public Capacity capacity(Player player, String recipeId) {
        CraftingRecipeData recipe = recipes.get(recipeId).orElse(null);
        if (player == null || recipe == null || !recipe.enabled()) return Capacity.empty();
        ItemStack output = createOutput(player, recipe);
        if (output == null || output.getType().isAir()) return Capacity.empty();
        return capacity(recipe, output, cloneStorage(player.getInventory().getStorageContents()), player);
    }

    public int ingredientCount(Player player, CraftingRecipeData.Ingredient ingredient) {
        if (player == null || ingredient == null) return 0;
        return count(player.getInventory().getStorageContents(), ingredient, player);
    }

    private Capacity capacity(CraftingRecipeData recipe, ItemStack output, ItemStack[] storage, Player player) {
        int materialMaximum = Integer.MAX_VALUE;
        for (CraftingRecipeData.Ingredient ingredient : recipe.ingredients()) {
            materialMaximum = Math.min(materialMaximum, count(storage, ingredient, player) / ingredient.amount());
        }
        ItemStack[] simulated = cloneStorage(storage);
        int capacityMaximum = 0;
        while (capacityMaximum < materialMaximum && addExact(simulated, output.clone())) capacityMaximum++;
        int maximum = Math.min(materialMaximum, capacityMaximum);
        return new Capacity(materialMaximum, capacityMaximum, maximum);
    }



    private ItemStack createOutput(Player player, CraftingRecipeData recipe) {
        ItemStack output = dynamicOutputResolver.apply(player, recipe).orElseGet(() -> outputFactory.apply(player, recipe));
        if (output != null && soulbound.requiresBinding(output)) soulbound.bind(output, player.getUniqueId());
        if (output != null && vanillaStacking != null) vanillaStacking.normalize(output);
        return output;
    }

    private ItemStack createBaseOutput(Player player, CraftingRecipeData recipe) {
        ItemStack output;
        if (recipe.outputId().startsWith("vanilla:")) {
            Material material = Material.matchMaterial(recipe.outputId().substring("vanilla:".length()).toUpperCase(Locale.ROOT));
            output = material == null ? null : new ItemStack(material, recipe.outputAmount());
        } else {
            output = itemService.create(recipe.outputId(), recipe.outputAmount()).orElse(null);
        }
        return output;
    }

    private int count(ItemStack[] storage, CraftingRecipeData.Ingredient ingredient, Player player) {
        return Arrays.stream(storage).filter(item -> matches(item, ingredient, player)).mapToInt(ItemStack::getAmount).sum();
    }

    private void consume(ItemStack[] storage, CraftingRecipeData recipe, int crafts, Player player) {
        for (CraftingRecipeData.Ingredient ingredient : recipe.ingredients()) {
            int remaining = Math.multiplyExact(ingredient.amount(), crafts);
            for (int slot = 0; slot < storage.length && remaining > 0; slot++) {
                ItemStack item = storage[slot];
                if (!matches(item, ingredient, player)) continue;
                int used = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - used);
                if (item.getAmount() <= 0) storage[slot] = null;
                remaining -= used;
            }
            if (remaining != 0) throw new IllegalStateException("Ingredient plan changed during crafting");
        }
    }

    private boolean matches(ItemStack item, CraftingRecipeData.Ingredient ingredient, Player player) {
        if (item == null || item.getType().isAir()) return false;
        if (ingredient.tag()) {
            return itemService.getItemId(item)
                    .map(id -> itemService.hasTag(id, ingredient.tagId()))
                    .orElse(false)
                    && (!soulbound.isSoulbound(item) || soulbound.isOwnedBy(item, player));
        }
        if (!ingredient.vanilla()) return itemService.isItem(item, ingredient.id())
                && (!soulbound.isSoulbound(item) || soulbound.isOwnedBy(item, player));
        if (itemService.getItemId(item).isPresent()) return false;
        Material material = Material.matchMaterial(ingredient.id().substring("vanilla:".length()).toUpperCase(Locale.ROOT));
        if (material == null || item.getType() != material) return false;
        return material.getMaxStackSize() != 1 || isPristineVanillaEquipment(item);
    }

    private boolean isPristineVanillaEquipment(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;
        if (meta instanceof Damageable damageable && damageable.hasDamage()) return false;
        return !meta.hasDisplayName() && !meta.hasLore() && !meta.hasEnchants()
                && meta.getPersistentDataContainer().isEmpty();
    }

    private boolean canFit(ItemStack[] storage, ItemStack output, int crafts) {
        ItemStack[] simulated = cloneStorage(storage);
        for (int count = 0; count < crafts; count++) if (!addExact(simulated, output.clone())) return false;
        return true;
    }

    private boolean addExact(ItemStack[] storage, ItemStack output) {
        int remaining = output.getAmount();
        int outputMaxStackSize = Math.max(1, vanillaStacking == null
                ? output.getMaxStackSize() : vanillaStacking.effectiveMaxStackSize(output));
        for (ItemStack existing : storage) {
            if (existing == null || existing.getType().isAir() || !existing.isSimilar(output)) continue;
            int existingMaxStackSize = Math.max(1, vanillaStacking == null
                    ? existing.getMaxStackSize() : vanillaStacking.effectiveMaxStackSize(existing));
            int used = Math.min(remaining, existingMaxStackSize - existing.getAmount());
            if (used > 0) existing.setAmount(existing.getAmount() + used);
            remaining -= used;
            if (remaining == 0) return true;
        }
        for (int slot = 0; slot < storage.length && remaining > 0; slot++) {
            if (storage[slot] != null && !storage[slot].getType().isAir()) continue;
            ItemStack placed = output.clone();
            int amount = Math.min(remaining, outputMaxStackSize);
            placed.setAmount(amount);
            storage[slot] = placed;
            remaining -= amount;
        }
        return remaining == 0;
    }

    private ItemStack[] cloneStorage(ItemStack[] storage) {
        ItemStack[] copy = new ItemStack[storage.length];
        for (int index = 0; index < storage.length; index++) {
            copy[index] = storage[index] == null ? null : storage[index].clone();
        }
        return copy;
    }

    public enum Status { SUCCESS, UNKNOWN_RECIPE, REQUIREMENT_NOT_MET, INVALID_OUTPUT,
        MISSING_INGREDIENTS, INSUFFICIENT_ABUNDANCE_POINTS, NO_SPACE, BULK_DISABLED,
        BUSY, PLAYER_OFFLINE, FAILED }

    public record Capacity(int materialMaximum, int capacityMaximum, int maximum) {
        public static Capacity empty() { return new Capacity(0, 0, 0); }
    }

    public record Result(Status status, int crafted, Capacity capacity) {
        public static Result success(int crafted, Capacity capacity) { return new Result(Status.SUCCESS, crafted, capacity); }
        public static Result failure(Status status) { return new Result(status, 0, Capacity.empty()); }
        public static Result failure(Status status, Capacity capacity) { return new Result(status, 0, capacity); }
        public boolean success() { return status == Status.SUCCESS; }
    }
}
