package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Dedicated multi-input cooking workstation. */
public final class CookingGuiService implements Listener {
    private static final List<Integer> INPUT_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34);
    private static final Set<Integer> INPUT_SLOT_SET = Set.copyOf(INPUT_SLOTS);
    private static final int RESULT_SLOT = 40;
    private static final int EXECUTE_SLOT = 42;
    private static final int BACK_SLOT = 45;
    private static final int CLOSE_SLOT = 49;

    private final JavaPlugin plugin;
    private final CookingRegistry cooking;
    private final InventoryDeliveryService delivery;
    private final Set<UUID> processing = ConcurrentHashMap.newKeySet();
    private Consumer<Player> mainMenuOpener = Player::closeInventory;

    public CookingGuiService(JavaPlugin plugin, CookingRegistry cooking,
                             InventoryDeliveryService delivery) {
        this.plugin = plugin;
        this.cooking = cooking;
        this.delivery = delivery;
    }

    public void setMainMenuOpener(Consumer<Player> opener) {
        this.mainMenuOpener = opener == null ? Player::closeInventory : opener;
    }

    public void openRecipeList(Player player) {
        CookingMenuHolder holder = new CookingMenuHolder(player.getUniqueId(), "", true);
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text("요리"));
        holder.setInventory(inventory);
        for (int index = 0; index < cooking.getAll().size() && index < 45; index++) {
            CookingRecipe recipe = cooking.getAll().get(index);
            inventory.setItem(index, recipeIcon(recipe));
        }
        inventory.setItem(BACK_SLOT, icon(Material.ARROW, "뒤로", List.of()));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "닫기", List.of()));
        player.openInventory(inventory);
    }

    public void openRecipe(Player player, String recipeId) {
        if (cooking.recipe(recipeId).isEmpty()) {
            openRecipeList(player);
            return;
        }
        CookingMenuHolder holder = new CookingMenuHolder(player.getUniqueId(), recipeId, false);
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text("요리 조합"));
        holder.setInventory(inventory);
        renderWorkstation(inventory);
        player.openInventory(inventory);
    }

    public void returnOpenInputs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof CookingMenuHolder holder
                    && !holder.recipeList()) {
                returnInputs(player, player.getOpenInventory().getTopInventory());
                player.closeInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CookingMenuHolder holder)) return;
        if (!holder.playerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (holder.recipeList()) {
            event.setCancelled(true);
            if (event.getClickedInventory() != top) return;
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < 45 && event.isLeftClick()) {
                List<CookingRecipe> recipes = cooking.getAll();
                if (slot < recipes.size()) openRecipe(player, recipes.get(slot).id());
            } else if (slot == BACK_SLOT) {
                mainMenuOpener.accept(player);
            } else if (slot == CLOSE_SLOT) {
                player.closeInventory();
            }
            return;
        }

        if (unsafe(event.getClick()) || event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == top) {
            int slot = event.getRawSlot();
            if (INPUT_SLOT_SET.contains(slot)) {
                refreshLater(top);
                return;
            }
            event.setCancelled(true);
            if (slot == EXECUTE_SLOT) execute(player, top, holder);
            else if (slot == BACK_SLOT) {
                returnInputs(player, top);
                openRecipeList(player);
            } else if (slot == CLOSE_SLOT) {
                player.closeInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CookingMenuHolder holder) || holder.recipeList()) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize() && !INPUT_SLOT_SET.contains(slot))) {
            event.setCancelled(true);
            return;
        }
        refreshLater(top);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)
                || !(event.getInventory().getHolder() instanceof CookingMenuHolder holder)
                || holder.recipeList()) return;
        returnInputs(player, event.getInventory());
    }

    private void execute(Player player, Inventory inventory, CookingMenuHolder holder) {
        if (!processing.add(player.getUniqueId())) return;
        ItemStack[] original = inputContents(inventory);
        try {
            CookingRecipe recipe = cooking.recipe(holder.recipeId()).orElse(null);
            if (recipe == null || !cooking.canCraft(recipe, original)) {
                player.sendMessage(Component.text("요리 재료가 부족하거나 올바르지 않습니다.", NamedTextColor.RED));
                return;
            }
            ItemStack[] committed = cloneContents(original);
            cooking.consumeOne(recipe, committed);
            ItemStack output = cooking.createOutputForInputs(recipe.id(), original).orElse(null);
            if (output == null || output.getType().isAir()) {
                player.sendMessage(Component.text("요리 결과를 생성할 수 없습니다.", NamedTextColor.RED));
                return;
            }
            for (int slot = 0; slot < INPUT_SLOTS.size(); slot++) {
                inventory.setItem(INPUT_SLOTS.get(slot), committed[slot]);
            }
            delivery.giveOrDiscard(player, output);
            player.sendMessage(Component.text("요리 완료: " + output.getAmount() + "개", NamedTextColor.GREEN));
        } catch (RuntimeException exception) {
            for (int slot = 0; slot < INPUT_SLOTS.size(); slot++) {
                inventory.setItem(INPUT_SLOTS.get(slot), original[slot]);
            }
            player.sendMessage(Component.text("요리 처리에 실패하여 재료를 보존했습니다.", NamedTextColor.RED));
            plugin.getLogger().warning("Cooking transaction failed for " + holder.recipeId() + ": " + exception.getMessage());
        } finally {
            renderWorkstation(inventory);
            processing.remove(player.getUniqueId());
        }
    }

    private void renderWorkstation(Inventory inventory) {
        ItemStack[] inputs = inputContents(inventory);
        fill(inventory);
        for (int index = 0; index < INPUT_SLOTS.size(); index++) {
            inventory.setItem(INPUT_SLOTS.get(index), inputs[index]);
        }
        CookingMenuHolder holder = (CookingMenuHolder) inventory.getHolder();
        CookingRecipe recipe = cooking.recipe(holder.recipeId()).orElse(null);
        if (recipe == null) return;
        List<String> info = new ArrayList<>();
        info.add("재료를 입력 슬롯에 올려 조합합니다.");
        for (var entry : recipe.ingredients().entrySet()) {
            info.add(entry.getKey() + ": " + cooking.countIngredient(inputs, entry.getKey()) + "/" + entry.getValue());
        }
        inventory.setItem(4, icon(Material.BOOK, "요리 정보", info));
        cooking.createOutputForInputs(recipe.id(), inputs).ifPresent(output -> inventory.setItem(RESULT_SLOT, output));
        inventory.setItem(EXECUTE_SLOT, icon(Material.LIME_DYE, "요리 실행", List.of("현재 입력 재료를 1회 소비합니다.")));
        inventory.setItem(BACK_SLOT, icon(Material.ARROW, "요리 목록", List.of()));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "닫기", List.of()));
    }

    private ItemStack recipeIcon(CookingRecipe recipe) {
        ItemStack icon = cooking.createOutputForInputs(recipe.id(), new ItemStack[0])
                .orElseGet(() -> new ItemStack(Material.BOWL));
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) return icon;
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(Component.text("클릭하여 재료 입력", NamedTextColor.YELLOW));
        lore.addAll(recipe.ingredients().entrySet().stream()
                .map(entry -> Component.text(entry.getKey() + " x" + entry.getValue(), NamedTextColor.GRAY))
                .toList());
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private void returnInputs(Player player, Inventory inventory) {
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            inventory.setItem(slot, null);
            delivery.giveOrDiscard(player, item.clone());
        }
    }

    private void refreshLater(Inventory inventory) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (inventory.getHolder() instanceof CookingMenuHolder holder && !holder.recipeList()) {
                renderWorkstation(inventory);
            }
        });
    }

    private ItemStack[] inputContents(Inventory inventory) {
        ItemStack[] result = new ItemStack[INPUT_SLOTS.size()];
        for (int index = 0; index < INPUT_SLOTS.size(); index++) {
            ItemStack item = inventory.getItem(INPUT_SLOTS.get(index));
            result[index] = item == null ? null : item.clone();
        }
        return result;
    }

    private ItemStack[] cloneContents(ItemStack[] source) {
        ItemStack[] result = new ItemStack[source.length];
        for (int index = 0; index < source.length; index++) {
            result[index] = source[index] == null ? null : source[index].clone();
        }
        return result;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        for (int slot : INPUT_SLOTS) inventory.setItem(slot, null);
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> Component.text(line, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private boolean unsafe(ClickType click) {
        return click == ClickType.NUMBER_KEY || click == ClickType.DOUBLE_CLICK
                || click == ClickType.CREATIVE || click == ClickType.DROP
                || click == ClickType.CONTROL_DROP || click == ClickType.SWAP_OFFHAND;
    }
}
