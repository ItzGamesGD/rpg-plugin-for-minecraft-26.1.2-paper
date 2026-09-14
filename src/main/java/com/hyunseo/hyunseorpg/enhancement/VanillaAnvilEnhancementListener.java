package com.hyunseo.hyunseorpg.enhancement;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

/** Adds exactly one recipe to Minecraft's anvil; every other result remains vanilla-owned. */
public final class VanillaAnvilEnhancementListener implements Listener {
    static final int EQUIPMENT_SLOT = 0;
    static final int MATERIAL_SLOT = 1;
    static final int STONE_COST = 1;

    private final EquipmentEnhancementService enhancements;

    public VanillaAnvilEnhancementListener(EquipmentEnhancementService enhancements) {
        this.enhancements = enhancements;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepare(PrepareAnvilEvent event) {
        ItemStack equipment = event.getInventory().getItem(EQUIPMENT_SLOT);
        ItemStack material = event.getInventory().getItem(MATERIAL_SLOT);
        if (!enhancements.isRequiredStone(material)) return;

        // A recognized stone makes this our recipe. Invalid/special/capped inputs must not expose
        // a stale vanilla or plugin result.
        var next = enhancements.getNextLevel(equipment);
        if (next.isEmpty() || material.getAmount() < STONE_COST) {
            event.setResult(null);
            return;
        }

        ItemStack result = equipment.clone();
        enhancements.applySuccessfulEnhancement(result, next.get());
        event.getView().setRepairItemCountCost(STONE_COST);
        event.getView().setRepairCost(enhancements.getXpLevelCost(next.get().level()));
        event.setResult(result);
    }
}
