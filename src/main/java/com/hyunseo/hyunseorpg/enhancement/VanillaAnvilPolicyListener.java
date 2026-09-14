package com.hyunseo.hyunseorpg.enhancement;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;

/** Server-wide anvil policy: retain the computed XP price but never hide output at level 40. */
public final class VanillaAnvilPolicyListener implements Listener {
    public static final int MAXIMUM_REPAIR_COST = Integer.MAX_VALUE;

    @EventHandler(priority = EventPriority.LOWEST)
    public void removeTooExpensiveThreshold(PrepareAnvilEvent event) {
        event.getView().setMaximumRepairCost(MAXIMUM_REPAIR_COST);
    }
}
