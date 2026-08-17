package com.hyunseo.hyunseorpg.item;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Prevents custom item identities from silently using vanilla throw/place behavior. */
public final class CustomItemVanillaActionBlockListener implements Listener {
    private final RPGItemService itemService;
    public CustomItemVanillaActionBlockListener(RPGItemService itemService) { this.itemService = itemService; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!isBlockedCustomItem(item)) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (pearl.getShooter() instanceof Player player && isBlockedCustomItem(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            pearl.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isBlockedCustomItem(event.getItemInHand())) event.setCancelled(true);
    }

    private boolean isBlockedCustomItem(ItemStack item) {
        if (item == null || item.getType().isAir() || itemService.getItemId(item).isEmpty()) return false;
        Material material = item.getType();
        return material == Material.ENDER_PEARL || material.isBlock();
    }
}
