package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.alchemy.potion.PaperPotionPdcContract;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Blocks only custom alchemy items from vanilla brewing/automation paths.
 * Vanilla brewing remains available when no HyunseoRPG potion is present.
 */
public final class AlchemyVanillaBypassListener implements Listener {
    private final PaperPotionPdcContract pdc;
    private final AlchemyAuditLog audit;

    public AlchemyVanillaBypassListener(JavaPlugin plugin, AlchemyAuditLog audit) {
        this.pdc = new PaperPotionPdcContract(plugin);
        this.audit = audit;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (!containsCustomPotion(event.getContents())) return;
        event.setCancelled(true);
        audit.blocked("vanilla-brew", "custom potion in brewing inventory");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        if (event.getSource().getType() != InventoryType.BREWING
                && event.getDestination().getType() != InventoryType.BREWING) return;
        if (isCustomPotion(event.getItem()) || containsCustomPotion(event.getSource())
                || containsCustomPotion(event.getDestination())) {
            event.setCancelled(true);
            audit.blocked("vanilla-hopper", "custom potion movement");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.BREWING) return;
        if (containsCustomPotion(event.getView().getTopInventory())
                || isCustomPotion(event.getCurrentItem()) || isCustomPotion(event.getCursor())) {
            event.setCancelled(true);
            audit.blocked("vanilla-brew-click", "custom potion inventory interaction");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.BREWING) return;
        if (containsCustomPotion(event.getView().getTopInventory()) || isCustomPotion(event.getOldCursor())) {
            event.setCancelled(true);
            audit.blocked("vanilla-brew-drag", "custom potion inventory interaction");
        }
    }

    private boolean containsCustomPotion(Inventory inventory) {
        if (inventory == null) return false;
        for (ItemStack item : inventory.getContents()) if (isCustomPotion(item)) return true;
        return false;
    }

    private boolean isCustomPotion(ItemStack item) {
        return item != null && !item.getType().isAir() && !pdc.readPotionId(item).isBlank();
    }
}
