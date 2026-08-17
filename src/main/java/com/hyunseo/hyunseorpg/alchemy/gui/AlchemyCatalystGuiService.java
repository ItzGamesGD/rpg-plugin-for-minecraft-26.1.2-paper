package com.hyunseo.hyunseorpg.alchemy.gui;

import com.hyunseo.hyunseorpg.alchemy.catalyst.CatalystApplicationService;
import com.hyunseo.hyunseorpg.alchemy.catalyst.CatalystRegistry;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Holder-owned catalyst input screen. The service owns input return and commit state. */
public final class AlchemyCatalystGuiService implements Listener {
    public static final int POTION_SLOT = 10;
    public static final int CATALYST_SLOT = 14;
    public static final int APPLY_SLOT = 13;
    public static final int BACK_SLOT = 22;
    private final JavaPlugin plugin;
    private final CatalystApplicationService application;
    private final CatalystRegistry catalysts;
    private final InventoryDeliveryService delivery;
    private final RPGItemService items;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public AlchemyCatalystGuiService(JavaPlugin plugin, CatalystApplicationService application,
                                     CatalystRegistry catalysts, InventoryDeliveryService delivery,
                                     RPGItemService items) {
        this.plugin = plugin;
        this.application = application;
        this.catalysts = catalysts;
        this.delivery = delivery;
        this.items = items;
    }

