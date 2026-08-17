package com.hyunseo.hyunseorpg.special;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class SpecialEquipmentMenuHolder implements InventoryHolder {
    private final int page;
    private Inventory inventory;

    public SpecialEquipmentMenuHolder(int page) {
        this.page = page;
    }

    public int page() { return page; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
