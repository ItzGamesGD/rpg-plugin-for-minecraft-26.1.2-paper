package com.hyunseo.hyunseorpg.crafting;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.item.VanillaStackingService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Registers YAML recipes in the vanilla recipe book and validates PDC inputs at craft time. */
public final class CraftingRecipeService implements Listener {
    private final JavaPlugin plugin;
    private final RPGItemService itemService;
    private final SoulboundItemService soulbound;
    private final CraftingRecipeRegistry registry;
    private final VanillaStackingService vanillaStacking;
    private final Map<NamespacedKey, CraftingRecipeData> recipesByKey = new HashMap<>();
    private final Set<NamespacedKey> registeredRecipeKeys = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingCrafts = ConcurrentHashMap.newKeySet();
    private Function<CraftingRecipeData, ItemStack> outputFactory;

    public CraftingRecipeService(JavaPlugin plugin, RPGItemService itemService,
                                 SoulboundItemService soulbound, CraftingRecipeRegistry registry,
                                 VanillaStackingService vanillaStacking) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.soulbound = soulbound;
        this.registry = registry;
        this.vanillaStacking = vanillaStacking;
        this.outputFactory = this::createBaseOutput;
    }

    public void setOutputFactory(Function<CraftingRecipeData, ItemStack> outputFactory) {
        this.outputFactory = outputFactory == null ? this::createBaseOutput : outputFactory;
    }

    public void register() {
        // Remove recipes from the previous configuration before rebuilding the registry.
        // Clearing recipesByKey alone leaves deleted YAML recipes registered in Bukkit.
        Set<NamespacedKey> previousKeys = new HashSet<>(registeredRecipeKeys);
        for (NamespacedKey previousKey : previousKeys) {
            Bukkit.removeRecipe(previousKey);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.undiscoverRecipe(previousKey);
            }
        }
        registeredRecipeKeys.clear();
        recipesByKey.clear();
        for (CraftingRecipeData recipe : registry.getAll()) {
            // Special equipment is created only by the integrated crafting
            // transaction. Never expose its generated recipe to a vanilla
            // crafting table or recipe book.
            if (registry.specialEquipmentIdForRecipe(recipe.id()).isPresent()) continue;
            ItemStack output = createOutput(null, recipe);
            if (output == null) continue;
            NamespacedKey key = key(recipe.id());
            Bukkit.removeRecipe(key);
            ShapelessRecipe shapeless = new ShapelessRecipe(key, output);
            for (CraftingRecipeData.Ingredient ingredient : recipe.ingredients()) {
                RecipeChoice choice = choiceFor(ingredient.id());
                if (choice == null) {
                    plugin.getLogger().warning("Skipping crafting recipe with unknown ingredient: " + recipe.id() + " -> " + ingredient.id());
                    shapeless = null;
                    break;
                }
                // Stackable materials are represented by one recipe slot and their full quantity is
                // validated from stack totals below. Non-stackable equipment must reserve one slot
                // per item so recipes such as eight swords or three tridents can match Bukkit's
                // shapeless recipe matcher without allowing a vanilla one-item shortcut.
                for (int count = 0; count < registeredChoiceCount(ingredient); count++) {
                    shapeless.addIngredient(choice);
                }
            }
            if (shapeless != null) {
                Bukkit.addRecipe(shapeless);
                recipesByKey.put(key, recipe);
                registeredRecipeKeys.add(key);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            discover(player);
        }
    }

    public void discover(Player player) {
        recipesByKey.keySet().forEach(player::discoverRecipe);
    }

    public List<CraftingRecipeData> getRecipes() {
        return registry.getAll();
    }

    public CraftingRecipeRegistry registry() {
        return registry;
    }

    public NamespacedKey key(String recipeId) {
        return new NamespacedKey(plugin, "crafting_" + recipeId.toLowerCase(Locale.ROOT));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        discover(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingRecipeData recipe = recipe(event.getRecipe());
        if (recipe == null) return;
        Player player = event.getView().getPlayer() instanceof Player value ? value : null;
        event.getInventory().setResult(isValid(player, event.getInventory().getMatrix(), recipe)
                ? createOutput(player, recipe) : null);
    }

    // Kept as a compatibility helper for older integrations; the registered handler below
    // performs the transaction on the next tick so Bukkit cannot consume the full input stack.
    private void onCraftLegacy(CraftItemEvent event) {
        CraftingRecipeData recipe = recipe(event.getRecipe());
        if (recipe == null) return;
        if (!(event.getWhoClicked() instanceof Player player)
                || !isValid(player, event.getInventory().getMatrix(), recipe)) {
            event.setCancelled(true);
            return;
        }
        ItemStack output = createOutput(player, recipe);
        if (!hasSpace(player, output)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("제작 결과물을 받을 인벤토리 공간이 필요합니다.", NamedTextColor.RED));
            return;
        }
        event.setCancelled(true);
        ItemStack[] matrix = event.getInventory().getMatrix();
        consume(matrix, recipe, player);
        event.getInventory().setMatrix(matrix);
        player.getInventory().addItem(output);
    }

    private void onCraftDeferredLegacy(CraftItemEvent event) {
        CraftingRecipeData recipe = recipe(event.getRecipe());
        if (recipe == null) return;
        if (!(event.getWhoClicked() instanceof Player player)
                || !isValid(player, event.getInventory().getMatrix(), recipe)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        UUID playerId = player.getUniqueId();
        if (!pendingCrafts.add(playerId)) return;
        CraftingInventory inventory = event.getInventory();
        ItemStack[] originalMatrix = cloneMatrix(inventory.getMatrix());
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory
                        || !isValid(player, originalMatrix, recipe)
                        || !sameMatrix(inventory.getMatrix(), originalMatrix)) return;
                ItemStack output = createOutput(player, recipe);
                if (!hasSpace(player, output)) {
                    player.sendMessage(Component.text("Not enough inventory space for the crafting result.", NamedTextColor.RED));
                    return;
                }
                ItemStack[] remaining = cloneMatrix(originalMatrix);
                consume(remaining, recipe, player);
                inventory.setMatrix(remaining);
                player.getInventory().addItem(output);
                player.updateInventory();
            } finally {
                pendingCrafts.remove(playerId);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCraft(CraftItemEvent event) {
        CraftingRecipeData recipe = recipe(event.getRecipe());
        if (recipe == null) return;
        if (!(event.getWhoClicked() instanceof Player player)
                || !isValid(player, event.getInventory().getMatrix(), recipe)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        UUID playerId = player.getUniqueId();
        if (!pendingCrafts.add(playerId)) return;
        CraftingInventory inventory = event.getInventory();
        boolean shiftClick = event.isShiftClick();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) return;

                // Bukkit/Paper may restore a cancelled craft matrix after the event. Validate
                // the matrix that actually exists at commit time, then consume exactly once.
                ItemStack[] working = cloneMatrix(inventory.getMatrix());
                if (!isValid(player, working, recipe)) return;

                if (shiftClick) {
                    ItemStack[] simulatedStorage = cloneMatrix(player.getInventory().getStorageContents());
                    List<ItemStack> outputs = new ArrayList<>();
                    for (int count = 0; count < 64 && isValid(player, working, recipe); count++) {
                        ItemStack output = createOutput(player, recipe);
                        if (output == null || !simulateAdd(simulatedStorage, output)) break;
                        consume(working, recipe, player);
                        outputs.add(output);
                    }
                    if (outputs.isEmpty()) {
                        player.sendMessage(Component.text("Not enough inventory space for the crafting result.", NamedTextColor.RED));
                        return;
                    }
                    inventory.setMatrix(working);
                    for (ItemStack output : outputs) player.getInventory().addItem(output);
                } else {
                    ItemStack output = createOutput(player, recipe);
                    ItemStack cursor = player.getItemOnCursor();
                    if (!canPlaceInCursor(cursor, output)) return;
                    consume(working, recipe, player);
                    inventory.setMatrix(working);
                    if (cursor == null || cursor.getType().isAir()) {
                        player.setItemOnCursor(output);
                    } else {
                        ItemStack updatedCursor = cursor.clone();
                        updatedCursor.setAmount(updatedCursor.getAmount() + output.getAmount());
                        player.setItemOnCursor(updatedCursor);
                    }
                }
                player.updateInventory();
            } finally {
                pendingCrafts.remove(playerId);
            }
        });
    }

    private CraftingRecipeData recipe(org.bukkit.inventory.Recipe bukkitRecipe) {
        if (!(bukkitRecipe instanceof Keyed keyed)) return null;
        return recipesByKey.get(keyed.getKey());
    }

    private RecipeChoice choiceFor(String ingredientId) {
        if (ingredientId.startsWith("vanilla:")) {
            Material material = Material.matchMaterial(ingredientId.substring("vanilla:".length()).toUpperCase(Locale.ROOT));
            return material == null ? null : new RecipeChoice.MaterialChoice(material);
        }
        ItemStack customItem = itemService.create(ingredientId, 1).orElse(null);
        return customItem == null ? null : new RecipeChoice.ExactChoice(customItem);
    }

    private int registeredChoiceCount(CraftingRecipeData.Ingredient ingredient) {
        if (ingredient.id().startsWith("vanilla:")) {
            Material material = Material.matchMaterial(ingredient.id().substring("vanilla:".length()).toUpperCase(Locale.ROOT));
            return material != null && material.getMaxStackSize() == 1 ? ingredient.amount() : 1;
        }
        ItemStack item = itemService.create(ingredient.id(), 1).orElse(null);
        return item != null && item.getMaxStackSize() == 1 ? ingredient.amount() : 1;
    }

    private ItemStack createOutput(Player player, CraftingRecipeData recipe) {
        ItemStack output = outputFactory.apply(recipe);
        if (output != null && player != null && soulbound.requiresBinding(output)) soulbound.bind(output, player.getUniqueId());
        return output;
    }

    private ItemStack createBaseOutput(CraftingRecipeData recipe) {
        if (recipe.outputId().startsWith("vanilla:")) {
            Material material = Material.matchMaterial(recipe.outputId().substring("vanilla:".length()).toUpperCase(Locale.ROOT));
            ItemStack output = material == null ? null : new ItemStack(material, recipe.outputAmount());
            return vanillaStacking == null ? output : vanillaStacking.normalize(output);
        }
        return itemService.create(recipe.outputId(), recipe.outputAmount()).orElse(null);
    }

    private boolean isValid(Player player, ItemStack[] matrix, CraftingRecipeData recipe) {
        if (player == null || matrix == null) return false;
        Map<Integer, Integer> remaining = new HashMap<>();
        for (int i = 0; i < recipe.ingredients().size(); i++) remaining.put(i, recipe.ingredients().get(i).amount());
        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) continue;
            int matched = -1;
            for (int i = 0; i < recipe.ingredients().size(); i++) {
                if (remaining.get(i) <= 0) continue;
                if (matches(item, recipe.ingredients().get(i).id(), player)) {
                    matched = i;
                    break;
                }
            }
            if (matched < 0) return false;
            // One stack may contain more than the required amount. Only the exact required amount is consumed.
            remaining.put(matched, remaining.get(matched) - item.getAmount());
        }
        return remaining.values().stream().allMatch(value -> value <= 0);
    }

    private void consume(ItemStack[] matrix, CraftingRecipeData recipe, Player player) {
        for (CraftingRecipeData.Ingredient ingredient : recipe.ingredients()) {
            int amount = ingredient.amount();
            for (int slot = 0; slot < matrix.length && amount > 0; slot++) {
                ItemStack item = matrix[slot];
                if (!matches(item, ingredient.id(), player)) continue;
                int used = Math.min(amount, item.getAmount());
                item.setAmount(item.getAmount() - used);
                if (item.getAmount() <= 0) matrix[slot] = null;
                amount -= used;
            }
        }
    }

    private ItemStack[] cloneMatrix(ItemStack[] matrix) {
        ItemStack[] copy = new ItemStack[matrix == null ? 0 : matrix.length];
        if (matrix == null) return copy;
        for (int index = 0; index < matrix.length; index++) {
            copy[index] = matrix[index] == null ? null : matrix[index].clone();
        }
        return copy;
    }

    private boolean sameMatrix(ItemStack[] left, ItemStack[] right) {
        if (left == null || right == null || left.length != right.length) return false;
        for (int index = 0; index < left.length; index++) {
            ItemStack first = left[index];
            ItemStack second = right[index];
            if (first == null || first.getType().isAir()) {
                if (second != null && !second.getType().isAir()) return false;
                continue;
            }
            if (second == null || second.getType().isAir()
                    || first.getAmount() != second.getAmount()
                    || !first.isSimilar(second)) return false;
        }
        return true;
    }

    private boolean canPlaceInCursor(ItemStack cursor, ItemStack output) {
        if (output == null || output.getType().isAir()) return false;
        if (cursor == null || cursor.getType().isAir()) return true;
        return cursor.isSimilar(output)
                && cursor.getAmount() + output.getAmount() <= cursor.getMaxStackSize();
    }

    private boolean simulateAdd(ItemStack[] storage, ItemStack output) {
        int remaining = output.getAmount();
        for (ItemStack item : storage) {
            if (item == null || item.getType().isAir() || !item.isSimilar(output)) continue;
            int room = item.getMaxStackSize() - item.getAmount();
            int used = Math.min(room, remaining);
            item.setAmount(item.getAmount() + used);
            remaining -= used;
            if (remaining <= 0) return true;
        }
        for (int index = 0; index < storage.length && remaining > 0; index++) {
            ItemStack item = storage[index];
            if (item != null && !item.getType().isAir()) continue;
            int amount = Math.min(output.getMaxStackSize(), remaining);
            ItemStack placed = output.clone();
            placed.setAmount(amount);
            storage[index] = placed;
            remaining -= amount;
        }
        return remaining <= 0;
    }

    private boolean matches(ItemStack item, String ingredientId, Player player) {
        if (item == null || item.getType().isAir()) return false;
        if (ingredientId.startsWith("vanilla:")) {
            if (itemService.getItemId(item).isPresent()) return false;
            Material material = Material.matchMaterial(ingredientId.substring("vanilla:".length()).toUpperCase(Locale.ROOT));
            return material == item.getType();
        }
        return itemService.isItem(item, ingredientId)
                && (!soulbound.isSoulbound(item) || soulbound.isOwnedBy(item, player));
    }

    private boolean hasSpace(Player player, ItemStack output) {
        if (output == null) return false;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) return true;
            if (item.isSimilar(output) && item.getAmount() < item.getMaxStackSize()) return true;
        }
        return false;
    }
}
