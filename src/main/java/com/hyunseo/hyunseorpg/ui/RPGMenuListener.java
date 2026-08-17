package com.hyunseo.hyunseorpg.ui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class RPGMenuListener implements Listener {
    private final RPGMenuService menuService;

    public RPGMenuListener(RPGMenuService menuService) {
        this.menuService = menuService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        event.setCancelled(true);
        menuService.openMain(event.getPlayer());
        event.getPlayer().sendActionBar(Component.text("Shift + F: 메뉴 열기 | 퀘스트 확인", NamedTextColor.AQUA));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().sendActionBar(Component.text("Shift + F: 메뉴 열기 | 퀘스트 확인", NamedTextColor.AQUA));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof RPGMenuHolder holder)) return;
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == 49) {
            switch (holder.view()) {
                case MAIN -> player.closeInventory();
                default -> menuService.back(player);
            }
            return;
        }
        if (holder.view() == RPGMenuHolder.View.MAIN) {
            menuService.clickMain(player, slot);
        } else if (holder.view() == RPGMenuHolder.View.SHOP_LIST) {
            String shopId = holder.actions().get(slot);
            if (shopId != null && event.isLeftClick()) menuService.clickShop(player, shopId);
        } else if (holder.view() == RPGMenuHolder.View.GROWTH) {
            if (event.isLeftClick()) menuService.clickGrowth(player, slot);
        } else if (holder.view() == RPGMenuHolder.View.ENCHANT_SUPPORT) {
            if (event.isLeftClick()) menuService.clickEnchantSupport(player, slot);
        } else if (holder.view() == RPGMenuHolder.View.BOSS) {
            if (event.isLeftClick()) menuService.clickBoss(player, holder.actions().get(slot));
        } else if (holder.view() == RPGMenuHolder.View.QUESTS) {
            if (event.isLeftClick() || (event.isShiftClick() && event.isRightClick())) {
                menuService.clickQuest(player, holder.actions().get(slot), event.isShiftClick() && event.isRightClick());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof RPGMenuHolder)) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) event.setCancelled(true);
    }
}
