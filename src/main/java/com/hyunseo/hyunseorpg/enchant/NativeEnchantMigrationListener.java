package com.hyunseo.hyunseorpg.enchant;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.ItemStack;

/** Lazily normalizes legacy equipment/books at the vanilla workstations that consume them. */
public final class NativeEnchantMigrationListener implements Listener {
    private final EnchantService enchants;

    public NativeEnchantMigrationListener(EnchantService enchants) {
        this.enchants = enchants;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        normalize(event.getInventory().getItem(0));
        normalize(event.getInventory().getItem(1));
        normalize(event.getResult());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGrindstonePrepare(PrepareGrindstoneEvent event) {
        ItemStack result = event.getResult();
        normalize(result);
        event.setResult(result);
    }

    private void normalize(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        enchants.refreshEquippedLore(item);
        enchants.refreshBookLore(item);
    }
}
