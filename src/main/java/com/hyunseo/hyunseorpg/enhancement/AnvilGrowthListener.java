package com.hyunseo.hyunseorpg.enhancement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;


public final class AnvilGrowthListener implements Listener {
    private final EquipmentGrowthGuiService guiService;

    public AnvilGrowthListener(JavaPlugin plugin, EquipmentGrowthGuiService guiService) {
        this.guiService = guiService;
    }

    // Vanilla anvil interaction is deliberately not handled here. The growth UI remains
    // reachable through the existing RPG menu and this listener only owns its inventories.

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof EquipmentGrowthMenuHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != top) return;
            if (event.getRawSlot() == 11) player.sendMessage(Component.text("강화는 바닐라 모루에서 진행합니다.", NamedTextColor.YELLOW));
            else if (event.getRawSlot() == 17) guiService.openRepair(player);
            else if (event.getRawSlot() == 15) player.sendMessage(Component.text("인챈트는 인챈팅 테이블과 바닐라 모루를 사용합니다.", NamedTextColor.YELLOW));
            else if (event.getRawSlot() == 24 && guiService.getSupportService() != null) guiService.getSupportService().openFuture(player);
            else if (event.getRawSlot() == EquipmentGrowthGuiService.CLOSE_SLOT) player.closeInventory();
            return;
        }
        boolean repair = top.getHolder() instanceof RepairInventoryHolder;
        if (!repair) return;
        if (unsafe(event.getClick()) || event.isShiftClick()) { event.setCancelled(true); return; }
        if (event.getClickedInventory() != top) return;
        int slot = event.getRawSlot();
        if (repair && slot == EquipmentGrowthGuiService.STONE_SLOT) {
            event.setCancelled(true);
            return;
        }
        if (guiService.isInputSlot(slot)) { guiService.refreshLater(top); return; }
        event.setCancelled(true);
        if (slot == EquipmentGrowthGuiService.EXECUTE_SLOT) {
            guiService.repair(player, top);
        } else if (slot == EquipmentGrowthGuiService.BACK_SLOT) {
            guiService.returnInputs(player, top);
            guiService.openMain(player);
        } else if (slot == EquipmentGrowthGuiService.CLOSE_SLOT) player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof RepairInventoryHolder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize() && !guiService.isInputSlot(slot))) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player
                && event.getInventory().getHolder() instanceof RepairInventoryHolder) {
            guiService.returnInputs(player, event.getInventory());
        }
    }

    private boolean unsafe(ClickType click) {
        return click == ClickType.NUMBER_KEY || click == ClickType.DOUBLE_CLICK || click == ClickType.CREATIVE || click == ClickType.DROP || click == ClickType.CONTROL_DROP || click == ClickType.SWAP_OFFHAND;
    }
}
