package com.hyunseo.hyunseorpg.enhancement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/** Handles only the informational growth shell; vanilla anvils own all enhancement transactions. */
public final class AnvilGrowthListener implements Listener {
    public AnvilGrowthListener(org.bukkit.plugin.java.JavaPlugin plugin, EquipmentGrowthGuiService guiService) { }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EquipmentGrowthMenuHolder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != top) return;
        if (event.getRawSlot() == 11) player.sendMessage(Component.text("강화는 바닐라 모루에서 진행합니다.", NamedTextColor.YELLOW));
        else if (event.getRawSlot() == 15) player.sendMessage(Component.text("인챈트는 Minecraft 기본 작업대를 사용합니다.", NamedTextColor.YELLOW));
        else if (event.getRawSlot() == EquipmentGrowthGuiService.CLOSE_SLOT) player.closeInventory();
    }
}