    public void open(Player player, Consumer<Player> backOpener) {
        if (player == null || !player.isOnline()) return;
        closeSession(player);
        UUID id = UUID.randomUUID();
        AlchemyCatalystMenuHolder holder = new AlchemyCatalystMenuHolder(id);
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("양조 - 촉매 적용"));
        holder.inventory(inventory);
        fill(inventory);
        inventory.setItem(POTION_SLOT, null);
        inventory.setItem(CATALYST_SLOT, null);
        inventory.setItem(APPLY_SLOT, icon(Material.LIME_DYE, "촉매 적용", List.of("좌클릭: 원자적으로 적용")));
        inventory.setItem(BACK_SLOT, icon(Material.ARROW, "뒤로", List.of()));
        sessions.put(player.getUniqueId(), new Session(id, backOpener));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof AlchemyCatalystMenuHolder holder)) return;
        event.setCancelled(true);
        Session session = sessions.get(player.getUniqueId());
        if (session == null || !session.id().equals(holder.sessionId())) return;
        int raw = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
        if (raw == POTION_SLOT || raw == CATALYST_SLOT) {
            moveCursor(event, event.getView().getTopInventory(), raw);
            return;
        }
        if (raw >= topSize) {
            moveBottomItem(event, event.getView().getTopInventory());
            return;
        }
        if (raw == APPLY_SLOT) apply(player, event.getView().getTopInventory());
        else if (raw == BACK_SLOT) {
            player.closeInventory();
            if (session.backOpener() != null) Bukkit.getScheduler().runTask(plugin, () -> session.backOpener().accept(player));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AlchemyCatalystMenuHolder) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(InventoryMoveItemEvent event) {
        if (event.getDestination().getHolder() instanceof AlchemyCatalystMenuHolder
                || event.getSource().getHolder() instanceof AlchemyCatalystMenuHolder) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) { closeSession(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)
                || !(event.getInventory().getHolder() instanceof AlchemyCatalystMenuHolder holder)) return;
        Session session = sessions.get(player.getUniqueId());
        if (session == null || !session.id().equals(holder.sessionId()) || session.closed()) return;
        session.closed(true);
        sessions.remove(player.getUniqueId(), session);
        returnInput(player, event.getInventory());
    }

    public synchronized void closeAll() {
        for (Map.Entry<UUID, Session> entry : new HashMap<>(sessions).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.getOpenInventory().getTopInventory().getHolder() instanceof AlchemyCatalystMenuHolder holder
                    && holder.sessionId().equals(entry.getValue().id())) {
                returnInput(player, player.getOpenInventory().getTopInventory());
                player.closeInventory();
            }
            entry.getValue().closed(true);
        }
        sessions.clear();
    }

    private void apply(Player player, Inventory inventory) {
        ItemStack source = inventory.getItem(POTION_SLOT);
        ItemStack catalyst = inventory.getItem(CATALYST_SLOT);
        CatalystApplicationService.Result result = application.transform(source, catalyst);
        if (!result.applied()) {
            player.sendMessage(Component.text("촉매를 적용할 수 없습니다: " + result.status(), NamedTextColor.RED));
            return;
        }
        if (!canFit(player, result.convertedItem())) {
            player.sendMessage(Component.text("변환된 포션을 받을 공간이 없습니다.", NamedTextColor.RED));
            return;
        }
        ItemStack sourceBackup = source.clone();
        ItemStack catalystBackup = catalyst.clone();
        try {
            decrement(inventory, POTION_SLOT);
            decrement(inventory, CATALYST_SLOT);
            if (!delivery.giveExactly(player, result.convertedItem().clone())) {
                inventory.setItem(POTION_SLOT, sourceBackup);
                inventory.setItem(CATALYST_SLOT, catalystBackup);
                player.sendMessage(Component.text("변환 결과 지급에 실패하여 입력을 복구했습니다.", NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("촉매 적용이 완료되었습니다.", NamedTextColor.GREEN));
        } catch (RuntimeException failure) {
            inventory.setItem(POTION_SLOT, sourceBackup);
            inventory.setItem(CATALYST_SLOT, catalystBackup);
            player.sendMessage(Component.text("촉매 적용에 실패하여 입력을 복구했습니다.", NamedTextColor.RED));
        }
    }

    private void moveCursor(InventoryClickEvent event, Inventory inventory, int slot) {
        ItemStack slotItem = inventory.getItem(slot);
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            event.setCursor(slotItem == null ? null : slotItem.clone());
            inventory.setItem(slot, null);
            return;
        }
        if (slotItem != null && !slotItem.isSimilar(cursor)) return;
        inventory.setItem(slot, cursor.clone());
        event.setCursor(slotItem == null ? null : slotItem.clone());
    }

    private void moveBottomItem(InventoryClickEvent event, Inventory inventory) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        int target = items.getItemId(clicked).isPresent() ? POTION_SLOT : CATALYST_SLOT;
        if (target == POTION_SLOT && items.getItemId(clicked).map(id -> !id.startsWith("potion_")).orElse(true)) {
            target = CATALYST_SLOT;
        }
        if (target == CATALYST_SLOT && catalysts.findByItem(clicked).isEmpty()) return;
        ItemStack existing = inventory.getItem(target);
        if (existing != null && !existing.isSimilar(clicked)) return;
        if (existing == null) {
            inventory.setItem(target, clicked.clone());
            clicked.setAmount(0);
        } else {
            int room = existing.getMaxStackSize() - existing.getAmount();
            int moved = Math.min(room, clicked.getAmount());
            if (moved <= 0) return;
            existing.setAmount(existing.getAmount() + moved);
            clicked.setAmount(clicked.getAmount() - moved);
        }
    }

    private void returnInput(Player player, Inventory inventory) {
        for (int slot : new int[]{POTION_SLOT, CATALYST_SLOT}) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) delivery.giveOrDiscard(player, item.clone());
            inventory.setItem(slot, null);
        }
    }

    private boolean canFit(Player player, ItemStack output) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int i = 0; i < storage.length; i++) storage[i] = storage[i] == null ? null : storage[i].clone();
        int remaining = output.getAmount();
        for (ItemStack item : storage) {
            if (item == null || !item.isSimilar(output)) continue;
            int room = item.getMaxStackSize() - item.getAmount();
            int moved = Math.min(room, remaining);
            item.setAmount(item.getAmount() + moved);
            remaining -= moved;
            if (remaining == 0) return true;
        }
        for (int i = 0; i < storage.length && remaining > 0; i++) {
            if (storage[i] != null && !storage[i].getType().isAir()) continue;
            storage[i] = output.clone();
            storage[i].setAmount(Math.min(output.getMaxStackSize(), remaining));
            remaining -= storage[i].getAmount();
        }
        return remaining == 0;
    }

    private void decrement(Inventory inventory, int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null) throw new IllegalStateException("missing input");
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) inventory.setItem(slot, null);
    }

    private void closeSession(Player player) {
        Session previous = sessions.remove(player.getUniqueId());
        if (previous == null) return;
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof AlchemyCatalystMenuHolder holder
                && holder.sessionId().equals(previous.id())) {
            returnInput(player, player.getOpenInventory().getTopInventory());
            player.closeInventory();
        }
        previous.closed(true);
    }

    private void fill(Inventory inventory) {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text(name));
        meta.lore(lore.stream().map(Component::text).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static final class Session {
        private final UUID id;
        private final Consumer<Player> backOpener;
        private boolean closed;
        private Session(UUID id, Consumer<Player> backOpener) { this.id = id; this.backOpener = backOpener; }
        private UUID id() { return id; }
        private Consumer<Player> backOpener() { return backOpener; }
        private boolean closed() { return closed; }
        private void closed(boolean value) { closed = value; }
    }
}
