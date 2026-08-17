package com.hyunseo.hyunseorpg.shop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** Protects player shop views while leaving normal player-inventory actions available. */
public final class ShopGuiListener implements Listener {
    private final ShopGuiService guiService;

    public ShopGuiListener(ShopGuiService guiService) {
        this.guiService = guiService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof ShopInventoryHolder holder) {
            handlePlayerShopClick(event, player, holder, top);
        } else if (top.getHolder() instanceof ShopAdminInventoryHolder holder) {
            handleAdminShopClick(event, player, holder, top);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize());
        if (top.getHolder() instanceof ShopInventoryHolder && touchesTop) {
            event.setCancelled(true);
        }
        if (top.getHolder() instanceof ShopAdminInventoryHolder && event.getRawSlots().stream()
                .anyMatch(slot -> slot >= ShopGuiService.PREVIOUS_SLOT && slot < top.getSize())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player
                && event.getInventory().getHolder() instanceof ShopAdminInventoryHolder holder) {
            guiService.closeAdmin(player, holder);
        }
    }

    private void handlePlayerShopClick(InventoryClickEvent event, Player player, ShopInventoryHolder holder, Inventory top) {
        if (isUnsafeTransfer(event.getClick())) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() != top) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == ShopGuiService.PREVIOUS_SLOT) {
            guiService.navigateShop(player, holder, -1);
        } else if (slot == ShopGuiService.NEXT_SLOT) {
            guiService.navigateShop(player, holder, 1);
        } else if (slot >= 0 && slot < ShopGuiService.PAGE_SIZE && (event.isLeftClick() || event.isRightClick())) {
            guiService.transact(player, holder, slot, event.isLeftClick(), event.isShiftClick());
        }
    }

    private void handleAdminShopClick(InventoryClickEvent event, Player player, ShopAdminInventoryHolder holder, Inventory top) {
        if (event.isShiftClick() || isUnsafeTransfer(event.getClick())) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() != top) return;
        int slot = event.getRawSlot();
        if (slot >= ShopGuiService.PREVIOUS_SLOT) {
            event.setCancelled(true);
            if (slot == ShopGuiService.PREVIOUS_SLOT) guiService.navigateAdmin(player, holder, -1);
            if (slot == ShopGuiService.NEXT_SLOT) guiService.navigateAdmin(player, holder, 1);
        }
    }

    private boolean isUnsafeTransfer(ClickType click) {
        return click == ClickType.NUMBER_KEY || click == ClickType.DOUBLE_CLICK || click == ClickType.CREATIVE
                || click == ClickType.DROP || click == ClickType.CONTROL_DROP || click == ClickType.SWAP_OFFHAND;
    }
}
