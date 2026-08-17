package com.hyunseo.hyunseorpg.economy;

import org.bukkit.Material;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;

/** Blocks economy items from vanilla boss/crafting paths. */
public final class EconomySafetyListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWitherDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Wither)) return;
        event.getDrops().removeIf(item -> item != null && item.getType() == Material.NETHER_STAR);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getInventory().getResult() != null
                && event.getInventory().getResult().getType() == Material.BEACON) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCraft(CraftItemEvent event) {
        if (event.getRecipe() != null && event.getRecipe().getResult().getType() == Material.BEACON) {
            event.setCancelled(true);
        }
    }
}
