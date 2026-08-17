package com.hyunseo.hyunseorpg.alchemy.gui;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.crafting.CraftingGuiService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.function.Consumer;

/** Session-only controller; recipe execution remains in the transaction/service layer. */
public final class AlchemyGuiControllerService implements AlchemyGuiController<AlchemyGuiSessionImpl>, Listener {
    private final Map<UUID, AlchemyGuiSessionImpl> sessions = new HashMap<>();
    private final ConfigService config;
    private final CraftingGuiService crafting;
    private Consumer<Player> mainMenuOpener = Player::closeInventory;
    private Consumer<Player> catalystOpener = Player::closeInventory;

    public AlchemyGuiControllerService() { this(null, null); }
    public AlchemyGuiControllerService(ConfigService config, CraftingGuiService crafting) {
        this.config = config;
        this.crafting = crafting;
    }

    public void setMainMenuOpener(Consumer<Player> opener) {
        this.mainMenuOpener = opener == null ? Player::closeInventory : opener;
    }

    public void setCatalystOpener(Consumer<Player> opener) {
        this.catalystOpener = opener == null ? Player::closeInventory : opener;
    }

    public boolean enabled() {
        return config != null && config.getAlchemyGuiBoolean("enabled", false);
    }

    /** Opens the actual alchemy hub only when the GUI configuration is enabled. */
    public boolean openInventory(Player player) {
        if (player == null || !player.isOnline()) return false;
        if (!enabled()) {
            player.sendMessage(Component.text("양조 메뉴는 현재 비활성화되어 있습니다."));
            return false;
        }
        AlchemyGuiSessionImpl session = open(player.getUniqueId());
        AlchemyMenuHolder holder = new AlchemyMenuHolder(session.sessionId());
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("양조"));
        holder.inventory(inventory);
        fill(inventory);
        inventory.setItem(11, icon(Material.AMETHYST_SHARD, "풍요의 정수 제작", List.of(
                "기존 CraftingTransactionService 사용", "클릭하여 제작 메뉴 열기")));
        inventory.setItem(13, icon(Material.POTION, "물약 제작", List.of(
                "활성화된 양조 물약 레시피", "제작 레시피가 없으면 준비 중으로 표시")));
        inventory.setItem(15, icon(Material.REDSTONE, "촉매 적용", List.of(
                "완성 물약의 상태 변화", "활성화된 촉매 레시피만 사용")));
        inventory.setItem(22, icon(Material.ARROW, "메인 메뉴", List.of()));
        inventory.setItem(24, icon(Material.BARRIER, "닫기", List.of()));
        player.openInventory(inventory);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof AlchemyMenuHolder holder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()
                || event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()
                || !sessionMatches(player, holder)) return;
        if (!event.isLeftClick()) return;
        switch (event.getRawSlot()) {
            case 11 -> { if (crafting != null) crafting.openAlchemyEssences(player, this::openInventory); }
            case 13 -> { if (crafting != null) crafting.openAlchemyPotions(player, this::openInventory); }
            case 15 -> catalystOpener.accept(player);
            case 22 -> mainMenuOpener.accept(player);
            case 24 -> player.closeInventory();
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AlchemyMenuHolder) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AlchemyMenuHolder holder
                && event.getPlayer() instanceof Player player && sessionMatches(player, holder)) {
            close(player.getUniqueId(), CloseReason.PLAYER_CLOSE);
        }
    }

    private boolean sessionMatches(Player player, AlchemyMenuHolder holder) {
        AlchemyGuiSessionImpl current = sessions.get(player.getUniqueId());
        return current != null && current.sessionId().equals(holder.sessionId());
    }

    private void fill(Inventory inventory) {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
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

    @Override public synchronized AlchemyGuiSessionImpl open(UUID playerId) {
        AlchemyGuiSessionImpl previous = sessions.remove(playerId);
        if (previous != null) previous.close();
        AlchemyGuiSessionImpl session = new AlchemyGuiSessionImpl(playerId);
        sessions.put(playerId, session);
        return session;
    }

    @Override public synchronized void close(UUID playerId, CloseReason reason) {
        AlchemyGuiSessionImpl session = sessions.remove(playerId);
        if (session != null) session.close();
    }

    @Override public synchronized ClickResult handleClick(AlchemyGuiSessionImpl session, ClickAction action) {
        if (session == null || session.state() == AlchemyGuiSession.SessionState.CLOSED) return ClickResult.SESSION_CLOSED;
        if (session.state() == AlchemyGuiSession.SessionState.LOCKED) return ClickResult.TRANSACTION_LOCKED;
        return switch (action) {
            case SHIFT, NUMBER_KEY, DRAG, DOUBLE_CLICK, COLLECT_TO_CURSOR, HOPPER -> ClickResult.BLOCKED;
            case NORMAL -> ClickResult.ACCEPTED;
        };
    }

    @Override public synchronized void returnUncommitted(AlchemyGuiSessionImpl session) {
        if (session == null || session.state() == AlchemyGuiSession.SessionState.CLOSED) return;
        session.close();
        sessions.remove(session.playerId(), session);
    }

    public synchronized void closeAll(CloseReason reason) {
        sessions.values().forEach(AlchemyGuiSessionImpl::close);
        sessions.clear();
    }
}
