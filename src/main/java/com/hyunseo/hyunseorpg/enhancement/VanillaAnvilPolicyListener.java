package com.hyunseo.hyunseorpg.enhancement;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.view.AnvilView;

/** Server-wide anvil policy: retain the computed XP price but never hide output at level 40. */
public final class VanillaAnvilPolicyListener implements Listener {
    public static final int MAXIMUM_REPAIR_COST = Integer.MAX_VALUE;

    /**
     * InventoryOpenEvent fires after AnvilMenu construction but before the player can insert the
     * first inputs. Setting the menu field here therefore precedes its first meaningful result
     * calculation; PrepareAnvilEvent alone is too late because vanilla may already have emptied it.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void initializePolicy(InventoryOpenEvent event) {
        if (event.getView() instanceof AnvilView anvil) apply(anvil);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void removeTooExpensiveThreshold(PrepareAnvilEvent event) {
        apply(event.getView());
    }

    private void apply(AnvilView anvil) {
        anvil.setMaximumRepairCost(MAXIMUM_REPAIR_COST);
    }
}
