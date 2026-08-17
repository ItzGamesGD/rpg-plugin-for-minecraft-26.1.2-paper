package com.hyunseo.hyunseorpg.enchant;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Repairs generated enchant lore from stable PDC IDs when an existing player joins. */
public final class EnchantLoreRefreshListener implements Listener {
    private final JavaPlugin plugin;
    private final EnchantService enchants;

    public EnchantLoreRefreshListener(JavaPlugin plugin, EnchantService enchants) {
        this.plugin = plugin;
        this.enchants = enchants;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!event.getPlayer().isOnline()) return;
            for (ItemStack item : event.getPlayer().getInventory().getContents()) refresh(item);
            for (ItemStack item : event.getPlayer().getInventory().getArmorContents()) refresh(item);
            refresh(event.getPlayer().getInventory().getItemInOffHand());
        });
    }

    private void refresh(ItemStack item) {
        enchants.refreshEquippedLore(item);
        enchants.refreshBookLore(item);
    }
}
