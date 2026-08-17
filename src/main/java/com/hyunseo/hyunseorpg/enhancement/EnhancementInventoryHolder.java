package com.hyunseo.hyunseorpg.enhancement;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class EnhancementInventoryHolder implements InventoryHolder {
    private Inventory inventory;
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
